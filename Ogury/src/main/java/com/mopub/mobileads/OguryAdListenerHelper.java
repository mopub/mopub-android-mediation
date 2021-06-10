package com.mopub.mobileads;

import androidx.annotation.NonNull;

import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.ogury.core.OguryError;
import com.ogury.ed.OguryAdFormatErrorCode;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.DID_DISAPPEAR;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;

public class OguryAdListenerHelper {
    private final String mAdapterName;
    private final String mAdUnitId;

    private AdLifecycleListener.LoadListener mLoadListener;
    private AdLifecycleListener.InteractionListener mInteractionListener;

    public OguryAdListenerHelper(@NonNull String adapterName, @NonNull String adUnitId) {
        Preconditions.checkNotNull(adapterName);
        Preconditions.checkNotNull(adUnitId);

        mAdapterName = adapterName;
        mAdUnitId = adUnitId;
    }

    public void setLoadListener(AdLifecycleListener.LoadListener loadListener) {
        mLoadListener = loadListener;
    }

    public void setInteractionListener(AdLifecycleListener.InteractionListener interactionListener) {
        mInteractionListener = interactionListener;
    }

    public void onAdLoaded() {
        MoPubLog.log(mAdUnitId, LOAD_SUCCESS, mAdapterName);

        if (mLoadListener != null) {
            mLoadListener.onAdLoaded();
        }
    }

    public void onAdError(OguryError error) {
        final MoPubErrorCode errorCode = getMoPubErrorCodeForError(error);

        MoPubLog.log(mAdUnitId, CUSTOM, mAdapterName, "Ad failed to show/load with error code "
                + errorCode);

        if (mInteractionListener != null) {
            MoPubLog.log(mAdUnitId, SHOW_FAILED, mAdapterName, errorCode.getIntCode(), errorCode);

            mInteractionListener.onAdFailed(errorCode);
        } else if (mLoadListener != null) {
            MoPubLog.log(mAdUnitId, LOAD_FAILED, mAdapterName, errorCode.getIntCode(), errorCode);

            mLoadListener.onAdLoadFailed(errorCode);
        }
    }

    private MoPubErrorCode getMoPubErrorCodeForError(OguryError error) {
        switch (error.getErrorCode()) {
            case OguryAdFormatErrorCode.NO_INTERNET_CONNECTION:
                return MoPubErrorCode.NO_CONNECTION;
            case OguryAdFormatErrorCode.LOAD_FAILED:
            case OguryAdFormatErrorCode.AD_DISABLED:
            case OguryAdFormatErrorCode.PROFIG_NOT_SYNCED:
            case OguryAdFormatErrorCode.AD_NOT_AVAILABLE:
                return MoPubErrorCode.NETWORK_NO_FILL;
            case OguryAdFormatErrorCode.AD_EXPIRED:
                return MoPubErrorCode.EXPIRED;
            case OguryAdFormatErrorCode.SDK_INIT_NOT_CALLED:
            case OguryAdFormatErrorCode.SDK_INIT_FAILED:
            case OguryAdFormatErrorCode.ACTIVITY_IN_BACKGROUND:
                return MoPubErrorCode.NETWORK_INVALID_STATE;
            case OguryAdFormatErrorCode.ANOTHER_AD_ALREADY_DISPLAYED:
            case OguryAdFormatErrorCode.AD_NOT_LOADED:
            case OguryAdFormatErrorCode.SHOW_FAILED:
                return MoPubErrorCode.AD_SHOW_ERROR;
            default:
                return MoPubErrorCode.UNSPECIFIED;
        }
    }

    public void onAdDisplayed() {
        MoPubLog.log(mAdUnitId, SHOW_SUCCESS, mAdapterName);

        if (mInteractionListener != null) {
            mInteractionListener.onAdShown();
        }
    }

    public void onAdImpression() {
        if (mInteractionListener != null) {
            mInteractionListener.onAdImpression();
        }
    }

    public void onAdClicked() {
        MoPubLog.log(mAdUnitId, CLICKED, mAdapterName);

        if (mInteractionListener != null) {
            mInteractionListener.onAdClicked();
        }
    }

    public void onAdClosed() {
        MoPubLog.log(mAdUnitId, DID_DISAPPEAR, mAdapterName);

        if (mInteractionListener != null) {
            mInteractionListener.onAdDismissed();
        }
    }
}

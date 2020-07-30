package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdListener;
import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;
import com.facebook.ads.AudienceNetworkAds;
import com.mopub.common.DataKeys;
import com.mopub.common.LifecycleListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Views;

import java.util.Map;

import static com.facebook.ads.AdError.BROKEN_MEDIA_ERROR_CODE;
import static com.facebook.ads.AdError.CACHE_ERROR_CODE;
import static com.facebook.ads.AdError.INTERNAL_ERROR_CODE;
import static com.facebook.ads.AdError.LOAD_TOO_FREQUENTLY_ERROR_CODE;
import static com.facebook.ads.AdError.MEDIATION_ERROR_CODE;
import static com.facebook.ads.AdError.NETWORK_ERROR_CODE;
import static com.facebook.ads.AdError.NO_FILL_ERROR_CODE;
import static com.facebook.ads.AdError.SERVER_ERROR_CODE;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.mobileads.MoPubErrorCode.CANCELLED;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_NO_FILL;
import static com.mopub.mobileads.MoPubErrorCode.NO_CONNECTION;
import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;
import static com.mopub.mobileads.MoPubErrorCode.VIDEO_CACHE_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.VIDEO_PLAYBACK_ERROR;

public class FacebookBanner extends BaseAd implements AdListener {
    private static final String PLACEMENT_ID_KEY = "placement_id";
    private static final String ADAPTER_NAME = FacebookBanner.class.getSimpleName();

    private AdView mFacebookBanner;
    @NonNull
    private FacebookAdapterConfiguration mFacebookAdapterConfiguration;
    private String mPlacementId;

    public FacebookBanner() {
        mFacebookAdapterConfiguration = new FacebookAdapterConfiguration();
    }

    @Override
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        if (!AudienceNetworkAds.isInitialized(context)) {
            AudienceNetworkAds.initialize(context);
        }

        setAutomaticImpressionAndClickTracking(false);

        final Map<String, String> extras = adData.getExtras();
        if (extrasAreValid(extras)) {
            mPlacementId = extras.get(PLACEMENT_ID_KEY);
            mFacebookAdapterConfiguration.setCachedInitializationParameters(context, extras);
        } else {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(NETWORK_NO_FILL);
            }
            return;
        }

        final AdSize adSize = calculateAdSize(adData.getAdHeight() == null ? 0 : adData.getAdHeight());

        mFacebookBanner = new AdView(context, mPlacementId, adSize);

        AdView.AdViewLoadConfigBuilder bannerConfigBuilder = mFacebookBanner.buildLoadAdConfig()
                .withAdListener(this);

        final String adMarkup = extras.get(DataKeys.ADM_KEY);
        if (!TextUtils.isEmpty(adMarkup)) {
            mFacebookBanner.loadAd(bannerConfigBuilder.withBid(adMarkup).build());
            MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
        } else {
            mFacebookBanner.loadAd(bannerConfigBuilder.build());
            MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
        }
    }

    @Override
    public View getAdView() {
        return mFacebookBanner;
    }

    @Override
    protected void onInvalidate() {
        if (mFacebookBanner != null) {
            Views.removeFromParent(mFacebookBanner);
            mFacebookBanner.destroy();
            mFacebookBanner = null;
        }
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    /**
     * AdListener implementation
     */

    @Override
    public void onAdLoaded(Ad ad) {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Facebook banner ad loaded " +
                "successfully. Showing ad...");

        if (mLoadListener != null) {
            mLoadListener.onAdLoaded();
            MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
        }
    }

    @Override
    public void onError(final Ad ad, final AdError error) {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Facebook banner ad failed " +
                "to load.");
        MoPubErrorCode errorCode;

        switch (error.getErrorCode()) {
            case NO_FILL_ERROR_CODE:
                errorCode = NETWORK_NO_FILL;
                break;
            case INTERNAL_ERROR_CODE:
                errorCode = MoPubErrorCode.INTERNAL_ERROR;
                break;
            case NETWORK_ERROR_CODE:
                errorCode = NO_CONNECTION;
                break;
            case LOAD_TOO_FREQUENTLY_ERROR_CODE:
                errorCode = CANCELLED;
                break;
            case SERVER_ERROR_CODE:
                errorCode = MoPubErrorCode.SERVER_ERROR;
                break;
            case CACHE_ERROR_CODE:
                errorCode = VIDEO_CACHE_ERROR;
                break;
            case MEDIATION_ERROR_CODE:
                errorCode = NETWORK_INVALID_STATE;
                break;
            case BROKEN_MEDIA_ERROR_CODE:
                errorCode = VIDEO_PLAYBACK_ERROR;
                break;
            default:
                errorCode = UNSPECIFIED;
        }

        MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, errorCode.getIntCode(), errorCode);

        if (mInteractionListener == null && mLoadListener != null) {
            mLoadListener.onAdLoadFailed(errorCode);
        } else if (mInteractionListener != null) {
            mInteractionListener.onAdFailed(errorCode);
        }
    }

    @Override
    public void onAdClicked(Ad ad) {
        if (mInteractionListener != null) {
            mInteractionListener.onAdClicked();
        }
        MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
    }

    @Override
    public void onLoggingImpression(Ad ad) {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Facebook banner ad logged " +
                "impression.");
        if (mInteractionListener != null) {
            mInteractionListener.onAdImpression();
        }
    }

    private boolean extrasAreValid(final Map<String, String> extras) {
        final String placementId = extras.get(PLACEMENT_ID_KEY);
        return (placementId != null && placementId.length() > 0);
    }

    @Nullable
    private AdSize calculateAdSize(int height) {
        if (height >= AdSize.RECTANGLE_HEIGHT_250.getHeight()) {
            return AdSize.RECTANGLE_HEIGHT_250;
        } else if (height >= AdSize.BANNER_HEIGHT_90.getHeight()) {
            return AdSize.BANNER_HEIGHT_90;
        } else {
            // Default to standard banner size
            return AdSize.BANNER_HEIGHT_50;
        }
    }

    @NonNull
    public String getAdNetworkId() {
        return mPlacementId == null ? "" : mPlacementId;
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull AdData adData) {
        return false;
    }
}

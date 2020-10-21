package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.LifecycleListener;
import com.mopub.common.logging.MoPubLog;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.mediation.IUnityAdsExtendedListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.metadata.MediationMetaData;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.unity3d.ads.UnityAds.UnityAdsError.SHOW_ERROR;

public class UnityInterstitial extends BaseAd implements IUnityAdsExtendedListener {

    private static final String ADAPTER_NAME = UnityInterstitial.class.getSimpleName();

    private Context mContext;
    private String mPlacementId = "video";
    private int impressionOrdinal;
    private int missedImpressionOrdinal;
    @NonNull
    private UnityAdsAdapterConfiguration mUnityAdsAdapterConfiguration;

    public UnityInterstitial() {
        mUnityAdsAdapterConfiguration = new UnityAdsAdapterConfiguration();
    }

    /**
     * IUnityAdsLoadListener instance.
     */
    private IUnityAdsLoadListener mUnityLoadListener = new IUnityAdsLoadListener() {
        @Override
        public void onUnityAdsAdLoaded(String placementId) {
            if (mLoadListener != null) {
                mLoadListener.onAdLoaded();
            }
            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
        }

        @Override
        public void onUnityAdsFailedToLoad(String placementId) {
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity interstitial failed to load for placement " + placementId);
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
        }
    };

    @Override
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {

        final Map<String, String> extras = adData.getExtras();
        mPlacementId = UnityRouter.placementIdForServerExtras(extras, mPlacementId);
        mContext = context;

        setAutomaticImpressionAndClickTracking(false);

        if (!UnityAds.isInitialized()) {
            UnityRouter.initUnityAds(extras, context, null);
        }
        UnityAds.load(mPlacementId, mUnityLoadListener);
        mUnityAdsAdapterConfiguration.setCachedInitializationParameters(context, extras);
    }

    @Override
    protected void show() {
        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);

        if (UnityAds.isReady(mPlacementId) && mContext != null) {
            MediationMetaData metadata = new MediationMetaData(mContext);
            metadata.setOrdinal(++impressionOrdinal);
            metadata.commit();

            UnityAds.addListener(UnityInterstitial.this);

            UnityAds.show((Activity) mContext, mPlacementId);
        } else {
            // lets Unity Ads know when ads fail to show
            MediationMetaData metadata = new MediationMetaData(mContext);
            metadata.setMissedImpressionOrdinal(++missedImpressionOrdinal);
            metadata.commit();

            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Attempted to show Unity interstitial video before it was available.");
        }
    }

    @Override
    protected void onInvalidate() {
        UnityAds.removeListener(UnityInterstitial.this);
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return mPlacementId != null ? mPlacementId : "";
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull final Activity activity, @NonNull final AdData adData) {
        return false;
    }

    @Override
    public void onUnityAdsReady(String placementId) {
    }

    @Override
    public void onUnityAdsStart(String placementId) {
        if (mInteractionListener != null) {
            mInteractionListener.onAdShown();
            mInteractionListener.onAdImpression();
        }

        MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);
    }

    @Override
    public void onUnityAdsFinish(String placementId, UnityAds.FinishState finishState) {
        if (mInteractionListener != null) {
            if (finishState == UnityAds.FinishState.ERROR) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity interstitial video encountered a playback error for " +
                        "placement " + placementId);
                mInteractionListener.onAdFailed(MoPubErrorCode.VIDEO_PLAYBACK_ERROR);

                MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.VIDEO_PLAYBACK_ERROR.getIntCode(),
                        MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
            } else {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity interstitial video completed for placement " + placementId);
                mInteractionListener.onAdDismissed();
            }
        }
        UnityAds.removeListener(UnityInterstitial.this);
    }

    @Override
    public void onUnityAdsClick(String placementId) {
        if (mInteractionListener != null) {
            mInteractionListener.onAdClicked();
        }
        MoPubLog.log(CLICKED, ADAPTER_NAME);
    }

    public void onUnityAdsPlacementStateChanged(String placementId, UnityAds.PlacementState oldState, UnityAds.PlacementState newState) {
    }

    @Override
    public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String message) {
        if (unityAdsError == SHOW_ERROR) {
            UnityAds.removeListener(UnityInterstitial.this);
        }
    }
}

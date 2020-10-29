package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.LifecycleListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.mediation.IUnityAdsExtendedListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.metadata.MediationMetaData;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
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

    @Override
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        final Map<String, String> extras = adData.getExtras();
        mPlacementId = UnityRouter.placementIdForServerExtras(extras, mPlacementId);
        mContext = context;

        setAutomaticImpressionAndClickTracking(false);

        if (!UnityAds.isInitialized()) {
            UnityRouter.initUnityAds(extras, context, new IUnityAdsInitializationListener() {
                @Override
                public void onInitializationComplete() {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity Ads successfully initialized.");
                }

                @Override
                public void onInitializationFailed(UnityAds.UnityAdsInitializationError unityAdsInitializationError, String errorMessage) {
                    if (errorMessage != null) {
                        MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity Ads failed to initialize initialize with message: " + errorMessage);
                    }
                }
            });

            if (mLoadListener != null) {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME,
                        "Unity Ads adapter failed to request interstitial ad, Unity Ads is not initialized yet. " +
                                "Failing this ad request and calling Unity Ads initialization, " +
                                "so it would be available for an upcoming ad request");
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return;
        }

        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);

        UnityAds.load(mPlacementId, mUnityLoadListener);
        mUnityAdsAdapterConfiguration.setCachedInitializationParameters(context, extras);
    }

    /**
     * IUnityAdsLoadListener instance. Contains ad load success and fail logic.
     */
    private IUnityAdsLoadListener mUnityLoadListener = new IUnityAdsLoadListener() {
        @Override
        public void onUnityAdsAdLoaded(String placementId) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity interstitial successfully loaded for placementId " + placementId);
            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);

            if (mLoadListener != null) {
                mLoadListener.onAdLoaded();
            }
        }

        @Override
        public void onUnityAdsFailedToLoad(String placementId) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity interstitial failed to load for placement " + placementId);
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);

            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
        }
    };

    @Override
    protected void show() {
        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);

        if (mContext == null || !(mContext instanceof Activity)) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Failed to show Unity interstitial as the context calling it " +
                    "is null, or is not an Activity");
            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            if (mInteractionListener != null) {
                mInteractionListener.onAdFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return;
        }

        if (UnityAds.isReady(mPlacementId)) {
            // Lets Unity Ads know when ads succeeds to show
            MediationMetaData metadata = new MediationMetaData(mContext);
            metadata.setOrdinal(++impressionOrdinal);
            metadata.commit();

            UnityAds.addListener(UnityInterstitial.this);

            UnityAds.show((Activity) mContext, mPlacementId);
        } else {
            // Lets Unity Ads know when ads fail to show
            MediationMetaData metadata = new MediationMetaData(mContext);
            metadata.setMissedImpressionOrdinal(++missedImpressionOrdinal);
            metadata.commit();

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Attempted to show Unity interstitial before it was available.");
            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            if (mInteractionListener != null) {
                mInteractionListener.onAdFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
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
        // no-op
    }

    @Override
    public void onUnityAdsStart(String placementId) {
        MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);

        if (mInteractionListener != null) {
            mInteractionListener.onAdShown();
            mInteractionListener.onAdImpression();
        }
    }

    @Override
    public void onUnityAdsFinish(String placementId, UnityAds.FinishState finishState) {
        if (mInteractionListener != null) {
            if (finishState == UnityAds.FinishState.ERROR) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity interstitial video encountered a playback error for " +
                        "placement " + placementId);
                MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.VIDEO_PLAYBACK_ERROR.getIntCode(),
                        MoPubErrorCode.VIDEO_PLAYBACK_ERROR);

                mInteractionListener.onAdFailed(MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
            } else {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity interstitial video completed for placement " + placementId);
                mInteractionListener.onAdDismissed();
            }
        }
        UnityAds.removeListener(UnityInterstitial.this);
    }

    @Override
    public void onUnityAdsClick(String placementId) {
        MoPubLog.log(CLICKED, ADAPTER_NAME);

        if (mInteractionListener != null) {
            mInteractionListener.onAdClicked();
        }
    }

    public void onUnityAdsPlacementStateChanged(String placementId, UnityAds.PlacementState oldState, UnityAds.PlacementState newState) {
    }

    @Override
    public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String message) {
        if (unityAdsError == SHOW_ERROR) {
            if (mContext != null) {
                // Lets Unity Ads know when ads fail to show
                MediationMetaData metadata = new MediationMetaData(mContext);
                metadata.setMissedImpressionOrdinal(++missedImpressionOrdinal);
                metadata.commit();
            }

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Failed to show Unity interstitial with error message: " + message);
            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            if (mInteractionListener != null) {
                mInteractionListener.onAdFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }

            UnityAds.removeListener(UnityInterstitial.this);

        } else if (mLoadListener != null) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity interstitial failed with error message: " + message);
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.UNSPECIFIED.getIntCode(),
                    MoPubErrorCode.UNSPECIFIED);
            mLoadListener.onAdLoadFailed(MoPubErrorCode.UNSPECIFIED);
        }
    }
}

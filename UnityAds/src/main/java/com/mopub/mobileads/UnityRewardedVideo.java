package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.BaseLifecycleListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubReward;
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
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOULD_REWARD;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.unity3d.ads.UnityAds.UnityAdsError.SHOW_ERROR;

public class UnityRewardedVideo extends BaseAd implements IUnityAdsExtendedListener {
    private static final LifecycleListener sLifecycleListener = new UnityLifecycleListener();
    private static final String ADAPTER_NAME = UnityRewardedVideo.class.getSimpleName();

    @NonNull
    private String mPlacementId = "rewardedVideo";

    @NonNull
    private UnityAdsAdapterConfiguration mUnityAdsAdapterConfiguration;

    @Nullable
    private Activity mLauncherActivity;

    private int impressionOrdinal;
    private int missedImpressionOrdinal;

    @Override
    @NonNull
    public LifecycleListener getLifecycleListener() {
        return sLifecycleListener;
    }

    @Override
    @NonNull
    public String getAdNetworkId() {
        return mPlacementId;
    }

    public UnityRewardedVideo() {
        mUnityAdsAdapterConfiguration = new UnityAdsAdapterConfiguration();
    }

    @Override
    public boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
                                         @NonNull final AdData adData) {
        Preconditions.checkNotNull(launcherActivity);
        Preconditions.checkNotNull(adData);

        mLauncherActivity = launcherActivity;

        final Map<String, String> extras = adData.getExtras();
        mPlacementId = UnityRouter.placementIdForServerExtras(extras, mPlacementId);

        if (UnityAds.isInitialized()) {
            return true;
        }

        mUnityAdsAdapterConfiguration.setCachedInitializationParameters(launcherActivity, extras);

        if (UnityAds.isInitialized()) {
            return true;
        } else {
            UnityRouter.initUnityAds(extras, launcherActivity, new IUnityAdsInitializationListener() {
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
                        "Unity Ads adapter failed to request rewarded video ad, Unity Ads is not initialized yet. " +
                                "Failing this ad request and calling Unity Ads initialization, " +
                                "so it would be available for an upcoming ad request");
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return false;
        }
    }

    @Override
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        mPlacementId = UnityRouter.placementIdForServerExtras(adData.getExtras(), mPlacementId);
        setAutomaticImpressionAndClickTracking(false);

        UnityAds.load(mPlacementId, mUnityLoadListener);
    }

    /**
     * IUnityAdsLoadListener instance. Contains ad load success and fail logic.
     */
    private IUnityAdsLoadListener mUnityLoadListener = new IUnityAdsLoadListener() {
        @Override
        public void onUnityAdsAdLoaded(String placementId) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity rewarded video successfully loaded for placementId " + placementId);
            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);

            if (mLoadListener != null) {
                mLoadListener.onAdLoaded();
            }
        }

        @Override
        public void onUnityAdsFailedToLoad(String placementId) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity rewarded video failed to load for placement " + placementId);
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);

            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
        }
    };

    @Override
    public void show() {
        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);

        if (mLauncherActivity == null) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Failed to show Unity rewarded video as the activity calling it is null.");
            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.VIDEO_PLAYBACK_ERROR.getIntCode(),
                    MoPubErrorCode.VIDEO_PLAYBACK_ERROR);

            if (mInteractionListener != null) {
                mInteractionListener.onAdFailed(MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
            }
            return;
        }

        if (UnityAds.isReady(mPlacementId)) {
            // Lets Unity Ads know when ads succeeds to show
            MediationMetaData metadata = new MediationMetaData(mLauncherActivity);
            metadata.setOrdinal(++impressionOrdinal);
            metadata.commit();

            UnityAds.addListener(UnityRewardedVideo.this);

            UnityAds.show(mLauncherActivity, mPlacementId);
        } else {
            // Lets Unity Ads know when ads fail to show
            MediationMetaData metadata = new MediationMetaData(mLauncherActivity);
            metadata.setMissedImpressionOrdinal(++missedImpressionOrdinal);
            metadata.commit();

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Attempted to show Unity rewarded video before it was available.");
            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.VIDEO_PLAYBACK_ERROR.getIntCode(),
                    MoPubErrorCode.VIDEO_PLAYBACK_ERROR);

            if (mInteractionListener != null) {
                mInteractionListener.onAdFailed(MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
            }
        }
    }

    @Override
    protected void onInvalidate() {
        UnityAds.removeListener(UnityRewardedVideo.this);
    }

    @Override
    public void onUnityAdsClick(String placementId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity rewarded video clicked for placement " +
                placementId + ".");
        MoPubLog.log(CLICKED, ADAPTER_NAME);

        if (mInteractionListener != null) {
            mInteractionListener.onAdClicked();
        }
    }

    @Override
    public void onUnityAdsPlacementStateChanged(String placementId, UnityAds.PlacementState oldState, UnityAds.PlacementState newState) {
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
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity rewarded video started for placement " +
                mPlacementId + ".");

        MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);
    }

    @Override
    public void onUnityAdsFinish(String placementId, UnityAds.FinishState finishState) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity Ad finished with finish state = " + finishState);

        if (finishState == UnityAds.FinishState.ERROR) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity rewarded video encountered a playback error for " +
                    "placement " + placementId);
            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.VIDEO_PLAYBACK_ERROR.getIntCode(),
                    MoPubErrorCode.VIDEO_PLAYBACK_ERROR);

            if (mInteractionListener != null) {
                mInteractionListener.onAdFailed(MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
            }

        } else if (finishState == UnityAds.FinishState.COMPLETED) {
            MoPubLog.log(SHOULD_REWARD, ADAPTER_NAME, MoPubReward.NO_REWARD_AMOUNT, MoPubReward.NO_REWARD_LABEL);

            if (mInteractionListener != null) {
                mInteractionListener.onAdComplete(MoPubReward.success(MoPubReward.NO_REWARD_LABEL,
                        MoPubReward.DEFAULT_REWARD_AMOUNT));
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity rewarded video completed for placement " +
                        placementId);
            }

        } else if (finishState == UnityAds.FinishState.SKIPPED) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity ad was skipped, no reward will be given.");
        }

        if (mInteractionListener != null) {
            mInteractionListener.onAdDismissed();
        }
        UnityAds.removeListener(UnityRewardedVideo.this);
    }

    @Override
    public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String message) {
        if (unityAdsError == SHOW_ERROR) {
            if (mLauncherActivity != null) {
                // Lets Unity Ads know when ads fail to show
                MediationMetaData metadata = new MediationMetaData(mLauncherActivity);
                metadata.setMissedImpressionOrdinal(++missedImpressionOrdinal);
                metadata.commit();
            }

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Failed to show Unity rewarded video with error message: " + message);
            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.VIDEO_PLAYBACK_ERROR.getIntCode(),
                    MoPubErrorCode.VIDEO_PLAYBACK_ERROR);

            if (mInteractionListener != null) {
                mInteractionListener.onAdFailed(MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
            }

            UnityAds.removeListener(UnityRewardedVideo.this);

        } else if (mLoadListener != null) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity rewarded video failed with error message: " + message);
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.UNSPECIFIED.getIntCode(),
                    MoPubErrorCode.UNSPECIFIED);
            mLoadListener.onAdLoadFailed(MoPubErrorCode.UNSPECIFIED);
        }
    }

    private static final class UnityLifecycleListener extends BaseLifecycleListener {
        @Override
        public void onCreate(@NonNull final Activity activity) {
            super.onCreate(activity);
        }

        @Override
        public void onResume(@NonNull final Activity activity) {
            super.onResume(activity);
        }
    }
}

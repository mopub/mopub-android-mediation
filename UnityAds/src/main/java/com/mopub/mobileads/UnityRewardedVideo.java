package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.BaseLifecycleListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubReward;
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

    /**
     * IUnityAdsLoadListener instance.
     */
    private IUnityAdsLoadListener mUnityLoadListener = new IUnityAdsLoadListener() {
        @Override
        public void onUnityAdsAdLoaded(String placementId) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity rewarded video cached for placement " + placementId + ".");
            if (mLoadListener != null) {
                mLoadListener.onAdLoaded();
            }
            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
        }

        @Override
        public void onUnityAdsFailedToLoad(String placementId) {
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.NO_FILL);
            }
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity rewarded video cache failed for placement " + mPlacementId + ".");
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
        }
    };

    @Override
    public boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
                                         @NonNull final AdData adData) {
        mLauncherActivity = launcherActivity;

        final Map<String, String> extras = adData.getExtras();
        mPlacementId = UnityRouter.placementIdForServerExtras(extras, mPlacementId);
        if (UnityAds.isInitialized()) {
            return false;
        }

        mUnityAdsAdapterConfiguration.setCachedInitializationParameters(launcherActivity, extras);

        UnityRouter.initUnityAds(extras, launcherActivity, new IUnityAdsInitializationListener() {
            @Override
            public void onInitializationComplete() {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity Ads successfully initialized.");
            }

            @Override
            public void onInitializationFailed(UnityAds.UnityAdsInitializationError unityAdsInitializationError, String errorMessage) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, errorMessage);
            }
        });

        return true;
    }

    @Override
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {

        mPlacementId = UnityRouter.placementIdForServerExtras(adData.getExtras(), mPlacementId);

        final Map<String, String> extras = adData.getExtras();

        setAutomaticImpressionAndClickTracking(false);
        if (!UnityAds.isInitialized()) {
            UnityRouter.initUnityAds(extras, context, null);
        }
        UnityAds.load(mPlacementId, mUnityLoadListener);
    }

    @Override
    public void show() {
        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);

        if (UnityAds.isReady(mPlacementId) && mLauncherActivity != null) {
            MediationMetaData metadata = new MediationMetaData(mLauncherActivity);
            metadata.setOrdinal(++impressionOrdinal);
            metadata.commit();

            UnityAds.addListener(UnityRewardedVideo.this);

            UnityAds.show(mLauncherActivity, mPlacementId);
            return;
        }

        if (mLauncherActivity != null) {
            // lets Unity Ads know when ads fail to show
            MediationMetaData metadata = new MediationMetaData(mLauncherActivity);
            metadata.setMissedImpressionOrdinal(++missedImpressionOrdinal);
            metadata.commit();
        }

        MoPubLog.log(CUSTOM, ADAPTER_NAME, "Attempted to show Unity rewarded video before it was " +
                "available.");
        if (mInteractionListener != null) {
            mInteractionListener.onAdFailed(MoPubErrorCode.NETWORK_NO_FILL);
        }
        MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                MoPubErrorCode.NETWORK_NO_FILL);
    }

    @Override
    protected void onInvalidate() {
        UnityAds.removeListener(UnityRewardedVideo.this);
    }

    @Override
    public void onUnityAdsClick(String placementId) {
        if (mInteractionListener != null) {
            mInteractionListener.onAdClicked();
        }

        MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity rewarded video clicked for placement " +
                placementId + ".");
        MoPubLog.log(CLICKED, ADAPTER_NAME);
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
           if (mInteractionListener != null) {
               mInteractionListener.onAdFailed(MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
           }

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity rewarded video encountered a playback error for " +
                    "placement " + placementId);

            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.VIDEO_PLAYBACK_ERROR.getIntCode(),
                    MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
        } else if (finishState == UnityAds.FinishState.COMPLETED) {
            MoPubLog.log(SHOULD_REWARD, ADAPTER_NAME, MoPubReward.NO_REWARD_AMOUNT, MoPubReward.NO_REWARD_LABEL);

            if (mInteractionListener != null) {
                mInteractionListener.onAdComplete(MoPubReward.success(MoPubReward.NO_REWARD_LABEL,
                        MoPubReward.DEFAULT_REWARD_AMOUNT));
            }

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity rewarded video completed for placement " +
                    placementId);
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
            UnityAds.removeListener(UnityRewardedVideo.this);
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

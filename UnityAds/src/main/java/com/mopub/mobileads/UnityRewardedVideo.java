package com.mopub.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.BaseLifecycleListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubReward;
import com.mopub.common.logging.MoPubLog;
import com.unity3d.ads.mediation.IUnityAdsExtendedListener;
import com.unity3d.ads.UnityAds;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOULD_REWARD;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;

public class UnityRewardedVideo extends CustomEventRewardedVideo {
    private static final LifecycleListener sLifecycleListener = new UnityLifecycleListener();
    private static final UnityAdsListener sUnityAdsListener = new UnityAdsListener();
    private static final String ADAPTER_NAME = UnityRewardedVideo.class.getSimpleName();

    @NonNull
    private static String sPlacementId = "";
    @NonNull
    private UnityAdsAdapterConfiguration mUnityAdsAdapterConfiguration;

    @Nullable
    private Activity mLauncherActivity;

    @Override
    @NonNull
    public CustomEventRewardedVideoListener getVideoListenerForSdk() {
        return sUnityAdsListener;
    }

    @Override
    @NonNull
    public LifecycleListener getLifecycleListener() {
        return sLifecycleListener;
    }

    @Override
    @NonNull
    public String getAdNetworkId() {
        return sPlacementId;
    }

    public UnityRewardedVideo() {
        mUnityAdsAdapterConfiguration = new UnityAdsAdapterConfiguration();
    }

    @Override
    public boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
                                         @NonNull final Map<String, Object> localExtras,
                                         @NonNull final Map<String, String> serverExtras) throws Exception {
        synchronized (UnityRewardedVideo.class) {
            sPlacementId = UnityRouter.placementIdForServerExtras(serverExtras, sPlacementId);
            if (UnityAds.isInitialized()) {
                return false;
            }

            mUnityAdsAdapterConfiguration.setCachedInitializationParameters(launcherActivity, serverExtras);

            UnityRouter.getInterstitialRouter().setCurrentPlacementId(sPlacementId);
            if (UnityRouter.initUnityAds(serverExtras, launcherActivity)) {
                UnityRouter.getInterstitialRouter().addListener(sPlacementId, sUnityAdsListener);
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity,
                                          @NonNull Map<String, Object> localExtras,
                                          @NonNull Map<String, String> serverExtras) throws Exception {

        sPlacementId = UnityRouter.placementIdForServerExtras(serverExtras, sPlacementId);
        mLauncherActivity = activity;

        UnityRouter.getInterstitialRouter().addListener(sPlacementId, sUnityAdsListener);

        if (hasVideoAvailable()) {
            MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(UnityRewardedVideo.class, sPlacementId);

            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
        } else if (UnityAds.getPlacementState(sPlacementId) == UnityAds.PlacementState.NO_FILL) {
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(UnityRewardedVideo.class, sPlacementId, MoPubErrorCode.NETWORK_NO_FILL);
            UnityRouter.getInterstitialRouter().removeListener(sPlacementId);
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    @Override
    public boolean hasVideoAvailable() {
        return UnityAds.isReady(sPlacementId);
    }

    @Override
    public void showVideo() {
        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);

        if (hasVideoAvailable()) {
            UnityAds.show(mLauncherActivity, sPlacementId);
        } else {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Attempted to show Unity rewarded video before it was " +
                    "available.");

            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    @Override
    protected void onInvalidate() {
        UnityRouter.getInterstitialRouter().removeListener(sPlacementId);
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

    private static class UnityAdsListener implements IUnityAdsExtendedListener,
            CustomEventRewardedVideoListener {
        @Override
        public void onUnityAdsReady(String placementId) {
            if (placementId.equals(sPlacementId)) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity rewarded video cached for placement " +
                        placementId + ".");
                MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(UnityRewardedVideo.class, placementId);

                MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
            }
        }

        @Override
        public void onUnityAdsStart(String placementId) {
            MoPubRewardedVideoManager.onRewardedVideoStarted(UnityRewardedVideo.class, placementId);
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity rewarded video started for placement " +
                    placementId + ".");

            MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);
        }

        @Override
        public void onUnityAdsFinish(String placementId, UnityAds.FinishState finishState) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity Ad finished with finish state = " + finishState);

            if (finishState == UnityAds.FinishState.ERROR) {
                MoPubRewardedVideoManager.onRewardedVideoPlaybackError(
                        UnityRewardedVideo.class,
                        sPlacementId,
                        MoPubErrorCode.NETWORK_NO_FILL);

                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity rewarded video encountered a playback error for " +
                        "placement " + placementId);

                MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                        MoPubErrorCode.NETWORK_NO_FILL);
            } else if (finishState == UnityAds.FinishState.COMPLETED) {
                MoPubLog.log(SHOULD_REWARD, ADAPTER_NAME, MoPubReward.NO_REWARD_AMOUNT, MoPubReward.NO_REWARD_LABEL);

                MoPubRewardedVideoManager.onRewardedVideoCompleted(
                        UnityRewardedVideo.class,
                        sPlacementId,
                        MoPubReward.success(MoPubReward.NO_REWARD_LABEL, MoPubReward.NO_REWARD_AMOUNT));

                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity rewarded video completed for placement " +
                        placementId);
            } else if (finishState == UnityAds.FinishState.SKIPPED) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity ad was skipped, no reward will be given.");
            }
            MoPubRewardedVideoManager.onRewardedVideoClosed(UnityRewardedVideo.class, sPlacementId);
            UnityRouter.getInterstitialRouter().removeListener(placementId);
        }

        @Override
        public void onUnityAdsClick(String placementId) {
            MoPubRewardedVideoManager.onRewardedVideoClicked(UnityRewardedVideo.class, placementId);

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity rewarded video clicked for placement " +
                    placementId + ".");

            MoPubLog.log(CLICKED, ADAPTER_NAME);
        }

        // @Override
        public void onUnityAdsPlacementStateChanged(String placementId, UnityAds.PlacementState oldState, UnityAds.PlacementState newState) {
            if (placementId.equals(sPlacementId)) {
                if (newState == UnityAds.PlacementState.NO_FILL) {
                    MoPubRewardedVideoManager.onRewardedVideoLoadFailure(UnityRewardedVideo.class, sPlacementId, MoPubErrorCode.NETWORK_NO_FILL);

                    MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                            MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                            MoPubErrorCode.NETWORK_NO_FILL);

                    UnityRouter.getInterstitialRouter().removeListener(sPlacementId);
                }
            }
        }

        @Override
        public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String message) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity rewarded video cache failed for placement " +
                    sPlacementId + ".");
            MoPubErrorCode errorCode = UnityRouter.UnityAdsUtils.getMoPubErrorCode(unityAdsError);
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(UnityRewardedVideo.class, sPlacementId, errorCode);

            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    errorCode.getIntCode(),
                    errorCode);
        }
    }
}
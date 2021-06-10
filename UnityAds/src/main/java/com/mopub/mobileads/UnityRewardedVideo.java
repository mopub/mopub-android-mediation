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
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAds.UnityAdsLoadError;
import com.unity3d.ads.UnityAds.UnityAdsShowError;
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

public class UnityRewardedVideo extends BaseAd {
    private static final LifecycleListener sLifecycleListener = new UnityLifecycleListener();
    private static final String ADAPTER_NAME = UnityRewardedVideo.class.getSimpleName();

    private String mPlacementId;

    @NonNull
    private UnityAdsAdapterConfiguration mUnityAdsAdapterConfiguration;

    @Nullable
    private Activity mLauncherActivity;

    private int impressionOrdinal;

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

            return false;
        }
    }

    @Override
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        setAutomaticImpressionAndClickTracking(false);

        UnityAds.load(UnityRouter.placementIdForServerExtras(adData.getExtras(), ""), mUnityLoadListener);
    }

    /**
     * IUnityAdsLoadListener instance. Contains ad load success and fail logic.
     */
    private IUnityAdsLoadListener mUnityLoadListener = new IUnityAdsLoadListener() {
        @Override
        public void onUnityAdsAdLoaded(String placementId) {
            mPlacementId = placementId;

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity rewarded video successfully loaded for placementId " + placementId);
            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);

            if (mLoadListener != null) {
                mLoadListener.onAdLoaded();
            }
        }

        @Override
        public void onUnityAdsFailedToLoad(String placementId, UnityAdsLoadError error, String message) {
            mPlacementId = placementId;

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity rewarded video failed to load for placement " +
                    placementId + ", with error message: " + message);
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

        if (mPlacementId == null) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity Ads received call to show before successfully loading an ad");
        }

        MediationMetaData metadata = new MediationMetaData(mLauncherActivity);
        metadata.setOrdinal(++impressionOrdinal);
        metadata.commit();

        UnityAds.show(mLauncherActivity, mPlacementId, mUnityShowListener);
    }

    /**
     * IUnityAdsShowListener instance. Contains logic for callbacks when showing ads.
     */
    private IUnityAdsShowListener mUnityShowListener = new IUnityAdsShowListener() {

        @Override
        public void onUnityAdsShowStart(String placementId) {
            if (mInteractionListener != null) {
                mInteractionListener.onAdShown();
                mInteractionListener.onAdImpression();
            }
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity rewarded video started for placement " +
                    mPlacementId + ".");

            MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);
        }

        @Override
        public void onUnityAdsShowClick(String placementId) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity rewarded video clicked for placement " +
                    placementId + ".");
            MoPubLog.log(CLICKED, ADAPTER_NAME);

            if (mInteractionListener != null) {
                mInteractionListener.onAdClicked();
            }
        }

        @Override
        public void onUnityAdsShowComplete(String placementId, UnityAds.UnityAdsShowCompletionState state) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity Ad finished with finish state = " + state);

            if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                MoPubLog.log(SHOULD_REWARD, ADAPTER_NAME, MoPubReward.NO_REWARD_AMOUNT, MoPubReward.NO_REWARD_LABEL);

                if (mInteractionListener != null) {
                    mInteractionListener.onAdComplete(MoPubReward.success(MoPubReward.NO_REWARD_LABEL,
                            MoPubReward.DEFAULT_REWARD_AMOUNT));
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity rewarded video completed for placement " +
                            placementId);
                }

            } else if (state == UnityAds.UnityAdsShowCompletionState.SKIPPED) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity ad was skipped, no reward will be given.");
            }

            if (mInteractionListener != null) {
                mInteractionListener.onAdDismissed();
            }
        }

        @Override
        public void onUnityAdsShowFailure(String placementId, UnityAdsShowError error, String message) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME,
                    "Unity rewarded video encountered a playback error for " +
                            "placement " + placementId + ", with error message: " + message);
            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.VIDEO_PLAYBACK_ERROR.getIntCode(),
                    MoPubErrorCode.VIDEO_PLAYBACK_ERROR);

            if (mInteractionListener != null) {
                mInteractionListener.onAdFailed(MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
            }
        }
    };

    @Override
    protected void onInvalidate() {
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

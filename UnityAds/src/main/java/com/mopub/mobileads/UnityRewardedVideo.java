package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.BaseLifecycleListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubReward;
import com.mopub.common.logging.MoPubLog;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.mediation.IUnityAdsExtendedListener;
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

public class UnityRewardedVideo extends BaseAd implements IUnityAdsExtendedListener {
    private static final LifecycleListener sLifecycleListener = new UnityLifecycleListener();
    private static final String ADAPTER_NAME = UnityRewardedVideo.class.getSimpleName();

    @NonNull
    private String mPlacementId = "";

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
        mLauncherActivity = launcherActivity;

        synchronized (UnityRewardedVideo.class) {
            final Map<String, String> extras = adData.getExtras();
            mPlacementId = UnityRouter.placementIdForServerExtras(extras, mPlacementId);
            if (UnityAds.isInitialized()) {
                return false;
            }

            mUnityAdsAdapterConfiguration.setCachedInitializationParameters(launcherActivity, extras);

            UnityRouter.getInterstitialRouter().setCurrentPlacementId(mPlacementId);
            if (UnityRouter.initUnityAds(extras, launcherActivity)) {
                UnityRouter.getInterstitialRouter().addListener(mPlacementId, this);
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {

        mPlacementId = UnityRouter.placementIdForServerExtras(adData.getExtras(), mPlacementId);

        UnityRouter.getInterstitialRouter().addListener(mPlacementId, this);

        setAutomaticImpressionAndClickTracking(false);

        UnityAds.load(mPlacementId);
    }

    @Override
    public void show() {
        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);

        if (UnityAds.isReady(mPlacementId) && mLauncherActivity != null) {
            MediationMetaData metadata = new MediationMetaData(mLauncherActivity);
            metadata.setOrdinal(++impressionOrdinal);
            metadata.commit();

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
        UnityRouter.getInterstitialRouter().removeListener(mPlacementId);
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
        if (placementId.equals(mPlacementId)) {
            if (newState == UnityAds.PlacementState.NO_FILL) {
                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);
                }

                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                        MoPubErrorCode.NETWORK_NO_FILL);

                UnityRouter.getInterstitialRouter().removeListener(mPlacementId);
            }
        }

    }

    @Override
    public void onUnityAdsReady(String placementId) {
        if (placementId.equals(mPlacementId)) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity rewarded video cached for placement " +
                    placementId + ".");
            if (mLoadListener != null) {
                mLoadListener.onAdLoaded();
            }

            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
        }

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
                mInteractionListener.onAdFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity rewarded video encountered a playback error for " +
                    "placement " + placementId);

            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);
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
        UnityRouter.getInterstitialRouter().removeListener(placementId);
    }

    @Override
    public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String message) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity rewarded video cache failed for placement " +
                mPlacementId + "." + message);
        MoPubErrorCode errorCode = UnityRouter.UnityAdsUtils.getMoPubErrorCode(unityAdsError);
        if (mLoadListener != null) {
            mLoadListener.onAdLoadFailed(errorCode);
        }

        MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                errorCode.getIntCode(),
                errorCode);

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

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTRewardVideoAd;
import com.mopub.common.DataKeys;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubReward;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.DID_DISAPPEAR;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOULD_REWARD;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;

public class PangleAdRewardedVideo extends BaseAd {
    private static final String ADAPTER_NAME = PangleAdRewardedVideo.class.getSimpleName();

    /**
     * Flag to determine whether or not the adapter has been initialized.
     */
    private static AtomicBoolean mIsSDKInitialized;
    private static String mPlacementId;

    /**
     * Flag to determine whether or not the Pangle Rewarded Video Ad instance has loaded.
     */
    private boolean mIsLoaded;

    private PangleAdapterConfiguration mPangleAdapterConfiguration;
    private WeakReference<Activity> mWeakActivity;
    private TTRewardVideoAd mTTRewardVideoAd;


    public PangleAdRewardedVideo() {
        mIsSDKInitialized = new AtomicBoolean(false);
        mPangleAdapterConfiguration = new PangleAdapterConfiguration();
    }


    private boolean hasVideoAvailable() {
        return mTTRewardVideoAd != null && mIsLoaded;
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity activity, @NonNull final AdData adData) {
        Preconditions.checkNotNull(activity);
        Preconditions.checkNotNull(adData);

        final Map<String, String> extras = adData.getExtras();
        if (!mIsSDKInitialized.get()) {
            if (extras != null && !extras.isEmpty()) {
                final String appId = adData.getExtras().get(PangleAdapterConfiguration.KEY_EXTRA_APP_ID);

                if (TextUtils.isEmpty(appId)) {
                    if (mLoadListener != null) {
                        mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                    }
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME,
                            "Invalid Pangle app ID. Failing Pangle sdk init. " +
                                    "Ensure the ad placement ID is valid on the MoPub dashboard.");
                    return false;
                }
                PangleAdapterConfiguration.pangleSdkInit(activity, appId);
                mPangleAdapterConfiguration.setCachedInitializationParameters(activity, extras);
            }
            return true;
        }
        return false;
    }

    @Override
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        mWeakActivity = new WeakReference<>((Activity) context);
        setAutomaticImpressionAndClickTracking(false);

        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "loadWithSdkInitialized method execute ......getCodeId=" + PangleAdapterConfiguration.getPlacementId() + ",getOrientation=" + PangleAdapterConfiguration.getOrientation());
        TTAdManager adManager = PangleAdapterConfiguration.getPangleSdkManager();
        TTAdNative adInstance = adManager.createAdNative(context.getApplicationContext());

        /** obtain adunit from server by mopub */
        final Map<String, String> extras = adData.getExtras();
        mPlacementId = extras.get(PangleAdapterConfiguration.KEY_EXTRA_AD_PLACEMENT_ID);

        if (TextUtils.isEmpty(mPlacementId)) {
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME,
                    "Invalid Pangle placement ID. Failing ad request. " +
                            "Ensure the ad placement ID is valid on the MoPub dashboard.");
            return;
        }

        final String adm = extras.get(DataKeys.ADM_KEY);

        final AdSlot adSlot = new AdSlot.Builder()
                .setCodeId(getAdNetworkId())
                .setSupportDeepLink(true)
                .setImageAcceptedSize(1080, 1920)
                .setRewardName(PangleAdapterConfiguration.getRewardName())
                .setRewardAmount(PangleAdapterConfiguration.getRewardAmount())
                .setUserID(PangleAdapterConfiguration.getUserID())
                .setMediaExtra(PangleAdapterConfiguration.getMediaExtra())
                .withBid(adm)
                .build();

        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
        adInstance.loadRewardVideoAd(adSlot, mLoadRewardVideoAdListener);
    }

    @Override
    protected void show() {
        if (hasVideoAvailable() && mWeakActivity != null && mWeakActivity.get() != null) {
            mTTRewardVideoAd.setRewardAdInteractionListener(mRewardAdInteractionListener);
            mTTRewardVideoAd.showRewardVideoAd(mWeakActivity.get());
            MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
        } else {
            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Failed to show an Pangle rewarded video before one was loaded");
            if (mInteractionListener != null) {
                mInteractionListener.onAdFailed(MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
            }
        }
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return mPlacementId;
    }

    @Override
    protected void onInvalidate() {
        if (mTTRewardVideoAd != null) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Performing cleanup tasks...");
            mTTRewardVideoAd = null;
        }
    }

    private TTAdNative.RewardVideoAdListener mLoadRewardVideoAdListener = new TTAdNative.RewardVideoAdListener() {

        @Override
        public void onError(int code, String message) {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    "Loading Rewarded Video creative encountered an error: "
                            + PangleAdapterConfiguration.mapErrorCode(code).toString()
                            + ",error message:"
                            + message);
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(PangleAdapterConfiguration.mapErrorCode(code));
            }
        }

        @Override
        public void onRewardVideoAdLoad(TTRewardVideoAd ad) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onRewardVideoAdLoad method execute ......ad = " + ad);
            if (ad != null) {
                mIsLoaded = true;
                mTTRewardVideoAd = ad;

                MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
                if (mLoadListener != null) {
                    mLoadListener.onAdLoaded();
                }
            } else {
                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);
                }
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, " RewardVideoAd is null !");
            }
        }

        @Override
        public void onRewardVideoCached() {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Pangle RewardVideoAd onRewardVideoCached...");
        }
    };

    private TTRewardVideoAd.RewardAdInteractionListener mRewardAdInteractionListener = new TTRewardVideoAd.RewardAdInteractionListener() {
        @Override
        public void onAdShow() {
            MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
            if (mInteractionListener != null) {
                mInteractionListener.onAdShown();
            }
        }

        @Override
        public void onAdVideoBarClick() {
            MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
            if (mInteractionListener != null) {
                mInteractionListener.onAdClicked();
            }
        }

        @Override
        public void onAdClose() {
            MoPubLog.log(getAdNetworkId(), DID_DISAPPEAR, ADAPTER_NAME);
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Pangle RewardVideoAd onAdClose...");
            if (mInteractionListener != null) {
                mInteractionListener.onAdDismissed();
            }
        }

        @Override
        public void onVideoComplete() {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Pangle RewardVideoAd onVideoComplete...");
        }

        @Override
        public void onVideoError() {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Pangle RewardVideoAd onVideoError...");
            if (mInteractionListener != null) {
                mInteractionListener.onAdFailed(MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
            }
        }

        @Override
        public void onRewardVerify(boolean rewardVerify, int rewardAmount, String rewardName) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Pangle TTRewardVideoAd onRewardVerify...rewardVerify："
                    + rewardVerify + "，rewardAmount=" + rewardAmount + "，rewardName=" + rewardName);
            MoPubLog.log(getAdNetworkId(), SHOULD_REWARD, ADAPTER_NAME, rewardAmount, rewardName);
            if (mInteractionListener != null) {
                mInteractionListener.onAdComplete(MoPubReward.success(rewardName, rewardAmount));
            }
        }

        @Override
        public void onSkippedVideo() {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Pangle TTRewardVideoAd skipped video...");
        }
    };
}

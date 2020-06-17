package com.mopub.mobileads;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.content.Context;
import android.text.TextUtils;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTRewardVideoAd;
import com.mopub.common.DataKeys;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MediationSettings;
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

        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "loadWithSdkInitialized method execute ......getCodeId=" + PangleRewardMediationSettings.getCodeId() + ",getOrientation=" + PangleRewardMediationSettings.getOrientation());
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
                .setRewardName(PangleRewardMediationSettings.getRewardName()) /** Parameter for rewarded video ad requests, name of the reward */
                .setRewardAmount(PangleRewardMediationSettings.getRewardAmount())  /**The number of rewards in rewarded video ad */
                .setUserID(PangleRewardMediationSettings.getUserID()) /**User ID, a optional parameter for rewarded video ads */
                .setMediaExtra(PangleRewardMediationSettings.getMediaExtra()) /** optional parameter */
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
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, "Loading Rewarded Video creative encountered an error: " + mapErrorCode(code).toString() + ",error message:" + message);
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(mapErrorCode(code));
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

    /**
     * obtain extra parameters from MediationSettings
     */
    public static class PangleRewardMediationSettings implements MediationSettings {
        private static String mCodeId;
        private static int mOrientation = TTAdConstant.VERTICAL;
        private static String mRewardName;
        private static int mRewardAmount;
        private static String mMediaExtra;
        private static String mUserID;

        private PangleRewardMediationSettings() {
        }

        public static String getCodeId() {
            return mCodeId;
        }

        public static int getOrientation() {
            return mOrientation;
        }

        public static String getRewardName() {
            return mRewardName;
        }

        public static int getRewardAmount() {
            return mRewardAmount;
        }

        public static String getMediaExtra() {
            return mMediaExtra;
        }

        public static String getUserID() {
            return mUserID;
        }

        public static class Builder {
            private String mCodeId;
            private int mOrientation = TTAdConstant.VERTICAL;
            private String mRewardName;
            private int mRewardAmount;
            private String mMediaExtra;
            private String mUserID;


            public Builder setCodeId(String codeId) {
                mCodeId = codeId;
                return this;
            }

            public Builder setOrientation(int orientation) {
                mOrientation = orientation;
                return this;
            }

            public Builder setRewardName(String rewardName) {
                this.mRewardName = rewardName;
                return this;
            }

            public Builder setRewardAmount(int rewardAmount) {
                this.mRewardAmount = rewardAmount;
                return this;
            }

            public Builder setMediaExtra(String mediaExtra) {
                this.mMediaExtra = mediaExtra;
                return this;
            }

            public Builder setUserID(String userID) {
                this.mUserID = userID;
                return this;
            }

            public PangleRewardMediationSettings builder() {
                PangleRewardMediationSettings settings = new PangleRewardMediationSettings();
                PangleRewardMediationSettings.mCodeId = this.mCodeId;
                PangleRewardMediationSettings.mOrientation = this.mOrientation;
                PangleRewardMediationSettings.mRewardName = this.mRewardName;
                PangleRewardMediationSettings.mRewardAmount = this.mRewardAmount;
                PangleRewardMediationSettings.mMediaExtra = this.mMediaExtra;
                PangleRewardMediationSettings.mUserID = this.mUserID;
                return settings;
            }
        }


    }

    private static MoPubErrorCode mapErrorCode(int error) {
        switch (error) {
            case PangleAdapterConfiguration.CONTENT_TYPE:
            case PangleAdapterConfiguration.REQUEST_PB_ERROR:
                return MoPubErrorCode.NO_CONNECTION;
            case PangleAdapterConfiguration.NO_AD:
                return MoPubErrorCode.NETWORK_NO_FILL;
            case PangleAdapterConfiguration.ADSLOT_EMPTY:
            case PangleAdapterConfiguration.ADSLOT_ID_ERROR:
                return MoPubErrorCode.MISSING_AD_UNIT_ID;
            default:
                return MoPubErrorCode.UNSPECIFIED;
        }
    }
}

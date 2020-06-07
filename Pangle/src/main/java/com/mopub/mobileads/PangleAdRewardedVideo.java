package com.mopub.mobileads;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import com.mopub.common.logging.MoPubLog;


import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;

public class PangleAdRewardedVideo extends CustomEventRewardedVideo {
    private static final String ADAPTER_NAME = PangleAdRewardedVideo.class.getSimpleName();

    /**
     * Flag to determine whether or not the adapter has been  initialized.
     */
    private static AtomicBoolean sIsInitialized;
    private static String mPlacementId;

    /**
     * Flag to determine whether or not the Pangolin Rewarded Video Ad instance has loaded.
     */
    private boolean mIsLoaded;

    private PangleAdapterConfiguration mPangleAdapterConfiguration;
    private WeakReference<Activity> mWeakActivity;
    private TTRewardVideoAd mTTRewardVideoAd;


    public PangleAdRewardedVideo() {
        sIsInitialized = new AtomicBoolean(false);
        mPangleAdapterConfiguration = new PangleAdapterConfiguration();
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "PangleAdRewardedVideo has been create ....");
    }


    @Override
    protected boolean hasVideoAvailable() {
        return mTTRewardVideoAd != null && mIsLoaded;
    }

    @Override
    protected void showVideo() {
        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);
        if (hasVideoAvailable() && mWeakActivity != null && mWeakActivity.get() != null) {
            mTTRewardVideoAd.setRewardAdInteractionListener(mRewardAdInteractionListener);
            mTTRewardVideoAd.showRewardVideoAd(mWeakActivity.get());
        } else {
            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            MoPubRewardedVideoManager.onRewardedVideoPlaybackError(
                    PangleAdRewardedVideo.class,
                    getAdNetworkId(), MoPubErrorCode.NETWORK_NO_FILL);
        }
    }


    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    /**
     * obtain Pangolin extra parameters from the extras provided by MoPub server.
     *
     * @param launcherActivity
     * @param localExtras
     * @param serverExtras
     * @return
     * @throws Exception
     */
    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras) throws Exception {

        if (MoPubLog.getLogLevel() == MoPubLog.LogLevel.DEBUG) {
            Set<Map.Entry<String, String>> set2 = serverExtras.entrySet();
            for (Map.Entry<String, String> entry : set2) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "serverExtras => key=" + entry.getKey() + ",value=" + entry.getValue());
            }
        }

        if (!sIsInitialized.getAndSet(true)) {
            if (serverExtras != null) {

                /** obtain adunit from server by mopub */
                mPlacementId = serverExtras.get(PangleAdapterConfiguration.KEY_EXTRA_AD_PLACEMENT_ID);

                if (TextUtils.isEmpty(mPlacementId)) {
                    MoPubRewardedVideoManager.onRewardedVideoLoadFailure(PangleAdRewardedVideo.class, getAdNetworkId(), MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                    MoPubLog.log(CUSTOM, ADAPTER_NAME,
                            "Invalid Pangle placement ID. Failing ad request. " +
                                    "Ensure the ad placement id is valid on the MoPub dashboard.");

                    return false;
                }
                /** init pangolin SDK */
                String appId = serverExtras.get(PangleAdapterConfiguration.KEY_EXTRA_APP_ID);
                PangleAdapterConfiguration.pangleSdkInit(launcherActivity, appId);
                mPangleAdapterConfiguration.setCachedInitializationParameters(launcherActivity, serverExtras);
            }


            return true;
        }

        return false;
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity, @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras) throws Exception {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "loadWithSdkInitialized method execute ......getCodeId=" + PangolinRewardMediationSettings.getCodeId() + ",getOrientation=" + PangolinRewardMediationSettings.getOrientation());
        mWeakActivity = new WeakReference<>(activity);
        TTAdManager ttAdManager = PangleAdapterConfiguration.getPangleSdkManager();
        TTAdNative ttAdNative = ttAdManager.createAdNative(activity.getApplicationContext());

        if (PangolinRewardMediationSettings.getGdpr() != -1) {
            ttAdManager.setGdpr(PangolinRewardMediationSettings.getGdpr());
        }

        if (TextUtils.isEmpty(getAdNetworkId())) {
            MoPubRewardedVideoManager.onRewardedVideoPlaybackError(
                    PangleAdRewardedVideo.class,
                    getAdNetworkId(), MoPubErrorCode.MISSING_AD_UNIT_ID);

            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.MISSING_AD_UNIT_ID.getIntCode(),
                    MoPubErrorCode.MISSING_AD_UNIT_ID);

            return;
        }

        /** Create a parameter AdSlot for reward ad request type,
         refer to the document for meanings of specific parameters */
        final String adm = serverExtras.get(DataKeys.ADM_KEY);

        /** create AdSlot and set request parameters */
        AdSlot adSlot = new AdSlot.Builder()
                .setCodeId(getAdNetworkId())
                .setSupportDeepLink(true)
                .setImageAcceptedSize(1080, 1920)
                .setRewardName(PangolinRewardMediationSettings.getRewardName()) /** Parameter for rewarded video ad requests, name of the reward */
                .setRewardAmount(PangolinRewardMediationSettings.getRewardAmount())  /**The number of rewards in rewarded video ad */
                .setUserID(PangolinRewardMediationSettings.getUserID()) /**User ID, a required parameter for rewarded video ads */
                .setMediaExtra(PangolinRewardMediationSettings.getMediaExtra()) /** optional parameter */
                .withBid(adm)
                .build();
        /**load ad */
        ttAdNative.loadRewardVideoAd(adSlot, mLoadRewardVideoAdListener);
        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return TextUtils.isEmpty(mPlacementId) ? PangolinRewardMediationSettings.getCodeId() : mPlacementId;
    }

    @Override
    protected void onInvalidate() {
        if (mTTRewardVideoAd != null) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Performing cleanup tasks...");
            mTTRewardVideoAd = null;
        }
    }

    private TTAdNative.RewardVideoAdListener mLoadRewardVideoAdListener = new TTAdNative.RewardVideoAdListener() {

        @Override
        public void onError(int code, String message) {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME);
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(PangleAdRewardedVideo.class, getAdNetworkId(), mapErrorCode(code));
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, "Loading Rewarded Video creative encountered an error: " + mapErrorCode(code).toString() + ",error message:" + message);
        }

        @Override
        public void onRewardVideoAdLoad(TTRewardVideoAd ad) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "onRewardVideoAdLoad method execute ......ad = " + ad);
            if (ad != null) {
                mIsLoaded = true;
                mTTRewardVideoAd = ad;
                MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
                MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(
                        PangleAdRewardedVideo.class,
                        getAdNetworkId());
            } else {
                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(PangleAdRewardedVideo.class, getAdNetworkId(), MoPubErrorCode.NETWORK_NO_FILL);
                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, " TTRewardVideoAd is null !");
            }
        }

        @Override
        public void onRewardVideoCached() {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Pangle TTRewardVideoAd onRewardVideoCached...");
        }
    };

    private TTRewardVideoAd.RewardAdInteractionListener mRewardAdInteractionListener = new TTRewardVideoAd.RewardAdInteractionListener() {
        @Override
        public void onAdShow() {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Pangle TTRewardVideoAd onAdShow...");
            MoPubRewardedVideoManager.onRewardedVideoStarted(PangleAdRewardedVideo.class, getAdNetworkId());
        }

        @Override
        public void onAdVideoBarClick() {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Pangle TTRewardVideoAd onAdVideoBarClick...");
            MoPubRewardedVideoManager.onRewardedVideoClicked(PangleAdRewardedVideo.class, getAdNetworkId());
        }

        @Override
        public void onAdClose() {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Pangle TTRewardVideoAd onAdClose...");
            MoPubRewardedVideoManager.onRewardedVideoClosed(PangleAdRewardedVideo.class, getAdNetworkId());
        }

        @Override
        public void onVideoComplete() {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Pangle TTRewardVideoAd onVideoComplete...");
            MoPubRewardedVideoManager.onRewardedVideoCompleted(PangleAdRewardedVideo.class, getAdNetworkId(), MoPubReward.success(MoPubReward.NO_REWARD_LABEL, MoPubReward.DEFAULT_REWARD_AMOUNT));
        }

        @Override
        public void onVideoError() {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Pangle TTRewardVideoAd onVideoError...");
            MoPubRewardedVideoManager.onRewardedVideoPlaybackError(PangleAdRewardedVideo.class, getAdNetworkId(), MoPubErrorCode.UNSPECIFIED);
        }

        @Override
        public void onRewardVerify(boolean rewardVerify, int rewardAmount, String rewardName) {
            MoPubRewardedVideoManager.onRewardedVideoCompleted(
                    PangleAdRewardedVideo.class,
                    getAdNetworkId(),
                    MoPubReward.success(rewardName, rewardAmount));

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Pangle TTRewardVideoAd onRewardVerify...rewardVerify：" + rewardVerify + "，rewardAmount=" + rewardAmount + "，rewardName=" + rewardName);
        }

        @Override
        public void onSkippedVideo() {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Pangle TTRewardVideoAd skipped video...");
        }
    };

    /**
     * obtain extra parameters from MediationSettings
     */
    public static class PangolinRewardMediationSettings implements MediationSettings {
        private static String mCodeId;
        private static int mOrientation = TTAdConstant.VERTICAL;
        private static int mGdpr;
        private static String mRewardName;
        private static int mRewardAmount;
        private static String mMediaExtra;
        private static String mUserID;

        private PangolinRewardMediationSettings() {
        }

        public static int getGdpr() {
            return mGdpr;
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
            private int mGdpr;
            private int mCoppa;
            private String mRewardName;
            private int mRewardAmount;
            private String mMediaExtra;
            private String mUserID;


            public Builder setCodeId(String codeId) {
                mCodeId = codeId;
                return this;
            }

            public Builder setCoppa(int coppa) {
                mCoppa = coppa;
                return this;
            }


            public Builder setOrientation(int orientation) {
                mOrientation = orientation;
                return this;
            }

            public Builder setGdpr(int gdpr) {
                mGdpr = gdpr;
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

            public PangolinRewardMediationSettings builder() {
                PangolinRewardMediationSettings settings = new PangolinRewardMediationSettings();
                PangolinRewardMediationSettings.mCodeId = this.mCodeId;
                PangolinRewardMediationSettings.mOrientation = this.mOrientation;
                PangolinRewardMediationSettings.mGdpr = this.mGdpr;
                PangolinRewardMediationSettings.mRewardName = this.mRewardName;
                PangolinRewardMediationSettings.mRewardAmount = this.mRewardAmount;
                PangolinRewardMediationSettings.mMediaExtra = this.mMediaExtra;
                PangolinRewardMediationSettings.mUserID = this.mUserID;
                return settings;
            }
        }


    }

    private static MoPubErrorCode mapErrorCode(int error) {
        switch (error) {
            case PangleSharedUtil.CONTENT_TYPE:
            case PangleSharedUtil.REQUEST_PB_ERROR:
                return MoPubErrorCode.NO_CONNECTION;
            case PangleSharedUtil.NO_AD:
                return MoPubErrorCode.NETWORK_NO_FILL;
            case PangleSharedUtil.ADSLOT_EMPTY:
            case PangleSharedUtil.ADSLOT_ID_ERROR:
                return MoPubErrorCode.MISSING_AD_UNIT_ID;
            default:
                return MoPubErrorCode.UNSPECIFIED;
        }
    }
}

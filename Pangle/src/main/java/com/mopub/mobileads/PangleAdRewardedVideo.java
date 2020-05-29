package com.mopub.mobileads;

import android.annotation.SuppressLint;
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

/**
 * created by wuzejian on 2020/5/11
 */
@SuppressLint("LongLogTag")
public class PangleAdRewardedVideo extends CustomEventRewardedVideo {
    private static final String ADAPTER_NAME = "PangleAdRewardedVideo";

    /**
     * Flag to determine whether or not the adapter has been  initialized.
     */
    private static AtomicBoolean sIsInitialized;

    /**
     * Key to obtain Pangolin ad orientation from the extras provided by MoPub.
     */
    private static final String KEY_EXTRA_AD_ORIENTATION = "orientation";

    /**
     * Flag to determine whether or not the Pangolin Rewarded Video Ad instance has loaded.
     */
    private boolean mIsLoaded;


    private PangleAdapterConfiguration mPangleAdapterConfiguration;

    private String placementId;

    private int mOrientation = -1;

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
                /** obtain ad orientation from server by mopub */
                if (!TextUtils.isEmpty(serverExtras.get(KEY_EXTRA_AD_ORIENTATION))) {
                    mOrientation = Integer.valueOf(serverExtras.get(KEY_EXTRA_AD_ORIENTATION));
                }

                /** obtain adunit from server by mopub */
                String adunit = serverExtras.get(PangleAdapterConfiguration.KEY_EXTRA_AD_UNIT_ID);
                if (!TextUtils.isEmpty(adunit)) {
                    this.placementId = adunit;
                }
                /** init pangolin SDK */
                String appId = serverExtras.get(PangleAdapterConfiguration.PANGLE_APP_ID_KEY);
                String appName = serverExtras.get(PangleAdapterConfiguration.PANGLE_APP_NAME_KEY);
                PangleAdapterConfiguration.pangleSdkInit(launcherActivity, appId, appName);
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

        /** Create a parameter AdSlot for reward ad request type,
         refer to the document for meanings of specific parameters */
        final String adm = serverExtras.get(DataKeys.ADM_KEY);

        /** create AdSlot and set request parameters */
        AdSlot adSlot = new AdSlot.Builder()
                .setCodeId(getAdUnitId())
                .setSupportDeepLink(true)
                .setImageAcceptedSize(1080, 1920)
                .setRewardName(PangolinRewardMediationSettings.getRewardName()) /** Parameter for rewarded video ad requests, name of the reward */
                .setRewardAmount(PangolinRewardMediationSettings.getRewardAmount())  /**The number of rewards in rewarded video ad */
                .setUserID(PangolinRewardMediationSettings.getUserID()) /**User ID, a required parameter for rewarded video ads */
                .setMediaExtra(PangolinRewardMediationSettings.getMediaExtra()) /** optional parameter */
                .setOrientation(getOrienttation()) /** Set how you wish the video ad to be displayed, choose from TTAdConstant.HORIZONTAL or TTAdConstant.VERTICAL */
                .withBid(adm)
                .build();
        /**load ad */
        ttAdNative.loadRewardVideoAd(adSlot, mLoadRewardVideoAdListener);
        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return getAdUnitId();
    }

    @Override
    protected void onInvalidate() {
        if (mTTRewardVideoAd != null) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Performing cleanup tasks...");
            mTTRewardVideoAd = null;
        }
    }

    private String getAdUnitId() {
        return TextUtils.isEmpty(placementId) ? PangolinRewardMediationSettings.getCodeId() : placementId;
    }

    private int getOrienttation() {
        return mOrientation != -1 ? mOrientation : PangolinRewardMediationSettings.getOrientation();
    }


    private TTAdNative.RewardVideoAdListener mLoadRewardVideoAdListener = new TTAdNative.RewardVideoAdListener() {

        @Override
        public void onError(int code, String message) {
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
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "TTRewardVideoAd onRewardVideoCached...");
        }
    };

    private TTRewardVideoAd.RewardAdInteractionListener mRewardAdInteractionListener = new TTRewardVideoAd.RewardAdInteractionListener() {
        @Override
        public void onAdShow() {
            MoPubRewardedVideoManager.onRewardedVideoStarted(PangleAdRewardedVideo.class, getAdUnitId());
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "TTRewardVideoAd onAdShow...");
        }

        @Override
        public void onAdVideoBarClick() {
            MoPubRewardedVideoManager.onRewardedVideoClicked(PangleAdRewardedVideo.class, getAdUnitId());
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "TTRewardVideoAd onAdVideoBarClick...");
        }

        @Override
        public void onAdClose() {
            MoPubRewardedVideoManager.onRewardedVideoClosed(PangleAdRewardedVideo.class, getAdUnitId());
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "TTRewardVideoAd onAdClose...");
        }

        @Override
        public void onVideoComplete() {
            MoPubRewardedVideoManager.onRewardedVideoCompleted(PangleAdRewardedVideo.class, getAdUnitId(), MoPubReward.success(MoPubReward.NO_REWARD_LABEL, MoPubReward.DEFAULT_REWARD_AMOUNT));
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "TTRewardVideoAd onVideoComplete...");
        }

        @Override
        public void onVideoError() {
            MoPubRewardedVideoManager.onRewardedVideoPlaybackError(PangleAdRewardedVideo.class, getAdUnitId(), MoPubErrorCode.UNSPECIFIED);
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "TTRewardVideoAd onVideoError...");
        }

        @Override
        public void onRewardVerify(boolean rewardVerify, int rewardAmount, String rewardName) {
            MoPubRewardedVideoManager.onRewardedVideoCompleted(
                    PangleAdRewardedVideo.class,
                    getAdNetworkId(),
                    MoPubReward.success(rewardName, rewardAmount));

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "TTRewardVideoAd onRewardVerify...rewardVerify：" + rewardVerify + "，rewardAmount=" + rewardAmount + "，rewardName=" + rewardName);
        }

        @Override
        public void onSkippedVideo() {

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

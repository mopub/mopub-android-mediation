// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/
// 2020.3.3- add class HuaweiRewardedVideo
// Huawei Technologies Co., Ltd.

package com.mopub.mobileads;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.HwAds;
import com.huawei.hms.ads.RequestOptions;
import com.huawei.hms.ads.reward.Reward;
import com.huawei.hms.ads.reward.RewardAd;
import com.huawei.hms.ads.reward.RewardAdLoadListener;
import com.huawei.hms.ads.reward.RewardAdStatusListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MediationSettings;
import com.mopub.common.MoPubReward;
import com.mopub.common.logging.MoPubLog;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.huawei.hms.ads.TagForChild.TAG_FOR_CHILD_PROTECTION_FALSE;
import static com.huawei.hms.ads.TagForChild.TAG_FOR_CHILD_PROTECTION_TRUE;
import static com.huawei.hms.ads.TagForChild.TAG_FOR_CHILD_PROTECTION_UNSPECIFIED;
import static com.huawei.hms.ads.UnderAge.PROMISE_FALSE;
import static com.huawei.hms.ads.UnderAge.PROMISE_TRUE;
import static com.huawei.hms.ads.UnderAge.PROMISE_UNSPECIFIED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.DID_DISAPPEAR;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOULD_REWARD;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;

public class HuaweiRewardedVideo extends CustomEventRewardedVideo {

    /**
     * Key to obtain Huawei application ID from the server extras provided by Huawei ads.
     */
    public static final String KEY_EXTRA_APPLICATION_ID = "appid";

    /**
     * Key to obtain Huawei ad unit ID from the extras provided by Huawei.
     */
    public static final String KEY_EXTRA_AD_UNIT_ID = "adunit";

    /**
     * Key to set and obtain the content URL to be passed with Huawei's ad request.
     */
    public static final String KEY_CONTENT_URL = "contentUrl";

    /**
     * Key to set and obtain the flag whether the application's content is child-directed.
     */
    public static final String TAG_FOR_CHILD_DIRECTED_KEY = "tagForChildDirectedTreatment";

    /**
     * Key to set and obtain the flag to mark ad requests to Huawei to receive treatment for
     * users in the European Economic Area (EEA) under the age of consent.
     */
    public static final String TAG_FOR_UNDER_AGE_OF_CONSENT_KEY = "tagForUnderAgeOfConsent";

    /**
     * String to represent the simple class name to be used in log entries.
     */
    private static final String ADAPTER_NAME = HuaweiRewardedVideo.class.getSimpleName();

    /**
     * Flag to determine whether or not the adapter has been initialized.
     */
    private static AtomicBoolean sIsInitialized;

    /**
     * Huawei Mobile Ads rewarded video ad unit ID.
     */
    private String mAdUnitId = "";

    /**
     * The Huawei Rewarded Video Ad instance.
     */
    private RewardAd mRewardAd;

    /**
     * Flag to determine whether or not the Huawei Rewarded Video Ad instance has loaded.
     */
    private boolean mIsLoaded;

    /**
     * A Weak reference of the activity used to show the Huawei Rewarded Video Ad
     */
    private WeakReference<Activity> mWeakActivity;

    /**
     * The Huawei adapter configuration to use to cache network IDs from Huawei
     */
    @NonNull
    private HuaweiAdapterConfiguration mHuaweiAdAdapterConfiguration;

    public HuaweiRewardedVideo() {
        sIsInitialized = new AtomicBoolean(false);
        mHuaweiAdAdapterConfiguration = new HuaweiAdapterConfiguration();
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        // Huawei rewarded videos do not have a unique identifier for each ad; using ad unit ID as
        // an identifier for all ads.
        return mAdUnitId;
    }

    @Override
    protected void onInvalidate() {
        if (mRewardAd != null) {
            mRewardAd = null;
        }
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity,
                                            @NonNull Map<String, Object> localExtras,
                                            @NonNull Map<String, String> serverExtras)
            throws Exception {
        if (!sIsInitialized.getAndSet(true)) {
            if (TextUtils.isEmpty(serverExtras.get(KEY_EXTRA_APPLICATION_ID))) {
                HwAds.init(launcherActivity);
            } else {
                HwAds.init(launcherActivity, serverExtras.get(KEY_EXTRA_APPLICATION_ID));
            }

            mAdUnitId = serverExtras.get(KEY_EXTRA_AD_UNIT_ID);
            if (TextUtils.isEmpty(mAdUnitId)) {
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                        MoPubErrorCode.NETWORK_NO_FILL);

                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(
                        HuaweiRewardedVideo.class,
                        getAdNetworkId(),
                        MoPubErrorCode.NETWORK_NO_FILL);
                return false;
            }

            mHuaweiAdAdapterConfiguration
                    .setCachedInitializationParameters(launcherActivity, serverExtras);
            return true;
        }

        return false;
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull final Activity activity,
                                          @NonNull final Map<String, Object> localExtras,
                                          @NonNull final Map<String, String> serverExtras)
            throws Exception {

        mAdUnitId = serverExtras.get(KEY_EXTRA_AD_UNIT_ID);
        if (TextUtils.isEmpty(mAdUnitId)) {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(
                    HuaweiRewardedVideo.class,
                    HuaweiRewardedVideo.class.getSimpleName(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        mWeakActivity = new WeakReference<>(activity);
        mRewardAd = new RewardAd(activity, mAdUnitId);

        AdParam.Builder builder = new AdParam.Builder();
        builder.setRequestOrigin("MoPub");

        final Object contentUrlObject = localExtras.get(KEY_CONTENT_URL);
        final String contentUrl;

        if (contentUrlObject instanceof String) {
            contentUrl = (String) contentUrlObject;
        } else {
            contentUrl = HuaweiMediationSettings.getContentUrl();
        }

        if (!TextUtils.isEmpty(contentUrl)) {
            builder.setTargetingContentUrl(contentUrl);
        }

        final RequestOptions.Builder requestConfigurationBuilder = new RequestOptions.Builder();

        // Publishers may want to indicate that their content is child-directed and
        // forward this information to Huawei.
        final Object isTFCDObject = localExtras.get(TAG_FOR_CHILD_DIRECTED_KEY);
        final Boolean isTFCD;

        if (isTFCDObject instanceof Boolean) {
            isTFCD = (Boolean) isTFCDObject;
        } else {
            isTFCD = HuaweiMediationSettings.isTaggedForChildDirectedTreatment();
        }

        if (isTFCD != null) {
            if (isTFCD) {
                requestConfigurationBuilder.setTagForChildProtection(TAG_FOR_CHILD_PROTECTION_TRUE);
            } else {
                requestConfigurationBuilder.setTagForChildProtection(TAG_FOR_CHILD_PROTECTION_FALSE);
            }
        } else {
            requestConfigurationBuilder.setTagForChildProtection(TAG_FOR_CHILD_PROTECTION_UNSPECIFIED);
        }

        // Publishers may want to mark their requests to receive treatment for users
        // in the European Economic Area (EEA) under the age of consent.
        final Object isTFUAObject = localExtras.get(TAG_FOR_UNDER_AGE_OF_CONSENT_KEY);
        final Boolean isTFUA;

        if (isTFUAObject instanceof Boolean) {
            isTFUA = (Boolean) isTFUAObject;
        } else {
            isTFUA = HuaweiMediationSettings.isTaggedForUnderAgeOfConsent();
        }

        if (isTFUA != null) {
            if (isTFUA) {
                requestConfigurationBuilder.setTagForUnderAgeOfPromise(PROMISE_TRUE);
            } else {
                requestConfigurationBuilder.setTagForUnderAgeOfPromise(PROMISE_FALSE);
            }
        } else {
            requestConfigurationBuilder.setTagForUnderAgeOfPromise(PROMISE_UNSPECIFIED);
        }

        final RequestOptions requestConfiguration = requestConfigurationBuilder.build();
        HwAds.setRequestOptions(requestConfiguration);

        final AdParam adRequest = builder.build();
        mRewardAd.loadAd(adRequest, mRewardedAdLoadCallback);

        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    @Override
    protected boolean hasVideoAvailable() {
        return mRewardAd != null && mIsLoaded;
    }

    @Override
    protected void showVideo() {
        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);

        if (hasVideoAvailable() && mWeakActivity != null && mWeakActivity.get() != null) {
            mRewardAd.show(mWeakActivity.get(), mRewardedAdCallback);
        } else {
            MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            MoPubRewardedVideoManager.onRewardedVideoPlaybackError(
                    HuaweiRewardedVideo.class,
                    getAdNetworkId(),
                    getMoPubRequestErrorCode(AdParam.ErrorCode.NO_AD));
        }
    }

    private RewardAdLoadListener mRewardedAdLoadCallback = new RewardAdLoadListener() {
        @Override
        public void onRewardedLoaded() {
            mIsLoaded = true;
            MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);

            MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(
                    HuaweiRewardedVideo.class,
                    getAdNetworkId());
        }

        @Override
        public void onRewardAdFailedToLoad(int error) {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    getMoPubRequestErrorCode(error).getIntCode(),
                    getMoPubRequestErrorCode(error));

            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(
                    HuaweiRewardedVideo.class,
                    getAdNetworkId(),
                    getMoPubRequestErrorCode(error));
        }
    };

    private RewardAdStatusListener mRewardedAdCallback = new RewardAdStatusListener() {
        @Override
        public void onRewardAdOpened() {
            MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);

            MoPubRewardedVideoManager.onRewardedVideoStarted(
                    HuaweiRewardedVideo.class,
                    getAdNetworkId());
        }

        @Override
        public void onRewardAdClosed() {
            MoPubLog.log(getAdNetworkId(), DID_DISAPPEAR, ADAPTER_NAME);

            MoPubRewardedVideoManager.onRewardedVideoClosed(
                    HuaweiRewardedVideo.class,
                    getAdNetworkId());
        }

        @Override
        public void onRewarded(@NonNull Reward rewardItem) {
            MoPubLog.log(getAdNetworkId(), SHOULD_REWARD, ADAPTER_NAME,
                    rewardItem.getAmount(), rewardItem.getName());

            MoPubRewardedVideoManager.onRewardedVideoCompleted(
                    HuaweiRewardedVideo.class,
                    getAdNetworkId(),
                    MoPubReward.success(rewardItem.getName(), rewardItem.getAmount()));
        }

        @Override
        public void onRewardAdFailedToShow(int error) {
            MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME);

            MoPubRewardedVideoManager.onRewardedVideoPlaybackError(
                    HuaweiRewardedVideo.class,
                    getAdNetworkId(),
                    getMoPubShowErrorCode(error));
        }
    };

    /**
     * Converts a given Huawei Mobile Ads SDK Ad Request error code into {@link MoPubErrorCode}.
     *
     * @param error Huawei Mobile Ads SDK Ad Request error code.
     * @return an equivalent MoPub SDK error code for the given Huawei Mobile Ads SDK Ad Request
     * error code.
     */
    private MoPubErrorCode getMoPubRequestErrorCode(int error) {
        switch (error) {
            case AdParam.ErrorCode.INNER:
                return MoPubErrorCode.INTERNAL_ERROR;
            case AdParam.ErrorCode.INVALID_REQUEST:
                return MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
            case AdParam.ErrorCode.NETWORK_ERROR:
                return MoPubErrorCode.NO_CONNECTION;
            case AdParam.ErrorCode.NO_AD:
                return MoPubErrorCode.NO_FILL;
        }
        return MoPubErrorCode.UNSPECIFIED;
    }

    /**
     * Converts a given Huawei Mobile Ads SDK error code when showing Rewarded Video Ads into
     * {@link MoPubErrorCode}.
     *
     * @param error Huawei Mobile Ads SDK Ad Request error code when showing Rewarded Video Ads.
     * @return an equivalent MoPub SDK error code for the given Huawei Mobile Ads SDK Ad Request
     * error code thrown when showing Rewarded Video Ads.
     */
    private MoPubErrorCode getMoPubShowErrorCode(int error) {
        switch (error) {
            case RewardAdStatusListener.ErrorCode.REUSED:
            case RewardAdStatusListener.ErrorCode.INTERNAL:
                return MoPubErrorCode.INTERNAL_ERROR;
            case RewardAdStatusListener.ErrorCode.BACKGROUND:
                return MoPubErrorCode.VIDEO_PLAYBACK_ERROR;
            case RewardAdStatusListener.ErrorCode.NOT_LOADED:
                return MoPubErrorCode.WARMUP;
        }
        return MoPubErrorCode.UNSPECIFIED;
    }

    public static final class HuaweiMediationSettings implements MediationSettings {
        private static String contentUrl;
        private static Boolean taggedForChildDirectedTreatment;
        private static Boolean taggedForUnderAgeOfConsent;

        public HuaweiMediationSettings() {
        }

        public HuaweiMediationSettings(@NonNull Bundle bundle) {
            if (bundle.containsKey(KEY_CONTENT_URL)) {
                contentUrl = bundle.getString(KEY_CONTENT_URL);
            }

            if (bundle.containsKey(TAG_FOR_CHILD_DIRECTED_KEY)) {
                taggedForChildDirectedTreatment = bundle.getBoolean(TAG_FOR_CHILD_DIRECTED_KEY);
            }

            if (bundle.containsKey(TAG_FOR_UNDER_AGE_OF_CONSENT_KEY)) {
                taggedForUnderAgeOfConsent = bundle.getBoolean(TAG_FOR_UNDER_AGE_OF_CONSENT_KEY);
            }
        }

        public void setContentUrl(String url) {
            contentUrl = url;
        }

        public void setTaggedForChildDirectedTreatment(boolean flag) {
            taggedForChildDirectedTreatment = flag;
        }

        public void setTaggedForUnderAgeOfConsent(boolean flag) {
            taggedForUnderAgeOfConsent = flag;
        }

        private static String getContentUrl() {
            return contentUrl;
        }

        private static Boolean isTaggedForChildDirectedTreatment() {
            return taggedForChildDirectedTreatment;
        }

        private static Boolean isTaggedForUnderAgeOfConsent() {
            return taggedForUnderAgeOfConsent;
        }
    }
}

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MediationSettings;
import com.mopub.common.MoPubReward;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.DID_DISAPPEAR;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOULD_REWARD;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.mobileads.GooglePlayServicesAdapterConfiguration.forwardNpaIfSet;

public class GooglePlayServicesRewardedVideo extends BaseAd {

    /**
     * Key to obtain AdMob application ID from the server extras provided by MoPub.
     */
    public static final String KEY_EXTRA_APPLICATION_ID = "appid";

    /**
     * Key to obtain AdMob ad unit ID from the extras provided by MoPub.
     */
    public static final String KEY_EXTRA_AD_UNIT_ID = "adunit";

    /**
     * Key to set and obtain the content URL to be passed with AdMob's ad request.
     */
    public static final String KEY_CONTENT_URL = "contentUrl";

    /**
     * Key to set and obtain the flag whether the application's content is child-directed.
     */
    public static final String TAG_FOR_CHILD_DIRECTED_KEY = "tagForChildDirectedTreatment";

    /**
     * Key to set and obtain the flag to mark ad requests to Google to receive treatment for
     * users in the European Economic Area (EEA) under the age of consent.
     */
    public static final String TAG_FOR_UNDER_AGE_OF_CONSENT_KEY = "tagForUnderAgeOfConsent";

    /**
     * Key to set and obtain the test device ID String to be passed with AdMob's ad request.
     */
    public static final String TEST_DEVICES_KEY = "testDevices";

    /**
     * String to represent the simple class name to be used in log entries.
     */
    private static final String ADAPTER_NAME = GooglePlayServicesRewardedVideo.class.getSimpleName();

    /**
     * Flag to determine whether or not the adapter has been initialized.
     */
    private static AtomicBoolean sIsInitialized;

    /**
     * Google Mobile Ads rewarded video ad unit ID.
     */
    private String mAdUnitId = "";

    /**
     * The Google Rewarded Video Ad instance.
     */
    private RewardedAd mRewardedAd;

    /**
     * The current context with which to request and show the ad.
     */
    private Context mContext;

    /**
     * The AdMob adapter configuration to use to cache network IDs from AdMob
     */
    @NonNull
    private final GooglePlayServicesAdapterConfiguration mGooglePlayServicesAdapterConfiguration;

    public GooglePlayServicesRewardedVideo() {
        sIsInitialized = new AtomicBoolean(false);
        mGooglePlayServicesAdapterConfiguration = new GooglePlayServicesAdapterConfiguration();
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return mAdUnitId;
    }

    @Override
    protected void onInvalidate() {
        if (mRewardedAd != null) {
            mRewardedAd = null;
        }
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
                                            @NonNull final AdData adData) {
        Preconditions.checkNotNull(launcherActivity);
        Preconditions.checkNotNull(adData);

        if (!sIsInitialized.getAndSet(true)) {
            final Map<String, String> extras = adData.getExtras();

            MobileAds.initialize(launcherActivity);

            mAdUnitId = extras.get(KEY_EXTRA_AD_UNIT_ID);
            if (TextUtils.isEmpty(mAdUnitId)) {
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                        MoPubErrorCode.NETWORK_NO_FILL);

                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);
                }
                return false;
            }

            mGooglePlayServicesAdapterConfiguration.setCachedInitializationParameters(launcherActivity,
                    extras);
            return true;
        }

        return false;
    }

    @Override
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        setAutomaticImpressionAndClickTracking(false);

        mContext = context;

        final Map<String, String> extras = adData.getExtras();

        mAdUnitId = extras.get(KEY_EXTRA_AD_UNIT_ID);
        if (TextUtils.isEmpty(mAdUnitId)) {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return;
        }

        if (!(context instanceof Activity)) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Context passed to load " +
                    "was not an Activity.");
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return;
        }

        final AdRequest.Builder builder = new AdRequest.Builder();
        builder.setRequestAgent("MoPub");

        // Publishers may append a content URL by passing it to the
        // GooglePlayServicesMediationSettings instance when initializing the MoPub SDK:
        // https://developers.mopub.com/docs/mediation/networks/google/#android
        String contentUrl = extras.get(KEY_CONTENT_URL);

        if (TextUtils.isEmpty(contentUrl)) {
            contentUrl = GooglePlayServicesMediationSettings.getContentUrl();
        }
        if (!TextUtils.isEmpty(contentUrl)) {
            builder.setContentUrl(contentUrl);
        }

        forwardNpaIfSet(builder);

        final RequestConfiguration.Builder requestConfigurationBuilder = new RequestConfiguration.Builder();

        // Publishers may request for test ads by passing test device IDs to the
        // GooglePlayServicesMediationSettings instance when initializing the MoPub SDK:
        // https://developers.mopub.com/docs/mediation/networks/google/#android
        String testDeviceId = extras.get(TEST_DEVICES_KEY);

        if (TextUtils.isEmpty(testDeviceId)) {
            testDeviceId = GooglePlayServicesMediationSettings.getTestDeviceId();
        }
        if (!TextUtils.isEmpty(testDeviceId)) {
            requestConfigurationBuilder.setTestDeviceIds(Collections.singletonList(testDeviceId));
        }

        // Publishers may want to indicate that their content is child-directed and
        // forward this information to Google.
        final String isTFCDString = extras.get(TAG_FOR_CHILD_DIRECTED_KEY);
        final Boolean isTFCD;

        if (!TextUtils.isEmpty(isTFCDString)) {
            isTFCD = Boolean.valueOf(isTFCDString);
        } else {
            isTFCD = GooglePlayServicesMediationSettings.isTaggedForChildDirectedTreatment();
        }

        if (isTFCD != null) {
            if (isTFCD) {
                requestConfigurationBuilder.setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE);
            } else {
                requestConfigurationBuilder.setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE);
            }
        } else {
            requestConfigurationBuilder.setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED);
        }

        // Publishers may want to mark their requests to receive treatment for users
        // in the European Economic Area (EEA) under the age of consent.
        final String isTFUAString = extras.get(TAG_FOR_UNDER_AGE_OF_CONSENT_KEY);
        final Boolean isTFUA;

        if (!TextUtils.isEmpty(isTFUAString)) {
            isTFUA = Boolean.valueOf(isTFUAString);
        } else {
            isTFUA = GooglePlayServicesMediationSettings.isTaggedForUnderAgeOfConsent();
        }

        if (isTFUA != null) {
            if (isTFUA) {
                requestConfigurationBuilder.setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE);
            } else {
                requestConfigurationBuilder.setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE);
            }
        } else {
            requestConfigurationBuilder.setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED);
        }

        final RequestConfiguration requestConfiguration = requestConfigurationBuilder.build();
        MobileAds.setRequestConfiguration(requestConfiguration);

        final AdRequest adRequest = builder.build();
        RewardedAd.load(context, mAdUnitId, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                Preconditions.checkNotNull(rewardedAd);

                if (mLoadListener != null) {
                    mLoadListener.onAdLoaded();
                }

                MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);

                mRewardedAd = rewardedAd;
                mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdShowedFullScreenContent() {
                        MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);

                        if (mInteractionListener != null) {
                            mInteractionListener.onAdShown();
                            mInteractionListener.onAdImpression();
                        }
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Failed to show " +
                                "Google rewarded video. " + adError.getMessage());

                        MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME,
                                MoPubErrorCode.VIDEO_PLAYBACK_ERROR.getIntCode(),
                                MoPubErrorCode.VIDEO_PLAYBACK_ERROR);

                        if (mInteractionListener != null) {
                            mInteractionListener.onAdFailed(MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
                        }

                        mRewardedAd = null;
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        MoPubLog.log(getAdNetworkId(), DID_DISAPPEAR, ADAPTER_NAME);

                        if (mInteractionListener != null) {
                            mInteractionListener.onAdDismissed();
                        }

                        mRewardedAd = null;
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Preconditions.checkNotNull(loadAdError);

                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Failed to load " +
                        "Google rewarded video. " + loadAdError.getMessage());

                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                        MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Failed to load Google " +
                        "interstitial with message: " + loadAdError.getMessage() + ". Caused by: " +
                        loadAdError.getCause());

                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }

                mRewardedAd = null;
            }
        });

        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    @Override
    protected void show() {
        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);

        if (mRewardedAd != null) {
            if (mContext instanceof Activity) {
                mRewardedAd.show((Activity) mContext, new OnUserEarnedRewardListener() {
                    @Override
                    public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                        Preconditions.checkNotNull(rewardItem);

                        MoPubLog.log(getAdNetworkId(), SHOULD_REWARD,
                                ADAPTER_NAME, rewardItem.getAmount(), rewardItem.getType());

                        if (mInteractionListener != null) {
                            mInteractionListener.onAdComplete(MoPubReward.success(rewardItem.getType(),
                                    rewardItem.getAmount()));
                        }
                    }
                });
            } else {
                MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.VIDEO_PLAYBACK_ERROR.getIntCode(),
                        MoPubErrorCode.VIDEO_PLAYBACK_ERROR);

                if (mInteractionListener != null) {
                    mInteractionListener.onAdFailed(MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
                }

            }
        } else {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Failed to show " +
                    "Google rewarded video because it wasn't ready yet.");

            MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.VIDEO_PLAYBACK_ERROR.getIntCode(),
                    MoPubErrorCode.VIDEO_PLAYBACK_ERROR);

            if (mInteractionListener != null) {
                mInteractionListener.onAdFailed(MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
            }
        }
    }

    public static final class GooglePlayServicesMediationSettings implements MediationSettings {
        private static String contentUrl;
        private static String testDeviceId;
        private static Boolean taggedForChildDirectedTreatment;
        private static Boolean taggedForUnderAgeOfConsent;

        public GooglePlayServicesMediationSettings() {
        }

        public GooglePlayServicesMediationSettings(@NonNull Bundle bundle) {
            if (bundle.containsKey(KEY_CONTENT_URL)) {
                contentUrl = bundle.getString(KEY_CONTENT_URL);
            }

            if (bundle.containsKey(TEST_DEVICES_KEY)) {
                testDeviceId = bundle.getString(TEST_DEVICES_KEY);
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

        public void setTestDeviceId(String id) {
            testDeviceId = id;
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

        private static String getTestDeviceId() {
            return testDeviceId;
        }

        private static Boolean isTaggedForChildDirectedTreatment() {
            return taggedForChildDirectedTreatment;
        }

        private static Boolean isTaggedForUnderAgeOfConsent() {
            return taggedForUnderAgeOfConsent;
        }
    }
}

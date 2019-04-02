package com.mopub.mobileads;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdCallback;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MediationSettings;
import com.mopub.common.MoPubReward;
import com.mopub.common.logging.MoPubLog;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.DID_DISAPPEAR;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOULD_REWARD;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;

public class GooglePlayServicesRewardedVideo extends CustomEventRewardedVideo {

    /**
     * String to represent the simple class name to be used in log entries.
     */
    private static final String ADAPTER_NAME =
            GooglePlayServicesRewardedVideo.class.getSimpleName();

    /**
     * Key to obtain AdMob application ID from the server extras provided by MoPub.
     */
    private static final String KEY_EXTRA_APPLICATION_ID = "appid";

    /**
     * Key to obtain AdMob ad unit ID from the extras provided by MoPub.
     */
    private static final String KEY_EXTRA_AD_UNIT_ID = "adunit";

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
     * A Weak reference of the activity used to show the Google Rewarded Video Ad
     */
    private WeakReference<Activity> mWeakActivity;

    /**
     * Flag to indicate whether the rewarded video has cached. AdMob's isLoaded() call crashes the
     * app when called from a thread other than the main UI thread. Since this is unavoidable with
     * some platforms, e.g. Unity, we implement this workaround.
     */
    private boolean isAdLoaded;

    /**
     * The AdMob adapter configuration to use to cache network IDs from AdMob
     */
    @NonNull
    private GooglePlayServicesAdapterConfiguration mGooglePlayServicesAdapterConfiguration;

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
        // Google rewarded videos do not have a unique identifier for each ad; using ad unit ID as
        // an identifier for all ads.
        return mAdUnitId;
    }

    @Override
    protected void onInvalidate() {
        if (mRewardedAd != null) {
            mRewardedAd = null;
        }
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity,
                                            @NonNull Map<String, Object> localExtras,
                                            @NonNull Map<String, String> serverExtras)
            throws Exception {
        if (!sIsInitialized.getAndSet(true)) {
            if (TextUtils.isEmpty(serverExtras.get(KEY_EXTRA_APPLICATION_ID))) {
                MobileAds.initialize(launcherActivity);
            } else {
                MobileAds.initialize(launcherActivity, serverExtras.get(KEY_EXTRA_APPLICATION_ID));
            }

            mGooglePlayServicesAdapterConfiguration
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

        /* AdMob's isLoaded() has to be called on the main thread to avoid multithreading crashes
        when mediating on Unity */
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                isAdLoaded = false;

                mAdUnitId = serverExtras.get(KEY_EXTRA_AD_UNIT_ID);
                if (TextUtils.isEmpty(mAdUnitId)) {
                    // Using class name as the network ID for this callback since the ad unit ID is
                    // invalid.
                    MoPubRewardedVideoManager.onRewardedVideoLoadFailure(
                            GooglePlayServicesRewardedVideo.class,
                            GooglePlayServicesRewardedVideo.class.getSimpleName(),
                            MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                    return;
                }

                mWeakActivity = new WeakReference<>(activity);
                mRewardedAd = new RewardedAd(activity, mAdUnitId);

                AdRequest.Builder builder = new AdRequest.Builder();
                builder.setRequestAgent("MoPub");

                // Publishers may append a content URL by passing it to the
                // GooglePlayServicesMediationSettings instance when initializing the MoPub SDK:
                // https://developers.mopub.com/docs/mediation/networks/google/#android
                String contentUrl = GooglePlayServicesMediationSettings.getContentUrl();
                if (!TextUtils.isEmpty(contentUrl)) {
                    builder.setContentUrl(contentUrl);
                }

                // Publishers may request for test ads by passing test device IDs to the
                // GooglePlayServicesMediationSettings instance when initializing the MoPub SDK:
                // https://developers.mopub.com/docs/mediation/networks/google/#android
                String testDeviceId = GooglePlayServicesMediationSettings.getTestDeviceId();
                if (!TextUtils.isEmpty(testDeviceId)) {
                    builder.addTestDevice(testDeviceId);
                }

                // Consent collected from the MoPub’s consent dialogue should not be used
                // to set up Google's personalization preference.
                // Publishers should work with Google to be GDPR-compliant.
                forwardNpaIfSet(builder);

                // Publishers may want to indicate that their content is child-directed and
                // forward this information to Google.
                Boolean isTFCD = GooglePlayServicesMediationSettings.isTaggedForChildDirectedTreatment();
                if (isTFCD != null) {
                    builder.tagForChildDirectedTreatment(isTFCD);
                }

                // Publishers may want to mark your their requests to receive treatment for users
                // in the European Economic Area (EEA) under the age of consent.
                Boolean isTFUA = GooglePlayServicesMediationSettings.isTaggedForUnderAgeOfConsent();
                if (isTFUA != null) {
                    if (isTFUA) {
                        builder.setTagForUnderAgeOfConsent(AdRequest.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE);
                    } else {
                        builder.setTagForUnderAgeOfConsent(AdRequest.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE);
                    }
                }

                AdRequest adRequest = builder.build();
                mRewardedAd.loadAd(adRequest, mRewardedAdLoadCallback);

                MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
            }
        });
    }

    private void forwardNpaIfSet(AdRequest.Builder builder) {
        // Only forward the "npa" bundle if it is explicitly set.
        // Otherwise, don't attach it with the ad request.
        if (GooglePlayServicesMediationSettings.getNpaBundle() != null &&
                !GooglePlayServicesMediationSettings.getNpaBundle().isEmpty()) {
            builder.addNetworkExtrasBundle(AdMobAdapter.class,
                    GooglePlayServicesMediationSettings.getNpaBundle());
        }
    }

    @Override
    protected boolean hasVideoAvailable() {
        return mRewardedAd != null && isAdLoaded;
    }

    @Override
    protected void showVideo() {
        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);

        if (hasVideoAvailable() && mWeakActivity != null && mWeakActivity.get() != null) {
            mRewardedAd.show(mWeakActivity.get(), mRewardedAdCallback);
        } else {
            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            MoPubRewardedVideoManager.onRewardedVideoPlaybackError(
                    GooglePlayServicesRewardedVideo.class,
                    getAdNetworkId(),
                    getMoPubRequestErrorCode(AdRequest.ERROR_CODE_NO_FILL));
        }
    }

    private RewardedAdLoadCallback mRewardedAdLoadCallback = new RewardedAdLoadCallback() {
        @Override
        public void onRewardedAdLoaded() {
            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);

            MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(
                    GooglePlayServicesRewardedVideo.class,
                    getAdNetworkId());
            isAdLoaded = true;
        }

        @Override
        public void onRewardedAdFailedToLoad(int error) {
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    getMoPubRequestErrorCode(error).getIntCode(),
                    getMoPubRequestErrorCode(error));

            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(
                    GooglePlayServicesRewardedVideo.class,
                    getAdNetworkId(),
                    getMoPubRequestErrorCode(error));
        }
    };

    private RewardedAdCallback mRewardedAdCallback = new RewardedAdCallback() {
        @Override
        public void onRewardedAdOpened() {
            MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);

            MoPubRewardedVideoManager.onRewardedVideoStarted(
                    GooglePlayServicesRewardedVideo.class,
                    getAdNetworkId());
        }

        @Override
        public void onRewardedAdClosed() {
            MoPubLog.log(DID_DISAPPEAR, ADAPTER_NAME);

            MoPubRewardedVideoManager.onRewardedVideoClosed(
                    GooglePlayServicesRewardedVideo.class,
                    getAdNetworkId());
        }

        @Override
        public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
            MoPubLog.log(SHOULD_REWARD, ADAPTER_NAME,
                    rewardItem.getAmount(), rewardItem.getType());

            MoPubRewardedVideoManager.onRewardedVideoCompleted(
                    GooglePlayServicesRewardedVideo.class,
                    getAdNetworkId(),
                    MoPubReward.success(rewardItem.getType(), rewardItem.getAmount()));
        }

        @Override
        public void onRewardedAdFailedToShow(int error) {
            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME);

            MoPubRewardedVideoManager.onRewardedVideoPlaybackError(
                    GooglePlayServicesRewardedVideo.class,
                    getAdNetworkId(),
                    getMoPubShowErrorCode(error));
        }
    };

    /**
     * Converts a given Google Mobile Ads SDK Ad Request error code into {@link MoPubErrorCode}.
     *
     * @param error Google Mobile Ads SDK Ad Request error code.
     * @return an equivalent MoPub SDK error code for the given Google Mobile Ads SDK Ad Request
     * error code.
     */
    private MoPubErrorCode getMoPubRequestErrorCode(int error) {
        MoPubErrorCode errorCode;
        switch (error) {
            case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                errorCode = MoPubErrorCode.INTERNAL_ERROR;
                break;
            case AdRequest.ERROR_CODE_INVALID_REQUEST:
                errorCode = MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
                break;
            case AdRequest.ERROR_CODE_NETWORK_ERROR:
                errorCode = MoPubErrorCode.NO_CONNECTION;
                break;
            case AdRequest.ERROR_CODE_NO_FILL:
                errorCode = MoPubErrorCode.NO_FILL;
                break;
            default:
                errorCode = MoPubErrorCode.UNSPECIFIED;
        }
        return errorCode;
    }

    /**
     * Converts a given Google Mobile Ads SDK error code when showing Rewarded Video Ads into
     * {@link MoPubErrorCode}.
     *
     * @param error Google Mobile Ads SDK Ad Request error code when showing Rewarded Video Ads.
     * @return an equivalent MoPub SDK error code for the given Google Mobile Ads SDK Ad Request
     * error code thrown when showing Rewarded Video Ads.
     */
    private MoPubErrorCode getMoPubShowErrorCode(int error) {
        MoPubErrorCode errorCode;
        switch (error) {
            case RewardedAdCallback.ERROR_CODE_AD_REUSED:
                errorCode = MoPubErrorCode.INTERNAL_ERROR;
                break;
            case RewardedAdCallback.ERROR_CODE_APP_NOT_FOREGROUND:
                errorCode = MoPubErrorCode.VIDEO_PLAYBACK_ERROR;
                break;
            case RewardedAdCallback.ERROR_CODE_INTERNAL_ERROR:
                errorCode = MoPubErrorCode.INTERNAL_ERROR;
                break;
            case RewardedAdCallback.ERROR_CODE_NOT_READY:
                errorCode = MoPubErrorCode.WARMUP;
                break;
            default:
                errorCode = MoPubErrorCode.UNSPECIFIED;
        }
        return errorCode;
    }

    public static final class GooglePlayServicesMediationSettings implements MediationSettings {
        private static Bundle npaBundle;
        private static String contentUrl;
        private static String testDeviceId;
        private static Boolean taggedForChildDirectedTreatment = null;
        private static Boolean taggedForUnderAgeOfConsent = null;

        public GooglePlayServicesMediationSettings() {
        }

        public GooglePlayServicesMediationSettings(Bundle bundle) {
            npaBundle = bundle;
        }

        public GooglePlayServicesMediationSettings(Bundle bundle, String url) {
            npaBundle = bundle;
            contentUrl = url;
        }

        public GooglePlayServicesMediationSettings(Bundle bundle, String url, String id) {
            npaBundle = bundle;
            contentUrl = url;
            testDeviceId = id;
        }

        public GooglePlayServicesMediationSettings(Bundle bundle,
                                                   String url,
                                                   String id,
                                                   Boolean tagForChildDirectedTreatment) {
            npaBundle = bundle;
            contentUrl = url;
            testDeviceId = id;
            taggedForChildDirectedTreatment = tagForChildDirectedTreatment;
        }

        public void setNpaBundle(Bundle bundle) {
            npaBundle = bundle;
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

        /* The MoPub Android SDK queries MediationSettings from the rewarded video code
        (MoPubRewardedVideoManager.getGlobalMediationSettings). That API might not always be
        available to publishers importing the modularized SDK(s) based on select ad formats.
        This is a workaround to statically get the "npa" Bundle passed to us via the constructor. */
        private static Bundle getNpaBundle() {
            return npaBundle;
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

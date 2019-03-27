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
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.mopub.common.BaseLifecycleListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MediationSettings;
import com.mopub.common.MoPubReward;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOULD_REWARD;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;

public class GooglePlayServicesRewardedVideo extends CustomEventRewardedVideo implements
        RewardedVideoAdListener {

    /**
     * String to represent the simple class name to be used in log entries.
     */
    private static final String ADAPTER_NAME = GooglePlayServicesRewardedVideo.class.getSimpleName();

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
    @NonNull
    private String mAdUnitId = "";

    /**
     * The Google Rewarded Video Ad instance.
     */
    private RewardedVideoAd mRewardedVideoAd;

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

    /**
     * A {@link LifecycleListener} used to forward the activity lifecycle events from MoPub SDK to
     * Google Mobile Ads SDK.
     */
    private LifecycleListener mLifecycleListener = new BaseLifecycleListener() {
        @Override
        public void onPause(@NonNull Activity activity) {
            super.onPause(activity);
            if (mRewardedVideoAd != null) {
                mRewardedVideoAd.pause(activity);
            }
        }

        @Override
        public void onResume(@NonNull Activity activity) {
            super.onResume(activity);
            if (mRewardedVideoAd != null) {
                mRewardedVideoAd.resume(activity);
            }
        }
    };

    public GooglePlayServicesRewardedVideo() {
        sIsInitialized = new AtomicBoolean(false);
        mGooglePlayServicesAdapterConfiguration = new GooglePlayServicesAdapterConfiguration();
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return mLifecycleListener;
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
        if (mRewardedVideoAd != null) {
            mRewardedVideoAd.setRewardedVideoAdListener(null);
            mRewardedVideoAd = null;
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

            if (TextUtils.isEmpty(serverExtras.get(KEY_EXTRA_AD_UNIT_ID))) {
                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                        MoPubErrorCode.NETWORK_NO_FILL);

                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(
                        GooglePlayServicesRewardedVideo.class,
                        getAdNetworkId(),
                        MoPubErrorCode.NETWORK_NO_FILL);
                return false;
            }

            mAdUnitId = serverExtras.get(KEY_EXTRA_AD_UNIT_ID);

            mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(launcherActivity);
            mRewardedVideoAd.setRewardedVideoAdListener(GooglePlayServicesRewardedVideo.this);

            mGooglePlayServicesAdapterConfiguration.setCachedInitializationParameters(launcherActivity, serverExtras);
            return true;
        }

        return false;
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull final Activity activity,
                                          @NonNull Map<String, Object> localExtras,
                                          @NonNull final Map<String, String> serverExtras)
            throws Exception {

        /* AdMob's isLoaded() has to be called on the main thread to avoid multithreading crashes
        when mediating on Unity */
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                isAdLoaded = false;

                if (TextUtils.isEmpty(serverExtras.get(KEY_EXTRA_AD_UNIT_ID))) {
                    // Using class name as the network ID for this callback since the ad unit ID is
                    // invalid.
                    MoPubRewardedVideoManager.onRewardedVideoLoadFailure(
                            GooglePlayServicesRewardedVideo.class,
                            GooglePlayServicesRewardedVideo.class.getSimpleName(),
                            MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                    return;
                }
                mAdUnitId = serverExtras.get(KEY_EXTRA_AD_UNIT_ID);

                if (mRewardedVideoAd == null) {
                    mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(activity);
                    mRewardedVideoAd.setRewardedVideoAdListener(GooglePlayServicesRewardedVideo.this);
                }

                if (mRewardedVideoAd.isLoaded()) {
                    MoPubRewardedVideoManager
                            .onRewardedVideoLoadSuccess(GooglePlayServicesRewardedVideo.class, getAdNetworkId());
                } else {
                    AdRequest.Builder builder = new AdRequest.Builder();
                    builder.setRequestAgent("MoPub");

                    /* Publishers may append a content URL by passing it to the GooglePlayServicesMediationSettings
                    instance when initializing the MoPub SDK: https://developers.mopub.com/docs/mediation/networks/google/#android */
                    String contentUrl = GooglePlayServicesMediationSettings.getContentUrl();
                    if (!TextUtils.isEmpty(contentUrl)) {
                        builder.setContentUrl(contentUrl);
                    }

                    /* Publishers may request for test ads by passing test device IDs to the GooglePlayServicesMediationSettings
                    instance when initializing the MoPub SDK: https://developers.mopub.com/docs/mediation/networks/google/#android */
                    String testDeviceId = GooglePlayServicesMediationSettings.getTestDeviceId();
                    if (!TextUtils.isEmpty(testDeviceId)) {
                        builder.addTestDevice(testDeviceId);
                    }

                    // Consent collected from the MoPub’s consent dialogue should not be used to set up
                    // Google's personalization preference. Publishers should work with Google to be GDPR-compliant.
                    forwardNpaIfSet(builder);

                    AdRequest adRequest = builder.build();
                    mRewardedVideoAd.loadAd(mAdUnitId, adRequest);

                    MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
                }
            }
        });
    }

    private void forwardNpaIfSet(AdRequest.Builder builder) {

        // Only forward the "npa" bundle if it is explicitly set. Otherwise, don't attach it with the ad request.
        if (GooglePlayServicesMediationSettings.getNpaBundle() != null &&
                !GooglePlayServicesMediationSettings.getNpaBundle().isEmpty()) {
            builder.addNetworkExtrasBundle(AdMobAdapter.class, GooglePlayServicesMediationSettings.getNpaBundle());
        }
    }

    @Override
    protected boolean hasVideoAvailable() {
        return mRewardedVideoAd != null && isAdLoaded;
    }

    @Override
    protected void showVideo() {
        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);

        if (hasVideoAvailable()) {
            mRewardedVideoAd.show();
        } else {
            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            MoPubRewardedVideoManager.onRewardedVideoPlaybackError(
                    GooglePlayServicesRewardedVideo.class,
                    getAdNetworkId(),
                    getMoPubErrorCode(AdRequest.ERROR_CODE_NO_FILL));
        }
    }

    @Override
    public void onRewardedVideoAdLoaded() {

        MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(
                GooglePlayServicesRewardedVideo.class,
                getAdNetworkId());
        isAdLoaded = true;

        MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
    }

    @Override
    public void onRewardedVideoAdOpened() {
        // MoPub SDK does not have an equivalent callback for an ad opened event. Do nothing.
    }

    @Override
    public void onRewardedVideoStarted() {

        MoPubRewardedVideoManager.onRewardedVideoStarted(
                GooglePlayServicesRewardedVideo.class,
                getAdNetworkId());

        MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);
    }

    @Override
    public void onRewardedVideoAdClosed() {
        MoPubRewardedVideoManager.onRewardedVideoClosed(
                GooglePlayServicesRewardedVideo.class,
                getAdNetworkId());
    }

    @Override
    public void onRewardedVideoCompleted() {
    }

    @Override
    public void onRewarded(RewardItem rewardItem) {

        MoPubLog.log(SHOULD_REWARD, ADAPTER_NAME, rewardItem.getAmount(), rewardItem.getType());

        MoPubRewardedVideoManager.onRewardedVideoCompleted(
                GooglePlayServicesRewardedVideo.class,
                getAdNetworkId(),
                MoPubReward.success(rewardItem.getType(), rewardItem.getAmount()));
    }

    @Override
    public void onRewardedVideoAdLeftApplication() {
        MoPubRewardedVideoManager.onRewardedVideoClicked(
                GooglePlayServicesRewardedVideo.class,
                getAdNetworkId());
    }

    @Override
    public void onRewardedVideoAdFailedToLoad(int error) {
        MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                getMoPubErrorCode(error).getIntCode(),
                getMoPubErrorCode(error));

        MoPubRewardedVideoManager.onRewardedVideoLoadFailure(
                GooglePlayServicesRewardedVideo.class,
                getAdNetworkId(),
                getMoPubErrorCode(error));
    }

    /**
     * Converts a given Google Mobile Ads SDK error code into {@link MoPubErrorCode}.
     *
     * @param error Google Mobile Ads SDK error code.
     * @return an equivalent MoPub SDK error code for the given Google Mobile Ads SDK error
     * code.
     */
    private MoPubErrorCode getMoPubErrorCode(int error) {
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

    public static final class GooglePlayServicesMediationSettings implements MediationSettings {
        private static Bundle npaBundle;
        private static String contentUrl;
        private static String testDeviceId;

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

        public void setNpaBundle(Bundle bundle) {
            npaBundle = bundle;
        }

        public void setContentUrl(String url) {
            contentUrl = url;
        }

        public void setTestDeviceId(String id) {
            testDeviceId = id;
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
    }
}

package com.mopub.mobileads;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.mopub.common.BaseLifecycleListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubReward;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class GooglePlayServicesRewardedVideo extends CustomEventRewardedVideo implements
        RewardedVideoAdListener {
    private static final String TAG = "MoPubToAdMobRewarded";

    /**
     * The current version of the adapter.
     */
    private static final String ADAPTER_VERSION = "0.1.0";

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
    private String mAdUnitId;

    /**
     * The Google Rewarded Video Ad instance.
     */
    private RewardedVideoAd mRewardedVideoAd;

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
            Log.i(TAG, "Adapter version - " + ADAPTER_VERSION);

            if (TextUtils.isEmpty(serverExtras.get(KEY_EXTRA_APPLICATION_ID))) {
                MobileAds.initialize(launcherActivity);
            } else {
                MobileAds.initialize(launcherActivity, serverExtras.get(KEY_EXTRA_APPLICATION_ID));
            }

            if (TextUtils.isEmpty(serverExtras.get(KEY_EXTRA_AD_UNIT_ID))) {
                // Using class name as the network ID for this callback since the ad unit ID is
                // invalid.
                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(
                        GooglePlayServicesRewardedVideo.class,
                        GooglePlayServicesRewardedVideo.class.getSimpleName(),
                        MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                return false;
            }

            mAdUnitId = serverExtras.get(KEY_EXTRA_AD_UNIT_ID);

            mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(launcherActivity);
            mRewardedVideoAd.setRewardedVideoAdListener(GooglePlayServicesRewardedVideo.this);
            return true;
        }

        return false;
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity,
                                          @NonNull Map<String, Object> localExtras,
                                          @NonNull Map<String, String> serverExtras)
            throws Exception {
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

        final Map<String, Object> localExtrasMap = localExtras;

        if (mRewardedVideoAd == null) {
            mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(activity);
            mRewardedVideoAd.setRewardedVideoAdListener(GooglePlayServicesRewardedVideo.this);
        }

        /* AdMob's isLoaded() has to be called on the main thread to avoid multithreading crashes
        when mediating on Unity */
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (mRewardedVideoAd.isLoaded()) {
                    MoPubRewardedVideoManager
                            .onRewardedVideoLoadSuccess(GooglePlayServicesRewardedVideo.class, mAdUnitId);
                } else {
                    mRewardedVideoAd
                            .loadAd(mAdUnitId, new AdRequest.Builder()
                                    // Consent collected from the MoPub’s consent dialogue should not be used to set up
                                    // Google's personalization preference. Publishers should work with Google to be GDPR-compliant.
                                    .addNetworkExtrasBundle(AdMobAdapter.class, getGooglePersonalizationPreference(localExtrasMap))
                                    .setRequestAgent("MoPub")
                                    .build());
                }
            }
        });
    }

    @Override
    protected boolean hasVideoAvailable() {
        return mRewardedVideoAd != null && mRewardedVideoAd.isLoaded();
    }

    @Override
    protected void showVideo() {
        if (hasVideoAvailable()) {
            mRewardedVideoAd.show();
        } else {
            MoPubRewardedVideoManager.onRewardedVideoPlaybackError(
                    GooglePlayServicesRewardedVideo.class,
                    mAdUnitId,
                    getMoPubErrorCode(AdRequest.ERROR_CODE_INTERNAL_ERROR));
        }
    }

    @Override
    public void onRewardedVideoAdLoaded() {
        MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(
                GooglePlayServicesRewardedVideo.class,
                mAdUnitId);
    }

    @Override
    public void onRewardedVideoAdOpened() {
        // MoPub SDK does not have an equivalent callback for an ad opened event. Do nothing.
    }

    @Override
    public void onRewardedVideoStarted() {
        MoPubRewardedVideoManager.onRewardedVideoStarted(
                GooglePlayServicesRewardedVideo.class,
                mAdUnitId);
    }

    @Override
    public void onRewardedVideoAdClosed() {
        MoPubRewardedVideoManager.onRewardedVideoClosed(
                GooglePlayServicesRewardedVideo.class,
                mAdUnitId);
    }

    @Override
    public void onRewardedVideoCompleted() {
        // Already notifying MoPub of playback completion in onRewarded(). Do nothing.
    }

    @Override
    public void onRewarded(RewardItem rewardItem) {
        MoPubRewardedVideoManager.onRewardedVideoCompleted(
                GooglePlayServicesRewardedVideo.class,
                mAdUnitId,
                MoPubReward.success(rewardItem.getType(), rewardItem.getAmount()));
    }

    @Override
    public void onRewardedVideoAdLeftApplication() {
        MoPubRewardedVideoManager.onRewardedVideoClicked(
                GooglePlayServicesRewardedVideo.class,
                mAdUnitId);
    }

    @Override
    public void onRewardedVideoAdFailedToLoad(int error) {
        MoPubRewardedVideoManager.onRewardedVideoLoadFailure(
                GooglePlayServicesRewardedVideo.class,
                mAdUnitId,
                getMoPubErrorCode(error));
    }

    private Bundle getGooglePersonalizationPreference(Map<String, Object> localExtras) {
        Bundle extras = new Bundle();
        if (localExtras.get("npa") != null) {
            String personalizationPref = localExtras.get("npa").toString();
            if (!TextUtils.isEmpty(personalizationPref)) {
                extras.putString("npa", personalizationPref);
            }
        }
        return extras;
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
}

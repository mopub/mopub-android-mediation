package com.mopub.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.chartboost.sdk.Chartboost;
import com.chartboost.sdk.ChartboostDelegate;
import com.chartboost.sdk.Model.CBError;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.privacy.ConsentStatus;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.mobileads.CustomEventInterstitial.CustomEventInterstitialListener;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOULD_REWARD;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.VIDEO_DOWNLOAD_ERROR;

public class ChartboostShared {
    private static volatile ChartboostSingletonDelegate sDelegate = new ChartboostSingletonDelegate();

    /*
     * These keys are intended for MoPub internal use. Do not modify.
     */
    public static final String LOCATION_KEY = "location";
    public static final String LOCATION_DEFAULT = "Default";

    private static final String APP_ID_KEY = "appId";
    private static final String APP_SIGNATURE_KEY = "appSignature";
    private static final String ADAPTER_NAME = ChartboostShared.class.getSimpleName();

    @Nullable
    private static String mAppId;
    @Nullable
    private static String mAppSignature;

    /**
     * Initialize the Chartboost SDK for the provided application id and app signature.
     */
    public static synchronized boolean initializeSdk(@NonNull Activity launcherActivity,
                                                     @NonNull Map<String, String> serverExtras) {
        Preconditions.checkNotNull(launcherActivity);
        Preconditions.checkNotNull(serverExtras);

        // Pass the user consent from the MoPub SDK to Chartboost as per GDPR
        PersonalInfoManager personalInfoManager = MoPub.getPersonalInformationManager();

        final boolean canCollectPersonalInfo = MoPub.canCollectPersonalInformation();
        final boolean shouldAllowLegitimateInterest = MoPub.shouldAllowLegitimateInterest();

        if (personalInfoManager != null && personalInfoManager.gdprApplies() == Boolean.TRUE) {

            if (shouldAllowLegitimateInterest) {
                if (personalInfoManager.getPersonalInfoConsentStatus() == ConsentStatus.EXPLICIT_NO
                        || personalInfoManager.getPersonalInfoConsentStatus() == ConsentStatus.DNT) {
                    Chartboost.setPIDataUseConsent(launcherActivity.getApplicationContext(),
                            Chartboost.CBPIDataUseConsent.NO_BEHAVIORAL);
                } else {
                    Chartboost.setPIDataUseConsent(launcherActivity.getApplicationContext(),
                            Chartboost.CBPIDataUseConsent.YES_BEHAVIORAL);
                }
            } else {
                Chartboost.setPIDataUseConsent(launcherActivity.getApplicationContext(),
                        canCollectPersonalInfo ? Chartboost.CBPIDataUseConsent.YES_BEHAVIORAL :
                                Chartboost.CBPIDataUseConsent.NO_BEHAVIORAL);
            }
        }

        // Validate Chartboost args
        if (!serverExtras.containsKey(APP_ID_KEY)) {
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            throw new IllegalStateException("Chartboost rewarded video initialization" +
                    " failed due to missing application ID.");
        }

        if (!serverExtras.containsKey(APP_SIGNATURE_KEY)) {
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            throw new IllegalStateException("Chartboost rewarded video initialization" +
                    " failed due to missing application signature.");
        }

        final String appId = serverExtras.get(APP_ID_KEY);
        final String appSignature = serverExtras.get(APP_SIGNATURE_KEY);

        if (!TextUtils.isEmpty(appId) && !TextUtils.isEmpty(appSignature)) {
            if (appId.equals(mAppId) && appSignature.equals(mAppSignature)) {
                // We don't need to reinitialize.
                return false;
            }
        }

        mAppId = appId;
        mAppSignature = appSignature;

        // Perform all the common SDK initialization steps including startAppWithId
        Chartboost.startWithAppId(launcherActivity, mAppId, mAppSignature);
        Chartboost.setMediation(Chartboost.CBMediation.CBMediationMoPub, MoPub.SDK_VERSION);
        Chartboost.setDelegate(sDelegate);
        Chartboost.setShouldRequestInterstitialsInFirstSession(true);
        Chartboost.setAutoCacheAds(false);
        Chartboost.setShouldDisplayLoadingViewForMoreApps(false);

        // Callers of this method need to call onCreate & onStart themselves.
        return true;
    }

    @NonNull
    public static ChartboostSingletonDelegate getDelegate() {
        return sDelegate;
    }

    /**
     * A {@link ChartboostDelegate} that can forward events for Chartboost interstitials
     * and rewarded videos to the appropriate listener based on the Chartboost location used.
     */
    public static class ChartboostSingletonDelegate extends ChartboostDelegate
            implements CustomEventRewardedVideo.CustomEventRewardedVideoListener {
        private static final CustomEventInterstitialListener NULL_LISTENER =
                new CustomEventInterstitialListener() {
                    @Override
                    public void onInterstitialLoaded() {
                    }

                    @Override
                    public void onInterstitialFailed(MoPubErrorCode errorCode) {
                    }

                    @Override
                    public void onInterstitialShown() {
                    }

                    @Override
                    public void onInterstitialClicked() {
                    }

                    @Override
                    public void onInterstitialImpression() {
                    }

                    @Override
                    public void onLeaveApplication() {
                    }

                    @Override
                    public void onInterstitialDismissed() {
                    }
                };

        //***************
        // Chartboost Location Management for interstitials and rewarded videos
        //***************

        private Map<String, CustomEventInterstitialListener> mInterstitialListenersForLocation
                = Collections.synchronizedMap(new TreeMap<String, CustomEventInterstitialListener>());

        private Set<String> mRewardedVideoLocationsToLoad = Collections.synchronizedSet(new TreeSet<String>());

        public void registerInterstitialListener(@NonNull String location,
                                                 @NonNull CustomEventInterstitialListener interstitialListener) {
            Preconditions.checkNotNull(location);
            Preconditions.checkNotNull(interstitialListener);
            mInterstitialListenersForLocation.put(location, interstitialListener);
        }

        public void unregisterInterstitialListener(@NonNull String location) {
            Preconditions.checkNotNull(location);
            mInterstitialListenersForLocation.remove(location);
        }

        public void registerRewardedVideoLocation(@NonNull String location) {
            Preconditions.checkNotNull(location);
            mRewardedVideoLocationsToLoad.add(location);
        }

        public void unregisterRewardedVideoLocation(@NonNull String location) {
            Preconditions.checkNotNull(location);
            mRewardedVideoLocationsToLoad.remove(location);
        }

        @NonNull
        public CustomEventInterstitialListener getInterstitialListener(@NonNull String location) {
            final CustomEventInterstitialListener listener = mInterstitialListenersForLocation.get(location);
            return listener != null ? listener : NULL_LISTENER;
        }

        public boolean hasInterstitialLocation(@NonNull String location) {
            return mInterstitialListenersForLocation.containsKey(location);
        }

        //******************
        // Chartboost Delegate methods.
        //******************

        //******************
        // Interstitials
        //******************
        @Override
        public void didCacheInterstitial(String location) {
            getInterstitialListener(location).onInterstitialLoaded();
            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);

        }

        @Override
        public void didFailToLoadInterstitial(String location, CBError.CBImpressionError error) {
            String suffix = error != null ? "Error: " + error.name() : "";
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Chartboost interstitial ad failed to load." + suffix);

            getInterstitialListener(location).onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);

            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);
        }

        @Override
        public void didDismissInterstitial(String location) {
            // Note that this method is fired before didCloseInterstitial and didClickInterstitial.
            getInterstitialListener(location).onInterstitialDismissed();
        }

        @Override
        public void didCloseInterstitial(String location) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Chartboost interstitial ad closed.");
        }

        @Override
        public void didClickInterstitial(String location) {
            getInterstitialListener(location).onInterstitialClicked();

            MoPubLog.log(CLICKED, ADAPTER_NAME);
        }

        @Override
        public void didDisplayInterstitial(String location) {
            getInterstitialListener(location).onInterstitialShown();

            MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);
        }

        //******************
        // Rewarded Videos
        //******************
        @Override
        public void didCacheRewardedVideo(String location) {
            super.didCacheRewardedVideo(location);

            if (mRewardedVideoLocationsToLoad.contains(location)) {

                MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(ChartboostRewardedVideo.class, location);
                mRewardedVideoLocationsToLoad.remove(location);

                MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Chartboost rewarded video cached for location " +
                        location + ".");
            }
        }

        @Override
        public void didFailToLoadRewardedVideo(String location, CBError.CBImpressionError error) {
            super.didFailToLoadRewardedVideo(location, error);
            String suffix = error != null ? " with error: " + error.name() : "";
            if (mRewardedVideoLocationsToLoad.contains(location)) {
                MoPubErrorCode errorCode = VIDEO_DOWNLOAD_ERROR;

                if (CBError.CBImpressionError.INVALID_LOCATION.equals(error)) {
                    errorCode = ADAPTER_CONFIGURATION_ERROR;
                }
                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(ChartboostRewardedVideo.class, location, errorCode);
                mRewardedVideoLocationsToLoad.remove(location);

                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                        errorCode.getIntCode(),
                        errorCode);
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Chartboost rewarded video cache failed for location " +
                        location + suffix);
            }
        }

        @Override
        public void didDismissRewardedVideo(String location) {
            // This is called before didCloseRewardedVideo and didClickRewardedVideo
            super.didDismissRewardedVideo(location);
            MoPubRewardedVideoManager.onRewardedVideoClosed(ChartboostRewardedVideo.class, location);

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Chartboost rewarded video dismissed for location " +
                    location + ".");
        }

        @Override
        public void didCloseRewardedVideo(String location) {
            super.didCloseRewardedVideo(location);

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Chartboost rewarded video closed for location " +
                    location + ".");
        }

        @Override
        public void didClickRewardedVideo(String location) {
            super.didClickRewardedVideo(location);

            MoPubRewardedVideoManager.onRewardedVideoClicked(ChartboostRewardedVideo.class, location);

            MoPubLog.log(CLICKED, ADAPTER_NAME);
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Chartboost rewarded video clicked for location " +
                    location + ".");
        }

        @Override
        public void didCompleteRewardedVideo(String location, int reward) {
            super.didCompleteRewardedVideo(location, reward);

            MoPubLog.log(SHOULD_REWARD, ADAPTER_NAME, reward, location);

            MoPubRewardedVideoManager.onRewardedVideoCompleted(
                    ChartboostRewardedVideo.class,
                    location,
                    MoPubReward.success(MoPubReward.NO_REWARD_LABEL, reward));

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Chartboost rewarded video completed for location " +
                    location + " with " + "reward amount " + reward);
        }

        @Override
        public void didDisplayRewardedVideo(String location) {
            super.didDisplayRewardedVideo(location);

            MoPubRewardedVideoManager.onRewardedVideoStarted(ChartboostRewardedVideo.class, location);

            MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Chartboost rewarded video displayed for location " +
                    location + ".");
        }

        //******************
        // More Apps
        //******************
        @Override
        public boolean shouldRequestMoreApps(String location) {
            return false;
        }

        @Override
        public boolean shouldDisplayMoreApps(final String location) {
            return false;
        }
    }


    @VisibleForTesting
    @Deprecated
    static void reset() {
        // Clears all the locations to load and other state.
        sDelegate = new ChartboostSingletonDelegate();
        mAppId = null;
        mAppSignature = null;
    }
}

package com.mopub.mobileads;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chartboost.sdk.Chartboost;
import com.chartboost.sdk.ChartboostDelegate;
import com.chartboost.sdk.Model.CBError;
import com.chartboost.sdk.Privacy.model.GDPR;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.privacy.ConsentStatus;
import com.mopub.common.privacy.PersonalInfoManager;

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
import static com.mopub.mobileads.MoPubErrorCode.CANCELLED;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_NO_FILL;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_TIMEOUT;
import static com.mopub.mobileads.MoPubErrorCode.NO_CONNECTION;
import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;
import static com.mopub.mobileads.MoPubErrorCode.VIDEO_NOT_AVAILABLE;
import static com.mopub.mobileads.MoPubErrorCode.VIDEO_PLAYBACK_ERROR;

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
    public static synchronized boolean initializeSdk(@NonNull Context context,
                                                     @NonNull Map<String, String> serverExtras) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(serverExtras);

        // Pass the user consent from the MoPub SDK to Chartboost as per GDPR
        PersonalInfoManager personalInfoManager = MoPub.getPersonalInformationManager();

        final boolean canCollectPersonalInfo = MoPub.canCollectPersonalInformation();
        final boolean shouldAllowLegitimateInterest = MoPub.shouldAllowLegitimateInterest();

        if (personalInfoManager != null && personalInfoManager.gdprApplies() == Boolean.TRUE) {
            if (shouldAllowLegitimateInterest) {
                boolean isExplicitNoConsent = personalInfoManager.getPersonalInfoConsentStatus() == ConsentStatus.EXPLICIT_NO
                        || personalInfoManager.getPersonalInfoConsentStatus() == ConsentStatus.DNT;
                addChartboostPrivacyConsent(context, !isExplicitNoConsent);
            } else {
                addChartboostPrivacyConsent(context, canCollectPersonalInfo);
            }
        }

        // Validate Chartboost args
        if (!serverExtras.containsKey(APP_ID_KEY)) {
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            throw new IllegalStateException("Chartboost rewarded video initialization" +
                    " failed due to missing application ID.");
        }

        if (!serverExtras.containsKey(APP_SIGNATURE_KEY)) {
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

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
        Chartboost.startWithAppId(context, mAppId, mAppSignature);
        Chartboost.setMediation(Chartboost.CBMediation.CBMediationMoPub, MoPub.SDK_VERSION,
                new ChartboostAdapterConfiguration().getAdapterVersion());
        Chartboost.setDelegate(sDelegate);
        Chartboost.setAutoCacheAds(false);
        return true;
    }

    private static void addChartboostPrivacyConsent(Context context, boolean canCollectPersonalInfo) {
        if (context == null) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Skipped setting Chartboost Privacy consent " +
                    "as context is null.");
            return;
        }

        if (canCollectPersonalInfo) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Setting Chartboost GDPR data use consent as BEHAVIORAL");
            Chartboost.addDataUseConsent(context, new GDPR(GDPR.GDPR_CONSENT.BEHAVIORAL));
        } else {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Setting Chartboost GDPR data use consent as NON_BEHAVIORAL");
            Chartboost.addDataUseConsent(context, new GDPR(GDPR.GDPR_CONSENT.NON_BEHAVIORAL));
        }
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
            implements AdLifecycleListener.LoadListener, AdLifecycleListener.InteractionListener {

        private static final AdLifecycleListener.LoadListener NULL_LOAD_LISTENER = new AdLifecycleListener.LoadListener() {
            @Override
            public void onAdLoaded() {
            }

            @Override
            public void onAdLoadFailed(MoPubErrorCode errorCode) {
            }
        };

        private static final AdLifecycleListener.InteractionListener NULL_INTERACTION_LISTENER = new AdLifecycleListener.InteractionListener() {
            @Override
            public void onAdFailed(MoPubErrorCode errorCode) {
            }

            @Override
            public void onAdShown() {
            }

            @Override
            public void onAdClicked() {
            }

            @Override
            public void onAdImpression() {
            }

            @Override
            public void onAdDismissed() {
            }

            @Override
            public void onAdComplete(MoPubReward moPubReward) {
            }

            @Override
            public void onAdCollapsed() {
            }

            @Override
            public void onAdExpanded() {
            }

            @Override
            public void onAdPauseAutoRefresh() {
            }

            @Override
            public void onAdResumeAutoRefresh() {
            }
        };

        @Override
        public void onAdLoaded() {

        }

        @Override
        public void onAdLoadFailed(MoPubErrorCode errorCode) {

        }

        @Override
        public void onAdFailed(MoPubErrorCode errorCode) {

        }

        @Override
        public void onAdShown() {

        }

        @Override
        public void onAdClicked() {

        }

        @Override
        public void onAdImpression() {

        }

        //***************
        // Chartboost Location Management for interstitials and rewarded videos
        //***************

        private static Map<String, AdLifecycleListener.LoadListener> mLoadListenersForLocation
                = Collections.synchronizedMap(new TreeMap<String, AdLifecycleListener.LoadListener>());

        private static Map<String, AdLifecycleListener.InteractionListener> mInteractionListenersForLocation
                = Collections.synchronizedMap(new TreeMap<String, AdLifecycleListener.InteractionListener>());

        private Set<String> mRewardedVideoLocationsToLoad = Collections.synchronizedSet(new TreeSet<String>());

        public void registerLoadListener(@NonNull String location, @NonNull AdLifecycleListener.LoadListener loadListener) {
            Preconditions.checkNotNull(location);
            Preconditions.checkNotNull(loadListener);
            mLoadListenersForLocation.put(location, loadListener);
        }

        public void registerInteractionListener(@NonNull String location, @NonNull AdLifecycleListener.InteractionListener interactionListener) {
            Preconditions.checkNotNull(location);
            Preconditions.checkNotNull(interactionListener);
            mInteractionListenersForLocation.put(location, interactionListener);
        }

        public void unregisterLoadListener(@NonNull String location) {
            Preconditions.checkNotNull(location);
            mLoadListenersForLocation.remove(location);
        }

        public void unregisterInteractionListener(@NonNull String location) {
            Preconditions.checkNotNull(location);
            mInteractionListenersForLocation.remove(location);
        }

        public void registerRewardedVideoLocation(@NonNull String location) {
            Preconditions.checkNotNull(location);
            mRewardedVideoLocationsToLoad.add(location);
        }

        public void unregisterRewardedVideoLocation(@NonNull String location) {
            Preconditions.checkNotNull(location);
            mRewardedVideoLocationsToLoad.remove(location);
        }

        private void invalidateLocation(String location) {
            if (!TextUtils.isEmpty(location)) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Invalidating listeners for location: " + location);
                
                unregisterLoadListener(location);
                unregisterInteractionListener(location);
            }
        }

        @NonNull
        public AdLifecycleListener.LoadListener getLoadListener(@NonNull String location) {
            final AdLifecycleListener.LoadListener listener = mLoadListenersForLocation.get(location);
            return listener != null ? listener : NULL_LOAD_LISTENER;
        }

        @NonNull
        public AdLifecycleListener.InteractionListener getInteractionListener(@NonNull String location) {
            final AdLifecycleListener.InteractionListener listener = mInteractionListenersForLocation.get(location);
            return listener != null ? listener : NULL_INTERACTION_LISTENER;
        }

        public boolean hasLoadLocation(@NonNull String location) {
            return mLoadListenersForLocation.containsKey(location);
        }

        public boolean hasInteractionlLocation(@NonNull String location) {
            return mInteractionListenersForLocation.containsKey(location);
        }

        //******************
        // Chartboost Delegate methods.
        //******************

        //******************
        // Interstitials
        //******************
        @Override
        public void didCacheInterstitial(String location) {
            getLoadListener(location).onAdLoaded();
            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
        }

        @Override
        public void didFailToLoadInterstitial(String location, CBError.CBImpressionError error) {
            String suffix = error != null ? "Error: " + error.name() : "";
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Chartboost interstitial ad failed to load." + suffix);

            MoPubErrorCode errorCode = null;

            if (error != null) {
                switch (error) {
                    case INTERNAL:
                        errorCode = NETWORK_INVALID_STATE;
                        break;
                    case INTERNET_UNAVAILABLE:
                        errorCode = NO_CONNECTION;
                        break;
                    case TOO_MANY_CONNECTIONS:
                        errorCode = CANCELLED;
                        break;
                    case NETWORK_FAILURE:
                        errorCode = NETWORK_TIMEOUT;
                        break;
                    case NO_AD_FOUND:
                        errorCode = NETWORK_NO_FILL;
                        break;
                    case VIDEO_UNAVAILABLE:
                        errorCode = VIDEO_NOT_AVAILABLE;
                        break;
                    case ERROR_PLAYING_VIDEO:
                        errorCode = VIDEO_PLAYBACK_ERROR;
                        break;
                    default:
                        errorCode = UNSPECIFIED;
                }

                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, errorCode.getIntCode(), errorCode);
            }

            getLoadListener(location).onAdLoadFailed(errorCode);
            invalidateLocation(location);
        }

        @Override
        public void didDismissInterstitial(String location) {
            // Note that this method is fired before didCloseInterstitial and didClickInterstitial.
            getInteractionListener(location).onAdDismissed();
            invalidateLocation(location);
        }

        @Override
        public void didCloseInterstitial(String location) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Chartboost interstitial ad closed.");
        }

        @Override
        public void didClickInterstitial(String location) {
            getInteractionListener(location).onAdClicked();

            MoPubLog.log(CLICKED, ADAPTER_NAME);
        }

        @Override
        public void didDisplayInterstitial(String location) {
            getInteractionListener(location).onAdShown();
            getInteractionListener(location).onAdImpression();

            MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);
        }

        //******************
        // Rewarded Videos
        //******************
        @Override
        public void didCacheRewardedVideo(String location) {
            super.didCacheRewardedVideo(location);
            if (mRewardedVideoLocationsToLoad.contains(location)) {
                mRewardedVideoLocationsToLoad.remove(location);

                MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Chartboost rewarded video cached for location " +
                        location + ".");

                getLoadListener(location).onAdLoaded();
            }
        }

        @Override
        public void didFailToLoadRewardedVideo(String location, CBError.CBImpressionError error) {
            super.didFailToLoadRewardedVideo(location, error);
            String suffix = error != null ? " with error: " + error.name() : "";
            if (mRewardedVideoLocationsToLoad.contains(location)) {

                MoPubErrorCode errorCode = null;

                if (error != null) {
                    switch (error) {
                        case INTERNAL:
                            errorCode = NETWORK_INVALID_STATE;
                            break;
                        case INTERNET_UNAVAILABLE:
                            errorCode = NO_CONNECTION;
                            break;
                        case TOO_MANY_CONNECTIONS:
                            errorCode = CANCELLED;
                            break;
                        case NETWORK_FAILURE:
                            errorCode = NETWORK_TIMEOUT;
                            break;
                        case NO_AD_FOUND:
                            errorCode = NETWORK_NO_FILL;
                            break;
                        case VIDEO_UNAVAILABLE:
                            errorCode = VIDEO_NOT_AVAILABLE;
                            break;
                        case ERROR_PLAYING_VIDEO:
                            errorCode = VIDEO_PLAYBACK_ERROR;
                            break;
                        default:
                            errorCode = UNSPECIFIED;
                    }

                    MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, errorCode.getIntCode(), errorCode);
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "Chartboost rewarded video cache " +
                            "failed for location " + location + suffix);
                }
                mRewardedVideoLocationsToLoad.remove(location);

                getLoadListener(location).onAdLoadFailed(errorCode);
            }
        }

        @Override
        public void didDismissRewardedVideo(String location) {
            // This is called before didCloseRewardedVideo and didClickRewardedVideo
            super.didDismissRewardedVideo(location);

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Chartboost rewarded video dismissed for location " +
                    location + ".");

            getInteractionListener(location).onAdDismissed();
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

            MoPubLog.log(CLICKED, ADAPTER_NAME);
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Chartboost rewarded video clicked for location " +
                    location + ".");

            getInteractionListener(location).onAdClicked();
        }

        @Override
        public void didCompleteRewardedVideo(String location, int reward) {
            super.didCompleteRewardedVideo(location, reward);

            MoPubLog.log(SHOULD_REWARD, ADAPTER_NAME, reward, location);
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Chartboost rewarded video completed for location " +
                    location + " with " + "reward amount " + reward);

            getInteractionListener(location).onAdComplete(MoPubReward.success(MoPubReward.NO_REWARD_LABEL, reward));
        }

        @Override
        public void didDisplayRewardedVideo(String location) {
            super.didDisplayRewardedVideo(location);

            MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Chartboost rewarded video displayed for location " +
                    location + ".");

            getInteractionListener(location).onAdShown();
            getInteractionListener(location).onAdImpression();
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

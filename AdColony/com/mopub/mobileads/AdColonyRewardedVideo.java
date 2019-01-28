package com.mopub.mobileads;

import android.app.Activity;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.logging.MoPubLog;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOULD_REWARD;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAdOptions;
import com.adcolony.sdk.AdColonyAppOptions;
import com.adcolony.sdk.AdColonyInterstitial;
import com.adcolony.sdk.AdColonyInterstitialListener;
import com.adcolony.sdk.AdColonyReward;
import com.adcolony.sdk.AdColonyRewardListener;
import com.adcolony.sdk.AdColonyZone;

import com.mopub.common.BaseLifecycleListener;
import com.mopub.common.DataKeys;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MediationSettings;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.common.privacy.ConsentStatus;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.common.util.Json;

import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AdColonyRewardedVideo extends CustomEventRewardedVideo {
    /*
     * We recommend passing the AdColony client options, app ID, all zone IDs, and current zone ID
     * in the serverExtras Map by specifying Custom Event Data in MoPub's web interface.
     *
     * Please see AdColony's documentation for more information:
     * https://github.com/AdColony/AdColony-Android-SDK-3
     */
    private static final String DEFAULT_CLIENT_OPTIONS = "version=YOUR_APP_VERSION_HERE,store:google";
    private static final String DEFAULT_APP_ID = "YOUR_AD_COLONY_APP_ID_HERE";
    private static final String[] DEFAULT_ALL_ZONE_IDS = {"ZONE_ID_1", "ZONE_ID_2", "..."};
    private static final String DEFAULT_ZONE_ID = "YOUR_CURRENT_ZONE_ID";
    private static final String CONSENT_RESPONSE = "consent_response";
    private static final String CONSENT_GIVEN = "explicit_consent_given";

    /*
     * These keys are intended for MoPub internal use. Do not modify.
     */
    public static final String CLIENT_OPTIONS_KEY = "clientOptions";
    public static final String APP_ID_KEY = "appId";
    public static final String ALL_ZONE_IDS_KEY = "allZoneIds";
    public static final String ZONE_ID_KEY = "zoneId";
    public static final String ADAPTER_NAME = AdColonyRewardedVideo.class.getSimpleName();

    private static boolean sInitialized = false;
    private static LifecycleListener sLifecycleListener = new BaseLifecycleListener();
    private static String[] previousAdColonyAllZoneIds;
    @NonNull
    private AdColonyAdapterConfiguration mAdColonyAdapterConfiguration;

    private AdColonyInterstitial mAd;
    @NonNull
    private String mZoneId = DEFAULT_ZONE_ID;
    private AdColonyListener mAdColonyListener;
    private AdColonyAdOptions mAdColonyAdOptions = new AdColonyAdOptions();
    private AdColonyAppOptions mAdColonyAppOptions = new AdColonyAppOptions();
    private static WeakHashMap<String, AdColonyInterstitial> sZoneIdToAdMap = new WeakHashMap<>();
    @NonNull
    private String mAdUnitId = "";
    private boolean mIsLoading = false;

    // For waiting and notifying the SDK:
    private final Handler mHandler;
    private final ScheduledThreadPoolExecutor mScheduledThreadPoolExecutor;

    public AdColonyRewardedVideo() {
        mScheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
        mHandler = new Handler();
        mAdColonyAdapterConfiguration = new AdColonyAdapterConfiguration();
    }

    @Nullable
    @Override
    public CustomEventRewardedVideoListener getVideoListenerForSdk() {
        return mAdColonyListener;
    }

    @Nullable
    @Override
    public LifecycleListener getLifecycleListener() {
        return sLifecycleListener;
    }

    @NonNull
    @Override
    public String getAdNetworkId() {
        return mZoneId;
    }

    @Override
    protected void onInvalidate() {
        mScheduledThreadPoolExecutor.shutdownNow();
        AdColonyInterstitial ad = sZoneIdToAdMap.get(mZoneId);
        if (ad != null) {
            ad.destroy();
            sZoneIdToAdMap.remove(mZoneId);
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "AdColony rewarded video destroyed");
        }
    }

    @Override
    public boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
                                         @NonNull final Map<String, Object> localExtras,
                                         @NonNull final Map<String, String> serverExtras) throws Exception {
        synchronized (AdColonyRewardedVideo.class) {
            if (sInitialized) {
                return false;
            }

            String adColonyClientOptions = DEFAULT_CLIENT_OPTIONS;
            String adColonyAppId = DEFAULT_APP_ID;
            String[] adColonyAllZoneIds = DEFAULT_ALL_ZONE_IDS;

            // Set up serverExtras
            if (extrasAreValid(serverExtras)) {
                adColonyClientOptions = serverExtras.get(CLIENT_OPTIONS_KEY);
                adColonyAppId = serverExtras.get(APP_ID_KEY);
                adColonyAllZoneIds = extractAllZoneIds(serverExtras);
            }

            if (!TextUtils.isEmpty(adColonyClientOptions)) {
                mAdColonyAppOptions = AdColonyAppOptions.getMoPubAppOptions(adColonyClientOptions);
            }

            if (!isAdColonyConfigured() && !TextUtils.isEmpty(adColonyAppId)) {
                previousAdColonyAllZoneIds = adColonyAllZoneIds;
                AdColony.configure(launcherActivity, mAdColonyAppOptions, adColonyAppId, adColonyAllZoneIds);
            }

            sInitialized = true;
            return true;
        }
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull final Activity activity,
                                          @NonNull final Map<String, Object> localExtras,
                                          @NonNull final Map<String, String> serverExtras) throws Exception {

        if (extrasAreValid(serverExtras)) {
            mAdColonyAdapterConfiguration.setCachedInitializationParameters(activity, serverExtras);
            mZoneId = serverExtras.get(ZONE_ID_KEY);
            String adColonyAppId = serverExtras.get(APP_ID_KEY);
            String[] adColonyAllZoneIds = extractAllZoneIds(serverExtras);

            // Pass the user consent from the MoPub SDK to AdColony as per GDPR
            PersonalInfoManager personalInfoManager = MoPub.getPersonalInformationManager();

            boolean canCollectPersonalInfo = MoPub.canCollectPersonalInformation();
            boolean shouldAllowLegitimateInterest = MoPub.shouldAllowLegitimateInterest();

            mAdColonyAppOptions = mAdColonyAppOptions == null ? new AdColonyAppOptions() :
                    mAdColonyAppOptions;

            if (personalInfoManager != null && personalInfoManager.gdprApplies() == Boolean.TRUE) {
                if (shouldAllowLegitimateInterest) {
                    if (personalInfoManager.getPersonalInfoConsentStatus() == ConsentStatus.EXPLICIT_NO
                            || personalInfoManager.getPersonalInfoConsentStatus() == ConsentStatus.DNT) {
                        mAdColonyAppOptions.setOption(CONSENT_GIVEN, true)
                                .setOption(CONSENT_RESPONSE, false);
                    } else {
                        mAdColonyAppOptions.setOption(CONSENT_GIVEN, true)
                                .setOption(CONSENT_RESPONSE, true);
                    }
                } else {
                    mAdColonyAppOptions.setOption(CONSENT_GIVEN, true)
                            .setOption(CONSENT_RESPONSE, canCollectPersonalInfo);
                }
            }

            setUpGlobalSettings();

            // Need to check the zone IDs sent from the MoPub portal and reconfigure if they are
            // different than the zones we initially called AdColony.configure() with
            if (shouldReconfigure(previousAdColonyAllZoneIds, adColonyAllZoneIds)) {
                if (!TextUtils.isEmpty(adColonyAppId)) {
                    AdColony.configure(activity, mAdColonyAppOptions, adColonyAppId, adColonyAllZoneIds);
                }
                previousAdColonyAllZoneIds = adColonyAllZoneIds;
            } else {
                // If we aren't reconfiguring we should update the app options via setAppOptions() in case
                // consent has changed since the last adapter initialization.
                AdColony.setAppOptions(mAdColonyAppOptions);
            }
        }

        Object adUnitObject = localExtras.get(DataKeys.AD_UNIT_ID_KEY);
        if (adUnitObject instanceof String) {
            mAdUnitId = (String) adUnitObject;
        }

        sZoneIdToAdMap.put(mZoneId, null);
        setUpAdOptions();
        mAdColonyListener = new AdColonyListener(mAdColonyAdOptions);
        AdColony.setRewardListener(mAdColonyListener);
        AdColony.requestInterstitial(mZoneId, mAdColonyListener, mAdColonyAdOptions);
        scheduleOnVideoReady();
        MoPubLog.log(mZoneId, LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    private static boolean shouldReconfigure(String[] previousZones, String[] newZones) {
        // If AdColony is configured already, but previousZones is null, then that means AdColony
        // was configured with the AdColonyInterstitial adapter so attempt to configure with
        // the ids in newZones. They will be ignored within the AdColony SDK if the zones are
        // the same as the zones that the other adapter called AdColony.configure() with.
        if (previousZones == null) {
            return true;
        } else if (newZones == null) {
            return false;
        } else if (previousZones.length != newZones.length) {
            return true;
        }
        Arrays.sort(previousZones);
        Arrays.sort(newZones);
        return !Arrays.equals(previousZones, newZones);
    }

    private void setUpAdOptions() {
        mAdColonyAdOptions.enableConfirmationDialog(getConfirmationDialogFromSettings());
        mAdColonyAdOptions.enableResultsDialog(getResultsDialogFromSettings());
    }

    private boolean isAdColonyConfigured() {
        return !AdColony.getSDKVersion().isEmpty();
    }

    @Override
    public boolean hasVideoAvailable() {
        return mAd != null && !mAd.isExpired();
    }

    @Override
    public void showVideo() {
        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);
        if (this.hasVideoAvailable()) {
            mAd.show();
        } else {
            MoPubRewardedVideoManager.onRewardedVideoPlaybackError(
                    AdColonyRewardedVideo.class,
                    mZoneId,
                    MoPubErrorCode.NETWORK_NO_FILL);
            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    private boolean extrasAreValid(Map<String, String> extras) {
        return extras != null
                && extras.containsKey(CLIENT_OPTIONS_KEY)
                && extras.containsKey(APP_ID_KEY)
                && extras.containsKey(ALL_ZONE_IDS_KEY)
                && extras.containsKey(ZONE_ID_KEY);
    }

    private String[] extractAllZoneIds(Map<String, String> serverExtras) {
        String[] result = Json.jsonArrayToStringArray(serverExtras.get(ALL_ZONE_IDS_KEY));

        // AdColony requires at least one valid String in the allZoneIds array.
        if (result.length == 0) {
            result = new String[]{""};
        }

        return result;
    }

    private void setUpGlobalSettings() {
        final AdColonyGlobalMediationSettings globalMediationSettings =
                MoPubRewardedVideoManager.getGlobalMediationSettings(AdColonyGlobalMediationSettings.class);
        if (globalMediationSettings != null) {
            if (globalMediationSettings.getUserId() != null) {
                mAdColonyAppOptions.setUserID(globalMediationSettings.getUserId());
            }
        }
    }

    private boolean getConfirmationDialogFromSettings() {
        final AdColonyInstanceMediationSettings settings =
                MoPubRewardedVideoManager.getInstanceMediationSettings(AdColonyInstanceMediationSettings.class, mAdUnitId);
        return settings != null && settings.withConfirmationDialog();
    }

    private boolean getResultsDialogFromSettings() {
        final AdColonyInstanceMediationSettings settings =
                MoPubRewardedVideoManager.getInstanceMediationSettings(AdColonyInstanceMediationSettings.class, mAdUnitId);
        return settings != null && settings.withResultsDialog();
    }

    private void scheduleOnVideoReady() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (isAdAvailable(mZoneId)) {
                    mAd = sZoneIdToAdMap.get(mZoneId);
                    mIsLoading = false;
                    mScheduledThreadPoolExecutor.shutdownNow();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (hasVideoAvailable()) {
                                MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
                                MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(
                                        AdColonyRewardedVideo.class,
                                        mZoneId);
                            } else {
                                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
                                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(
                                        AdColonyRewardedVideo.class,
                                        mZoneId,
                                        MoPubErrorCode.NETWORK_NO_FILL);
                            }
                        }
                    });
                }
            }
        };

        if (!mIsLoading) {
            mScheduledThreadPoolExecutor.scheduleAtFixedRate(runnable, 1, 1, TimeUnit.SECONDS);
            mIsLoading = true;
        }
    }

    private boolean isAdAvailable(String zoneId) {
        return sZoneIdToAdMap.get(zoneId) != null;
    }

    private static class AdColonyListener extends AdColonyInterstitialListener
            implements AdColonyRewardListener, CustomEventRewardedVideoListener {
        private AdColonyAdOptions mAdOptions;

        AdColonyListener(AdColonyAdOptions adOptions) {
            mAdOptions = adOptions;
        }

        @Override
        public void onReward(@NonNull AdColonyReward a) {
            MoPubReward reward;
            if (a.success()) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "AdColonyReward name - " + a.getRewardName());
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "AdColonyReward amount - " + a.getRewardAmount());
                reward = MoPubReward.success(a.getRewardName(), a.getRewardAmount());

                MoPubLog.log(SHOULD_REWARD, ADAPTER_NAME, a.getRewardAmount(), a.getRewardName());
            } else {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "AdColonyReward failed");
                reward = MoPubReward.failure();
            }

            MoPubRewardedVideoManager.onRewardedVideoCompleted(
                    AdColonyRewardedVideo.class,
                    a.getZoneID(),
                    reward);
        }

        @Override
        public void onRequestFilled(@NonNull AdColonyInterstitial adColonyInterstitial) {
            sZoneIdToAdMap.put(adColonyInterstitial.getZoneID(), adColonyInterstitial);
        }

        @Override
        public void onRequestNotFilled(@NonNull AdColonyZone zone) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "AdColony rewarded ad has no fill");
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(
                    AdColonyRewardedVideo.class,
                    zone.getZoneID(),
                    MoPubErrorCode.NETWORK_NO_FILL);
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
        }

        @Override
        public void onClosed(@NonNull AdColonyInterstitial ad) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Adcolony rewarded ad has been dismissed");
            MoPubRewardedVideoManager.onRewardedVideoClosed(
                    AdColonyRewardedVideo.class,
                    ad.getZoneID());
        }

        @Override
        public void onOpened(@NonNull AdColonyInterstitial ad) {
            MoPubRewardedVideoManager.onRewardedVideoStarted(
                    AdColonyRewardedVideo.class,
                    ad.getZoneID());
            MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);
        }

        @Override
        public void onExpiring(@NonNull AdColonyInterstitial ad) {
            AdColony.requestInterstitial(ad.getZoneID(), ad.getListener(), mAdOptions);
        }

        @Override
        public void onClicked(@NonNull AdColonyInterstitial ad) {
            MoPubRewardedVideoManager.onRewardedVideoClicked(
                    AdColonyRewardedVideo.class,
                    ad.getZoneID());
            MoPubLog.log(CLICKED, ADAPTER_NAME);
        }
    }

    public static final class AdColonyGlobalMediationSettings implements MediationSettings {
        @Nullable
        private final String mUserId;

        public AdColonyGlobalMediationSettings(@Nullable String userId) {
            mUserId = userId;
        }

        @Nullable
        public String getUserId() {
            return mUserId;
        }
    }

    public static final class AdColonyInstanceMediationSettings implements MediationSettings {
        private final boolean mWithConfirmationDialog;
        private final boolean mWithResultsDialog;

        public AdColonyInstanceMediationSettings(
                boolean withConfirmationDialog, boolean withResultsDialog) {
            mWithConfirmationDialog = withConfirmationDialog;
            mWithResultsDialog = withResultsDialog;
        }

        public boolean withConfirmationDialog() {
            return mWithConfirmationDialog;
        }

        public boolean withResultsDialog() {
            return mWithResultsDialog;
        }
    }
}

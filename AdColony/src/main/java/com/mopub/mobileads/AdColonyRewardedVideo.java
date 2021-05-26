package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAdOptions;
import com.adcolony.sdk.AdColonyAppOptions;
import com.adcolony.sdk.AdColonyInterstitial;
import com.adcolony.sdk.AdColonyInterstitialListener;
import com.adcolony.sdk.AdColonyReward;
import com.adcolony.sdk.AdColonyRewardListener;
import com.adcolony.sdk.AdColonyZone;
import com.mopub.common.BaseLifecycleListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MediationSettings;
import com.mopub.common.MoPubReward;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Json;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOULD_REWARD;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;

public class AdColonyRewardedVideo extends BaseAd {

    private static final String ADAPTER_NAME = AdColonyRewardedVideo.class.getSimpleName();

    private static boolean sInitialized = false;
    private static LifecycleListener sLifecycleListener = new BaseLifecycleListener();

    @NonNull
    private AdColonyAdapterConfiguration mAdColonyAdapterConfiguration;

    private AdColonyInterstitial mAd;
    private String mAdColonyClientOptions = "";
    private static WeakHashMap<String, AdColonyInterstitial> sZoneIdToAdMap = new WeakHashMap<>();
    @NonNull
    private String mAdUnitId = "";
    private boolean mIsLoading = false;

    @NonNull
    private static String mZoneId = AdColonyAdapterConfiguration.DEFAULT_ZONE_ID;

    @NonNull
    @Override
    public String getAdNetworkId() {
        return mZoneId;
    }

    private AdColonyAppOptions mAdColonyAppOptions;

    // For waiting and notifying the SDK:
    private final Handler mHandler;
    private final ScheduledThreadPoolExecutor mScheduledThreadPoolExecutor;

    public AdColonyRewardedVideo() {
        mScheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
        mHandler = new Handler(Looper.getMainLooper());
        mAdColonyAdapterConfiguration = new AdColonyAdapterConfiguration();
    }

    @Nullable
    @Override
    public LifecycleListener getLifecycleListener() {
        return sLifecycleListener;
    }

    @Override
    protected void onInvalidate() {
        mScheduledThreadPoolExecutor.shutdownNow();
        AdColonyInterstitial ad = sZoneIdToAdMap.get(mZoneId);
        if (ad != null) {
            ad.destroy();
            sZoneIdToAdMap.remove(mZoneId);
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "AdColony rewarded video destroyed");
        }
    }

    @Override
    public boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
                                         @NonNull final AdData adData) {
        synchronized (AdColonyRewardedVideo.class) {
            if (sInitialized) {
                return false;
            }

            final Map<String, String> extras = adData.getExtras();

            String clientOptions = extras.get(AdColonyAdapterConfiguration.CLIENT_OPTIONS_KEY);
            if (clientOptions == null)
                clientOptions = "";

            final String appId = AdColonyAdapterConfiguration.getAdColonyParameter(AdColonyAdapterConfiguration.APP_ID_KEY, extras);

            String[] allZoneIds;
            String allZoneIdsString = AdColonyAdapterConfiguration.getAdColonyParameter(AdColonyAdapterConfiguration.ALL_ZONE_IDS_KEY, extras);
            if (allZoneIdsString != null) {
                allZoneIds = Json.jsonArrayToStringArray(allZoneIdsString);
            } else {
                allZoneIds = null;
            }

            if (appId == null) {
                abortRequestForIncorrectParameter(AdColonyAdapterConfiguration.APP_ID_KEY);
                return false;
            }

            if (allZoneIds == null || allZoneIds.length == 0) {
                abortRequestForIncorrectParameter(AdColonyAdapterConfiguration.ZONE_ID_KEY);
                return false;
            }

            mAdColonyClientOptions = clientOptions;
            if (mAdColonyAppOptions == null) {
                mAdColonyAppOptions = AdColonyAdapterConfiguration.getAdColonyAppOptionsAndSetConsent(clientOptions);
            }

            AdColonyAdapterConfiguration.checkAndConfigureAdColonyIfNecessary(
                    launcherActivity,
                    clientOptions,
                    appId,
                    allZoneIds
            );

            sInitialized = true;
            return true;
        }
    }

    @Override
    protected void load(@NonNull final Context context,
                        @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        setAutomaticImpressionAndClickTracking(false);

        final Map<String, String> extras = adData.getExtras();

        mAdColonyAdapterConfiguration.setCachedInitializationParameters(context, extras);

        String clientOptions;
        String appId;
        String zoneId;
        String[] allZoneIds;

        // Set optional parameters
        clientOptions = extras.get(AdColonyAdapterConfiguration.CLIENT_OPTIONS_KEY);
        if (clientOptions == null)
            clientOptions = "";

        // Set mandatory parameters
        appId = AdColonyAdapterConfiguration.getAdColonyParameter(AdColonyAdapterConfiguration.APP_ID_KEY, extras);
        zoneId = AdColonyAdapterConfiguration.getAdColonyParameter(AdColonyAdapterConfiguration.ZONE_ID_KEY, extras);

        String allZoneIdsString = AdColonyAdapterConfiguration.getAdColonyParameter(AdColonyAdapterConfiguration.ALL_ZONE_IDS_KEY, extras);
        if (allZoneIdsString != null) {
            allZoneIds = Json.jsonArrayToStringArray(allZoneIdsString);
        } else {
            allZoneIds = null;
        }

        // Check if mandatory parameters are valid, abort otherwise
        if (appId == null) {
            abortRequestForIncorrectParameter(AdColonyAdapterConfiguration.APP_ID_KEY);
            return;
        }

        if (zoneId == null || allZoneIds == null || allZoneIds.length == 0) {
            abortRequestForIncorrectParameter(AdColonyAdapterConfiguration.ZONE_ID_KEY);
            return;
        }

        mZoneId = zoneId;
        mAdColonyClientOptions = clientOptions;
        if (mAdColonyAppOptions == null) {
            mAdColonyAppOptions = AdColonyAdapterConfiguration.getAdColonyAppOptionsAndSetConsent(clientOptions);
        }

        AdColonyAdapterConfiguration.checkAndConfigureAdColonyIfNecessary(
                context,
                clientOptions,
                appId,
                allZoneIds
        );

        setUpGlobalSettings();

        final String adUnitId = adData.getAdUnit();
        if (!TextUtils.isEmpty(adUnitId)) {
            mAdUnitId = adUnitId;
        }

        sZoneIdToAdMap.put(mZoneId, null);

        final AdColonyAdOptions mAdColonyAdOptions = mAdColonyAdapterConfiguration.getRewardAdOptionsFromExtras(extras, mAdUnitId);

        final AdColonyListener mAdColonyListener = new AdColonyListener(mAdColonyAdOptions);
        AdColony.setRewardListener(mAdColonyListener);
        AdColony.requestInterstitial(mZoneId, mAdColonyListener, mAdColonyAdOptions);
        scheduleOnVideoReady();
        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    private void abortRequestForIncorrectParameter(String parameterName) {
        AdColonyAdapterConfiguration.logAndFail("rewarded video request", parameterName);
        if (mLoadListener != null) {
            mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }
    }

    private boolean hasVideoAvailable() {
        return mAd != null && !mAd.isExpired();
    }

    @Override
    public void show() {
        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
        if (this.hasVideoAvailable()) {
            mAd.show();
        } else if (mInteractionListener != null) {
            mInteractionListener.onAdFailed(MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
            MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME, MoPubErrorCode.VIDEO_PLAYBACK_ERROR.getIntCode(), MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
        }
    }

    private void setUpGlobalSettings() {
        final AdColonyGlobalMediationSettings globalMediationSettings =
                MoPubRewardedVideoManager.getGlobalMediationSettings(AdColonyGlobalMediationSettings.class);
        if (globalMediationSettings != null) {
            if (globalMediationSettings.getUserId() != null) {
                if (mAdColonyAppOptions == null) {
                    mAdColonyAppOptions = AdColonyAdapterConfiguration.getAdColonyAppOptionsAndSetConsent(mAdColonyClientOptions);
                    AdColony.setAppOptions(mAdColonyAppOptions);
                }
                mAdColonyAppOptions.setUserID(globalMediationSettings.getUserId());
            }
        }
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
                            if (mLoadListener == null) {
                                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                                        MoPubErrorCode.NETWORK_INVALID_STATE.getIntCode(),
                                        MoPubErrorCode.NETWORK_INVALID_STATE);
                                return;
                            }
                            if (hasVideoAvailable()) {
                                MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
                                mLoadListener.onAdLoaded();
                            } else {
                                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
                                mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);
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

    private class AdColonyListener extends AdColonyInterstitialListener implements AdColonyRewardListener {
        private AdColonyAdOptions mAdOptions;

        AdColonyListener(AdColonyAdOptions adOptions) {
            mAdOptions = adOptions;
        }

        @NonNull
        private String getAdNetworkId() {
            return mZoneId;
        }

        @Override
        public void onReward(@NonNull AdColonyReward a) {
            MoPubReward reward;
            if (a.success()) {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "AdColonyReward name - " + a.getRewardName());
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "AdColonyReward amount - " + a.getRewardAmount());
                reward = MoPubReward.success(a.getRewardName(), a.getRewardAmount());

                MoPubLog.log(getAdNetworkId(), SHOULD_REWARD, ADAPTER_NAME, a.getRewardAmount(), a.getRewardName());
            } else {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "AdColonyReward failed");
                reward = MoPubReward.failure();
            }

            if (mInteractionListener != null) {
                mInteractionListener.onAdComplete(reward);
            }
        }

        @Override
        public void onRequestFilled(@NonNull AdColonyInterstitial adColonyInterstitial) {
            sZoneIdToAdMap.put(adColonyInterstitial.getZoneID(), adColonyInterstitial);
        }

        @Override
        public void onRequestNotFilled(@NonNull AdColonyZone zone) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "AdColony rewarded ad has no fill");
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
        }

        @Override
        public void onClosed(@NonNull AdColonyInterstitial ad) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Adcolony rewarded ad has been dismissed");
            if (mInteractionListener != null) {
                mInteractionListener.onAdDismissed();
            }
        }

        @Override
        public void onOpened(@NonNull AdColonyInterstitial ad) {
            if (mInteractionListener != null) {
                mInteractionListener.onAdShown();
                mInteractionListener.onAdImpression();
            }
            MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
        }

        @Override
        public void onExpiring(@NonNull AdColonyInterstitial ad) {
            Preconditions.checkNotNull(ad);

            if (ad.getListener() != null) {
                AdColony.requestInterstitial(ad.getZoneID(), ad.getListener(), mAdOptions);
            }
        }

        @Override
        public void onClicked(@NonNull AdColonyInterstitial ad) {
            if (mInteractionListener != null) {
                mInteractionListener.onAdClicked();
            }
            MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
        }
    }

    public static final class AdColonyGlobalMediationSettings implements MediationSettings {
        @Nullable
        private String userId;

        public AdColonyGlobalMediationSettings(@Nullable String userId) {
            this.userId = userId;
        }

        public AdColonyGlobalMediationSettings() {
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        @Nullable
        public String getUserId() {
            return userId;
        }
    }

    public static final class AdColonyInstanceMediationSettings implements MediationSettings {
        private boolean withConfirmationDialog;
        private boolean withResultsDialog;

        public AdColonyInstanceMediationSettings(
                boolean withConfirmationDialog, boolean withResultsDialog) {
            this.withConfirmationDialog = withConfirmationDialog;
            this.withResultsDialog = withResultsDialog;
        }

        public AdColonyInstanceMediationSettings() {
        }

        public void setWithConfirmationDialog(boolean withConfirmationDialog) {
            this.withConfirmationDialog = withConfirmationDialog;
        }

        public void setWithResultsDialog(boolean withResultsDialog) {
            this.withResultsDialog = withResultsDialog;
        }

        public boolean isWithConfirmationDialog() {
            return withConfirmationDialog;
        }

        public boolean isWithResultsDialog() {
            return withResultsDialog;
        }
    }
}

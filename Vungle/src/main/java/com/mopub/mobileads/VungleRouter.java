package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.BaseLifecycleListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPub;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.privacy.ConsentStatus;
import com.mopub.common.privacy.PersonalInfoManager;
import com.vungle.warren.AdConfig;
import com.vungle.warren.AdConfig.AdSize;
import com.vungle.warren.Banners;
import com.vungle.warren.InitCallback;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Plugin;
import com.vungle.warren.Vungle;
import com.vungle.warren.VungleApiClient;
import com.vungle.warren.VungleBanner;
import com.vungle.warren.VungleSettings;
import com.vungle.warren.error.VungleException;

import java.util.HashMap;
import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;
import static com.mopub.mobileads.MoPubErrorCode.MISSING_AD_UNIT_ID;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_NO_FILL;
import static com.mopub.mobileads.MoPubErrorCode.NO_CONNECTION;
import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;
import static com.mopub.mobileads.MoPubErrorCode.VIDEO_DOWNLOAD_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.VIDEO_PLAYBACK_ERROR;
import static com.vungle.warren.error.VungleException.AD_FAILED_TO_DOWNLOAD;
import static com.vungle.warren.error.VungleException.AD_PAST_EXPIRATION;
import static com.vungle.warren.error.VungleException.AD_UNABLE_TO_PLAY;
import static com.vungle.warren.error.VungleException.ASSET_DOWNLOAD_ERROR;
import static com.vungle.warren.error.VungleException.NETWORK_ERROR;
import static com.vungle.warren.error.VungleException.NO_SERVE;
import static com.vungle.warren.error.VungleException.PLACEMENT_NOT_FOUND;

public class VungleRouter {

    private static final String ADAPTER_NAME = VungleRouter.class.getSimpleName();

    private static final LifecycleListener sLifecycleListener = new BaseLifecycleListener() {
        @Override
        public void onPause(@NonNull final Activity activity) {
            super.onPause(activity);
        }

        @Override
        public void onResume(@NonNull final Activity activity) {
            super.onResume(activity);
        }
    };
    private static final VungleRouter sInstance = new VungleRouter();
    private static SDKInitState sInitState = SDKInitState.NOTINITIALIZED;
    private final static Map<String, VungleRouterListener> sVungleRouterListeners = new HashMap<>();
    private final static Map<AdRequest, VungleRouterListener> sWaitingList = new HashMap<>();

    private static class AdRequest {
        @NonNull
        private final String placementId;
        @Nullable
        private final String adMarkup;

        public AdRequest(@NonNull String placementId, @Nullable String adMarkup) {
            this.placementId = placementId;
            this.adMarkup = adMarkup;
        }

        @Override
        public int hashCode() {
            int result = placementId.hashCode();
            result = 31 * result + (adMarkup != null ? adMarkup.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;

            AdRequest request = (AdRequest) obj;

            if (!placementId.equals(request.placementId)) return false;
            return adMarkup != null ? adMarkup.equals(request.adMarkup) : request.adMarkup == null;
        }
    }

    private enum SDKInitState {
        NOTINITIALIZED,
        INITIALIZING,
        INITIALIZED
    }

    private VungleRouter() {
        Plugin.addWrapperInfo(VungleApiClient.WrapperFramework.mopub,
                VungleAdapterConfiguration.ADAPTER_VERSION.replace('.', '_'));
    }

    public static VungleRouter getInstance() {
        return sInstance;
    }

    LifecycleListener getLifecycleListener() {
        return sLifecycleListener;
    }

    void initVungle(final Context context, final String vungleAppId) {

        // Pass the user consent from the MoPub SDK to Vungle as per GDPR
        // Pass consentMessageVersion per Vungle 6.3.17:
        // https://support.vungle.com/hc/en-us/articles/360002922871#GDPRRecommendedImplementationInstructions
        InitCallback initCallback = new InitCallback() {
            @Override
            public void onSuccess() {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "SDK is initialized successfully.");

                sInitState = SDKInitState.INITIALIZED;

                clearWaitingList();

                // Pass the user consent from the MoPub SDK to Vungle as per GDPR
                PersonalInfoManager personalInfoManager = MoPub.getPersonalInformationManager();

                boolean canCollectPersonalInfo = MoPub.canCollectPersonalInformation();
                boolean shouldAllowLegitimateInterest = MoPub.shouldAllowLegitimateInterest();

                if (personalInfoManager != null && personalInfoManager.gdprApplies() == Boolean.TRUE) {
                    if (shouldAllowLegitimateInterest) {
                        if (personalInfoManager.getPersonalInfoConsentStatus() == ConsentStatus.EXPLICIT_NO
                                || personalInfoManager.getPersonalInfoConsentStatus() == ConsentStatus.DNT
                                || personalInfoManager.getPersonalInfoConsentStatus() ==
                                ConsentStatus.POTENTIAL_WHITELIST) {
                            Vungle.updateConsentStatus(Vungle.Consent.OPTED_OUT, "");
                        } else {
                            Vungle.updateConsentStatus(Vungle.Consent.OPTED_IN, "");
                        }
                    } else {
                        // Pass consentMessageVersion per Vungle 6.3.17:
                        // https://support.vungle.com/hc/en-us/articles/360002922871#GDPRRecommendedImplementationInstructions
                        Vungle.updateConsentStatus(canCollectPersonalInfo ? Vungle.Consent.OPTED_IN :
                                Vungle.Consent.OPTED_OUT, "");
                    }
                }

            }

            @Override
            public void onError(VungleException throwable) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Initialization failed.", throwable);

                sInitState = SDKInitState.NOTINITIALIZED;
            }

            @Override
            public void onAutoCacheAdAvailable(String placementId) {
                //no-op
            }
        };

        VungleSettings vungleSettings = VungleNetworkSettings.getVungleSettings();
        VungleSettings settings = (vungleSettings != null) ? vungleSettings : new VungleSettings.Builder().build();
        Vungle.init(vungleAppId, context.getApplicationContext(), initCallback, settings);

        sInitState = SDKInitState.INITIALIZING;
    }

    void setIncentivizedFields(String userID, String title, String body,
                               String keepWatching, String close) {
        Vungle.setIncentivizedFields(userID, title, body, keepWatching, close);
    }

    boolean isVungleInitialized() {
        if (sInitState == SDKInitState.NOTINITIALIZED) {
            return false;
        } else if (sInitState == SDKInitState.INITIALIZING) {
            return true;
        } else if (sInitState == SDKInitState.INITIALIZED) {
            return true;
        }

        return Vungle.isInitialized();
    }

    void loadAdForPlacement(String placementId, @Nullable String adMarkup, @Nullable AdConfig adConfig, VungleRouterListener routerListener) {
        switch (sInitState) {
            case NOTINITIALIZED:
                MoPubLog.log(placementId, CUSTOM, ADAPTER_NAME, "loadAdForPlacement is called before " +
                        "initialization starts. This is not an expect case.");
                break;
            case INITIALIZING:
                AdRequest adRequest = new AdRequest(placementId, adMarkup);
                sWaitingList.put(adRequest, routerListener);
                break;
            case INITIALIZED:
                addRouterListener(placementId, routerListener);
                Vungle.loadAd(placementId, adMarkup, adConfig, loadAdCallback);
                break;
        }
    }

    void loadBannerAd(@NonNull String placementId, @Nullable String adMarkup, @NonNull AdSize adSize,
                      @NonNull VungleRouterListener routerListener) {
        switch (sInitState) {
            case NOTINITIALIZED:
                MoPubLog.log(placementId, CUSTOM, ADAPTER_NAME, "loadBannerAdForPlacement is called before the " +
                        "Vungle SDK initialization.");
                break;

            case INITIALIZING:
                AdRequest adRequest = new AdRequest(placementId, adMarkup);
                sWaitingList.put(adRequest, routerListener);
                break;

            case INITIALIZED:
                addRouterListener(placementId, routerListener);
                Banners.loadBanner(placementId, adMarkup, adSize, loadAdCallback);
                break;
        }
    }

    void addRouterListener(String placementId, VungleRouterListener routerListener) {
        if (sVungleRouterListeners.containsKey(placementId) &&
                sVungleRouterListeners.get(placementId) == routerListener) {
            return;
        }
        sVungleRouterListeners.put(placementId, routerListener);
    }

    void removeRouterListener(String placementId) {
        if (!sVungleRouterListeners.containsKey(placementId)) {
            return;
        }
        sVungleRouterListeners.remove(placementId);
    }

    void playAdForPlacement(String placementId, @Nullable String adMarkup, AdConfig adConfig) {
        Vungle.playAd(placementId, adMarkup, adConfig, playAdCallback);
    }

    VungleBanner getVungleBannerAd(@NonNull String placementId, @Nullable String adMarkup, @NonNull AdSize adSize) {
        Preconditions.checkNotNull(placementId);
        Preconditions.checkNotNull(adSize);

        return Banners.getBanner(placementId, adMarkup, adSize, playAdCallback);
    }

    /**
     * Checks and returns if the passed Placement ID is a valid placement for App ID
     *
     * @param placementId
     * @return
     */
    boolean isValidPlacement(String placementId) {
        return Vungle.isInitialized() &&
                Vungle.getValidPlacements().contains(placementId);
    }

    public void updateConsentStatus(Vungle.Consent status) {
        // (New) Pass consentMessageVersion per Vungle 6.3.17:
        // https://support.vungle.com/hc/en-us/articles/360002922871#GDPRRecommendedImplementationInstructions
        Vungle.updateConsentStatus(status, "");
    }

    public Vungle.Consent getConsentStatus() {
        return Vungle.getConsentStatus();
    }

    private void clearWaitingList() {
        for (Map.Entry<AdRequest, VungleRouterListener> entry : sWaitingList.entrySet()) {
            AdRequest request = entry.getKey();
            Vungle.loadAd(request.placementId, request.adMarkup, null, loadAdCallback);
            sVungleRouterListeners.put(request.placementId, entry.getValue());
        }

        sWaitingList.clear();
    }

    private final PlayAdCallback playAdCallback = new PlayAdCallback() {
        @Override
        @Deprecated
        public void onAdEnd(String id, boolean completed, boolean isCTAClicked) {
            //Deprecated event
        }

        @Override
        public void onAdEnd(String id) {
            MoPubLog.log(id, CUSTOM, ADAPTER_NAME, "onAdEnd - Placement ID: " + id);
            VungleRouterListener targetListener = sVungleRouterListeners.get(id);
            if (targetListener != null) {
                targetListener.onAdEnd(id);
            } else {
                MoPubLog.log(id, CUSTOM, ADAPTER_NAME, "onAdEnd - VungleRouterListener is not found for " +
                        "Placement ID: " + id);
            }
        }

        @Override
        public void onAdClick(String id) {
            MoPubLog.log(id, CUSTOM, ADAPTER_NAME, "onAdClick - Placement ID: " + id);
            VungleRouterListener targetListener = sVungleRouterListeners.get(id);
            if (targetListener != null) {
                targetListener.onAdClick(id);
            } else {
                MoPubLog.log(id, CUSTOM, ADAPTER_NAME, "onAdClick - VungleRouterListener is not found for " +
                        "Placement ID: " + id);
            }
        }

        @Override
        public void onAdRewarded(String id) {
            MoPubLog.log(id, CUSTOM, ADAPTER_NAME, "onAdRewarded - Placement ID: " + id);
            final VungleRouterListener targetListener = sVungleRouterListeners.get(id);

            if (targetListener != null) {
                targetListener.onAdRewarded(id);
            } else {
                MoPubLog.log(id, CUSTOM, ADAPTER_NAME, "onAdRewarded - VungleRouterListener is not found for " +
                        "Placement ID: " + id);
            }
        }

        @Override
        public void onAdLeftApplication(String id) {
            MoPubLog.log(id, CUSTOM, ADAPTER_NAME, "onAdLeftApplication - Placement ID: " + id);
            final VungleRouterListener targetListener = sVungleRouterListeners.get(id);
            if (targetListener != null) {
                targetListener.onAdLeftApplication(id);
            } else {
                MoPubLog.log(id, CUSTOM, ADAPTER_NAME, "onAdLeftApplication - VungleRouterListener is not found for " +
                        "Placement ID: " + id);
            }
        }

        @Override
        public void creativeId(String creativeId) {
            // no-op
        }

        @Override
        public void onAdStart(String id) {
            MoPubLog.log(id, CUSTOM, ADAPTER_NAME, "onAdStart - Placement ID: " + id);

            VungleRouterListener targetListener = sVungleRouterListeners.get(id);
            if (targetListener != null) {
                targetListener.onAdStart(id);
            } else {
                MoPubLog.log(id, CUSTOM, ADAPTER_NAME, "onAdStart - VungleRouterListener is not found for " +
                        "Placement ID: " + id);
            }
        }

        @Override
        public void onError(String id, VungleException error) {
            MoPubLog.log(id, CUSTOM_WITH_THROWABLE, ADAPTER_NAME, "onPlayAdError - Placement ID: " + id, error);

            VungleRouterListener targetListener = sVungleRouterListeners.get(id);
            if (targetListener != null) {
                targetListener.onAdPlayError(id, error);
            } else {
                MoPubLog.log(id, CUSTOM, ADAPTER_NAME, "onUnableToPlayAd - VungleRouterListener is not found " +
                        "for Placement ID: " + id);
            }
        }

        @Override
        public void onAdViewed(String id) {
            MoPubLog.log(id, CUSTOM, ADAPTER_NAME, "onAdViewed - Placement ID: " + id);

            VungleRouterListener targetListener = sVungleRouterListeners.get(id);
            if (targetListener != null) {
                targetListener.onAdViewed(id);
            } else {
                MoPubLog.log(id, CUSTOM, ADAPTER_NAME, "onAdViewed - VungleRouterListener is not found for " +
                        "Placement ID: " + id);
            }

        }
    };

    private final LoadAdCallback loadAdCallback = new LoadAdCallback() {
        @Override
        public void onAdLoad(String id) {
            MoPubLog.log(id, CUSTOM, ADAPTER_NAME, "onAdLoad - Placement ID: " + id);

            VungleRouterListener targetListener = sVungleRouterListeners.get(id);
            if (targetListener != null) {
                targetListener.onAdLoaded(id);
            } else {
                MoPubLog.log(id, CUSTOM, ADAPTER_NAME, "onAdLoad - " +
                        "VungleRouterListener is not found for Placement ID: " + id);
            }
        }

        @Override
        public void onError(String id, VungleException error) {
            MoPubLog.log(id, CUSTOM_WITH_THROWABLE, ADAPTER_NAME, "onAdLoadError - Placement ID: " + id, error);

            VungleRouterListener targetListener = sVungleRouterListeners.get(id);
            if (targetListener != null) {
                targetListener.onAdLoadError(id, error);
            } else {
                MoPubLog.log(id, CUSTOM, ADAPTER_NAME, "onAdLoadError - " +
                        "VungleRouterListener is not found for Placement ID: " + id);
            }
        }
    };

    // might be called on pubs side with header bidding and pre init Vungle sdk
    public VungleSettings applyVungleNetworkSettings(Map<String, String> configuration) {
        if (configuration == null || configuration.isEmpty()) {
            return VungleNetworkSettings.getVungleSettings();
        }

        long minSpaceInit;
        try {
            minSpaceInit = Long.parseLong(configuration.get("VNG_MIN_SPACE_INIT"));
        } catch (NumberFormatException e) {
            //51 mb
            minSpaceInit = 51 << 20;
        }

        long minSpaceLoadAd;
        try {
            minSpaceLoadAd = Long.parseLong(configuration.get("VNG_MIN_SPACE_LOAD_AD"));
        } catch (NumberFormatException e) {
            //50 mb
            minSpaceLoadAd = 50 << 20;
        }

        boolean isAndroidIdOpted = Boolean.parseBoolean(configuration.get("VNG_DEVICE_ID_OPT_OUT"));

        //Apply settings.
        VungleNetworkSettings.setMinSpaceForInit(minSpaceInit);
        VungleNetworkSettings.setMinSpaceForAdLoad(minSpaceLoadAd);
        VungleNetworkSettings.setAndroidIdOptOut(isAndroidIdOpted);

        return VungleNetworkSettings.getVungleSettings();
    }

    @NonNull
    static MoPubErrorCode mapErrorCode(int vungleError) {
        switch (vungleError) {
            case NETWORK_ERROR:
                return NO_CONNECTION;
            case PLACEMENT_NOT_FOUND:
                return MISSING_AD_UNIT_ID;
            case NO_SERVE:
                return NETWORK_NO_FILL;
            case AD_FAILED_TO_DOWNLOAD:
            case ASSET_DOWNLOAD_ERROR:
                return VIDEO_DOWNLOAD_ERROR;
            case AD_UNABLE_TO_PLAY:
            case AD_PAST_EXPIRATION:
                return VIDEO_PLAYBACK_ERROR;
            default:
                return UNSPECIFIED;
        }
    }
}

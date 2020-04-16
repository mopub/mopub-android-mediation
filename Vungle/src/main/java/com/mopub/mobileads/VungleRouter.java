package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

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
import com.vungle.warren.VungleNativeAd;
import com.vungle.warren.VungleSettings;
import com.vungle.warren.error.VungleException;

import java.util.HashMap;
import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

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
    private static VungleRouter sInstance = new VungleRouter();
    private static SDKInitState sInitState = SDKInitState.NOTINITIALIZED;
    private static Map<String, VungleRouterListener> sVungleRouterListeners = new HashMap<>();
    private static Map<String, VungleRouterListener> sWaitingList = new HashMap<>();

    private enum SDKInitState {
        NOTINITIALIZED,
        INITIALIZING,
        INITIALIZED
    }

    private VungleRouter() {
        Plugin.addWrapperInfo(VungleApiClient.WrapperFramework.mopub,
                VungleAdapterConfiguration.ADAPTER_VERSION.replace('.', '_'));
    }

    static VungleRouter getInstance() {
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

    void loadAdForPlacement(String placementId, VungleRouterListener routerListener) {
        switch (sInitState) {
            case NOTINITIALIZED:
                MoPubLog.log(placementId, CUSTOM, ADAPTER_NAME, "loadAdForPlacement is called before " +
                        "initialization starts. This is not an expect case.");
                break;
            case INITIALIZING:
                sWaitingList.put(placementId, routerListener);
                break;
            case INITIALIZED:
                if (isValidPlacement(placementId)) {
                    addRouterListener(placementId, routerListener);
                    Vungle.loadAd(placementId, loadAdCallback);
                } else {
                    routerListener.onUnableToPlayAd(placementId, "Invalid/Inactive Placement Id");
                }
                break;
        }
    }

    void loadBannerAd(@NonNull String placementId, @NonNull AdSize adSize,
                      @NonNull VungleRouterListener routerListener) {
        switch (sInitState) {
            case NOTINITIALIZED:
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "loadBannerAdForPlacement is called before the " +
                        "Vungle SDK initialization.");
                break;

            case INITIALIZING:
                sWaitingList.put(placementId, routerListener);
                break;

            case INITIALIZED:
                if (isValidPlacement(placementId)) {
                    addRouterListener(placementId, routerListener);
                    Banners.loadBanner(placementId, adSize, loadAdCallback);
                } else {
                    routerListener.onUnableToPlayAd(placementId, "Invalid/Inactive Banner Placement Id");
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unable to play ad due to invalid/inactive Banner placement Id");
                }
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

    boolean isAdPlayableForPlacement(String placementId) {
        return Vungle.canPlayAd(placementId);
    }

    boolean isBannerAdPlayable(@NonNull String placementId, @NonNull AdSize adSize) {
        Preconditions.checkNotNull(placementId);
        Preconditions.checkNotNull(adSize);

        return Banners.canPlayAd(placementId, adSize);
    }

    void playAdForPlacement(String placementId, AdConfig adConfig) {
        if (isAdPlayableForPlacement(placementId)) {
            Vungle.playAd(placementId, adConfig, playAdCallback);
        } else {
            MoPubLog.log(placementId, CUSTOM, ADAPTER_NAME, "There should not be this case. " +
                    "playAdForPlacement is called before an ad is loaded for Placement ID: " + placementId);
        }
    }

    VungleNativeAd getVungleMrecAd(String placementId, AdConfig adConfig) {
        return Vungle.getNativeAd(placementId, adConfig, playAdCallback);
    }

    VungleBanner getVungleBannerAd(@NonNull String placementId, @NonNull AdSize adSize) {
        Preconditions.checkNotNull(placementId);
        Preconditions.checkNotNull(adSize);

        return Banners.getBanner(placementId, adSize, playAdCallback);
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
        for (Map.Entry<String, VungleRouterListener> entry : sWaitingList.entrySet()) {
            Vungle.loadAd(entry.getKey(), loadAdCallback);
            sVungleRouterListeners.put(entry.getKey(), entry.getValue());
        }

        sWaitingList.clear();
    }

    private final PlayAdCallback playAdCallback = new PlayAdCallback() {
        @Override
        public void onAdEnd(String id, boolean completed, boolean isCTAClicked) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "onAdEnd - Placement ID: " + id);

            VungleRouterListener targetListener = sVungleRouterListeners.get(id);
            if (targetListener != null) {
                targetListener.onAdEnd(id, completed, isCTAClicked);
            } else {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "onAdEnd - VungleRouterListener is not found for " +
                        "Placement ID: " + id);
            }
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
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "onUnableToPlayAd - Placement ID: " + id, error);

            VungleRouterListener targetListener = sVungleRouterListeners.get(id);
            if (targetListener != null) {
                targetListener.onUnableToPlayAd(id, error.getLocalizedMessage());
            } else {
                MoPubLog.log(id, CUSTOM, ADAPTER_NAME, "onUnableToPlayAd - VungleRouterListener is not found " +
                        "for Placement ID: " + id);
            }
        }
    };

    private final LoadAdCallback loadAdCallback = new LoadAdCallback() {
        @Override
        public void onAdLoad(String id) {
            onAdAvailabilityUpdate(id, true);
        }

        @Override
        public void onError(String id, VungleException cause) {
            onAdAvailabilityUpdate(id, false);
        }

        private void onAdAvailabilityUpdate(String placementReferenceId, boolean isAdAvailable) {
            MoPubLog.log(placementReferenceId, CUSTOM, ADAPTER_NAME, "onAdAvailabilityUpdate - Placement ID: " +
                    placementReferenceId);

            VungleRouterListener targetListener = sVungleRouterListeners.get(placementReferenceId);
            if (targetListener != null) {
                targetListener.onAdAvailabilityUpdate(placementReferenceId, isAdAvailable);
            } else {
                MoPubLog.log(placementReferenceId, CUSTOM, ADAPTER_NAME, "onAdAvailabilityUpdate - " +
                        "VungleRouterListener is not found for Placement ID: " + placementReferenceId);
            }
        }
    };
}

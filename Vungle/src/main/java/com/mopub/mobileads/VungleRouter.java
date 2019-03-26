package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;

import com.mopub.common.BaseLifecycleListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPub;
import com.mopub.common.logging.MoPubLog;

import com.vungle.warren.AdConfig;
import com.vungle.warren.InitCallback;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.network.VungleApiClient;

import java.util.HashMap;
import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class VungleRouter {

    // Version of the adapter, intended for Vungle internal use.
    private static final String VERSION = "6.3.24";
    private static final String ADAPTER_NAME = VungleRouter.class.getSimpleName();

    private static VungleRouter instance = new VungleRouter();

    private enum SDKInitState {
        NOTINITIALIZED,
        INITIALIZING,
        INITIALIZED
    }

    private static SDKInitState sInitState = SDKInitState.NOTINITIALIZED;
    private static Map<String, VungleRouterListener> sVungleRouterListeners = new HashMap<>();
    private static Map<String, VungleRouterListener> sWaitingList = new HashMap<>();

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

    private VungleRouter() {
        VungleApiClient.addWrapperInfo(VungleApiClient.WrapperFramework.mopub,
                VERSION.replace('.', '_'));
    }

    public static VungleRouter getInstance() {
        return instance;
    }

    public LifecycleListener getLifecycleListener() {
        return sLifecycleListener;
    }

    public void initVungle(Context context, String vungleAppId) {

        Vungle.init(vungleAppId, context.getApplicationContext(), new InitCallback() {
            @Override
            public void onSuccess() {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "SDK is initialized successfully.");

                sInitState = SDKInitState.INITIALIZED;

                clearWaitingList();

                // Pass the user consent from the MoPub SDK to Vungle as per GDPR
                boolean canCollectPersonalInfo = MoPub.canCollectPersonalInformation();

                // (New) Pass consentMessageVersion per Vungle 6.3.17:
                // https://support.vungle.com/hc/en-us/articles/360002922871#GDPRRecommendedImplementationInstructions
                Vungle.updateConsentStatus(canCollectPersonalInfo ? Vungle.Consent.OPTED_IN :
                        Vungle.Consent.OPTED_OUT, "");
            }

            @Override
            public void onError(Throwable throwable) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Initialization failed.", throwable);

                sInitState = SDKInitState.NOTINITIALIZED;
            }

            @Override
            public void onAutoCacheAdAvailable(String placementId) {
                // not used
            }
        });

        sInitState = SDKInitState.INITIALIZING;
    }

    public void setIncentivizedFields(String userID, String title, String body,
                                      String keepWatching, String close) {
        Vungle.setIncentivizedFields(userID, title, body, keepWatching, close);
    }

    public boolean isVungleInitialized() {
        if (sInitState == SDKInitState.NOTINITIALIZED) {
            return false;
        } else if (sInitState == SDKInitState.INITIALIZING) {
            return true;
        } else if (sInitState == SDKInitState.INITIALIZED) {
            return true;
        }

        return Vungle.isInitialized();
    }

    public void loadAdForPlacement(String placementId, VungleRouterListener routerListener) {
        switch (sInitState) {
            case NOTINITIALIZED:
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "loadAdForPlacement is called before " +
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

    private void addRouterListener(String placementId, VungleRouterListener routerListener) {
        sVungleRouterListeners.put(placementId, routerListener);
    }

    public void removeRouterListener(String placementId) {
        sVungleRouterListeners.remove(placementId);
    }

    public boolean isAdPlayableForPlacement(String placementId) {
        return Vungle.canPlayAd(placementId);
    }

    public void playAdForPlacement(String placementId, AdConfig adConfig) {
        if (Vungle.canPlayAd(placementId)) {
            Vungle.playAd(placementId, adConfig, playAdCallback);
        } else {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "There should not be this case. playAdForPlacement is called " +
                    "before an ad is loaded for Placement ID: " + placementId);
        }
    }

    /**
     * Checks and returns if the passed Placement ID is a valid placement for App ID
     *
     * @param placementId
     * @return
     */
    public boolean isValidPlacement(String placementId) {
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
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "onAdStart - Placement ID: " + id);

            VungleRouterListener targetListener = sVungleRouterListeners.get(id);
            if (targetListener != null) {
                targetListener.onAdStart(id);
            } else {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "onAdStart - VungleRouterListener is not found for " +
                        "Placement ID: " + id);
            }
        }

        @Override
        public void onError(String id, Throwable error) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "onUnableToPlayAd - Placement ID: " + id, error);

            VungleRouterListener targetListener = sVungleRouterListeners.get(id);
            if (targetListener != null) {
                targetListener.onUnableToPlayAd(id, error.getLocalizedMessage());
            } else {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "onUnableToPlayAd - VungleRouterListener is not found " +
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
        public void onError(String id, Throwable cause) {
            onAdAvailabilityUpdate(id, false);
        }

        private void onAdAvailabilityUpdate(String placementReferenceId, boolean isAdAvailable) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "onAdAvailabilityUpdate - Placement ID: " +
                    placementReferenceId);

            VungleRouterListener targetListener = sVungleRouterListeners.get(placementReferenceId);
            if (targetListener != null) {
                targetListener.onAdAvailabilityUpdate(placementReferenceId, isAdAvailable);
            } else {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "onAdAvailabilityUpdate - VungleRouterListener is not " +
                        "found for Placement ID: " + placementReferenceId);
            }
        }
    };
}
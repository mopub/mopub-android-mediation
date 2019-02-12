package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.mopub.common.MoPub;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.privacy.ConsentStatus;
import com.mopub.common.privacy.PersonalInfoManager;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.metadata.MediationMetaData;
import com.unity3d.ads.metadata.MetaData;
import com.unity3d.services.banners.UnityBanners;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;

public class UnityRouter {
    private static final String GAME_ID_KEY = "gameId";
    private static final String ZONE_ID_KEY = "zoneId";
    private static final String PLACEMENT_ID_KEY = "placementId";
    private static final String ADAPTER_NAME = UnityRouter.class.getSimpleName();

    private static final UnityInterstitialCallbackRouter interstitialRouter = new UnityInterstitialCallbackRouter();
    private static final UnityBannerCallbackRouter bannerRouter = new UnityBannerCallbackRouter();

    static boolean initUnityAds(Map<String, String> serverExtras, Activity launcherActivity) {
        initGdpr(launcherActivity);

        String gameId = serverExtras.get(GAME_ID_KEY);
        if (gameId == null || gameId.isEmpty()) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "gameId is missing or entered incorrectly in the MoPub UI");
            return false;
        }
        initMediationMetadata(launcherActivity);
        UnityBanners.setBannerListener(bannerRouter);

        UnityAds.initialize(launcherActivity, gameId, interstitialRouter);
        return true;
    }

    static void initGdpr(Activity activity) {

        // Pass the user consent from the MoPub SDK to Unity Ads as per GDPR
        PersonalInfoManager personalInfoManager = MoPub.getPersonalInformationManager();

        boolean canCollectPersonalInfo = MoPub.canCollectPersonalInformation();
        boolean shouldAllowLegitimateInterest = MoPub.shouldAllowLegitimateInterest();

        if (personalInfoManager != null && personalInfoManager.gdprApplies() == Boolean.TRUE) {
            MetaData gdprMetaData = new MetaData(activity.getApplicationContext());

            if (shouldAllowLegitimateInterest) {
                if (personalInfoManager.getPersonalInfoConsentStatus() == ConsentStatus.EXPLICIT_NO
                        || personalInfoManager.getPersonalInfoConsentStatus() == ConsentStatus.DNT) {
                    gdprMetaData.set("gdpr.consent", false);
                } else {
                    gdprMetaData.set("gdpr.consent", true);
                }
            } else {
                gdprMetaData.set("gdpr.consent", canCollectPersonalInfo);
            }
            gdprMetaData.commit();
        }
    }

    static void initMediationMetadata(Context context) {
        MediationMetaData mediationMetaData = new MediationMetaData(context);
        mediationMetaData.setName("MoPub");
        mediationMetaData.setVersion(MoPub.SDK_VERSION);
        mediationMetaData.commit();
    }

    static String placementIdForServerExtras(Map<String, String> serverExtras, String defaultPlacementId) {
        String placementId = null;
        if (serverExtras.containsKey(PLACEMENT_ID_KEY)) {
            placementId = serverExtras.get(PLACEMENT_ID_KEY);
        } else if (serverExtras.containsKey(ZONE_ID_KEY)) {
            placementId = serverExtras.get(ZONE_ID_KEY);
        }
        return TextUtils.isEmpty(placementId) ? defaultPlacementId : placementId;
    }

    static UnityInterstitialCallbackRouter getInterstitialRouter() {
        return interstitialRouter;
    }

    static UnityBannerCallbackRouter getBannerRouter() {
        return bannerRouter;
    }

    static final class UnityAdsUtils {
        static MoPubErrorCode getMoPubErrorCode(UnityAds.UnityAdsError unityAdsError) {
            MoPubErrorCode errorCode;
            switch (unityAdsError) {
                case VIDEO_PLAYER_ERROR:
                    errorCode = MoPubErrorCode.VIDEO_PLAYBACK_ERROR;
                    break;
                case INVALID_ARGUMENT:
                    errorCode = MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
                    break;
                case INTERNAL_ERROR:
                    errorCode = MoPubErrorCode.NETWORK_INVALID_STATE;
                    break;
                default:
                    errorCode = MoPubErrorCode.NETWORK_NO_FILL;
                    break;
            }
            return errorCode;
        }
    }
}

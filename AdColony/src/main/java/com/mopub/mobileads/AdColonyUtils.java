package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAppOptions;
import com.mopub.common.MoPub;
import com.mopub.common.privacy.ConsentStatus;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.common.util.Json;

import java.util.Arrays;
import java.util.Map;

public class AdColonyUtils {

    /*
     * We recommend passing the AdColony client options, app ID, all zone IDs, and current zone ID
     * in the serverExtras Map by specifying Custom Event Data in MoPub's web interface.
     *
     * Please see AdColony's documentation for more information:
     * https://github.com/AdColony/AdColony-Android-SDK-3
     */
    protected static final String DEFAULT_CLIENT_OPTIONS = "version=YOUR_APP_VERSION_HERE,store:google";
    protected static final String DEFAULT_APP_ID = "YOUR_AD_COLONY_APP_ID_HERE";
    protected static final String[] DEFAULT_ALL_ZONE_IDS = {"ZONE_ID_1", "ZONE_ID_2", "..."};
    protected static final String DEFAULT_ZONE_ID = "YOUR_CURRENT_ZONE_ID";
    protected static final String CONSENT_RESPONSE = "consent_response";
    protected static final String CONSENT_GIVEN = "explicit_consent_given";

    /*
     * These keys are intended for MoPub internal use. Do not modify.
     */
    protected static final String CLIENT_OPTIONS_KEY = "clientOptions";
    protected static final String APP_ID_KEY = "appId";
    protected static final String ALL_ZONE_IDS_KEY = "allZoneIds";
    protected static final String ZONE_ID_KEY = "zoneId";


    protected static String[] previousAdColonyAllZoneIds;

    protected static boolean extrasAreValid(Map<String, String> extras) {
        return extras != null
                && extras.containsKey(APP_ID_KEY)
                && extras.containsKey(ALL_ZONE_IDS_KEY)
                && extras.containsKey(ZONE_ID_KEY);
    }

    protected static boolean shouldReconfigure(String[] previousZones, String[] newZones) {
        // If AdColony is configured already, but previousZones is null, then that means AdColony
        // was configured with the AdColonyRewardedVideo adapter so attempt to configure with
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

    protected static String[] extractAllZoneIds(Map<String, String> serverExtras) {
        String[] result = Json.jsonArrayToStringArray(serverExtras.get(ALL_ZONE_IDS_KEY));

        // AdColony requires at least one valid String in the allZoneIds array.
        if (result.length == 0) {
            result = new String[]{""};
        }

        return result;
    }

    protected static AdColonyAppOptions getAdColonyAppOptions(String clientOptions) {
        AdColonyAppOptions adColonyAppOptions = AdColonyAppOptions.getMoPubAppOptions(clientOptions);

        // Pass the user consent from the MoPub SDK to AdColony as per GDPR
        PersonalInfoManager personalInfoManager = MoPub.getPersonalInformationManager();

        boolean canCollectPersonalInfo = MoPub.canCollectPersonalInformation();
        boolean shouldAllowLegitimateInterest = MoPub.shouldAllowLegitimateInterest();

        adColonyAppOptions = adColonyAppOptions == null ? new AdColonyAppOptions() :
                adColonyAppOptions;

        if (personalInfoManager != null && personalInfoManager.gdprApplies() == Boolean.TRUE) {
            if (shouldAllowLegitimateInterest) {
                if (personalInfoManager.getPersonalInfoConsentStatus() == ConsentStatus.EXPLICIT_NO
                        || personalInfoManager.getPersonalInfoConsentStatus() == ConsentStatus.DNT) {
                    adColonyAppOptions.setOption(AdColonyUtils.CONSENT_GIVEN, true)
                            .setOption(AdColonyUtils.CONSENT_RESPONSE, false);
                } else {
                    adColonyAppOptions.setOption(AdColonyUtils.CONSENT_GIVEN, true)
                            .setOption(AdColonyUtils.CONSENT_RESPONSE, true);
                }
            } else {
                adColonyAppOptions.setOption(AdColonyUtils.CONSENT_GIVEN, true)
                        .setOption(AdColonyUtils.CONSENT_RESPONSE, canCollectPersonalInfo);
            }
        }

        return adColonyAppOptions;
    }

    protected static boolean isAdColonyConfigured() {
        return !AdColony.getSDKVersion().isEmpty();
    }

    protected static void checkAndConfigureAdColony(Context context, String clientOptions, String appId, String[] allZoneIds)
    {
        AdColonyAppOptions mAdColonyAppOptions = AdColonyUtils.getAdColonyAppOptions(clientOptions);

        if (!isAdColonyConfigured()) {
            if (!TextUtils.isEmpty(appId)) {
                AdColony.configure((Activity) context, mAdColonyAppOptions, appId, allZoneIds);
            }
        } else if ((AdColonyUtils.shouldReconfigure(AdColonyUtils.previousAdColonyAllZoneIds, allZoneIds))) {
            // Need to check the zone IDs sent from the MoPub portal and reconfigure if they are
            // different than the zones we initially called AdColony.configure() with
            if (!TextUtils.isEmpty(appId)) {
                AdColony.configure((Activity) context, mAdColonyAppOptions, appId, allZoneIds);
            }
            AdColonyUtils.previousAdColonyAllZoneIds = allZoneIds;
        } else {
            // If state of consent has changed and we aren't calling configure again, we need
            // to pass this via setAppOptions()
            AdColony.setAppOptions(mAdColonyAppOptions);
        }
    }
}

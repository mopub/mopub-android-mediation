package com.mopub.mobileads;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.MoPub;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.privacy.ConsentStatus;
import com.mopub.common.privacy.PersonalInfoManager;
import com.ogury.cm.OguryChoiceManagerExternal;
import com.ogury.sdk.Ogury;
import com.ogury.sdk.OguryConfiguration;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.mobileads.OguryAdapterConfiguration.ADAPTER_NAME;
import static com.mopub.mobileads.OguryAdapterConfiguration.ADAPTER_VERSION;

public class OguryInitializer {
    // Monitoring constants
    private static final String MONITORING_KEY_MODULE_VERSION = "mopub_ce_version";
    private static final String MONITORING_KEY_MOPUB_VERSION = "mopub_mediation_version";

    private static final String CHOICE_MANAGER_CONSENT_ORIGIN = "MOPUB";

    private static boolean sInitialized = false;

    public static boolean startOgurySDK(@NonNull Context context, @Nullable String assetKey) {
        Preconditions.checkNotNull(context);

        if (!OguryConfigurationParser.isAssetKeyValid(assetKey)) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Ogury's initialization not started as the asset key is missing or empty.");
            return false;
        }

        if (sInitialized) {
            return true;
        }
        sInitialized = true;

        OguryConfiguration oguryConfiguration = new OguryConfiguration.Builder(context, assetKey)
                .putMonitoringInfo(MONITORING_KEY_MODULE_VERSION, ADAPTER_VERSION)
                .putMonitoringInfo(MONITORING_KEY_MOPUB_VERSION, getMoPubVersion())
                .build();

        Ogury.start(oguryConfiguration);
        return true;
    }

    public static void startOgurySDKIfNecessary(@NonNull Context context, @NonNull Map<String, String> adDataExtras) {
        if (sInitialized) {
            return;
        }
        String assetKey = OguryConfigurationParser.getAssetKey(adDataExtras);
        startOgurySDK(context, assetKey);
    }

    public static void updateConsent() {
        if (!sInitialized) {
            return;
        }
        PersonalInfoManager personalInfoManager = MoPub.getPersonalInformationManager();
        if (personalInfoManager != null) {
            Boolean gdprApplies = personalInfoManager.gdprApplies();
            if (gdprApplies != null && gdprApplies) {
                OguryChoiceManagerExternal.setConsent(
                        MoPub.canCollectPersonalInformation(),
                        CHOICE_MANAGER_CONSENT_ORIGIN
                );
            }
        }
    }


    /**
     * Retrieve MoPub version using reflection to know the exact version available in the application.
     * <p>
     * Using the constant instead will make the compiler replace the value in the produced binary.
     * By using reflection, we are able to obtain the version integrated by the final user.
     *
     * @return the version of MoPub integrated in the final user's application.
     */
    private static String getMoPubVersion() {
        String mopubVersion = "";
        try {
            mopubVersion = (String) MoPub.class.getDeclaredField("SDK_VERSION").get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Ignore errors since it is not recoverable.
        }
        return mopubVersion;
    }
}

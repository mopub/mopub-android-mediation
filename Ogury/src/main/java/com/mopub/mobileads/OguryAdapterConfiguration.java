package com.mopub.mobileads;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.MoPub;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.mobileads.ogury.BuildConfig;
import com.ogury.cm.OguryChoiceManagerExternal;
import com.ogury.sdk.Ogury;
import com.ogury.sdk.OguryConfiguration;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS;

public class OguryAdapterConfiguration extends BaseAdapterConfiguration {
    // Adapter's constants
    public static final String ADAPTER_NAME = OguryAdapterConfiguration.class.getSimpleName();
    public static final String ADAPTER_VERSION = BuildConfig.VERSION_NAME;
    private static final String MOPUB_NETWORK_NAME = BuildConfig.NETWORK_NAME;

    // Configuration constants
    private static final String CONFIG_KEY_AD_UNIT_ID = "ad_unit_id";
    private static final String CONFIG_KEY_ASSET_KEY = "asset_key";

    // Monitoring constants
    private static final String MONITORING_KEY_MODULE_VERSION = "mopub_ce_version";
    private static final String MONITORING_KEY_MOPUB_VERSION = "mopub_mediation_version";

    private static final String CHOICE_MANAGER_CONSENT_ORIGIN = "MOPUB";

    private static boolean sInitialized = false;

    @NonNull
    @Override
    public String getMoPubNetworkName() {
        return MOPUB_NETWORK_NAME;
    }

    @NonNull
    @Override
    public String getNetworkSdkVersion() {
        final String sdkVersion = Ogury.getSdkVersion();

        if (!TextUtils.isEmpty(sdkVersion)) {
            return sdkVersion;
        }

        final String adapterVersion = getAdapterVersion();
        return adapterVersion.substring(0, adapterVersion.lastIndexOf('.'));
    }

    @NonNull
    @Override
    public String getAdapterVersion() {
        return ADAPTER_VERSION;
    }

    @Nullable
    @Override
    public String getBiddingToken(@NonNull Context context) {
        return null;
    }

    @Override
    public void initializeNetwork(@NonNull Context context, @Nullable Map<String, String> configuration, @NonNull OnNetworkInitializationFinishedListener listener) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);

        String assetKey = null;
        if (configuration != null && !configuration.isEmpty()) {
            assetKey = configuration.get(CONFIG_KEY_ASSET_KEY);
        }

        if (startOgurySDK(context, assetKey)) {
            listener.onNetworkInitializationFinished(OguryAdapterConfiguration.class, ADAPTER_INITIALIZATION_SUCCESS);
        } else {
            listener.onNetworkInitializationFinished(OguryAdapterConfiguration.class, ADAPTER_CONFIGURATION_ERROR);
        }
    }

    public static boolean startOgurySDK(@NonNull Context context, @Nullable String assetKey) {
        Preconditions.checkNotNull(context);

        if (!isAssetKeyValid(assetKey)) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Ogury's initialization not started as the asset key is missing or empty. " +
                    "Make sure to copy the asset key from the Ogury dashboard into your MoPub configuration.");
            return false;
        }

        if (sInitialized) {
            return true;
        }
        sInitialized = true;

        final OguryConfiguration oguryConfiguration = new OguryConfiguration.Builder(context, assetKey)
                .putMonitoringInfo(MONITORING_KEY_MODULE_VERSION, ADAPTER_VERSION)
                .putMonitoringInfo(MONITORING_KEY_MOPUB_VERSION, getMoPubVersion())
                .build();

        Ogury.start(oguryConfiguration);
        return true;
    }

    public static boolean startOgurySDKIfNecessary(@NonNull Context context, @NonNull Map<String, String> adDataExtras) {
        if (sInitialized) {
            return false;
        }
        final String assetKey = getAssetKey(adDataExtras);
        startOgurySDK(context, assetKey);
        return true;
    }

    public static void updateConsent() {
        if (!sInitialized) {
            return;
        }
        final PersonalInfoManager personalInfoManager = MoPub.getPersonalInformationManager();
        if (personalInfoManager != null) {
            final Boolean gdprApplies = personalInfoManager.gdprApplies();
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

    @Nullable
    public static String getAdUnitId(Map<String, String> adDataExtras) {
        if (adDataExtras != null && !adDataExtras.isEmpty()) {
            return adDataExtras.get(CONFIG_KEY_AD_UNIT_ID);
        }
        return null;
    }

    @Nullable
    public static String getAssetKey(Map<String, String> adDataExtras) {
        if (adDataExtras != null && !adDataExtras.isEmpty()) {
            return adDataExtras.get(CONFIG_KEY_ASSET_KEY);
        }
        return null;
    }

    public static boolean isAssetKeyValid(@Nullable String assetKey) {
        return !TextUtils.isEmpty(assetKey);
    }

    public static boolean isAdUnitIdValid(@Nullable String adUnitId) {
        return !TextUtils.isEmpty(adUnitId);
    }
}

package com.mopub.mobileads;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.MoPub;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.ogury.BuildConfig;
import com.ogury.sdk.Ogury;
import com.ogury.sdk.OguryConfiguration;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class OguryAdapterConfiguration extends BaseAdapterConfiguration {
    public static final String CHOICE_MANAGER_CONSENT_ORIGIN = "MOPUB";

    private static final String ADAPTER_NAME = OguryAdapterConfiguration.class.getSimpleName();
    private static final String ADAPTER_VERSION = BuildConfig.VERSION_NAME;
    private static final String MOPUB_NETWORK_NAME = BuildConfig.NETWORK_NAME;

    // Configuration constants
    private static final String AD_UNIT_ID_KEY = "ad_unit_id";
    private static final String ASSET_KEY = "asset_key";

    // Monitoring constants
    private static final String MODULE_VERSION_KEY = "mopub_ce_version";
    private static final String MOPUB_VERSION_KEY = "mopub_mediation_version";

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
    public void initializeNetwork(@NonNull Context context, @Nullable Map<String, String> configuration,
                                  @NonNull OnNetworkInitializationFinishedListener listener) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);

        synchronized (OguryAdapterConfiguration.class) {
            try {
                if (configuration != null && !configuration.isEmpty()) {
                    final String assetKey = configuration.get(ASSET_KEY);

                    if (TextUtils.isEmpty(assetKey)) {
                        MoPubLog.log(CUSTOM, ADAPTER_NAME, "Ogury's initialization skipped as the " +
                                "asset key is missing or empty. Make sure to add the asset key from the Ogury " +
                                "dashboard into your MoPub initialization configuration call.");
                        return;
                    }

                    final OguryConfiguration oguryConfiguration = new OguryConfiguration.Builder(context, assetKey)
                            .putMonitoringInfo(MODULE_VERSION_KEY, ADAPTER_VERSION)
                            .putMonitoringInfo(MOPUB_VERSION_KEY, MoPub.SDK_VERSION)
                            .build();

                    Ogury.start(oguryConfiguration);

                    sInitialized = true;
                }
            } catch (Throwable t) {
                MoPubLog.log(
                        CUSTOM_WITH_THROWABLE,
                        "Initializing Ogury has encountered an exception.", t);
            }
        }

        if (sInitialized) {
            listener.onNetworkInitializationFinished(OguryAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
        } else {
            listener.onNetworkInitializationFinished(OguryAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }
    }

    /**
     * Initialize Ogury if needed before each ad request
     *
     * @param context      The current context
     * @param adDataExtras Ogury-specific data returned from MoPub's server
     * @return whether Ogury has initialized
     */
    public static boolean startOgurySDKIfNecessary(@NonNull Context context,
                                                   @NonNull Map<String, String> adDataExtras) {
        if (sInitialized) {
            return false;
        }

        final String assetKey = getAssetKey(adDataExtras);

        if (TextUtils.isEmpty(assetKey)) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Ogury's initialization skipped as the " +
                    "asset key is missing or empty. Make sure to add the asset key from the Ogury " +
                    "dashboard into your MoPub initialization configuration call.");
            return false;
        }

        final OguryConfiguration oguryConfiguration = new OguryConfiguration.Builder(context, assetKey)
                .putMonitoringInfo(MODULE_VERSION_KEY, ADAPTER_VERSION)
                .putMonitoringInfo(MOPUB_VERSION_KEY, MoPub.SDK_VERSION)
                .build();

        Ogury.start(oguryConfiguration);

        return true;
    }

    @Nullable
    public static String getAdUnitId(Map<String, String> adDataExtras) {
        if (adDataExtras != null && !adDataExtras.isEmpty()) {
            return adDataExtras.get(AD_UNIT_ID_KEY);
        }
        return null;
    }

    @Nullable
    public static String getAssetKey(Map<String, String> adDataExtras) {
        if (adDataExtras != null && !adDataExtras.isEmpty()) {
            return adDataExtras.get(ASSET_KEY);
        }
        return null;
    }

    public static boolean initialized() {
        return sInitialized;
    }
}

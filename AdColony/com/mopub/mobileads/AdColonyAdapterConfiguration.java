package com.mopub.mobileads;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAppOptions;
import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Json;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class AdColonyAdapterConfiguration extends BaseAdapterConfiguration {

    // AdColony's keys
    private static final String CLIENT_OPTIONS_KEY = "clientOptions";
    private static final String APP_ID_KEY = "appId";
    private static final String ALL_ZONE_IDS_KEY = "allZoneIds";

    // Adapter's keys
    private static final String ADAPTER_NAME = AdColonyAdapterConfiguration.class.getSimpleName();
    private static final String ADAPTER_VERSION = "3.3.7.1";
    private static final String BIDDING_TOKEN = "1";
    private static final String MOPUB_NETWORK_NAME = "adcolony";

    @NonNull
    @Override
    public String getAdapterVersion() {
        return ADAPTER_VERSION;
    }

    @Nullable
    @Override
    public String getBiddingToken(@NonNull final Context context) {
        return BIDDING_TOKEN;
    }

    @NonNull
    @Override
    public String getMoPubNetworkName() {
        return MOPUB_NETWORK_NAME;
    }

    @NonNull
    @Override
    public String getNetworkSdkVersion() {
        final String sdkVersion = AdColony.getSDKVersion();

        if (!TextUtils.isEmpty(sdkVersion)) {
            return sdkVersion;
        }

        final String adapterVersion = getAdapterVersion();
        return adapterVersion.substring(0, adapterVersion.lastIndexOf('.'));
    }

    @Override
    public void initializeNetwork(@NonNull final Context context,
                                  @Nullable final Map<String, String> configuration,
                                  @NonNull final OnNetworkInitializationFinishedListener listener) {

        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);

        boolean networkInitializationSucceeded = false;

        synchronized (AdColonyAdapterConfiguration.class) {
            try {
                if (isAdColonyConfigured()) {
                    networkInitializationSucceeded = true;
                } else if (configuration != null) {
                    final String adColonyClientOptions = configuration.get(CLIENT_OPTIONS_KEY);
                    final String adColonyAppId = configuration.get(APP_ID_KEY);
                    final String[] adColonyAllZoneIds = extractAllZoneIds(configuration);

                    if (TextUtils.isEmpty(adColonyClientOptions) || TextUtils.isEmpty(adColonyAppId)
                            || adColonyAllZoneIds.length == 0) {
                        MoPubLog.log(CUSTOM, ADAPTER_NAME, "AdColony's initialization not " +
                                "started. Ensure AdColony's appId, zoneId, and/or clientOptions " +
                                "are populated on the MoPub dashboard. Note that initialization " +
                                "on the first app launch is a no-op.");
                    } else {
                        final AdColonyAppOptions adColonyAppOptions =
                                AdColonyAppOptions.getMoPubAppOptions(adColonyClientOptions);

                        AdColony.configure((Application) context.getApplicationContext(),
                                adColonyAppOptions, adColonyAppId, adColonyAllZoneIds);
                        networkInitializationSucceeded = true;
                    }
                }
            } catch (Exception e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Initializing AdColony has encountered " +
                        "an exception.", e);
            }
        }

        if (networkInitializationSucceeded) {
            listener.onNetworkInitializationFinished(AdColonyAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
        } else {
            listener.onNetworkInitializationFinished(AdColonyAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }
    }

    private boolean isAdColonyConfigured() {
        return !AdColony.getSDKVersion().isEmpty();
    }

    @NonNull
    private static String[] extractAllZoneIds(@NonNull final Map<String, String> serverExtras) {
        Preconditions.checkNotNull(serverExtras);

        String[] result = Json.jsonArrayToStringArray(serverExtras.get(ALL_ZONE_IDS_KEY));

        // AdColony requires at least one valid String in the allZoneIds array.
        if (result.length == 0) {
            result = new String[]{""};
        }

        return result;
    }
}

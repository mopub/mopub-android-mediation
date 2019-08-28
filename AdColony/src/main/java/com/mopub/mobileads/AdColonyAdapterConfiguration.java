package com.mopub.mobileads;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAppOptions;
import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.MoPub;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.privacy.ConsentStatus;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.common.util.Json;
import com.mopub.mobileads.adcolony.BuildConfig;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class AdColonyAdapterConfiguration extends BaseAdapterConfiguration {

    // Adapter's keys
    private static final String ADAPTER_NAME = AdColonyAdapterConfiguration.class.getSimpleName();
    private static final String ADAPTER_VERSION = BuildConfig.VERSION_NAME;
    private static final String BIDDING_TOKEN = "1";
    private static final String MOPUB_NETWORK_NAME = BuildConfig.NETWORK_NAME;

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
                    final String adColonyClientOptions = configuration.get(AdColonyUtils.CLIENT_OPTIONS_KEY);
                    final String adColonyAppId = configuration.get(AdColonyUtils.APP_ID_KEY);
                    final String[] adColonyAllZoneIds = extractAllZoneIds(configuration);

                    if (TextUtils.isEmpty(adColonyAppId)
                            || adColonyAllZoneIds.length == 0) {
                        MoPubLog.log(CUSTOM, ADAPTER_NAME, "AdColony's initialization not " +
                                "started. Ensure AdColony's appId, zoneId, and/or clientOptions " +
                                "are populated on the MoPub dashboard. Note that initialization " +
                                "on the first app launch is a no-op.");
                    } else {
                        AdColonyAppOptions adColonyAppOptions = AdColonyUtils.getAdColonyAppOptions(adColonyClientOptions);

                        AdColony.configure((Application) context.getApplicationContext(),
                                adColonyAppOptions, adColonyAppId, adColonyAllZoneIds);
                        networkInitializationSucceeded = true;
                        AdColonyUtils.previousAdColonyAllZoneIds = adColonyAllZoneIds;
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

        String[] result = Json.jsonArrayToStringArray(serverExtras.get(AdColonyUtils.ALL_ZONE_IDS_KEY));

        // AdColony requires at least one valid String in the allZoneIds array.
        if (result.length == 0) {
            result = new String[]{""};
        }

        return result;
    }
}

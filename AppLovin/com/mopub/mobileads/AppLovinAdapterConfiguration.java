package com.mopub.mobileads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.applovin.sdk.AppLovinAdService;
import com.applovin.sdk.AppLovinMediationProvider;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkSettings;
import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class AppLovinAdapterConfiguration extends BaseAdapterConfiguration {

    private static final String ADAPTER_VERSION = "9.1.3.1";
    private static final String APPLOVIN_SDK_KEY = "sdk_key";
    private static final String MOPUB_NETWORK_NAME = "applovin_sdk";
    private static final String APPLOVIN_PLUGIN_VERSION = "MoPub-3.1.0";

    @NonNull
    @Override
    public String getAdapterVersion() {
        return ADAPTER_VERSION;
    }

    @Nullable
    @Override
    public String getBiddingToken(@NonNull Context context) {

        Preconditions.checkNotNull(context);

        final AppLovinSdk sdk = AppLovinSdk.getInstance(context);
        if (sdk == null) {
            return null;
        }

        final AppLovinAdService adService = sdk.getAdService();
        if (adService == null) {
            return null;
        }

        return adService.getBidToken();
    }

    @NonNull
    @Override
    public String getMoPubNetworkName() {
        return MOPUB_NETWORK_NAME;
    }

    @NonNull
    @Override
    public String getNetworkSdkVersion() {
        final String sdkVersion = AppLovinSdk.VERSION;

        if (!TextUtils.isEmpty(sdkVersion)) {
            return sdkVersion;
        }

        final String adapterVersion = getAdapterVersion();
        return adapterVersion.substring(0, adapterVersion.lastIndexOf('.'));
    }

    @Override
    public void initializeNetwork(@NonNull Context context, @Nullable Map<String, String>
            configuration, @NonNull OnNetworkInitializationFinishedListener listener) {

        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);

        boolean networkInitializationSucceeded = false;

        synchronized (AppLovinAdapterConfiguration.class) {
            try {
                AppLovinSdk.initializeSdk(context);

                final AppLovinSdk sdk;

                if (configuration != null) {
                    final String sdkKey = configuration.get(APPLOVIN_SDK_KEY);

                    if (!TextUtils.isEmpty(sdkKey)) {
                        sdk = AppLovinSdk.getInstance(sdkKey, new AppLovinSdkSettings(), context);
                    } else {
                        sdk = AppLovinSdk.getInstance(context);
                    }
                } else {
                    sdk = AppLovinSdk.getInstance(context);
                }

                if (sdk != null) {
                    sdk.setPluginVersion(APPLOVIN_PLUGIN_VERSION);
                    sdk.setMediationProvider(AppLovinMediationProvider.MOPUB);
                }

                networkInitializationSucceeded = true;
            } catch (Exception e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Initializing AppLovin has encountered " +
                        "an exception.", e);
            }
        }

        if (networkInitializationSucceeded) {
            listener.onNetworkInitializationFinished(AppLovinAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
        } else {
            listener.onNetworkInitializationFinished(AppLovinAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }
    }
}

package com.mopub.mobileads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.tapjoy.TJConnectListener;
import com.tapjoy.Tapjoy;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class TapjoyAdapterConfiguration extends BaseAdapterConfiguration {

    // Tapjoy's Configuration keys
    private static final String SDK_KEY = "sdkKey";

    // Adapter's keys
    private static final String ADAPTER_VERSION = "12.2.0.1";
    private static final String BIDDING_TOKEN = "1";
    private static final String MOPUB_NETWORK_NAME = "tapjoy";

    @NonNull
    @Override
    public String getAdapterVersion() {
        return ADAPTER_VERSION;
    }

    @Nullable
    @Override
    public String getBiddingToken(@NonNull Context context) {
        Preconditions.checkNotNull(context);
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
        final String sdkVersion = Tapjoy.getVersion();
        if (!TextUtils.isEmpty(sdkVersion)) {
            return sdkVersion;
        }
        final String adapterVersion = getAdapterVersion();
        return adapterVersion.substring(0, adapterVersion.lastIndexOf('.'));
    }

    @Override
    public void initializeNetwork(@NonNull final Context context, @Nullable final Map<String, String> configuration, @NonNull final OnNetworkInitializationFinishedListener listener) {

        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);

        boolean networkInitializationSucceeded = false;

        synchronized (TapjoyAdapterConfiguration.class) {
            try {
                if (Tapjoy.isConnected()) {
                    networkInitializationSucceeded = true;
                } else if (configuration != null) {
                    final String sdkKey = configuration.get(SDK_KEY);
                    Tapjoy.connect(context, sdkKey, null, new TJConnectListener() {
                        @Override
                        public void onConnectSuccess() {
                            listener.onNetworkInitializationFinished(TapjoyAdapterConfiguration.class,
                                    MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
                        }

                        @Override
                        public void onConnectFailure() {
                            listener.onNetworkInitializationFinished(TapjoyAdapterConfiguration.class,
                                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                            MoPubLog.log(CUSTOM, "Initializing Tapjoy has encountered a problem.");
                        }
                    });
                }
            } catch (Exception e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE,
                        "Initializing Tapjoy has encountered an exception.", e);
                networkInitializationSucceeded = false;
            }
        }

        if (networkInitializationSucceeded) {
            listener.onNetworkInitializationFinished(TapjoyAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
        } else {
            listener.onNetworkInitializationFinished(TapjoyAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }
    }
}

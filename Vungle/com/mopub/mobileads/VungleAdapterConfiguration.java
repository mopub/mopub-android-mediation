package com.mopub.mobileads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.vungle.warren.Vungle;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;

public class VungleAdapterConfiguration extends BaseAdapterConfiguration {

    // Vungle's keys
    private static final String APP_ID_KEY = "appId";
    // Adapter's keys
    private static final String ADAPTER_NAME = VungleAdapterConfiguration.class.getSimpleName();
    private static final String ADAPTER_VERSION = "6.3.24.1";
    private static final String MOPUB_NETWORK_NAME = "vungle";

    private static VungleRouter sVungleRouter;

    public VungleAdapterConfiguration() {
        sVungleRouter = VungleRouter.getInstance();
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

    @NonNull
    @Override
    public String getMoPubNetworkName() {
        return MOPUB_NETWORK_NAME;
    }

    @NonNull
    @Override
    public String getNetworkSdkVersion() {
        return com.vungle.warren.BuildConfig.VERSION_NAME;
    }

    @Override
    public void initializeNetwork(@NonNull final Context context, @Nullable final Map<String, String> configuration, @NonNull final OnNetworkInitializationFinishedListener listener) {

        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);

        boolean networkInitializationSucceeded = false;

        synchronized (VungleAdapterConfiguration.class) {
            try {
                if (Vungle.isInitialized()) {
                    networkInitializationSucceeded = true;

                } else if (configuration != null && sVungleRouter != null) {
                    final String mAppId = configuration.get(APP_ID_KEY);
                    if (TextUtils.isEmpty(mAppId)) {
                        MoPubLog.log(CUSTOM, ADAPTER_NAME, "Vungle's initialization not " +
                                "started. Ensure Vungle's appId is populated");
                        listener.onNetworkInitializationFinished(this.getClass(),
                                MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                        return;
                    }
                    if (!sVungleRouter.isVungleInitialized()) {
                        sVungleRouter.initVungle(context, mAppId);

                        networkInitializationSucceeded = true;
                    }
                }
            } catch (Exception e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Initializing Vungle has encountered" +
                        "an exception.", e);
            }
        }
        if (networkInitializationSucceeded) {
            listener.onNetworkInitializationFinished(this.getClass(),
                    MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
        } else {
            listener.onNetworkInitializationFinished(this.getClass(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }
    }
}

package com.mopub.mobileads;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.verizon.BuildConfig;
import com.verizon.ads.Logger;
import com.verizon.ads.SDKInfo;
import com.verizon.ads.VASAds;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class VerizonAdapterConfiguration extends BaseAdapterConfiguration {

    public static final String ADAPTER_VERSION = BuildConfig.VERSION_NAME;
    public static final String MEDIATOR_ID = "MoPubVAS-" + ADAPTER_VERSION;

    private static final String ADAPTER_NAME = VerizonAdapterConfiguration.class.getSimpleName();
    private static final String MOPUB_NETWORK_NAME = BuildConfig.NETWORK_NAME;
    private static final String SITE_ID = "siteId";

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
        final SDKInfo sdkInfo = VASAds.getSDKInfo();

        if (sdkInfo != null) {
            return sdkInfo.version;
        }

        final String adapterVersion = getAdapterVersion();
        return (!TextUtils.isEmpty(adapterVersion)) ? adapterVersion.substring(0, adapterVersion.lastIndexOf('.')) : "";
    }

    @Override
    public void initializeNetwork(@NonNull final Context context, @Nullable final Map<String, String> configuration,
                                  @NonNull final OnNetworkInitializationFinishedListener listener) {

        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);

        boolean networkInitializationSucceeded = false;

        synchronized (VerizonAdapterConfiguration.class) {
            try {
                if (VASAds.isInitialized()) {
                    networkInitializationSucceeded = true;
                } else if (configuration != null && context instanceof Application) {
                    final String siteId = configuration.get(SITE_ID);

                    if (TextUtils.isEmpty(siteId)) {
                        MoPubLog.log(CUSTOM, ADAPTER_NAME, "Verizon's initialization not " +
                                "started. Ensure Verizon's siteId is populated on the MoPub " +
                                "dashboard. Note that initialization on the first app launch is a no-op.");
                    } else {
                        VASAds.initialize((Application) context, siteId);
                        networkInitializationSucceeded = true;
                    }
                }
            } catch (Exception e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Initializing Verizon has encountered " +
                        "an exception.", e);
            }
        }

        if (networkInitializationSucceeded) {
            listener.onNetworkInitializationFinished(VerizonAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
        } else {
            listener.onNetworkInitializationFinished(VerizonAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }

        final MoPubLog.LogLevel mopubLogLevel = MoPubLog.getLogLevel();

        if (mopubLogLevel == MoPubLog.LogLevel.DEBUG) {
            VASAds.setLogLevel(Logger.DEBUG);
        } else if (mopubLogLevel == MoPubLog.LogLevel.INFO) {
            VASAds.setLogLevel(Logger.INFO);
        }
    }
}

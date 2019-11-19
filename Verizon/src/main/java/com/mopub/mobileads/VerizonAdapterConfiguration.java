package com.mopub.mobileads;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.text.TextUtils;

import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.verizon.BuildConfig;
import com.verizon.ads.Configuration;
import com.verizon.ads.Logger;
import com.verizon.ads.VASAds;
import com.verizon.ads.edition.StandardEdition;
import com.verizon.ads.utils.ThreadUtils;

import java.util.Map;

public class VerizonAdapterConfiguration extends BaseAdapterConfiguration {

    private static final String ADAPTER_VERSION = BuildConfig.VERSION_NAME;
    private static final String MOPUB_NETWORK_NAME = BuildConfig.NETWORK_NAME;

    public static final String MEDIATOR_ID = "MoPubVAS-" + ADAPTER_VERSION;
    public static final String VAS_SITE_ID_KEY = "siteId";

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

        final String editionVersion = Configuration.getString("com.verizon.ads",
                "editionVersion", null);

        final String editionName = Configuration.getString("com.verizon.ads",
                "editionName", null);

        if (!TextUtils.isEmpty(editionVersion) && !TextUtils.isEmpty(editionName)) {
            return editionName + "-" + editionVersion;
        }

        final String adapterVersion = getAdapterVersion();
        return (!TextUtils.isEmpty(adapterVersion)) ? adapterVersion.substring(0,
                adapterVersion.lastIndexOf('.')) : "";
    }

    @Override
    public void initializeNetwork(@NonNull final Context context,
                                  @Nullable final Map<String, String> configuration,
                                  @NonNull final OnNetworkInitializationFinishedListener listener) {

        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);

        final MoPubLog.LogLevel mopubLogLevel = MoPubLog.getLogLevel();

        if (mopubLogLevel == MoPubLog.LogLevel.DEBUG) {
            VASAds.setLogLevel(Logger.DEBUG);
        } else if (mopubLogLevel == MoPubLog.LogLevel.INFO) {
            VASAds.setLogLevel(Logger.INFO);
        }

        String siteId = null;

        if (configuration != null) {
            siteId = configuration.get(VAS_SITE_ID_KEY);
        }

        // The Verizon SDK needs a meaningful siteId to initialize. siteId is cached on the first request.
        if (TextUtils.isEmpty(siteId)) {
            listener.onNetworkInitializationFinished(VerizonAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);

            return;
        }

        final String finalSiteId = siteId;
        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (context instanceof Application && StandardEdition.initialize((Application) context, finalSiteId)) {
                    listener.onNetworkInitializationFinished(VerizonAdapterConfiguration.class,
                            MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
                } else {
                    listener.onNetworkInitializationFinished(VerizonAdapterConfiguration.class,
                            MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
            }
        });
    }
}

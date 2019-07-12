package com.mopub.mobileads;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.android.gms.ads.MobileAds;
import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.admob.BuildConfig;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class GooglePlayServicesAdapterConfiguration extends BaseAdapterConfiguration {

    private static final String ADAPTER_VERSION = BuildConfig.VERSION_NAME;
    private static final String KEY_EXTRA_APPLICATION_ID = "appid";
    private static final String KEY_NPA = "npa";
    private static final String MOPUB_NETWORK_NAME = BuildConfig.NETWORK_NAME;

    private static Bundle npaBundle;

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
        /* com.google.android.gms:play-services-ads (AdMob) does not have an API to get the compiled
        version */
        final String adapterVersion = getAdapterVersion();

        return (!TextUtils.isEmpty(adapterVersion)) ?
                adapterVersion.substring(0, adapterVersion.lastIndexOf('.')) : "";
    }

    @Override
    public void initializeNetwork(@NonNull Context context, @Nullable Map<String, String>
            configuration, @NonNull OnNetworkInitializationFinishedListener listener) {

        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);

        boolean networkInitializationSucceeded = false;

        synchronized (GooglePlayServicesAdapterConfiguration.class) {
            try {
                if (configuration != null && !configuration.isEmpty()) {
                    String appId = configuration.get(KEY_EXTRA_APPLICATION_ID);

                    if (!TextUtils.isEmpty(appId)) {
                        MobileAds.initialize(context, configuration.get(KEY_EXTRA_APPLICATION_ID));
                    }

                    String npaValue = configuration.get(KEY_NPA);

                    setNpaBundle(npaValue);
                } else {
                    MobileAds.initialize(context);
                }

                networkInitializationSucceeded = true;
            } catch (Exception e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Initializing AdMob has encountered " +
                        "an exception.", e);
            }
        }

        if (networkInitializationSucceeded) {
            listener.onNetworkInitializationFinished(GooglePlayServicesAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
        } else {
            listener.onNetworkInitializationFinished(GooglePlayServicesAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }
    }

    public static Bundle getNpaBundle() {
        return npaBundle;
    }

    private static void setNpaBundle(String npaValue) {
        npaBundle = new Bundle();
        npaBundle.putString(KEY_NPA, npaValue);
    }
}

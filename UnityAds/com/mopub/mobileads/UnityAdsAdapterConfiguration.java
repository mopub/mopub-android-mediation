package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.unity3d.ads.UnityAds;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class UnityAdsAdapterConfiguration extends BaseAdapterConfiguration {

    // Adapter's keys
    private static final String ADAPTER_VERSION = "3.0.0.1";
    private static final String MOPUB_NETWORK_NAME = "unity";
    private static final String ADAPTER_NAME = UnityAdsAdapterConfiguration.class.getSimpleName();

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
        final String sdkVersion = UnityAds.getVersion();

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

        synchronized (UnityAdsAdapterConfiguration.class) {
            try {
                if (UnityAds.isInitialized()) {
                    networkInitializationSucceeded = true;
                } else if (configuration != null && context instanceof Activity) {
                    UnityRouter.initUnityAds(configuration, (Activity) context);

                    networkInitializationSucceeded = true;
                } else {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity Ads initialization not started. " +
                            "Context is not an Activity. Note that initialization on the first app launch is a no-op.");
                }
            } catch (Exception e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Initializing Unity Ads has encountered " +
                        "an exception.", e);
            }
        }
        if (networkInitializationSucceeded) {
            listener.onNetworkInitializationFinished(UnityAdsAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
        } else {
            listener.onNetworkInitializationFinished(UnityAdsAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }
    }
}

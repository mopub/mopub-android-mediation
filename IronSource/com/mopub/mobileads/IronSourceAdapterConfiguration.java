package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.utils.IronSourceUtils;
import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class IronSourceAdapterConfiguration extends BaseAdapterConfiguration {

    // ironSource's keys
    private static final String APPLICATION_KEY = "applicationKey";
    private static final String MEDIATION_TYPE = "mopub";
    private static final String IRONSOURCE_ADAPTER_VERSION = "300";

    // Adapter's keys
    private static final String ADAPTER_VERSION = "6.8.0.1.1";
    private static final String ADAPTER_NAME = IronSourceAdapterConfiguration.class.getSimpleName();
    private static final String MOPUB_NETWORK_NAME = "Ironsource";

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
        final String sdkVersion = IronSourceUtils.getSDKVersion();

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

        synchronized (IronSourceAdapterConfiguration.class) {
            try {
                if (configuration != null && context instanceof Activity) {

                    final String appKey = configuration.get(APPLICATION_KEY);

                    if (TextUtils.isEmpty(appKey)) {
                        MoPubLog.log(CUSTOM, "ironSource's initialization not" +
                                " started. Ensure ironSource's " + APPLICATION_KEY +
                                " is populated on the MoPub dashboard.");
                    } else {
                        IronSource.setMediationType(MEDIATION_TYPE + IRONSOURCE_ADAPTER_VERSION);
                        IronSource.initISDemandOnly((Activity) context, appKey,
                                IronSource.AD_UNIT.REWARDED_VIDEO);
                        IronSource.initISDemandOnly((Activity) context, appKey,
                                IronSource.AD_UNIT.INTERSTITIAL);

                        networkInitializationSucceeded = true;
                    }
                } else if (!(context instanceof Activity)) {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource's initialization via " +
                            ADAPTER_NAME + " not started. An Activity Context is needed.");
                }
            } catch (Exception e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Initializing ironSource has encountered " +
                        "an exception.", e);
            }
        }

        if (networkInitializationSucceeded) {
            listener.onNetworkInitializationFinished(IronSourceAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
        } else {
            listener.onNetworkInitializationFinished(IronSourceAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }
    }
}

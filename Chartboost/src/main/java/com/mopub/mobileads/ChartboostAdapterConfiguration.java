package com.mopub.mobileads;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chartboost.sdk.Chartboost;
import com.chartboost.sdk.Libraries.CBLogging;
import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.MoPub;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.chartboost.BuildConfig;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class ChartboostAdapterConfiguration extends BaseAdapterConfiguration {

    private static volatile ChartboostShared.ChartboostSingletonDelegate sDelegate = new ChartboostShared.ChartboostSingletonDelegate();

    // Chartboost's keys
    private static final String APP_ID_KEY = "appId";
    private static final String APP_SIGNATURE_KEY = "appSignature";

    // Adapter's keys
    private static final String ADAPTER_NAME = ChartboostAdapterConfiguration.class.getSimpleName();
    private static final String ADAPTER_VERSION = BuildConfig.VERSION_NAME;
    private static final String MOPUB_NETWORK_NAME = BuildConfig.NETWORK_NAME;

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
        final String SdkVersion = Chartboost.getSDKVersion();

        if (!TextUtils.isEmpty(SdkVersion)) {
            return SdkVersion;
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

        synchronized (ChartboostAdapterConfiguration.class) {
            try {
                if (configuration != null && !configuration.isEmpty()) {

                    ChartboostShared.initializeSdk(context, configuration);

                    final String appId = configuration.get(APP_ID_KEY);
                    final String appSignature = configuration.get(APP_SIGNATURE_KEY);

                    if (TextUtils.isEmpty(appId) || TextUtils.isEmpty(appSignature)) {
                        MoPubLog.log(CUSTOM, ADAPTER_NAME, "Chartboost's initialization " +
                                "succeeded, but unable to call Chartboost's startWithAppId(). " +
                                "Ensure Chartboost's " + APP_ID_KEY + " and " + APP_SIGNATURE_KEY +
                                "are populated on the MoPub dashboard. Note that initialization on " +
                                "the first app launch is a no-op.");
                    } else {
                        Chartboost.startWithAppId(context, appId, appSignature);
                    }

                    Chartboost.setMediation(Chartboost.CBMediation.CBMediationMoPub, MoPub.SDK_VERSION, BuildConfig.VERSION_NAME);
                    Chartboost.setDelegate(sDelegate);
                    Chartboost.setAutoCacheAds(false);
                    networkInitializationSucceeded = true;
                } else {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "Chartboost's initialization via " +
                            ADAPTER_NAME + " not started as the context calling it is missing or null.");
                }
            } catch (Exception e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Initializing Chartboost has encountered " +
                        "an exception.", e);
            }
        }

        if (networkInitializationSucceeded) {
            listener.onNetworkInitializationFinished(ChartboostAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
        } else {
            listener.onNetworkInitializationFinished(ChartboostAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }

        MoPubLog.LogLevel mopubLogLevel = MoPubLog.getLogLevel();
        CBLogging.Level chartboostLogLevel = getChartboostLogLevel(mopubLogLevel);

        Chartboost.setLoggingLevel(chartboostLogLevel);
    }

    private CBLogging.Level getChartboostLogLevel(MoPubLog.LogLevel level) {
        switch (level) {
            case INFO:
                return CBLogging.Level.INTEGRATION;
            case DEBUG:
                return CBLogging.Level.ALL;
            default:
                return CBLogging.Level.NONE;
        }
    }

    public static void logChartboostError(@NonNull String chartboostLocation,
                                          @NonNull String adapterName,
                                          @NonNull MoPubLog.AdapterLogEvent event,
                                          String chartboostErrorName,
                                          Integer chartboostErrorCode) {
        if (chartboostErrorName != null && chartboostErrorCode != null) {
            MoPubLog.log(chartboostLocation, CUSTOM, adapterName,
                    "Chartboost " + event + " resulted in a Chartboost error: " + chartboostErrorName +
                            " with code: " + chartboostErrorCode);
        } else {
            MoPubLog.log(chartboostLocation, CUSTOM, adapterName,
                    "Chartboost " + event + " resulted in an error");
        }
    }
}

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.utils.IronSourceUtils;
import com.ironsource.sdk.utils.Logger;
import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.MoPub;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.ironsource.BuildConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class IronSourceAdapterConfiguration extends BaseAdapterConfiguration {
    public static final String IRONSOURCE_ADAPTER_VERSION = "500";
    public static final String DEFAULT_INSTANCE_ID = "0";

    private static final String ADAPTER_NAME = IronSourceAdapterConfiguration.class.getSimpleName();
    private static final String ADAPTER_VERSION = BuildConfig.VERSION_NAME;
    private static final String MOPUB_NETWORK_NAME = BuildConfig.NETWORK_NAME;
    private static final String MOPUB_SDK_VERSION = MoPub.SDK_VERSION;

    private static final String APPLICATION_KEY = "applicationKey";
    private static final String INTERSTITIAL_KEY = "interstitial";
    private static final String MEDIATION_TYPE = "mopub";
    private static final String REWARDEDVIDEO_KEY = "rewardedvideo";

    @NonNull
    @Override
    public String getAdapterVersion() {
        return ADAPTER_VERSION;
    }

    @Nullable
    @Override
    public String getBiddingToken(@NonNull Context context) {
        return IronSource.getISDemandOnlyBiddingData();
    }

    @NonNull
    @Override
    public String getMoPubNetworkName() {
        return MOPUB_NETWORK_NAME;
    }


    public static String getMoPubSdkVersion() {
        return MOPUB_SDK_VERSION.replaceAll("[^A-Za-z0-9]", "");
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
        final List<IronSource.AD_UNIT> adUnitsToInit = new ArrayList<>();

        synchronized (IronSourceAdapterConfiguration.class) {
            try {
                if (configuration != null && context instanceof Activity) {

                    final String appKey = configuration.get(APPLICATION_KEY);
                    final String rewardedVideoValue = configuration.get(REWARDEDVIDEO_KEY);
                    final String interstitialValue = configuration.get(INTERSTITIAL_KEY);

                    if (rewardedVideoValue.equals("true")) {
                        adUnitsToInit.add(IronSource.AD_UNIT.REWARDED_VIDEO);
                    }

                    if (interstitialValue.equals("true")) {
                        adUnitsToInit.add(IronSource.AD_UNIT.INTERSTITIAL);
                    }

                    if (TextUtils.isEmpty(appKey)) {
                        MoPubLog.log(CUSTOM, "IronSource's initialization not" +
                                " started. Ensure ironSource's " + APPLICATION_KEY +
                                " is populated on the MoPub dashboard.");
                    } else {
                        MoPubLog.log(CUSTOM, "IronSource's initialization ad units: " + adUnitsToInit.toString());
                        IronSource.setMediationType(MEDIATION_TYPE + IRONSOURCE_ADAPTER_VERSION
                                + "SDK" + getMoPubSdkVersion());
                        IronSource.initISDemandOnly((Activity) context, appKey,
                                adUnitsToInit.toArray(new IronSource.AD_UNIT[adUnitsToInit.size()]));

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

        MoPubLog.LogLevel logLevel = MoPubLog.getLogLevel();
        boolean loggingDisabled = logLevel == MoPubLog.LogLevel.NONE;

        if (loggingDisabled) {
            Logger.enableLogging(0);
        } else {
            Logger.enableLogging(1);
        }
    }

    /**
     * Class Helper Methods
     **/

    public static MoPubErrorCode getMoPubErrorCode(IronSourceError ironSourceError) {
        if (ironSourceError == null) {
            return MoPubErrorCode.INTERNAL_ERROR;
        }

        switch (ironSourceError.getErrorCode()) {
            case IronSourceError.ERROR_CODE_NO_CONFIGURATION_AVAILABLE:
            case IronSourceError.ERROR_CODE_KEY_NOT_SET:
            case IronSourceError.ERROR_CODE_INVALID_KEY_VALUE:
            case IronSourceError.ERROR_CODE_INIT_FAILED:
                return MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
            case IronSourceError.ERROR_CODE_USING_CACHED_CONFIGURATION:
                return MoPubErrorCode.VIDEO_CACHE_ERROR;
            case IronSourceError.ERROR_CODE_NO_ADS_TO_SHOW:
                return MoPubErrorCode.NETWORK_NO_FILL;
            case IronSourceError.ERROR_CODE_GENERIC:
                return MoPubErrorCode.INTERNAL_ERROR;
            case IronSourceError.ERROR_NO_INTERNET_CONNECTION:
                return MoPubErrorCode.NO_CONNECTION;
            default:
                return MoPubErrorCode.UNSPECIFIED;
        }
    }
}

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
import java.util.Arrays;
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

        synchronized (IronSourceAdapterConfiguration.class) {
            try {
                if (configuration != null) {
                    if (!(context instanceof Activity)) {
                        MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource's initialization via " +
                                ADAPTER_NAME + " not started. An Activity Context is needed.");
                        return;
                    }

                    final String appKey = configuration.get(APPLICATION_KEY);

                    if (TextUtils.isEmpty(appKey)) {
                        MoPubLog.log(CUSTOM, "IronSource's initialization not" +
                                " started. Ensure ironSource's " + APPLICATION_KEY +
                                " is populated on the MoPub dashboard.");
                    } else {
                        IronSource.AD_UNIT[] adUnitsToInitList = getIronSourceAdUnitsToInitList(context, configuration);
                        initIronSourceSDK((Activity) context, appKey, adUnitsToInitList);
                        networkInitializationSucceeded = true;
                    }

                } else {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource's initialization via " +
                            ADAPTER_NAME + " not started. No configuration information present to initialize." +
                            "Make sure you pass applicationKey on MoPub UI " +
                            "or via Mediated Network Configuration during MoPub initialization.");
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
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource's initialization via " +
                    ADAPTER_NAME + " failed. Will attempt to initialize on the first ad request to ironSource.");
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

    public static void initIronSourceSDK(Activity activity, String appKey, IronSource.AD_UNIT[] adUnitsToInitList) {
        IronSource.setMediationType(MEDIATION_TYPE + IRONSOURCE_ADAPTER_VERSION
                + "SDK" + getMoPubSdkVersion());
        IronSource.initISDemandOnly(activity, appKey, adUnitsToInitList);
    }

    /**
     * IronSource Ad Units to initialize helper methods.
     * IronSource can be configured with Interstitial only, Rewarded Video only modes. Publishers wanting to do this,
     * must pass in the appropriate properties on network configuration details of iS on initialization.
     * These helper methods facilitate the parsing, and reusing of these preferences by utilizing cached parameters.
     **/

    // Parses ad units to initialize preferences from configuration dictionary and logs the results
    public IronSource.AD_UNIT[] getIronSourceAdUnitsToInitList(@NonNull Context context, @Nullable Map<String, String> configuration) {

        // Parse ad units from config map parameter
        IronSource.AD_UNIT[] adUnitsToInit = parseIronSourceAdUnitsToInit(configuration);

        // If no config for ad units to init present, try cached parameters
        if (adUnitsToInit.length == 0)
            adUnitsToInit = parseIronSourceAdUnitsToInit(
                    getCachedInitializationParameters(context)
            );

        return adUnitsToInit;
    }

    private IronSource.AD_UNIT[] parseIronSourceAdUnitsToInit(@Nullable Map<String, String> configuration) {
        if (configuration == null || configuration.isEmpty())
            return new IronSource.AD_UNIT[0];

        List<IronSource.AD_UNIT> adUnitsToInit = new ArrayList<>();

        final String rewardedVideoValue = configuration.get(REWARDEDVIDEO_KEY);
        final String interstitialValue = configuration.get(INTERSTITIAL_KEY);

        if (rewardedVideoValue != null && rewardedVideoValue.equals("true"))
            adUnitsToInit.add(IronSource.AD_UNIT.REWARDED_VIDEO);

        if (interstitialValue != null && interstitialValue.equals("true"))
            adUnitsToInit.add(IronSource.AD_UNIT.INTERSTITIAL);

        return adUnitsToInit.toArray(new IronSource.AD_UNIT[adUnitsToInit.size()]);
    }

    public void retainIronSourceAdUnitsToInitPrefsIfNecessary(@NonNull Context context, @Nullable Map<String, String> configuration) {
        // No action needed if config is null as ad requests won't take place anyway
        if (configuration == null)
            return;

        // No action needed if configuration already contains ad units to init
        if (parseIronSourceAdUnitsToInit(configuration).length > 0)
            return;

        // Otherwise, if no ad units preferences present, then check if previous cached parameters contain it
        // If so add them to configuration to reuse them
        IronSource.AD_UNIT[] adUnitsToInit = parseIronSourceAdUnitsToInit(
                getCachedInitializationParameters(context)
        );
        if (adUnitsToInit.length == 0)
            return;

        for (IronSource.AD_UNIT adUnit : adUnitsToInit)
            configuration.put(adUnit.toString(), "true");
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

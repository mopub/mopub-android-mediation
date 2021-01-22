package com.mopub.mobileads;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chartboost.sdk.Chartboost;
import com.chartboost.sdk.Libraries.CBLogging;
import com.chartboost.sdk.Privacy.model.GDPR;
import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.MoPub;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.privacy.ConsentStatus;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.mobileads.chartboost.BuildConfig;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;

public class ChartboostAdapterConfiguration extends BaseAdapterConfiguration {

    private static final ChartboostShared.ChartboostSingletonDelegate sDelegate = 
            new ChartboostShared.ChartboostSingletonDelegate();

    // Chartboost's keys
    private static final String APP_ID_KEY = "appId";
    private static final String APP_SIGNATURE_KEY = "appSignature";

    // Chartboost specific ids
    @Nullable
    private static String mAppId;
    @Nullable
    private static String mAppSignature;

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
                    networkInitializationSucceeded = initializeChartboostSdk(context, configuration);
                } else {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "Chartboost's initialization via " +
                            ADAPTER_NAME + " not started as the context calling it, or configuration info is missing or null.");
                }
            } catch (Exception exception) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, ADAPTER_NAME, "Initializing Chartboost has encountered " +
                        "an exception.", exception.getMessage());
            }
        }

        setChartboostLogLevel();

        if (networkInitializationSucceeded) {
            listener.onNetworkInitializationFinished(ChartboostAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
        } else {
            listener.onNetworkInitializationFinished(ChartboostAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }
    }

    /**
     * Initialize the Chartboost SDK for the provided application id and app signature.
     */
    public static synchronized boolean initializeChartboostSdk(@NonNull Context context,
                                                               @NonNull Map<String, String> configuration) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(configuration);

        // Pass the user consent from the MoPub SDK to Chartboost as per GDPR
        PersonalInfoManager personalInfoManager = MoPub.getPersonalInformationManager();

        final boolean canCollectPersonalInfo = MoPub.canCollectPersonalInformation();
        final boolean shouldAllowLegitimateInterest = MoPub.shouldAllowLegitimateInterest();

        if (personalInfoManager != null && personalInfoManager.gdprApplies() == Boolean.TRUE) {
            if (shouldAllowLegitimateInterest) {
                boolean isExplicitNoConsent = personalInfoManager.getPersonalInfoConsentStatus() == ConsentStatus.EXPLICIT_NO
                        || personalInfoManager.getPersonalInfoConsentStatus() == ConsentStatus.DNT;
                addChartboostPrivacyConsent(context, !isExplicitNoConsent);
            } else {
                addChartboostPrivacyConsent(context, canCollectPersonalInfo);
            }
        }

        // Validate Chartboost args
        if (!configuration.containsKey(APP_ID_KEY)) {
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            throw new IllegalStateException("Chartboost rewarded video initialization" +
                    " failed due to missing application ID.");
        }

        if (!configuration.containsKey(APP_SIGNATURE_KEY)) {
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            throw new IllegalStateException("Chartboost rewarded video initialization" +
                    " failed due to missing application signature.");
        }

        boolean initStarted = Chartboost.isSdkStarted();
        boolean initConfigExists = false;

        final String appId = configuration.get(APP_ID_KEY);
        final String appSignature = configuration.get(APP_SIGNATURE_KEY);

        if (!TextUtils.isEmpty(appId) && !TextUtils.isEmpty(appSignature)) {
            if (appId.equals(mAppId) && appSignature.equals(mAppSignature)) {
                initConfigExists = true;
            }
        } else {
            return false; // AppId or AppSignature is empty, fail initialization.
        }

        // If Chartboost is already initialized with same configuration, no need to initialize
        // For any other case, reinitialize
        if (initConfigExists && initStarted) {
            return true;
        }

        mAppId = appId;
        mAppSignature = appSignature;

        // Perform all the common SDK initialization steps including startAppWithId
        Chartboost.startWithAppId(context, mAppId, mAppSignature);
        Chartboost.setMediation(Chartboost.CBMediation.CBMediationMoPub, MoPub.SDK_VERSION,
                new ChartboostAdapterConfiguration().getAdapterVersion());
        Chartboost.setDelegate(sDelegate);
        Chartboost.setAutoCacheAds(false);
        return true;
    }

    private static void addChartboostPrivacyConsent(Context context, boolean canCollectPersonalInfo) {
        if (context == null) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Skipped setting Chartboost Privacy consent " +
                    "as context is null.");
            return;
        }

        if (canCollectPersonalInfo) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Setting Chartboost GDPR data use consent as BEHAVIORAL");
            Chartboost.addDataUseConsent(context, new GDPR(GDPR.GDPR_CONSENT.BEHAVIORAL));
        } else {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Setting Chartboost GDPR data use consent as NON_BEHAVIORAL");
            Chartboost.addDataUseConsent(context, new GDPR(GDPR.GDPR_CONSENT.NON_BEHAVIORAL));
        }
    }

    private void setChartboostLogLevel() {
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

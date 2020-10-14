package com.mopub.mobileads;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAppOptions;
import com.adcolony.sdk.AdColonyAdOptions;
import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.DataKeys;
import com.mopub.common.MoPub;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.privacy.ConsentStatus;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.common.util.Json;
import com.mopub.mobileads.adcolony.BuildConfig;

import java.util.Arrays;
import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class AdColonyAdapterConfiguration extends BaseAdapterConfiguration {

    // Adapter's keys
    private static final String ADAPTER_NAME = AdColonyAdapterConfiguration.class.getSimpleName();
    private static final String ADAPTER_VERSION = BuildConfig.VERSION_NAME;
    private static final String MOPUB_NETWORK_NAME = BuildConfig.NETWORK_NAME;

    // AdColony-specific keys (do not modify)
    protected static final String APP_ID_KEY = "appId";
    protected static final String ALL_ZONE_IDS_KEY = "allZoneIds";
    protected static final String CLIENT_OPTIONS_KEY = "clientOptions";
    protected static final String ZONE_ID_KEY = "zoneId";
    protected static final String KEY_ADCOLONY_BID_RESPONSE = "adm";

    // AdColony Default values
    protected static final String DEFAULT_ZONE_ID = "YOUR_CURRENT_ZONE_ID";

    protected static String[] previousAdColonyAllZoneIds;

    @NonNull
    @Override
    public String getAdapterVersion() {
        return ADAPTER_VERSION;
    }

    @Nullable
    @Override
    public String getBiddingToken(@NonNull final Context context) {
        return AdColony.collectSignals();
    }

    @NonNull
    @Override
    public String getMoPubNetworkName() {
        return MOPUB_NETWORK_NAME;
    }

    @NonNull
    @Override
    public String getNetworkSdkVersion() {
        final String sdkVersion = AdColony.getSDKVersion();

        if (!TextUtils.isEmpty(sdkVersion)) {
            return sdkVersion;
        }

        final String adapterVersion = getAdapterVersion();
        return adapterVersion.substring(0, adapterVersion.lastIndexOf('.'));
    }

    @Override
    public void initializeNetwork(@NonNull final Context context,
                                  @Nullable final Map<String, String> configuration,
                                  @NonNull final OnNetworkInitializationFinishedListener listener) {

        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);

        boolean networkInitializationSucceeded = false;

        synchronized (AdColonyAdapterConfiguration.class) {
            try {
                if (isAdColonyConfigured()) {
                    networkInitializationSucceeded = true;
                } else if (configuration != null && !configuration.isEmpty()) {
                    String clientOptions = configuration.get(CLIENT_OPTIONS_KEY);

                    if (clientOptions == null)
                        clientOptions = "";

                    final String appId = getAdColonyParameter(APP_ID_KEY, configuration);
                    final String[] allZoneIds = Json.jsonArrayToStringArray(
                            getAdColonyParameter(ALL_ZONE_IDS_KEY, configuration)
                    );

                    // Check if mandatory parameters are valid, abort otherwise
                    if (appId == null) {
                        logAndFail("initialization", APP_ID_KEY);
                    } else if (allZoneIds.length == 0) {
                        logAndFail("initialization", ALL_ZONE_IDS_KEY);
                    } else {
                        AdColonyAppOptions adColonyAppOptions = getAdColonyAppOptionsAndSetConsent(clientOptions);

                        AdColony.configure((Application) context.getApplicationContext(),
                                adColonyAppOptions, appId, allZoneIds);
                        networkInitializationSucceeded = true;
                        previousAdColonyAllZoneIds = allZoneIds;
                    }
                }
            } catch (Exception e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Initializing AdColony has encountered " +
                        "an exception.", e);
            }
        }

        if (networkInitializationSucceeded) {
            listener.onNetworkInitializationFinished(AdColonyAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
        } else {
            listener.onNetworkInitializationFinished(AdColonyAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }
    }

    protected static boolean isAdColonyConfigured() {
        return !AdColony.getSDKVersion().isEmpty();
    }

    protected static void logAndFail(String operation, String parameterName) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME,
                "Aborting AdColony " + operation + ", because " + parameterName + " is empty. " +
                        "Please make sure you enter the correct " + parameterName + " on the MoPub " +
                        "Dashboard under the AdColony network settings."
        );
    }

    protected static AdColonyAppOptions getAdColonyAppOptionsAndSetConsent(String clientOptions) {
        AdColonyAppOptions adColonyAppOptions = AdColonyAppOptions.getMoPubAppOptions(clientOptions);

        // Pass the user consent from the MoPub SDK to AdColony as per GDPR
        PersonalInfoManager personalInfoManager = MoPub.getPersonalInformationManager();

        boolean canCollectPersonalInfo = MoPub.canCollectPersonalInformation();
        boolean shouldAllowLegitimateInterest = MoPub.shouldAllowLegitimateInterest();

        adColonyAppOptions = adColonyAppOptions == null ? new AdColonyAppOptions() :
                adColonyAppOptions;

        adColonyAppOptions.setMediationNetwork("MoPub", ADAPTER_VERSION);

        if (personalInfoManager != null && personalInfoManager.gdprApplies() == Boolean.TRUE) {
            adColonyAppOptions.setPrivacyFrameworkRequired(AdColonyAppOptions.GDPR, true);
            if (shouldAllowLegitimateInterest) {
                if (personalInfoManager.getPersonalInfoConsentStatus() == ConsentStatus.EXPLICIT_NO
                        || personalInfoManager.getPersonalInfoConsentStatus() == ConsentStatus.DNT) {
                    adColonyAppOptions.setPrivacyConsentString(AdColonyAppOptions.GDPR, "0");
                } else {
                    adColonyAppOptions.setPrivacyConsentString(AdColonyAppOptions.GDPR, "1");
                }
            } else if (canCollectPersonalInfo) {
                adColonyAppOptions.setPrivacyConsentString(AdColonyAppOptions.GDPR, "1");
            } else {
                adColonyAppOptions.setPrivacyConsentString(AdColonyAppOptions.GDPR, "0");
            }
        }
        return adColonyAppOptions;
    }

    /*
     * Extracts desired parameter from AdColony publisher supplied configuration, returns null if parameter not valid
     */
    protected static String getAdColonyParameter(String key, Map<String, String> extras) {
        if (extras != null && extras.containsKey(key)
                && !TextUtils.isEmpty(extras.get(key))) {
            return extras.get(key);
        } else {
            return null;
        }
    }

    protected static void checkAndConfigureAdColonyIfNecessary(Context context, String clientOptions, String appId, String[] allZoneIds) {
        if (TextUtils.isEmpty(appId)) {
            MoPubLog.log(CUSTOM, null, "AdColony cannot configure without a valid appId parameter. " +
                    "Please ensure you enter a valid appId.");
            return;
        }

        AdColonyAppOptions mAdColonyAppOptions = getAdColonyAppOptionsAndSetConsent(clientOptions);

        if (!isAdColonyConfigured()) {
            configureAdColony(context, mAdColonyAppOptions, appId, allZoneIds);
            previousAdColonyAllZoneIds = allZoneIds;
        } else if (shouldAdColonyReconfigure(previousAdColonyAllZoneIds, allZoneIds)) {
            configureAdColony(context, mAdColonyAppOptions, appId, allZoneIds);
            previousAdColonyAllZoneIds = allZoneIds;
        } else {
            // If the state of consent has changed and we aren't calling configure again, we need
            // to pass this via setAppOptions()
            AdColony.setAppOptions(mAdColonyAppOptions);
        }
    }

    protected static void configureAdColony(Context context, AdColonyAppOptions adColonyAppOptions, String appId, String[] allZoneIds) {
        if (!TextUtils.isEmpty(appId)) {
            if (context instanceof Activity) {
                boolean isAdColonyConfigSucceeded = AdColony.configure((Activity) context, adColonyAppOptions, appId, allZoneIds);

                if (isAdColonyConfigSucceeded) {
                    MoPubLog.log(CUSTOM, null, "AdColony configuration was attempted and succeeded");
                } else {
                    MoPubLog.log(CUSTOM, null, "AdColony configuration was attempted but failed");
                }
            } else {
                MoPubLog.log(CUSTOM, null, "Cannot call AdColony configure as the context calling it is not an Activity");
            }
        } else {
            MoPubLog.log(CUSTOM, null, "Cannot call AdColony configure as appId is empty");
        }
    }

    // If AdColony is configured already, but previousZones is null, then that means AdColony
    // was configured with the AdColonyRewardedVideo adapter so attempt to configure with
    // the ids in newZones. They will be ignored within the AdColony SDK if the zones are
    // the same as the zones that the other adapter called AdColony.configure() with.
    protected static boolean shouldAdColonyReconfigure(String[] previousZones, String[] newZones) {
        if (previousZones == null) {
            return true;
        } else if (newZones == null) {
            return false;
        } else if (previousZones.length != newZones.length) {
            return true;
        }

        Arrays.sort(previousZones);
        Arrays.sort(newZones);

        return !Arrays.equals(previousZones, newZones);
    }

    protected AdColonyAdOptions getBannerAdOptionsFromExtras(Map<String, String> extras) {
        return getAdOptionsFromExtras(extras, null);
    }

    protected AdColonyAdOptions getInterstitialAdOptionsFromExtras(Map<String, String> extras) {
        return getAdOptionsFromExtras(extras, null);
    }

    protected AdColonyAdOptions getRewardAdOptionsFromExtras(Map<String, String> extras, String mAdUnitId) {
        return getAdOptionsFromExtras(extras, mAdUnitId);
    }

    private AdColonyAdOptions getAdOptionsFromExtras(Map<String, String> extras, String mAdUnitId) {
        final AdColonyAdOptions options = new AdColonyAdOptions();

        // Rewarded Video only
        if (mAdUnitId != null) {
            options.enableConfirmationDialog(getConfirmationDialogFromSettings(mAdUnitId));
            options.enableResultsDialog(getResultsDialogFromSettings(mAdUnitId));
        }

        // Forward ADM field contents to AdColony for Advanced Bidding requests.
        // This is a requirement of AdColony Advanced Bidding 3.0 spec.
        if (extras != null && extras.containsKey(DataKeys.ADM_KEY)
                && !TextUtils.isEmpty(extras.get(DataKeys.ADM_KEY))) {
            final String adMarkup = extras.get(DataKeys.ADM_KEY);

            if (adMarkup != null) {
                options.setOption(KEY_ADCOLONY_BID_RESPONSE, adMarkup);
            }
        }

        return options;
    }

    private boolean getConfirmationDialogFromSettings(String mAdUnitId) {
        final AdColonyRewardedVideo.AdColonyInstanceMediationSettings settings =
                MoPubRewardedVideoManager.getInstanceMediationSettings(AdColonyRewardedVideo.AdColonyInstanceMediationSettings.class, mAdUnitId);
        return settings != null && settings.isWithConfirmationDialog();
    }

    private boolean getResultsDialogFromSettings(String mAdUnitId) {
        final AdColonyRewardedVideo.AdColonyInstanceMediationSettings settings =
                MoPubRewardedVideoManager.getInstanceMediationSettings(AdColonyRewardedVideo.AdColonyInstanceMediationSettings.class, mAdUnitId);
        return settings != null && settings.isWithResultsDialog();
    }
}

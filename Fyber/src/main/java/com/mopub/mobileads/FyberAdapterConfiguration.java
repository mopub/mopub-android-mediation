package com.mopub.mobileads;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fyber.inneractive.sdk.external.InneractiveAdManager;
import com.fyber.inneractive.sdk.external.InneractiveAdRequest;
import com.fyber.inneractive.sdk.external.InneractiveUserConfig;
import com.fyber.inneractive.sdk.external.OnFyberMarketplaceInitializedListener;
import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.MoPub;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.privacy.ConsentStatus;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.fyber.BuildConfig;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class FyberAdapterConfiguration extends BaseAdapterConfiguration {

    public final static String KEY_FYBER_APP_ID = "appID";
    public final static String KEY_FYBER_DEBUG = "debug";

    private static final String MOPUB_NETWORK_NAME = BuildConfig.NETWORK_NAME;
    private static final String ADAPTER_VERSION = BuildConfig.VERSION_NAME;

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
        return InneractiveAdManager.getVersion();
    }

    @Override
    public void initializeNetwork(@NonNull Context context, @Nullable Map<String, String> configuration, @NonNull
    final OnNetworkInitializationFinishedListener listener) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);

        if (configuration != null && !configuration.isEmpty()) {
            final String appId = configuration.get(KEY_FYBER_APP_ID);
            if (!TextUtils.isEmpty(appId)) {
                initializeFyberMarketplace(context, appId,
                                           configuration.containsKey(KEY_FYBER_DEBUG),
                                           new OnFyberAdapterConfigurationResolvedListener() {
                                               @Override
                                               public void onFyberAdapterConfigurationResolved(
                                                       OnFyberMarketplaceInitializedListener.FyberInitStatus status) {
                                                   if (status ==
                                                       OnFyberMarketplaceInitializedListener.FyberInitStatus.SUCCESSFULLY ||
                                                           status ==
                                                                   OnFyberMarketplaceInitializedListener.FyberInitStatus.FAILED) {
                                                       listener.onNetworkInitializationFinished(FyberAdapterConfiguration.class, MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
                                                   } else if (status == OnFyberMarketplaceInitializedListener.FyberInitStatus.INVALID_APP_ID) {
                                                       MoPubLog.log(CUSTOM, "Attempted to initialize Fyber Marketplace with wrong app id - " + appId);
                                                       listener.onNetworkInitializationFinished(FyberAdapterConfiguration.class, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                                                   } else {
                                                       listener.onNetworkInitializationFinished(FyberAdapterConfiguration.class, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                                                   }
                                               }
                                           });
            } else {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "No Fyber app id given in configuration object. Initialization postponed. You can use FyberAdapterConfiguration.KEY_FYBER_APP_ID as your configuration key");
            }
        }
    }

    public static void initializeFyberMarketplace(Context context, String appId, boolean debugMode, @NonNull
    final OnFyberAdapterConfigurationResolvedListener listener) {
        synchronized (FyberAdapterConfiguration.class) {
            // Just to be on the safe side, wrap initialize with exception handling
            if (debugMode) {
                InneractiveAdManager.setLogLevel(Log.VERBOSE);
            }

            if (!InneractiveAdManager.wasInitialized()) {
                InneractiveAdManager.initialize(context, appId,
                                                new OnFyberMarketplaceInitializedListener() {
                                                    @Override
                                                    public void onFyberMarketplaceInitialized(
                                                            FyberInitStatus status) {
                                                        listener.onFyberAdapterConfigurationResolved(status);
                                                    }
                                                });
            } else if (!appId.equals(InneractiveAdManager.getAppId())) {
                MoPubLog.log(CUSTOM, "Fyber Marketplace was initialized with appId " + InneractiveAdManager.getAppId() +
                        " and now requests initialization with another appId (" + appId + ") You may have configured the wrong appId on the MoPub console?\n" +
                        " you can only use a single appId and it's related spots");
                listener.onFyberAdapterConfigurationResolved(
                        OnFyberMarketplaceInitializedListener.FyberInitStatus.INVALID_APP_ID);
            } else {
                listener.onFyberAdapterConfigurationResolved(
                        OnFyberMarketplaceInitializedListener.FyberInitStatus.SUCCESSFULLY);
            }

        }
    }

    public static void updateGdprConsentStatusFromMopub() {
        InneractiveAdManager.GdprConsentSource gdprConsentSource = InneractiveAdManager.getGdprStatusSource();

        if (gdprConsentSource == null || gdprConsentSource == InneractiveAdManager.GdprConsentSource.External) {
            final Boolean mopubGdpr = extractGdprFromMoPub();
            if (mopubGdpr == null) {
                InneractiveAdManager.clearGdprConsentData();
            } else {
                InneractiveAdManager.setGdprConsent(mopubGdpr, InneractiveAdManager.GdprConsentSource.External);
            }
        }
    }

    private static Boolean extractGdprFromMoPub() {
        PersonalInfoManager personalInfoManager = MoPub.getPersonalInformationManager();

        if (personalInfoManager != null && personalInfoManager.gdprApplies() == Boolean.TRUE) {
            /* Only set the GDPR consent flag, if GDPR is applied. If GDPR is not applied,
            canCollectPersonalInformation returns true, but there is no explicit consent */
                MoPubLog.log(CUSTOM, "Fyber sdk will use gdpr consent from MoPub. GdprConsent - " +
                        personalInfoManager.canCollectPersonalInformation());
                return personalInfoManager.canCollectPersonalInformation();
            } else if (personalInfoManager.getPersonalInfoConsentStatus() == ConsentStatus.UNKNOWN && MoPub.shouldAllowLegitimateInterest()) {
                MoPubLog.log(CUSTOM, "Gdpr result from MoPub is unknown and publisher allowed legitimateInterest. GdprConsent - true");
                return true;
            } else {
                MoPubLog.log(CUSTOM, "Fyber sdk has not found any Gdpr values");
            }
        return null;
    }
    
    /**
     * Internal interface to bridge the gap between the custom event classes and the initializeNetwork
     */
    public interface OnFyberAdapterConfigurationResolvedListener {
        public void onFyberAdapterConfigurationResolved(
                OnFyberMarketplaceInitializedListener.FyberInitStatus status);
    }

    public static void updateRequestFromExtras(InneractiveAdRequest request, Map<String, String> extras) {
        String keywords = null;
        InneractiveUserConfig.Gender gender = null;
        int age = 0;
        String zipCode = null;
        if (extras != null) {
            if (extras.containsKey(FyberMoPubMediationDefs.KEY_KEYWORDS)) {
                keywords = (String) extras.get(FyberMoPubMediationDefs.KEY_KEYWORDS);
            }

            if (extras.containsKey(FyberMoPubMediationDefs.KEY_AGE)) {
                try {
                    age = Integer.valueOf(extras.get(FyberMoPubMediationDefs.KEY_AGE));
                } catch (NumberFormatException e) {
                    MoPubLog.log(CUSTOM, "local extra contains Invalid Age");
                }
            }

            if (extras.containsKey(FyberMoPubMediationDefs.KEY_ZIPCODE)) {
                zipCode = (String) extras.get(FyberMoPubMediationDefs.KEY_ZIPCODE);
            }

            if (extras.containsKey(FyberMoPubMediationDefs.KEY_GENDER)) {
                String genderStr = extras.get(FyberMoPubMediationDefs.KEY_GENDER)    ;
                if (FyberMoPubMediationDefs.GENDER_MALE.equals(genderStr)) {
                    gender = InneractiveUserConfig.Gender.MALE;
                } else if (FyberMoPubMediationDefs.GENDER_FEMALE.equals(genderStr)) {
                    gender = InneractiveUserConfig.Gender.FEMALE;
                }
            }

            InneractiveUserConfig userConfig = new InneractiveUserConfig()
                    .setZipCode(zipCode);

            if (gender != null) {
                userConfig.setGender(gender);
            }

            if (InneractiveUserConfig.ageIsValid(age)) {
                userConfig.setAge(age);
            }

            request.setUserParams(new InneractiveUserConfig()
                    .setGender(gender)
                    .setZipCode(zipCode)
                    .setAge(age));

            if (!TextUtils.isEmpty(keywords)) {
                request.setKeywords(keywords);
            }
        }
    }
}
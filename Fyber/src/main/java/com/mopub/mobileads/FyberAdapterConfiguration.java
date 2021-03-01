/** Copyright 2020 Fyber N.V.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License
 */

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

/**
 * Fyber's instance of Mopub adapter configuration class
 */
public class FyberAdapterConfiguration extends BaseAdapterConfiguration {

    private static final String TAG = "FyberAdapterConfig";
    private static final String MOPUB_NETWORK_NAME = BuildConfig.NETWORK_NAME;
    private static final String ADAPTER_VERSION = BuildConfig.VERSION_NAME;

    // Configuration keys
    /**
     * The application id you have received from the Fyber Marketplace console
     */
    public final static String KEY_FYBER_APP_ID = "appID";
    /**
     * Set to 1" or "true" in order to enable Fyber Marketplace debug mode
     */
    public final static String KEY_FYBER_DEBUG = "debug";

    /** 4-digit versioning scheme, of which the leftmost 3 digits correspond to the network SDK version,
     * and the last digit denotes the minor version number referring to an adapter release */
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
    /**
     * @return the name of Fyber's network
     */
    @NonNull
    @Override
    public String getMoPubNetworkName() {
        return MOPUB_NETWORK_NAME;
    }

    /**
     * @return the version of Fyber's SDK
     */
    @NonNull
    @Override
    public String getNetworkSdkVersion() {
        return InneractiveAdManager.getVersion();
    }

    /**
     * Called once by the Mopub infra-structure on SDK initialization
     * Gets Fyber's application id from the given configuration map, and initializes the Fyber SDK
     * @param context Android's context
     * @param configuration Key/Value Map of Fyber's configuration
     * @param listener SDK initialization status listener
     */
    @Override
    public void initializeNetwork(@NonNull Context context, @Nullable Map<String, String> configuration, @NonNull
    final OnNetworkInitializationFinishedListener listener) {
        Preconditions.checkNotNull(context);

        if (configuration != null) {
            final String appId = configuration.get(KEY_FYBER_APP_ID);
            if (!TextUtils.isEmpty(appId)) {
                initializeFyberMarketplace(context, appId,
                                           configuration.containsKey(KEY_FYBER_DEBUG),
                                           new OnFyberAdapterConfigurationResolvedListener() {
                                               @Override
                                               public void onFyberAdapterConfigurationResolved(
                                                       OnFyberMarketplaceInitializedListener.FyberInitStatus status) {
                                                   //note - we try to load ads when "FAILED" because an ad request will re-attempt to initialize the relevant parts of the SDK.
                                                   if (status ==
                                                       OnFyberMarketplaceInitializedListener.FyberInitStatus.SUCCESSFULLY ||
                                                           status ==
                                                                   OnFyberMarketplaceInitializedListener.FyberInitStatus.FAILED) {
                                                       listener.onNetworkInitializationFinished(FyberAdapterConfiguration.class, MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
                                                   } else if (status == OnFyberMarketplaceInitializedListener.FyberInitStatus.INVALID_APP_ID) {
                                                       log("Attempted to initialize Fyber MarketPlace with wrong app id - " + appId);
                                                       listener.onNetworkInitializationFinished(FyberAdapterConfiguration.class, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                                                   } else {
                                                       listener.onNetworkInitializationFinished(FyberAdapterConfiguration.class, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                                                   }
                                               }
                                           });
            } else {
                Log.d(TAG, "No Fyber app id given in configuration object. Initialization postponed. You can use FyberAdapterConfiguration.KEY_FYBER_APP_ID as your configuration key");

            }
        }
    }

    /**
     * This method initializes the Fyber Marketplace SDK, and returns true if the initialization was successfull. It can either be called from the initializeNetwork method
     * or called by one of the custom adapters classes, if the appId is only defined in the Mopub console
     * @param context
     * @param appId Fyber's application id
     * @param debugMode if set to true, runs Fyber Marketplace with debug logs
     * @return true if initialized successfully. false otherwise
     */
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
                Log.w(TAG, "Fyber Marketplace was initialized with appId " + InneractiveAdManager.getAppId() +
                        " and now requests initialization with another appId (" + appId + ") You may have configured the wrong appId on the Mopub console?\n" +
                        " you can only use a single appId and its related spots");
                listener.onFyberAdapterConfigurationResolved(
                        OnFyberMarketplaceInitializedListener.FyberInitStatus.INVALID_APP_ID);
            } else {
                listener.onFyberAdapterConfigurationResolved(
                        OnFyberMarketplaceInitializedListener.FyberInitStatus.SUCCESSFULLY);
            }

        }
    }

    /**
     * A helper for getting the current consent status from Mopub. Called before each load request
     */
    public static void updateGdprConsentStatusFromMopub() {
        InneractiveAdManager.GdprConsentSource gdprConsentSource = InneractiveAdManager.getGdprStatusSource();
        if (gdprConsentSource == null || gdprConsentSource == InneractiveAdManager.GdprConsentSource.External) {
            Boolean mopubGdpr = extractGdprFromMopub();
            if (mopubGdpr == null) {
                InneractiveAdManager.clearGdprConsentData();
            } else {
                InneractiveAdManager.setGdprConsent(mopubGdpr, InneractiveAdManager.GdprConsentSource.External);
            }
        }

    }

    /**
     * A helper for getting the current consent status from Mopub. Called before each load request
     */
    private static Boolean extractGdprFromMopub() {
        PersonalInfoManager personalInfoManager = MoPub.getPersonalInformationManager();
        if (personalInfoManager != null) {
            Boolean gdprApplies = personalInfoManager.gdprApplies();
            // Only set the GDPR consent flag, if GDPR is applied. If GDPR is not applied, canCollectPersonalInformation returns true, but there is no explicit consent
            if (gdprApplies != null && gdprApplies) {
                log("Fyber sdk will user gdpr consent from mopub. GdprConsent- " + personalInfoManager.canCollectPersonalInformation());
                return personalInfoManager.canCollectPersonalInformation();
            } else if (personalInfoManager.getPersonalInfoConsentStatus() == ConsentStatus.UNKNOWN && MoPub.shouldAllowLegitimateInterest()) {
                log("Gdpr result from mopub is unkown and publisher allowed liegitmateInterset. GdprConsent - true");
                return true;
            } else {
                log("Fyber sdk has not found any Gdpr values");
            }
        }
        return null;
    }
    
    /**
     * Internal interface to bridge the gap between the custom event classes and the initializeNetwork part
     */
    public interface OnFyberAdapterConfigurationResolvedListener {
        public void onFyberAdapterConfigurationResolved(
                OnFyberMarketplaceInitializedListener.FyberInitStatus status);
    }

    /**
     * Helper for popupating an ad request with extra params
     * @param request
     * @param extras
     */
    public static void updateRequestFromExtras(InneractiveAdRequest request, Map<String, String> extras) {
        String keywords = null;
        InneractiveUserConfig.Gender gender = null;
        int age = 0;
        String zipCode = null;
        if (extras != null) {
            if (extras.containsKey(FyberMopubMediationDefs.KEY_KEYWORDS)) {
                keywords = (String) extras.get(FyberMopubMediationDefs.KEY_KEYWORDS);
            }

            // Set the age variable as defined on IaMediationActivity class.
            // in case the variable is not initialized, the variable will not be in use

            if (extras.containsKey(FyberMopubMediationDefs.KEY_AGE)) {
                try {
                    age = Integer.valueOf(extras.get(FyberMopubMediationDefs.KEY_AGE));
                } catch (NumberFormatException e) {
                    log("local extra contains Invalid Age");
                }
            }

            //in case the variable is not initialized, the variable will not be in use
            if (extras.containsKey(FyberMopubMediationDefs.KEY_ZIPCODE)) {
                zipCode = (String) extras.get(FyberMopubMediationDefs.KEY_ZIPCODE);
            }

            // Set the gender variable as defined on IaMediationActivity class.
            // in case the variable is not initialized, the variable will not be in use

            if (extras.containsKey(FyberMopubMediationDefs.KEY_GENDER)) {
                String genderStr = extras.get(FyberMopubMediationDefs.KEY_GENDER)    ;
                if (FyberMopubMediationDefs.GENDER_MALE.equals(genderStr)) {
                    gender = InneractiveUserConfig.Gender.MALE;
                } else if (FyberMopubMediationDefs.GENDER_FEMALE.equals(genderStr)) {
                    gender = InneractiveUserConfig.Gender.FEMALE;
                }
            }

            // Populate user configuration
            InneractiveUserConfig userConfig = new InneractiveUserConfig()
                    .setZipCode(zipCode);

            if (gender != null) {
                userConfig.setGender(gender);
            }

            if (InneractiveUserConfig.ageIsValid(age)) {
                userConfig.setAge(age);
            }

            // Set optional parameters for better targeting.
            request.setUserParams(new InneractiveUserConfig()
                    .setGender(gender)
                    .setZipCode(zipCode)
                    .setAge(age));

            // Populate keywords
            if (!TextUtils.isEmpty(keywords)) {
                request.setKeywords(keywords);
            }
        }
    }

    private static void log(String message) {
        MoPubLog.log(CUSTOM, TAG, message);
    }
}
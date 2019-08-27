package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAdSize;
import com.adcolony.sdk.AdColonyAdView;
import com.adcolony.sdk.AdColonyAdViewListener;
import com.adcolony.sdk.AdColonyAppOptions;
import com.adcolony.sdk.AdColonyZone;
import com.mopub.common.DataKeys;
import com.mopub.common.MoPub;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.privacy.ConsentStatus;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.common.util.Json;

import java.util.Arrays;
import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.WILL_LEAVE_APPLICATION;

public class AdColonyBanner extends CustomEventBanner {

    /*
     * We recommend passing the AdColony client options, app ID, all zone IDs, and current zone ID
     * in the serverExtras Map by specifying Custom Event Data in MoPub's web interface.
     *
     * Please see AdColony's documentation for more information:
     * https://github.com/AdColony/AdColony-Android-SDK-3
     */
    private static final String DEFAULT_CLIENT_OPTIONS = "version=YOUR_APP_VERSION_HERE,store:google";
    private static final String DEFAULT_APP_ID = "YOUR_AD_COLONY_APP_ID_HERE";
    private static final String[] DEFAULT_ALL_ZONE_IDS = {"ZONE_ID_1", "ZONE_ID_2", "..."};
    private static final String DEFAULT_ZONE_ID = "YOUR_CURRENT_ZONE_ID";
    private static final String CONSENT_RESPONSE = "consent_response";
    private static final String CONSENT_GIVEN = "explicit_consent_given";

    /*
     * These keys are intended for MoPub internal use. Do not modify.
     */
    public static final String CLIENT_OPTIONS_KEY = "clientOptions";
    public static final String APP_ID_KEY = "appId";
    public static final String ALL_ZONE_IDS_KEY = "allZoneIds";
    public static final String ZONE_ID_KEY = "zoneId";

    public static final String ADAPTER_NAME = AdColonyBanner.class.getSimpleName();

    private CustomEventBannerListener mCustomEventBannerListener;
    private AdColonyAdViewListener mAdColonyBannerListener;
    private final Handler mHandler;
    private static String[] previousAdColonyAllZoneIds;

    @NonNull
    private AdColonyAdapterConfiguration mAdColonyAdapterConfiguration;

    private AdColonyAdSize adSize;

    private AdColonyAdView mAdColonyAdView;

    public AdColonyBanner() {
        mHandler = new Handler();
        mAdColonyAdapterConfiguration = new AdColonyAdapterConfiguration();
    }

    @Override
    protected void loadBanner(@NonNull Context context,
                              @NonNull CustomEventBannerListener customEventBannerListener,
                              @NonNull Map<String, Object> localExtras,
                              @NonNull Map<String, String> serverExtras) {
        if (!(context instanceof Activity)) {
            customEventBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(), MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        String clientOptions = DEFAULT_CLIENT_OPTIONS;
        String appId = DEFAULT_APP_ID;
        String[] allZoneIds = DEFAULT_ALL_ZONE_IDS;
        String zoneId = DEFAULT_ZONE_ID;

        mCustomEventBannerListener = customEventBannerListener;

        adSize = getAdSizeFromLocalExtras(localExtras);
        if (extrasAreValid(serverExtras)) {
            clientOptions = serverExtras.get(CLIENT_OPTIONS_KEY);
            appId = serverExtras.get(APP_ID_KEY);
            allZoneIds = extractAllZoneIds(serverExtras);
            zoneId = serverExtras.get(ZONE_ID_KEY);
            mAdColonyAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
        }

        // Check to see if app ID parameter is present. If not AdColony will not return an ad.
        // So there's no need to make a request. If so, must fail and log the flow.
        if (TextUtils.isEmpty(appId) || TextUtils.equals(appId, DEFAULT_APP_ID)) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "AppId parameter cannot be empty. " +
                    "Please make sure you enter correct AppId on the MoPub Dashboard " +
                    "for AdColony.");

            mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        AdColonyAppOptions mAdColonyAppOptions = null;
        if (!TextUtils.isEmpty(clientOptions)) {
            mAdColonyAppOptions = AdColonyAppOptions.getMoPubAppOptions(clientOptions);
        }

        // Pass the user consent from the MoPub SDK to AdColony as per GDPR
        PersonalInfoManager personalInfoManager = MoPub.getPersonalInformationManager();

        boolean canCollectPersonalInfo = MoPub.canCollectPersonalInformation();
        boolean shouldAllowLegitimateInterest = MoPub.shouldAllowLegitimateInterest();

        mAdColonyAppOptions = mAdColonyAppOptions == null ? new AdColonyAppOptions() :
                mAdColonyAppOptions;

        if (personalInfoManager != null && personalInfoManager.gdprApplies() == Boolean.TRUE) {
            if (shouldAllowLegitimateInterest) {
                if (personalInfoManager.getPersonalInfoConsentStatus() == ConsentStatus.EXPLICIT_NO
                        || personalInfoManager.getPersonalInfoConsentStatus() == ConsentStatus.DNT) {
                    mAdColonyAppOptions.setOption(CONSENT_GIVEN, true)
                            .setOption(CONSENT_RESPONSE, false);
                } else {
                    mAdColonyAppOptions.setOption(CONSENT_GIVEN, true)
                            .setOption(CONSENT_RESPONSE, true);
                }
            } else {
                mAdColonyAppOptions.setOption(CONSENT_GIVEN, true)
                        .setOption(CONSENT_RESPONSE, canCollectPersonalInfo);
            }
        }

        mAdColonyBannerListener = getAdColonyBannerListener();
        if (!isAdColonyConfigured()) {
            if (!TextUtils.isEmpty(appId)) {
                AdColony.configure((Activity) context, mAdColonyAppOptions, appId, allZoneIds);
            }
        } else if ((shouldReconfigure(previousAdColonyAllZoneIds, allZoneIds))) {
            // Need to check the zone IDs sent from the MoPub portal and reconfigure if they are
            // different than the zones we initially called AdColony.configure() with
            if (!TextUtils.isEmpty(appId)) {
                AdColony.configure((Activity) context, mAdColonyAppOptions, appId, allZoneIds);
            }
            previousAdColonyAllZoneIds = allZoneIds;
        } else {
            // If state of consent has changed and we aren't calling configure again, we need
            // to pass this via setAppOptions()
            AdColony.setAppOptions(mAdColonyAppOptions);
        }

        if (!TextUtils.isEmpty(zoneId)) {
            AdColony.requestAdView(zoneId, mAdColonyBannerListener, adSize);
            MoPubLog.log(zoneId, LOAD_ATTEMPTED, ADAPTER_NAME);
        }

    }

    @Override
    protected void onInvalidate() {
        if (mAdColonyAdView != null) {
            mAdColonyBannerListener = null;
            mAdColonyAdView.destroy();
            mAdColonyAdView = null;
        }
    }

    private boolean isAdColonyConfigured() {
        return !AdColony.getSDKVersion().isEmpty();
    }

    private AdColonyAdViewListener getAdColonyBannerListener() {
        if(mAdColonyBannerListener != null) {
            return mAdColonyBannerListener;
        } else {
            return new AdColonyAdViewListener() {
                @Override
                public void onRequestFilled(final AdColonyAdView adColonyAdView) {
                    mAdColonyAdView = adColonyAdView;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCustomEventBannerListener.onBannerLoaded(adColonyAdView);
                            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
                        }
                    });
                }
                @Override
                public void onRequestNotFilled(AdColonyZone zone) {
                    super.onRequestNotFilled(zone);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
                            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                                    MoPubErrorCode.NETWORK_NO_FILL);
                        }
                    });
                }
                @Override
                public void onClicked(AdColonyAdView ad) {
                    super.onClicked(ad);
                    mCustomEventBannerListener.onBannerClicked();
                    MoPubLog.log(CLICKED, ADAPTER_NAME);
                }
                @Override
                public void onLeftApplication(AdColonyAdView ad) {
                    super.onLeftApplication(ad);
                    mCustomEventBannerListener.onLeaveApplication();
                    MoPubLog.log(WILL_LEAVE_APPLICATION, ADAPTER_NAME);
                }
                @Override
                public void onOpened(AdColonyAdView ad) {
                    super.onOpened(ad);
                    mCustomEventBannerListener.onBannerExpanded();
                }
                @Override
                public void onClosed(AdColonyAdView ad) {
                    super.onClosed(ad);
                    mCustomEventBannerListener.onBannerCollapsed();
                }
            };
        }
    }

    private boolean extrasAreValid(Map<String, String> extras) {
        return extras != null
                && extras.containsKey(CLIENT_OPTIONS_KEY)
                && extras.containsKey(APP_ID_KEY)
                && extras.containsKey(ALL_ZONE_IDS_KEY)
                && extras.containsKey(ZONE_ID_KEY);
    }

    private AdColonyAdSize getAdSizeFromLocalExtras(Map<String, Object> localExtras) {
        AdColonyAdSize adSize =  AdColonyAdSize.BANNER;

        if (localExtras == null || localExtras.isEmpty()) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "No local extras provided");
            return adSize;
        }

        try {
            final int width = (Integer) localExtras.get(DataKeys.AD_WIDTH);
            final int height = (Integer) localExtras.get(DataKeys.AD_HEIGHT);
            adSize = new AdColonyAdSize(width,height);
            if(adSize == null) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Invalid width (" + width + ") and height " +
                        "(" + height + ") provided. Setting Default AdSize.");
            }
        } catch (Throwable th) {

        }

        return adSize;
    }

    private static boolean shouldReconfigure(String[] previousZones, String[] newZones) {
        // If AdColony is configured already, but previousZones is null, then that means AdColony
        // was configured with the AdColonyRewardedVideo adapter so attempt to configure with
        // the ids in newZones. They will be ignored within the AdColony SDK if the zones are
        // the same as the zones that the other adapter called AdColony.configure() with.
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

    private String[] extractAllZoneIds(Map<String, String> serverExtras) {
        String[] result = Json.jsonArrayToStringArray(serverExtras.get(ALL_ZONE_IDS_KEY));

        // AdColony requires at least one valid String in the allZoneIds array.
        if (result.length == 0) {
            result = new String[]{""};
        }

        return result;
    }
}

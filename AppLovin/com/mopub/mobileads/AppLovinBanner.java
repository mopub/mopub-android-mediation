package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.applovin.adview.AppLovinAdView;
import com.applovin.adview.AppLovinAdViewDisplayErrorCode;
import com.applovin.adview.AppLovinAdViewEventListener;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinErrorCodes;
import com.applovin.sdk.AppLovinMediationProvider;
import com.applovin.sdk.AppLovinPrivacySettings;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkSettings;
import com.mopub.common.DataKeys;
import com.mopub.common.MoPub;
import com.mopub.common.privacy.PersonalInfoManager;

import java.util.Map;

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;

/**
 * AppLovin SDK banner adapter for MoPub.
 * <p>
 * Created by Thomas So on 3/6/17.
 */

public class AppLovinBanner
        extends CustomEventBanner {
    private static final boolean LOGGING_ENABLED = true;
    private static final Handler UI_HANDLER = new Handler(Looper.getMainLooper());

    private static final int BANNER_STANDARD_HEIGHT = 50;
    private static final int BANNER_HEIGHT_OFFSET_TOLERANCE = 10;
    private static final int LEADER_STANDARD_HEIGHT = 90;
    private static final int LEADER_HEIGHT_OFFSET_TOLERANCE = 16;

    private static final String AD_WIDTH_KEY = "com_mopub_ad_width";
    private static final String AD_HEIGHT_KEY = "com_mopub_ad_height";

    private static final String ZONE_ID_SERVER_EXTRAS_KEY = "zone_id";

    //
    // MoPub Custom Event Methods
    //

    @Override
    protected void loadBanner(final Context context, final CustomEventBannerListener customEventBannerListener, final Map<String, Object> localExtras, final Map<String, String> serverExtras) {
        // SDK versions BELOW 7.1.0 require a instance of an Activity to be passed in as the context
        if (AppLovinSdk.VERSION_CODE < 710 && !(context instanceof Activity)) {
            log(ERROR, "Unable to request AppLovin banner. Invalid context provided.");
            customEventBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            return;
        }

        // Pass the user consent from the MoPub SDK as per GDPR
        PersonalInfoManager personalInfoManager = MoPub.getPersonalInformationManager();
        if (personalInfoManager != null && personalInfoManager.gdprApplies()) {
            boolean canCollectPersonalInfo = personalInfoManager.canCollectPersonalInformation();
            AppLovinPrivacySettings.setHasUserConsent(canCollectPersonalInfo, context);
        }

        AppLovinSdk sdk = retrieveSdk(serverExtras, context);
        sdk.setPluginVersion("MoPub-3.1.0");
        sdk.setMediationProvider(AppLovinMediationProvider.MOPUB);


        final AppLovinAdSize adSize = appLovinAdSizeFromLocalExtras(localExtras);
        if (adSize != null) {
            final String adMarkup = serverExtras.get(DataKeys.ADM_KEY);
            final boolean hasAdMarkup = !TextUtils.isEmpty(adMarkup);

            log(DEBUG, "Requesting AppLovin banner with serverExtras: " + serverExtras + ", localExtras: " + localExtras + " and has ad markup: " + hasAdMarkup);

            final AppLovinAdView adView = new AppLovinAdView(sdk, adSize, context);
            adView.setAdDisplayListener(new AppLovinAdDisplayListener() {
                @Override
                public void adDisplayed(final AppLovinAd ad) {
                    log(DEBUG, "Banner displayed");
                }

                @Override
                public void adHidden(final AppLovinAd ad) {
                    log(DEBUG, "Banner dismissed");
                }
            });
            adView.setAdClickListener(new AppLovinAdClickListener() {
                @Override
                public void adClicked(final AppLovinAd ad) {
                    log(DEBUG, "Banner clicked");

                    customEventBannerListener.onBannerClicked();
                    customEventBannerListener.onLeaveApplication();
                }
            });

            adView.setAdViewEventListener(new AppLovinAdViewEventListener() {
                @Override
                public void adOpenedFullscreen(final AppLovinAd appLovinAd, final AppLovinAdView appLovinAdView) {
                    log(DEBUG, "Banner opened fullscreen");
                    customEventBannerListener.onBannerExpanded();
                }

                @Override
                public void adClosedFullscreen(final AppLovinAd appLovinAd, final AppLovinAdView appLovinAdView) {
                    log(DEBUG, "Banner closed fullscreen");
                    customEventBannerListener.onBannerCollapsed();
                }

                @Override
                public void adLeftApplication(final AppLovinAd appLovinAd, final AppLovinAdView appLovinAdView) {
                    log(DEBUG, "Banner left application");
                }

                @Override
                public void adFailedToDisplay(final AppLovinAd appLovinAd, final AppLovinAdView appLovinAdView, final AppLovinAdViewDisplayErrorCode appLovinAdViewDisplayErrorCode) {
                }
            });

            final AppLovinAdLoadListener adLoadListener = new AppLovinAdLoadListener() {
                @Override
                public void adReceived(final AppLovinAd ad) {
                    // Ensure logic is ran on main queue
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adView.renderAd(ad);

                            log(DEBUG, "Successfully loaded banner ad");

                            try {
                                customEventBannerListener.onBannerLoaded(adView);
                            } catch (Throwable th) {
                                log(ERROR, "Unable to notify listener of successful ad load.", th);
                            }
                        }
                    });
                }

                @Override
                public void failedToReceiveAd(final int errorCode) {
                    // Ensure logic is ran on main queue
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            log(ERROR, "Failed to load banner ad with code: " + errorCode);

                            try {
                                customEventBannerListener.onBannerFailed(toMoPubErrorCode(errorCode));
                            } catch (Throwable th) {
                                log(ERROR, "Unable to notify listener of failure to receive ad.", th);
                            }
                        }
                    });
                }
            };

            if (hasAdMarkup) {
                sdk.getAdService().loadNextAdForAdToken(adMarkup, adLoadListener);
            } else {
                // Determine zone
                final String zoneId = serverExtras.get(ZONE_ID_SERVER_EXTRAS_KEY);
                if (!TextUtils.isEmpty(zoneId)) {
                    sdk.getAdService().loadNextAdForZoneId(zoneId, adLoadListener);
                } else {
                    sdk.getAdService().loadNextAd(adSize, adLoadListener);
                }
            }
        } else {
            log(ERROR, "Unable to request AppLovin banner");

            customEventBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }
    }

    @Override
    protected void onInvalidate() {
    }

    //
    // Utility Methods
    //

    private AppLovinAdSize appLovinAdSizeFromLocalExtras(final Map<String, Object> localExtras) {
        // Handle trivial case
        if (localExtras == null || localExtras.isEmpty()) {
            log(ERROR, "No serverExtras provided");
            return null;
        }

        try {
            final int width = (Integer) localExtras.get(AD_WIDTH_KEY);
            final int height = (Integer) localExtras.get(AD_HEIGHT_KEY);

            // We have valid dimensions
            if (width > 0 && height > 0) {
                log(DEBUG, "Valid width (" + width + ") and height (" + height + ") provided");

                // Assume fluid width, and check for height with offset tolerance
                final int bannerOffset = Math.abs(BANNER_STANDARD_HEIGHT - height);
                final int leaderOffset = Math.abs(LEADER_STANDARD_HEIGHT - height);

                if (bannerOffset <= BANNER_HEIGHT_OFFSET_TOLERANCE) {
                    return AppLovinAdSize.BANNER;
                } else if (leaderOffset <= LEADER_HEIGHT_OFFSET_TOLERANCE) {
                    return AppLovinAdSize.LEADER;
                } else if (height <= AppLovinAdSize.MREC.getHeight()) {
                    return AppLovinAdSize.MREC;
                } else {
                    log(ERROR, "Provided dimensions does not meet the dimensions required of banner or mrec ads");
                }
            } else {
                log(ERROR, "Invalid width (" + width + ") and height (" + height + ") provided");
            }
        } catch (Throwable th) {
            log(ERROR, "Encountered error while parsing width and height from serverExtras", th);
        }

        return null;
    }

    //
    // Utility Methods
    //

    private static void log(final int priority, final String message) {
        log(priority, message, null);
    }

    private static void log(final int priority, final String message, final Throwable th) {
        if (LOGGING_ENABLED) {
            Log.println(priority, "AppLovinBanner", message + ((th == null) ? "" : Log.getStackTraceString(th)));
        }
    }

    private static MoPubErrorCode toMoPubErrorCode(final int applovinErrorCode) {
        if (applovinErrorCode == AppLovinErrorCodes.NO_FILL) {
            return MoPubErrorCode.NETWORK_NO_FILL;
        } else if (applovinErrorCode == AppLovinErrorCodes.UNSPECIFIED_ERROR) {
            return MoPubErrorCode.NETWORK_INVALID_STATE;
        } else if (applovinErrorCode == AppLovinErrorCodes.NO_NETWORK) {
            return MoPubErrorCode.NO_CONNECTION;
        } else if (applovinErrorCode == AppLovinErrorCodes.FETCH_AD_TIMEOUT) {
            return MoPubErrorCode.NETWORK_TIMEOUT;
        } else {
            return MoPubErrorCode.UNSPECIFIED;
        }
    }

    /**
     * Retrieves the appropriate instance of AppLovin's SDK from the SDK key given in the server parameters, or Android Manifest.
     */
    private static AppLovinSdk retrieveSdk(final Map<String, String> serverExtras, final Context context) {
        final String sdkKey = serverExtras != null ? serverExtras.get("sdk_key") : null;
        final AppLovinSdk sdk;

        if (!TextUtils.isEmpty(sdkKey)) {
            sdk = AppLovinSdk.getInstance(sdkKey, new AppLovinSdkSettings(), context);
        } else {
            sdk = AppLovinSdk.getInstance(context);
        }

        return sdk;
    }

    /**
     * Performs the given runnable on the main thread.
     */
    private static void runOnUiThread(final Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            UI_HANDLER.post(runnable);
        }
    }
}
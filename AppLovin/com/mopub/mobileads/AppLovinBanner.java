package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.applovin.adview.AppLovinAdView;
import com.applovin.adview.AppLovinAdViewDisplayErrorCode;
import com.applovin.adview.AppLovinAdViewEventListener;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinErrorCodes;
import com.applovin.sdk.AppLovinPrivacySettings;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkSettings;
import com.mopub.common.MoPub;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.privacy.PersonalInfoManager;

import java.util.Map;

public class AppLovinBanner extends CustomEventBanner {

    private static final Handler UI_HANDLER = new Handler(Looper.getMainLooper());

    private static final int BANNER_STANDARD_HEIGHT = 50;
    private static final int BANNER_HEIGHT_OFFSET_TOLERANCE = 10;
    private static final int LEADER_STANDARD_HEIGHT = 90;
    private static final int LEADER_HEIGHT_OFFSET_TOLERANCE = 16;

    private static final String AD_WIDTH_KEY = "com_mopub_ad_width";
    private static final String AD_HEIGHT_KEY = "com_mopub_ad_height";

    private AppLovinSdk sdk;

    //
    // MoPub Custom Event Methods
    //

    @Override
    protected void loadBanner(final Context context, final CustomEventBannerListener customEventBannerListener, final Map<String, Object> localExtras, final Map<String, String> serverExtras) {

        // Pass the user consent from the MoPub SDK to AppLovin as per GDPR
        boolean canCollectPersonalInfo = MoPub.canCollectPersonalInformation();
        AppLovinPrivacySettings.setHasUserConsent(canCollectPersonalInfo, context);

        // SDK versions BELOW 7.1.0 require a instance of an Activity to be passed in as the context
        if (AppLovinSdk.VERSION_CODE < 710 && !(context instanceof Activity)) {
            MoPubLog.d("Unable to request AppLovin banner. Invalid context provided.");
            customEventBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            return;
        }

        MoPubLog.d("Requesting AppLovin banner with serverExtras: " + serverExtras + " and localExtras: " + localExtras);

        final AppLovinAdSize adSize = appLovinAdSizeFromLocalExtras(localExtras);
        if (adSize != null) {
            sdk = retrieveSdk(serverExtras, context);
            sdk.setPluginVersion("MoPub-Certified-3.0.0");

            final AppLovinAdView adView = new AppLovinAdView(sdk, adSize, context);
            adView.setAdDisplayListener(new AppLovinAdDisplayListener() {
                @Override
                public void adDisplayed(final AppLovinAd ad) {
                    MoPubLog.d("Banner displayed");
                }

                @Override
                public void adHidden(final AppLovinAd ad) {
                    MoPubLog.d("Banner dismissed");
                }
            });
            adView.setAdClickListener(new AppLovinAdClickListener() {
                @Override
                public void adClicked(final AppLovinAd ad) {
                    MoPubLog.d("Banner clicked");
                    customEventBannerListener.onBannerClicked();
                }
            });


            adView.setAdViewEventListener(new AppLovinAdViewEventListener() {
                @Override
                public void adOpenedFullscreen(final AppLovinAd appLovinAd, final AppLovinAdView appLovinAdView) {
                    MoPubLog.d("Banner opened fullscreen");
                    customEventBannerListener.onBannerExpanded();
                }

                @Override
                public void adClosedFullscreen(final AppLovinAd appLovinAd, final AppLovinAdView appLovinAdView) {
                    MoPubLog.d("Banner closed fullscreen");
                    customEventBannerListener.onBannerCollapsed();
                }

                @Override
                public void adLeftApplication(final AppLovinAd appLovinAd, final AppLovinAdView appLovinAdView) {
                    MoPubLog.d("Banner left application");
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

                            MoPubLog.d("Successfully loaded banner ad");

                            try {
                                customEventBannerListener.onBannerLoaded(adView);
                            } catch (Throwable th) {
                                MoPubLog.e("Unable to notify listener of successful ad load.", th);
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
                            MoPubLog.d("Failed to load banner ad with code: " + errorCode);

                            try {
                                customEventBannerListener.onBannerFailed(toMoPubErrorCode(errorCode));
                            } catch (Throwable th) {
                                MoPubLog.e("Unable to notify listener of failure to receive ad.", th);
                            }
                        }
                    });
                }
            };

            // Zones support is available on AppLovin SDK 7.5.0 and higher
            final String zoneId;
            if (AppLovinSdk.VERSION_CODE >= 750 && serverExtras != null && serverExtras.containsKey("zone_id")) {
                zoneId = serverExtras.get("zone_id");
            } else {
                zoneId = null;
            }

            if (!TextUtils.isEmpty(zoneId)) {
                sdk.getAdService().loadNextAdForZoneId(zoneId, adLoadListener);
            } else {
                sdk.getAdService().loadNextAd(adSize, adLoadListener);
            }
        } else {
            MoPubLog.d("Unable to request AppLovin banner");

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
            MoPubLog.d("No serverExtras provided");
            return null;
        }

        try {
            final int width = (Integer) localExtras.get(AD_WIDTH_KEY);
            final int height = (Integer) localExtras.get(AD_HEIGHT_KEY);

            // We have valid dimensions
            if (width > 0 && height > 0) {
                MoPubLog.d("Valid width (" + width + ") and height (" + height + ") provided");

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
                    MoPubLog.d("Provided dimensions does not meet the dimensions required of banner or mrec ads");
                }
            } else {
                MoPubLog.d("Invalid width (" + width + ") and height (" + height + ") provided");
            }
        } catch (Throwable th) {
            MoPubLog.d("Encountered error while parsing width and height from serverExtras", th);
        }

        return null;
    }

    //
    // Utility Methods
    //

    private static MoPubErrorCode toMoPubErrorCode(final int applovinErrorCode) {
        if (applovinErrorCode == AppLovinErrorCodes.NO_FILL) {
            return MoPubErrorCode.NETWORK_NO_FILL;
        } else if (applovinErrorCode == AppLovinErrorCodes.UNSPECIFIED_ERROR) {
            return MoPubErrorCode.UNSPECIFIED;
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
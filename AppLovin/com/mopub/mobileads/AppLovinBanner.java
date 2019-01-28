package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
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
import com.applovin.sdk.AppLovinMediationProvider;
import com.applovin.sdk.AppLovinPrivacySettings;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkSettings;
import com.mopub.common.DataKeys;
import com.mopub.common.MoPub;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;

public class AppLovinBanner extends CustomEventBanner {

    private static final Handler UI_HANDLER = new Handler(Looper.getMainLooper());

    private static final int BANNER_STANDARD_HEIGHT = 50;
    private static final int BANNER_HEIGHT_OFFSET_TOLERANCE = 10;
    private static final int LEADER_STANDARD_HEIGHT = 90;
    private static final int LEADER_HEIGHT_OFFSET_TOLERANCE = 16;

    private static final String AD_WIDTH_KEY = "com_mopub_ad_width";
    private static final String AD_HEIGHT_KEY = "com_mopub_ad_height";

    private static final String ZONE_ID_SERVER_EXTRAS_KEY = "zone_id";

    private static final String ADAPTER_NAME = AppLovinBanner.class.getSimpleName();

    @NonNull
    private AppLovinAdapterConfiguration mAppLovinAdapterConfiguration;
    //
    // MoPub Custom Event Methods
    //

    public AppLovinBanner() {
        mAppLovinAdapterConfiguration = new AppLovinAdapterConfiguration();
    }

    @Override
    protected void loadBanner(final Context context, final CustomEventBannerListener customEventBannerListener, final Map<String, Object> localExtras, final Map<String, String> serverExtras) {

        // Pass the user consent from the MoPub SDK to AppLovin as per GDPR
        boolean canCollectPersonalInfo = MoPub.canCollectPersonalInformation();
        AppLovinPrivacySettings.setHasUserConsent(canCollectPersonalInfo, context);

        // SDK versions BELOW 7.1.0 require a instance of an Activity to be passed in as the context
        if (AppLovinSdk.VERSION_CODE < 710 && !(context instanceof Activity)) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unable to request AppLovin banner. Invalid context provided");

            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            if (customEventBannerListener != null) {
                customEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }

            return;
        }

        final AppLovinAdSize adSize = appLovinAdSizeFromLocalExtras(localExtras);
        if (adSize != null) {
            final String adMarkup = serverExtras.get(DataKeys.ADM_KEY);
            final boolean hasAdMarkup = !TextUtils.isEmpty(adMarkup);

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Requesting AppLovin banner with serverExtras: " +
                    serverExtras + ", localExtras: " + localExtras + " and has ad markup: " + hasAdMarkup);

            AppLovinSdk sdk = retrieveSdk(serverExtras, context);
            sdk.setPluginVersion("MoPub-3.1.0");
            sdk.setMediationProvider(AppLovinMediationProvider.MOPUB);

            mAppLovinAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);

            final AppLovinAdView adView = new AppLovinAdView(sdk, adSize, context);
            adView.setAdDisplayListener(new AppLovinAdDisplayListener() {
                @Override
                public void adDisplayed(final AppLovinAd ad) {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "Banner displayed");
                }

                @Override
                public void adHidden(final AppLovinAd ad) {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "Banner dismissed");
                }
            });
            adView.setAdClickListener(new AppLovinAdClickListener() {
                @Override
                public void adClicked(final AppLovinAd ad) {
                    MoPubLog.log(CLICKED, ADAPTER_NAME);

                    if (customEventBannerListener != null) {
                        customEventBannerListener.onBannerClicked();
                    }
                }
            });


            adView.setAdViewEventListener(new AppLovinAdViewEventListener() {
                @Override
                public void adOpenedFullscreen(final AppLovinAd appLovinAd, final AppLovinAdView appLovinAdView) {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "Banner opened fullscreen");

                    if (customEventBannerListener != null) {
                        customEventBannerListener.onBannerExpanded();
                    }
                }

                @Override
                public void adClosedFullscreen(final AppLovinAd appLovinAd, final AppLovinAdView appLovinAdView) {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "Banner closed fullscreen");

                    if (customEventBannerListener != null) {
                        customEventBannerListener.onBannerCollapsed();
                    }
                }

                @Override
                public void adLeftApplication(final AppLovinAd appLovinAd, final AppLovinAdView appLovinAdView) {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "Banner left application");
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
                            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
                            MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);

                            adView.renderAd(ad);
                            MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);

                            try {
                                if (customEventBannerListener != null) {
                                    customEventBannerListener.onBannerLoaded(adView);
                                }
                            } catch (Throwable th) {
                                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Unable to notify listener " +
                                        "of successful ad load.", th);
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
                            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Failed to load banner ad with code: ",
                                    errorCode);
                            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                                    toMoPubErrorCode(errorCode).getIntCode(),
                                    toMoPubErrorCode(errorCode));
                            try {
                                if (customEventBannerListener != null) {
                                    customEventBannerListener.onBannerFailed(toMoPubErrorCode(errorCode));
                                }
                            } catch (Throwable th) {
                                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Unable to notify " +
                                        "listener of failure to receive ad.", th);
                            }
                        }
                    });
                }
            };

            if (hasAdMarkup) {
                sdk.getAdService().loadNextAdForAdToken(adMarkup, adLoadListener);

                MoPubLog.log(LOAD_ATTEMPTED, ADAPTER_NAME);
            } else {
                // Determine zone
                final String zoneId = serverExtras.get(ZONE_ID_SERVER_EXTRAS_KEY);
                if (!TextUtils.isEmpty(zoneId)) {
                    sdk.getAdService().loadNextAdForZoneId(zoneId, adLoadListener);
                    MoPubLog.log(zoneId, LOAD_ATTEMPTED, ADAPTER_NAME);
                } else {
                    sdk.getAdService().loadNextAd(adSize, adLoadListener);
                    MoPubLog.log(zoneId, LOAD_ATTEMPTED, ADAPTER_NAME);
                }
            }
        } else {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unable to request AppLovin banner");

            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            if (customEventBannerListener != null) {
                customEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
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
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "No serverExtras provided");
            return null;
        }

        try {
            final int width = (Integer) localExtras.get(AD_WIDTH_KEY);
            final int height = (Integer) localExtras.get(AD_HEIGHT_KEY);

            // We have valid dimensions
            if (width > 0 && height > 0) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Valid width (" + width + ") and height " +
                        "(" + height + ") provided");

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
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "Provided dimensions does not meet the dimensions " +
                            "required of banner or mrec ads");
                }
            } else {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Invalid width (" + width + ") and height " +
                        "(" + height + ") provided");
            }
        } catch (Throwable th) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "Encountered error while parsing width and " +
                    "height from serverExtras", th);
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
package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.applovin.adview.AppLovinInterstitialAd;
import com.applovin.adview.AppLovinInterstitialAdDialog;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinAdVideoPlaybackListener;
import com.applovin.sdk.AppLovinErrorCodes;
import com.applovin.sdk.AppLovinPrivacySettings;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkSettings;
import com.mopub.common.MoPub;
import com.mopub.common.logging.MoPubLog;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class AppLovinInterstitial extends CustomEventInterstitial implements AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdClickListener, AppLovinAdVideoPlaybackListener {

    private static final Handler UI_HANDLER = new Handler(Looper.getMainLooper());
    private static final String DEFAULT_ZONE = "";

    private AppLovinSdk sdk;
    private CustomEventInterstitialListener listener;
    private Context context;

    // A map of Zone -> Queue of `AppLovinAd`s to be shared by instances of the custom event.
    // This prevents skipping of ads as this adapter will be re-created and preloaded
    // on every ad load regardless if ad was actually displayed or not.
    private static final Map<String, Queue<AppLovinAd>> GLOBAL_INTERSTITIAL_ADS = new HashMap<String, Queue<AppLovinAd>>();
    private static final Object GLOBAL_INTERSTITIAL_ADS_LOCK = new Object();

    private String zoneId; // The zone identifier this instance of the custom event is loading for

    //
    // MoPub Custom Event Methods
    //

    @Override
    public void loadInterstitial(final Context context, final CustomEventInterstitialListener listener, final Map<String, Object> localExtras, final Map<String, String> serverExtras) {

        setAutomaticImpressionAndClickTracking(false);

        // Pass the user consent from the MoPub SDK to AppLovin as per GDPR
        boolean canCollectPersonalInfo = MoPub.canCollectPersonalInformation();
        AppLovinPrivacySettings.setHasUserConsent(canCollectPersonalInfo, context);

        MoPubLog.d("Requesting AppLovin interstitial with serverExtras: " + serverExtras + " and localExtras: " + localExtras);

        // SDK versions BELOW 7.2.0 require a instance of an Activity to be passed in as the context
        if (AppLovinSdk.VERSION_CODE < 720 && !(context instanceof Activity)) {
            MoPubLog.d("Unable to request AppLovin banner. Invalid context provided.");

            if (listener != null) {
                listener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }


            return;
        }

        // Store parent objects
        this.listener = listener;
        this.context = context;

        sdk = retrieveSdk(serverExtras, context);
        sdk.setPluginVersion("MoPub-Certified-3.0.0");

        // Zones support is available on AppLovin SDK 7.5.0 and higher
        final String serverExtrasZoneId = serverExtras != null ? serverExtras.get("zone_id") : null;
        zoneId = (!TextUtils.isEmpty(serverExtrasZoneId) && AppLovinSdk.VERSION_CODE >= 750) ? serverExtrasZoneId : DEFAULT_ZONE;

        // Check if we already have a preloaded ad for the given zone
        final AppLovinAd preloadedAd = dequeueAd(zoneId);
        if (preloadedAd != null) {
            MoPubLog.d("Found preloaded ad for zone: {" + zoneId + "}");
            adReceived(preloadedAd);
        } else {
            // If this is a default Zone, create the incentivized ad normally
            if (DEFAULT_ZONE.equals(zoneId)) {
                sdk.getAdService().loadNextAd(AppLovinAdSize.INTERSTITIAL, this);
            }
            // Otherwise, use the Zones API
            else {
                // Dynamically load an ad for a given zone without breaking backwards compatibility for publishers on older SDKs
                sdk.getAdService().loadNextAdForZoneId(zoneId, this);
            }
        }
    }

    @Override
    public void showInterstitial() {
        final AppLovinAd preloadedAd = dequeueAd(zoneId);
        if (preloadedAd != null) {
            final AppLovinInterstitialAdDialog interstitialAd = AppLovinInterstitialAd.create(sdk, context);
            interstitialAd.setAdDisplayListener(this);
            interstitialAd.setAdClickListener(this);
            interstitialAd.setAdVideoPlaybackListener(this);
            interstitialAd.showAndRender(preloadedAd);
        } else {
            MoPubLog.d("Failed to show an AppLovin interstitial before one was loaded");

            if (listener != null) {
                listener.onInterstitialFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
            }
        }
    }

    @Override
    public void onInvalidate() {
    }

    //
    // Ad Load Listener
    //

    @Override
    public void adReceived(final AppLovinAd ad) {
        MoPubLog.d("Interstitial did load ad: " + ad.getAdIdNumber());

        enqueueAd(ad, zoneId);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (listener != null) {
                        listener.onInterstitialLoaded();
                    }
                } catch (Throwable th) {
                    MoPubLog.e("Unable to notify listener of successful ad load.", th);
                }
            }
        });
    }

    @Override
    public void failedToReceiveAd(final int errorCode) {
        MoPubLog.d("Interstitial failed to load with error: " + errorCode);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (listener != null) {
                        listener.onInterstitialFailed(toMoPubErrorCode(errorCode));
                    }
                } catch (Throwable th) {
                    MoPubLog.e("Unable to notify listener of failure to receive ad.", th);
                }
            }
        });
    }

    //
    // Ad Display Listener
    //

    @Override
    public void adDisplayed(final AppLovinAd appLovinAd) {
        MoPubLog.d("Interstitial displayed");

        if (listener != null) {
            listener.onInterstitialShown();
            listener.onInterstitialImpression();
        }
    }

    @Override
    public void adHidden(final AppLovinAd appLovinAd) {
        MoPubLog.d("Interstitial dismissed");

        if (listener != null) {
            listener.onInterstitialDismissed();
        }
    }

    //
    // Ad Click Listener
    //

    @Override
    public void adClicked(final AppLovinAd appLovinAd) {
        MoPubLog.d("Interstitial clicked");

        if (listener != null) {
            listener.onLeaveApplication();
        }
    }

    //
    // Video Playback Listener
    //

    @Override
    public void videoPlaybackBegan(final AppLovinAd ad) {
        MoPubLog.d("Interstitial video playback began");
    }

    @Override
    public void videoPlaybackEnded(final AppLovinAd ad, final double percentViewed, final boolean fullyWatched) {
        MoPubLog.d("Interstitial video playback ended at playback percent: " + percentViewed);
    }

    //
    // Utility Methods
    //

    private static AppLovinAd dequeueAd(final String zoneId) {
        synchronized (GLOBAL_INTERSTITIAL_ADS_LOCK) {
            AppLovinAd preloadedAd = null;

            final Queue<AppLovinAd> preloadedAds = GLOBAL_INTERSTITIAL_ADS.get(zoneId);
            if (preloadedAds != null && !preloadedAds.isEmpty()) {
                preloadedAd = preloadedAds.poll();
            }
            return preloadedAd;
        }
    }

    private static void enqueueAd(final AppLovinAd ad, final String zoneId) {
        synchronized (GLOBAL_INTERSTITIAL_ADS_LOCK) {
            Queue<AppLovinAd> preloadedAds = GLOBAL_INTERSTITIAL_ADS.get(zoneId);
            if (preloadedAds == null) {
                preloadedAds = new LinkedList<AppLovinAd>();
                GLOBAL_INTERSTITIAL_ADS.put(zoneId, preloadedAds);
            }
            preloadedAds.offer(ad);
        }
    }

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
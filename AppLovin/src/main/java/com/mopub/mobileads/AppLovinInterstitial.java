package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.applovin.adview.AppLovinInterstitialAd;
import com.applovin.adview.AppLovinInterstitialAdDialog;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinAdVideoPlaybackListener;
import com.applovin.sdk.AppLovinMediationProvider;
import com.applovin.sdk.AppLovinPrivacySettings;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkSettings;
import com.mopub.common.DataKeys;
import com.mopub.common.MoPub;
import com.mopub.common.logging.MoPubLog;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;

public class AppLovinInterstitial extends CustomEventInterstitial implements AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdClickListener, AppLovinAdVideoPlaybackListener {

    private static final String DEFAULT_ZONE = "";
    private static final String ZONE_ID_SERVER_EXTRAS_KEY = "zone_id";

    private static final String ADAPTER_NAME = AppLovinInterstitial.class.getSimpleName();

    private static final Handler UI_HANDLER = new Handler(Looper.getMainLooper());

    private AppLovinSdk sdk;
    private CustomEventInterstitialListener listener;
    private Context context;

    // A map of Zone -> Queue of `AppLovinAd`s to be shared by instances of the custom event.
    // This prevents skipping of ads as this adapter will be re-created and preloaded
    // on every ad load regardless if ad was actually displayed or not.
    private static final Map<String, Queue<AppLovinAd>> GLOBAL_INTERSTITIAL_ADS = new HashMap<String, Queue<AppLovinAd>>();
    private static final Object GLOBAL_INTERSTITIAL_ADS_LOCK = new Object();

    private boolean isTokenEvent;
    private AppLovinAd tokenAd;

    private static String mZoneId; // The zone identifier this instance of the custom event is loading for

    @NonNull
    private AppLovinAdapterConfiguration mAppLovinAdapterConfiguration;

    //
    // MoPub Custom Event Methods
    //

    public AppLovinInterstitial() {
        mAppLovinAdapterConfiguration = new AppLovinAdapterConfiguration();
    }

    @Override
    public void loadInterstitial(final Context context, final CustomEventInterstitialListener listener, final Map<String, Object> localExtras, final Map<String, String> serverExtras) {
        if (serverExtras == null || serverExtras.isEmpty()) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "No serverExtras provided");
            if (listener != null) {
                listener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return;
        }

        // Pass the user consent from the MoPub SDK to AppLovin as per GDPR
        boolean canCollectPersonalInfo = MoPub.canCollectPersonalInformation();
        AppLovinPrivacySettings.setHasUserConsent(canCollectPersonalInfo, context);

        // SDK versions BELOW 7.2.0 require a instance of an Activity to be passed in as the context
        if (AppLovinSdk.VERSION_CODE < 720 && !(context instanceof Activity)) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Unable to request AppLovin interstitial. Invalid context " +
                    "provided.");

            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            if (listener != null) {
                listener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return;
        }

        // Store parent objects
        this.listener = listener;
        this.context = context;

        sdk = retrieveSdk(context);

        if (sdk == null) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "AppLovinSdk instance is null likely because " +
                    "no AppLovin SDK key is available. Failing ad request.");
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            if (listener != null) {
                listener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }

            return;
        }

        sdk.setMediationProvider(AppLovinMediationProvider.MOPUB);
        sdk.setPluginVersion(AppLovinAdapterConfiguration.APPLOVIN_PLUGIN_VERSION);

        final String adMarkup = serverExtras.get(DataKeys.ADM_KEY);
        final boolean hasAdMarkup = !TextUtils.isEmpty(adMarkup);

        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Requesting AppLovin interstitial with serverExtras: " +
                serverExtras + ", localExtras: " + localExtras + " and has adMarkup: " + hasAdMarkup);

        mAppLovinAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);

        if (hasAdMarkup) {
            isTokenEvent = true;

            // Use token API
            sdk.getAdService().loadNextAdForAdToken(adMarkup, this);
            MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
        } else {
            final String serverExtrasZoneId = serverExtras.get(ZONE_ID_SERVER_EXTRAS_KEY);
            mZoneId = !TextUtils.isEmpty(serverExtrasZoneId) ? serverExtrasZoneId : DEFAULT_ZONE;

            // Check if we already have a preloaded ad for the given zone
            final AppLovinAd preloadedAd = dequeueAd(mZoneId);
            if (preloadedAd != null) {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Found preloaded ad for zone: {" + mZoneId + "}");
                adReceived(preloadedAd);
            }
            // No ad currently preloaded
            else {
                if (!TextUtils.isEmpty(mZoneId)) {
                    sdk.getAdService().loadNextAdForZoneId(mZoneId, this);
                    MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
                } else {
                    sdk.getAdService().loadNextAd(AppLovinAdSize.INTERSTITIAL, this);
                    MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
                }
            }
        }
    }

    @Override
    public void showInterstitial() {
        final AppLovinAd preloadedAd;

        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);

        if (isTokenEvent && tokenAd != null) {
            preloadedAd = tokenAd;
        } else {
            preloadedAd = dequeueAd(mZoneId);
        }

        if (preloadedAd != null) {

            final AppLovinInterstitialAdDialog interstitialAd = AppLovinInterstitialAd.create(sdk, context);
            interstitialAd.setAdDisplayListener(this);
            interstitialAd.setAdClickListener(this);
            interstitialAd.setAdVideoPlaybackListener(this);
            interstitialAd.showAndRender(preloadedAd);
        } else {
            MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Failed to show an AppLovin interstitial before one was " +
                    "loaded");

            if (listener != null) {
                listener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
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

        if (isTokenEvent) {
            tokenAd = ad;
        } else {
            enqueueAd(ad, mZoneId);
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);

                    if (listener != null) {
                        listener.onInterstitialLoaded();
                    }
                } catch (Throwable th) {
                    MoPubLog.log(getAdNetworkId(), CUSTOM_WITH_THROWABLE, "Unable to notify listener of " +
                            "successful ad load", th);
                }
            }
        });
    }

    @Override
    public void failedToReceiveAd(final int errorCode) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                            AppLovinAdapterConfiguration.getMoPubErrorCode(errorCode).getIntCode(),
                            AppLovinAdapterConfiguration.getMoPubErrorCode(errorCode));

                    if (listener != null) {
                        listener.onInterstitialFailed(AppLovinAdapterConfiguration.getMoPubErrorCode(errorCode));
                    }
                } catch (Throwable th) {
                    MoPubLog.log(getAdNetworkId(), CUSTOM_WITH_THROWABLE, "Unable to notify listener of failure" +
                            " to receive ad", th);
                }
            }
        });
    }

    //
    // Ad Display Listener
    //

    @Override
    public void adDisplayed(final AppLovinAd appLovinAd) {
        MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);

        if (listener != null) {
            listener.onInterstitialShown();
        }
    }

    @Override
    public void adHidden(final AppLovinAd appLovinAd) {
        if (listener != null) {
            listener.onInterstitialDismissed();
        }
    }

    //
    // Ad Click Listener
    //

    @Override
    public void adClicked(final AppLovinAd appLovinAd) {
        MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);

        if (listener != null) {
            listener.onInterstitialClicked();
        }
    }

    //
    // Video Playback Listener
    //

    @Override
    public void videoPlaybackBegan(final AppLovinAd ad) {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Interstitial video playback began");
    }

    @Override
    public void videoPlaybackEnded(final AppLovinAd ad, final double percentViewed, final boolean fullyWatched) {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Interstitial video playback ended at playback percent: ",
                percentViewed);
    }

    private static String getAdNetworkId() {
        return mZoneId;
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

    /**
     * Retrieves the appropriate instance of AppLovin's SDK from the SDK key. This check prioritizes
     * the SDK Key in the AndroidManifest, and only uses the one passed in to the AdapterConfiguration
     * if the former is not available.
     */
    private static AppLovinSdk retrieveSdk(final Context context) {

        if (!AppLovinAdapterConfiguration.androidManifestContainsValidSdkKey(context)) {
            final String sdkKey = AppLovinAdapterConfiguration.getSdkKey();

            return !TextUtils.isEmpty(sdkKey)
                    ? AppLovinSdk.getInstance(sdkKey, new AppLovinSdkSettings(), context)
                    : null;
        } else {
            return AppLovinSdk.getInstance(context);
        }
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
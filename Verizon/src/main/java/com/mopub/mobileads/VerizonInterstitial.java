package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.mopub.common.MoPub;
import com.mopub.common.logging.MoPubLog;
import com.verizon.ads.Bid;
import com.verizon.ads.BidRequestListener;
import com.verizon.ads.CreativeInfo;
import com.verizon.ads.ErrorInfo;
import com.verizon.ads.RequestMetadata;
import com.verizon.ads.VASAds;
import com.verizon.ads.edition.StandardEdition;
import com.verizon.ads.interstitialplacement.InterstitialAd;
import com.verizon.ads.interstitialplacement.InterstitialAdFactory;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.DID_DISAPPEAR;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.WILL_LEAVE_APPLICATION;

public class VerizonInterstitial extends CustomEventInterstitial {

    private static final String ADAPTER_NAME = VerizonInterstitial.class.getSimpleName();

    private static final String PLACEMENT_ID_KEY = "placementId";
    private static final String SITE_ID_KEY = "siteId";

    private Context context;
    private CustomEventInterstitialListener interstitialListener;
    private InterstitialAd verizonInterstitialAd;

    @NonNull
    private VerizonAdapterConfiguration verizonAdapterConfiguration;

    public VerizonInterstitial() {
        verizonAdapterConfiguration = new VerizonAdapterConfiguration();
    }

    @Override
    protected void loadInterstitial(final Context context,
                                    final CustomEventInterstitialListener customEventInterstitialListener,
                                    final Map<String, Object> localExtras,
                                    final Map<String, String> serverExtras) {

        interstitialListener = customEventInterstitialListener;
        this.context = context;

        if (serverExtras == null || serverExtras.isEmpty()) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Ad request to Verizon failed because " +
                    "serverExtras is null or empty");

            logAndNotifyInterstitialFailed(LOAD_FAILED, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            return;
        }

        if (!VASAds.isInitialized()) {
            final String siteId = serverExtras.get(getSiteIdKey());

            if (TextUtils.isEmpty(siteId)) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Ad request to Verizon failed because " +
                        "siteId is empty");

                logAndNotifyInterstitialFailed(LOAD_FAILED, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                        MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

                return;
            }

            if (!(context instanceof Activity)) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Ad request to Verizon failed because " +
                        "context is not an Activity");

                logAndNotifyInterstitialFailed(LOAD_FAILED, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                        MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

                return;
            }

            final boolean success = StandardEdition.initializeWithActivity((Activity) context, siteId);

            if (!success) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Failed to initialize the Verizon SDK");

                logAndNotifyInterstitialFailed(LOAD_FAILED, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                        MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

                return;
            }
        }

        final String placementId = serverExtras.get(getPlacementIdKey());

        if (TextUtils.isEmpty(placementId)) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Ad request to Verizon failed because placement " +
                    "ID is empty");

            logAndNotifyInterstitialFailed(LOAD_FAILED, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            return;
        }

        VASAds.setLocationEnabled(MoPub.getLocationAwareness() != MoPub.LocationAwareness.DISABLED);

        final InterstitialAdFactory interstitialAdFactory = new InterstitialAdFactory(context, placementId,
                new VerizonInterstitialFactoryListener());

        final Bid bid = BidCache.get(placementId);

        if (bid == null) {
            final RequestMetadata requestMetadata = new RequestMetadata.Builder().setMediator(VerizonAdapterConfiguration.MEDIATOR_ID).build();
            interstitialAdFactory.setRequestMetaData(requestMetadata);

            interstitialAdFactory.load(new VerizonInterstitialListener());
        } else {
            interstitialAdFactory.load(bid, new VerizonInterstitialListener());
        }

        verizonAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
    }

    /**
     * Call this method to cache a super auction bid for the specified placement ID
     *
     * @param context            a non-null Context
     * @param placementId        a valid placement ID. Cannot be null or empty.
     * @param requestMetadata    a {@link RequestMetadata} instance for the request or null
     * @param bidRequestListener an instance of {@link BidRequestListener}. Cannot be null.
     */
    public static void requestBid(final Context context, final String placementId, final RequestMetadata requestMetadata,
                                  final BidRequestListener bidRequestListener) {

        if (bidRequestListener == null) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "bidRequestListener parameter cannot be null.");

            return;
        }

        final RequestMetadata.Builder builder = new RequestMetadata.Builder(requestMetadata);
        final RequestMetadata actualRequestMetadata = builder.setMediator(VerizonAdapterConfiguration.MEDIATOR_ID).build();

        InterstitialAdFactory.requestBid(context, placementId, actualRequestMetadata, new BidRequestListener() {
            @Override
            public void onComplete(Bid bid, ErrorInfo errorInfo) {

                if (errorInfo == null) {
                    BidCache.put(placementId, bid);
                }

                bidRequestListener.onComplete(bid, errorInfo);
            }
        });
    }

    @Override
    protected void showInterstitial() {

        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);

        VerizonUtils.postOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (verizonInterstitialAd != null) {
                    verizonInterstitialAd.show(context);
                    return;
                }

                logAndNotifyInterstitialFailed(SHOW_FAILED, MoPubErrorCode.INTERNAL_ERROR.getIntCode(),
                        MoPubErrorCode.INTERNAL_ERROR);
            }
        });
    }

    @Override
    protected void onInvalidate() {

        VerizonUtils.postOnUiThread(new Runnable() {

            @Override
            public void run() {
                interstitialListener = null;

                // Destroy any hanging references
                if (verizonInterstitialAd != null) {
                    verizonInterstitialAd.destroy();
                    verizonInterstitialAd = null;
                }
            }
        });
    }

    protected String getPlacementIdKey() {
        return PLACEMENT_ID_KEY;
    }

    protected String getSiteIdKey() {
        return SITE_ID_KEY;
    }

    private void logAndNotifyInterstitialFailed(MoPubLog.AdapterLogEvent event, int intCode,
                                                MoPubErrorCode errorCode) {

        MoPubLog.log(event, ADAPTER_NAME, intCode, errorCode);

        if (interstitialListener != null) {
            interstitialListener.onInterstitialFailed(errorCode);
        }
    }

    private class VerizonInterstitialFactoryListener implements InterstitialAdFactory.InterstitialAdFactoryListener {
        final CustomEventInterstitialListener listener = interstitialListener;

        @Override
        public void onLoaded(final InterstitialAdFactory interstitialAdFactory, final InterstitialAd interstitialAd) {

            verizonInterstitialAd = interstitialAd;

            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);

            final CreativeInfo creativeInfo = verizonInterstitialAd == null ? null : verizonInterstitialAd.getCreativeInfo();
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Verizon creative info: " + creativeInfo);

            VerizonUtils.postOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (listener != null) {
                        listener.onInterstitialLoaded();
                    }
                }
            });
        }

        @Override
        public void onCacheLoaded(final InterstitialAdFactory interstitialAdFactory, final int i, final int i1) {
        }

        @Override
        public void onCacheUpdated(final InterstitialAdFactory interstitialAdFactory, final int i) {
        }

        @Override
        public void onError(final InterstitialAdFactory interstitialAdFactory, final ErrorInfo errorInfo) {

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Failed to load Verizon interstitial due to " +
                    "error: " + errorInfo);
            VerizonUtils.postOnUiThread(new Runnable() {

                @Override
                public void run() {
                    final MoPubErrorCode errorCode = VerizonUtils.convertErrorCodeToMoPub(errorInfo.getErrorCode());

                    logAndNotifyInterstitialFailed(LOAD_FAILED, errorCode.getIntCode(), errorCode);
                }
            });
        }
    }

    private class VerizonInterstitialListener implements InterstitialAd.InterstitialAdListener {
        final CustomEventInterstitialListener listener = interstitialListener;

        @Override
        public void onError(final InterstitialAd interstitialAd, final ErrorInfo errorInfo) {

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Failed to show Verizon interstitial due to " +
                    "error: " + errorInfo);
            VerizonUtils.postOnUiThread(new Runnable() {

                @Override
                public void run() {
                    final MoPubErrorCode errorCode = VerizonUtils.convertErrorCodeToMoPub(errorInfo.getErrorCode());

                    logAndNotifyInterstitialFailed(SHOW_FAILED, errorCode.getIntCode(), errorCode);
                }
            });
        }

        @Override
        public void onShown(final InterstitialAd interstitialAd) {

            MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);
            VerizonUtils.postOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (listener != null) {
                        listener.onInterstitialShown();
                    }
                }
            });
        }

        @Override
        public void onClosed(final InterstitialAd interstitialAd) {

            MoPubLog.log(DID_DISAPPEAR, ADAPTER_NAME);
            VerizonUtils.postOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (listener != null) {
                        listener.onInterstitialDismissed();
                    }
                }
            });
        }

        @Override
        public void onClicked(final InterstitialAd interstitialAd) {

            MoPubLog.log(CLICKED, ADAPTER_NAME);
            VerizonUtils.postOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (listener != null) {
                        listener.onInterstitialClicked();
                    }
                }
            });
        }

        @Override
        public void onAdLeftApplication(final InterstitialAd interstitialAd) {
            // Only logging this event. No need to call interstitialListener.onLeaveApplication()
            // because it's an alias for interstitialListener.onInterstitialClicked()
            MoPubLog.log(WILL_LEAVE_APPLICATION, ADAPTER_NAME);
        }

        @Override
        public void onEvent(final InterstitialAd interstitialAd, final String s, final String s1, final Map<String, Object> map) {
        }
    }
}

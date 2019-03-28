package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import com.mopub.common.MoPub;
import com.mopub.common.logging.MoPubLog;
import com.verizon.ads.Bid;
import com.verizon.ads.BidRequestListener;
import com.verizon.ads.CreativeInfo;
import com.verizon.ads.ErrorInfo;
import com.verizon.ads.RequestMetadata;
import com.verizon.ads.VASAds;
import com.verizon.ads.edition.StandardEdition;
import com.verizon.ads.inlineplacement.AdSize;
import com.verizon.ads.inlineplacement.InlineAdFactory;
import com.verizon.ads.inlineplacement.InlineAdView;

import java.util.Collections;
import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.WILL_LEAVE_APPLICATION;

public class VerizonBanner extends CustomEventBanner {

    private static final String ADAPTER_NAME = VerizonBanner.class.getSimpleName();

    private static final String PLACEMENT_ID_KEY = "placementId";
    private static final String SITE_ID_KEY = "siteId";
    private static final String HEIGHT_KEY = "height";
    private static final String WIDTH_KEY = "width";

    private InlineAdView verizonInlineAd;
    private CustomEventBannerListener bannerListener;
    private FrameLayout internalView;

    @NonNull
    private VerizonAdapterConfiguration verizonAdapterConfiguration;

    public VerizonBanner() {
        verizonAdapterConfiguration = new VerizonAdapterConfiguration();
    }

    @Override
    protected void loadBanner(final Context context, final CustomEventBannerListener customEventBannerListener,
                              final Map<String, Object> localExtras, final Map<String, String> serverExtras) {

        bannerListener = customEventBannerListener;

        if (serverExtras == null || serverExtras.isEmpty()) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Ad request to Verizon failed because " +
                    "serverExtras is null");
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            if (bannerListener != null) {
                bannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }

            return;
        }

        if (!VASAds.isInitialized()) {
            final String siteId = serverExtras.get(SITE_ID_KEY);

            if (TextUtils.isEmpty(siteId)) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Ad request to Verizon failed because " +
                        "siteId is empty");
                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                        MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

                if (bannerListener != null) {
                    bannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }

                return;
            }

            if (!(context instanceof Activity)) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Ad request to Verizon failed because " +
                        "context is not an Activity");
                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                        MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

                if (bannerListener != null) {
                    bannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }

                return;
            }

            final boolean success = StandardEdition.initializeWithActivity((Activity) context, siteId);

            if (!success) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Failed to initialize the Verizon SDK");
                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                        MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

                if (bannerListener != null) {
                    bannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }

                return;
            }
        }

        final String placementId = serverExtras.get(PLACEMENT_ID_KEY);
        final String widthString = serverExtras.get(WIDTH_KEY);
        final String heightString = serverExtras.get(HEIGHT_KEY);

        if (TextUtils.isEmpty(widthString) || TextUtils.isEmpty(heightString)) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Ad request to Verizon failed because width " +
                    "or height is empty");
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            if (bannerListener != null) {
                bannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }

            return;
        }

        try {
            final int width = Integer.parseInt(widthString);
            final int height = Integer.parseInt(heightString);

            if (TextUtils.isEmpty(placementId) || (width < 0) || (height < 0)) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME,
                        "Ad request to Verizon failed because the placement ID is empty, or either " +
                                "width or height is less than 0");

                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                        MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

                if (bannerListener != null) {
                    bannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }

                return;
            }

            internalView = new FrameLayout(context);

            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            internalView.setLayoutParams(lp);

            AdViewController.setShouldHonorServerDimensions(internalView);
            VASAds.setLocationEnabled(MoPub.getLocationAwareness() != MoPub.LocationAwareness.DISABLED);

            Bid bid = BidCache.get(placementId);
            final InlineAdFactory inlineAdFactory = new InlineAdFactory(context, placementId,
                    Collections.singletonList(new AdSize(width, height)), new VerizonInlineAdFactoryListener());

            if (bid == null) {
                RequestMetadata requestMetadata = new RequestMetadata.Builder().setMediator(VerizonAdapterConfiguration.MEDIATOR_ID).build();
                inlineAdFactory.setRequestMetaData(requestMetadata);

                inlineAdFactory.load(new VerizonInlineAdListener());
            } else {
                inlineAdFactory.load(bid, new VerizonInlineAdListener());
            }

            verizonAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
        } catch (NumberFormatException e) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "Fail to parse ad's width and/or height.", e);

            if (bannerListener != null) {
                bannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
        }
    }

    /**
     * Call this method to cache a super auction bid for the specified placement ID
     *
     * @param context            a non-null Context
     * @param placementId        a valid placement ID. Cannot be null or empty.
     * @param adSizes            a list of acceptable {@link AdSize}s. Cannot be null or empty.
     * @param requestMetadata    a {@link RequestMetadata} instance for the request or null
     * @param bidRequestListener an instance of {@link BidRequestListener}. Cannot be null.
     */
    public static void requestBid(final Context context, final String placementId, final List<AdSize> adSizes,
                                  final RequestMetadata requestMetadata, final BidRequestListener bidRequestListener) {

        if (bidRequestListener == null) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "bidRequestListener parameter cannot be null.");

            return;
        }

        RequestMetadata.Builder builder = new RequestMetadata.Builder(requestMetadata);
        RequestMetadata actualRequestMetadata = builder.setMediator(VerizonAdapterConfiguration.MEDIATOR_ID).build();

        InlineAdFactory.requestBid(context, placementId, adSizes, actualRequestMetadata, new BidRequestListener() {
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
    protected void onInvalidate() {
        VerizonUtils.postOnUiThread(new Runnable() {

            @Override
            public void run() {
                bannerListener = null;

                // Destroy any hanging references
                if (verizonInlineAd != null) {
                    verizonInlineAd.destroy();
                    verizonInlineAd = null;
                }
            }
        });
    }

    class VerizonInlineAdFactoryListener implements InlineAdFactory.InlineAdFactoryListener {
        final CustomEventBannerListener listener = bannerListener;

        @Override
        public void onLoaded(final InlineAdFactory inlineAdFactory, final InlineAdView inlineAdView) {
            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);

            final CreativeInfo creativeInfo = verizonInlineAd == null ? null : verizonInlineAd.getCreativeInfo();
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Verizon creative info: " + creativeInfo);

            VerizonUtils.postOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (internalView != null) {
                        internalView.addView(inlineAdView);
                    }

                    if (listener != null) {
                        listener.onBannerLoaded(internalView);
                    }
                }
            });
        }

        @Override
        public void onCacheLoaded(final InlineAdFactory inlineAdFactory, final int i, final int i1) {
        }

        @Override
        public void onCacheUpdated(final InlineAdFactory inlineAdFactory, final int i) {
        }

        @Override
        public void onError(final InlineAdFactory inlineAdFactory, final ErrorInfo errorInfo) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unable to show Verizon banner due to error: "
                    + errorInfo);

            VerizonUtils.postOnUiThread(new Runnable() {

                @Override
                public void run() {
                    final MoPubErrorCode errorCode = VerizonUtils.convertErrorCodeToMoPub(errorInfo.getErrorCode());

                    if (listener != null) {
                        listener.onBannerFailed(errorCode);
                    }
                    MoPubLog.log(SHOW_FAILED, ADAPTER_NAME, errorCode.getIntCode(), errorCode);
                }
            });
        }
    }

    private class VerizonInlineAdListener implements InlineAdView.InlineAdListener {
        final CustomEventBannerListener listener = bannerListener;

        @Override
        public void onError(final InlineAdView inlineAdView, final ErrorInfo errorInfo) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unable to show Verizon banner due to error: "
                    + errorInfo);

            VerizonUtils.postOnUiThread(new Runnable() {

                @Override
                public void run() {
                    final MoPubErrorCode errorCode = VerizonUtils.convertErrorCodeToMoPub(errorInfo.getErrorCode());

                    if (listener != null) {
                        listener.onBannerFailed(errorCode);
                    }
                    MoPubLog.log(SHOW_FAILED, ADAPTER_NAME, errorCode.getIntCode(), errorCode);
                }
            });
        }

        @Override
        public void onResized(final InlineAdView inlineAdView) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Verizon banner resized");
        }

        @Override
        public void onExpanded(final InlineAdView inlineAdView) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Verizon banner expanded");

            VerizonUtils.postOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (listener != null) {
                        listener.onBannerExpanded();
                    }
                }
            });
        }

        @Override
        public void onCollapsed(final InlineAdView inlineAdView) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Verizon banner collapsed");

            VerizonUtils.postOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (listener != null) {
                        listener.onBannerCollapsed();
                    }
                }
            });
        }

        @Override
        public void onClicked(final InlineAdView inlineAdView) {
            MoPubLog.log(CLICKED, ADAPTER_NAME);

            VerizonUtils.postOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (listener != null) {
                        listener.onBannerClicked();
                    }
                }
            });
        }

        @Override
        public void onAdLeftApplication(final InlineAdView inlineAdView) {
            // Only logging this event. No need to call bannerListener.onLeaveApplication()
            // because it's an alias for bannerListener.onBannerClicked()
            MoPubLog.log(WILL_LEAVE_APPLICATION, ADAPTER_NAME);
        }

        @Override
        public void onAdRefreshed(final InlineAdView inlineAdView) {
        }

        @Override
        public void onEvent(final InlineAdView inlineAdView, final String s, final String s1, final Map<String, Object> map) {
        }
    }
}

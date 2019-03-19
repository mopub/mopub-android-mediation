package com.mopub.nativeads;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;

import com.millennialmedia.AppInfo;
import com.millennialmedia.CreativeInfo;
import com.millennialmedia.MMException;
import com.millennialmedia.MMLog;
import com.millennialmedia.MMSDK;
import com.millennialmedia.NativeAd;
import com.millennialmedia.internal.ActivityListenerManager;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.MillennialAdapterConfiguration;
import com.mopub.mobileads.MillennialUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.EXPIRED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.nativeads.NativeImageHelper.preCacheImages;

public class MillennialNative extends CustomEventNative {

    private static final String DCN_KEY = "dcn";
    private static final String APID_KEY = "adUnitID";
    private static final String ADAPTER_NAME = MillennialNative.class.getSimpleName();

    MillennialStaticNativeAd staticNativeAd;
    @NonNull
    private MillennialAdapterConfiguration mMillennialAdapterConfiguration;


    static {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "Millennial Media Adapter Version: " + MillennialUtils.MEDIATOR_ID);
    }

    public CreativeInfo getCreativeInfo() {
        if (staticNativeAd == null) {
            return null;
        }
        return staticNativeAd.getCreativeInfo();
    }

    public MillennialNative() {
        mMillennialAdapterConfiguration = new MillennialAdapterConfiguration();
    }

    @Override
    protected void loadNativeAd(final Context context, final CustomEventNativeListener customEventNativeListener,
                                final Map<String, Object> localExtras, final Map<String, String> serverExtras) {

        if (context instanceof Activity) {
            try {
                MMSDK.initialize((Activity) context, ActivityListenerManager.LifecycleState.RESUMED);
                mMillennialAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
            } catch (IllegalStateException e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Exception occurred initializing the " +
                        "MM SDK.", e);
                customEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_NO_FILL);

                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                        NativeErrorCode.NETWORK_NO_FILL.getIntCode(),
                        NativeErrorCode.NETWORK_NO_FILL);

                return;
            }
        } else if (context instanceof Application) {
            try {
                MMSDK.initialize((Application) context);
                mMillennialAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
            } catch (MMException e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Exception occurred initializing the MM SDK.", e);
                customEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_NO_FILL);

                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                        NativeErrorCode.NETWORK_NO_FILL.getIntCode(),
                        NativeErrorCode.NETWORK_NO_FILL);

                return;
            }
        } else {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "MM SDK must be initialized with an Activity or " +
                    "Application context.");
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_NO_FILL);

            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    NativeErrorCode.NETWORK_NO_FILL.getIntCode(),
                    NativeErrorCode.NETWORK_NO_FILL);

            return;
        }

        String placementId = serverExtras.get(APID_KEY);
        String siteId = serverExtras.get(DCN_KEY);

        if (MillennialUtils.isEmpty(placementId)) {
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_NO_FILL);

            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    NativeErrorCode.NETWORK_NO_FILL.getIntCode(),
                    NativeErrorCode.NETWORK_NO_FILL);
            return;
        }

        AppInfo ai = new AppInfo().setMediator(MillennialUtils.MEDIATOR_ID).setSiteId(siteId);

        try {
            MMSDK.setAppInfo(ai);

            NativeAd nativeAd = NativeAd.createInstance(placementId, NativeAd.NATIVE_TYPE_INLINE);
            staticNativeAd = new MillennialStaticNativeAd(context, nativeAd, new ImpressionTracker(context),
                    new NativeClickHandler(context), customEventNativeListener);

            staticNativeAd.loadAd();

        } catch (MMException e) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "An exception occurred loading a native ad " +
                    "from MM SDK", e);
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_NO_FILL);

            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    NativeErrorCode.NETWORK_NO_FILL.getIntCode(),
                    NativeErrorCode.NETWORK_NO_FILL);
        }
    }

    static class MillennialStaticNativeAd extends StaticNativeAd implements NativeAd.NativeListener {
        private final Context context;
        private NativeAd nativeAd;
        private final ImpressionTracker impressionTracker;
        private final NativeClickHandler nativeClickHandler;
        private final CustomEventNativeListener listener;

        private MillennialStaticNativeAd(final Context context, final NativeAd nativeAd,
                                         final ImpressionTracker impressionTracker, final NativeClickHandler nativeClickHandler,
                                         final CustomEventNativeListener customEventNativeListener) {
            this.context = context.getApplicationContext();
            this.nativeAd = nativeAd;
            this.impressionTracker = impressionTracker;
            this.nativeClickHandler = nativeClickHandler;
            listener = customEventNativeListener;

            nativeAd.setListener(this);
        }

        void loadAd() throws MMException {
            nativeAd.load(context, null);

            MoPubLog.log(LOAD_ATTEMPTED, ADAPTER_NAME);
        }

        CreativeInfo getCreativeInfo() {
            if (nativeAd == null) {
                return null;
            }

            return nativeAd.getCreativeInfo();
        }

        // Lifecycle Handlers
        @Override
        public void prepare(final View view) {
            // Must access these methods directly to get impressions to fire.
            nativeAd.getIconImage();
            nativeAd.getDisclaimer();
            impressionTracker.addView(view, this);
            nativeClickHandler.setOnClickListener(view, this);
        }

        @Override
        public void clear(final View view) {
            impressionTracker.removeView(view);
            nativeClickHandler.clearOnClickListener(view);
        }

        @Override
        public void destroy() {
            impressionTracker.destroy();
            nativeAd.destroy();
            nativeAd = null;
        }

        // Event Handlers
        @Override
        public void recordImpression(final View view) {
            notifyAdImpressed();

            try {
                nativeAd.fireImpression();

                MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);
            } catch (MMException e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Error tracking Millennial native ad " +
                        "impression", e);
            }
        }

        @Override
        public void handleClick(final View view) {
            notifyAdClicked();

            if (getClickDestinationUrl() != null) {
                nativeClickHandler.openClickDestinationUrl(getClickDestinationUrl(), view);
                nativeAd.fireCallToActionClicked();
                MoPubLog.log(CLICKED, ADAPTER_NAME);
            }
        }

        // MM'S Native listener
        @Override
        public void onLoaded(NativeAd nativeAd) {
            CreativeInfo creativeInfo = getCreativeInfo();
            if ((creativeInfo != null) && MMLog.isDebugEnabled()) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Native Creative Info: " + creativeInfo);
            }

            // Set assets
            String iconImageUrl = nativeAd.getImageUrl(NativeAd.ComponentName.ICON_IMAGE, 1);
            String mainImageUrl = nativeAd.getImageUrl(NativeAd.ComponentName.MAIN_IMAGE, 1);

            setTitle(nativeAd.getTitle().getText().toString());
            setText(nativeAd.getBody().getText().toString());
            setCallToAction(nativeAd.getCallToActionButton().getText().toString());

            final String clickDestinationUrl = nativeAd.getCallToActionUrl();
            if (clickDestinationUrl == null) {
                MillennialUtils.postOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MoPubLog.log(CUSTOM, ADAPTER_NAME, "Millennial native ad encountered null destination url.");
                        listener.onNativeAdFailed(NativeErrorCode.NETWORK_NO_FILL);

                        MoPubLog.log(LOAD_FAILED,
                                ADAPTER_NAME,
                                NativeErrorCode.NETWORK_NO_FILL.getIntCode(),
                                NativeErrorCode.NETWORK_NO_FILL);
                    }
                });
                return;
            }

            setClickDestinationUrl(clickDestinationUrl);
            setIconImageUrl(iconImageUrl);
            setMainImageUrl(mainImageUrl);

            final List<String> urls = new ArrayList<>();
            if (iconImageUrl != null) {
                urls.add(iconImageUrl);
            }
            if (mainImageUrl != null) {
                urls.add(mainImageUrl);
            }

            // Add MM native assets that don't have a direct MoPub mapping
            if (nativeAd.getDisclaimer() != null) {
                addExtra("disclaimer", nativeAd.getDisclaimer().getText());
            }

            if (nativeAd.getRating() != null) {
                addExtra("rating", nativeAd.getRating().getText());
            }

            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // This has to be run on the main thread:
                    preCacheImages(context, urls, new NativeImageHelper.ImageListener() {
                        @Override
                        public void onImagesCached() {
                            listener.onNativeAdLoaded(MillennialStaticNativeAd.this);
                            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
                        }

                        @Override
                        public void onImagesFailedToCache(NativeErrorCode errorCode) {
                            listener.onNativeAdFailed(errorCode);

                            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                                    errorCode.getIntCode(),
                                    errorCode);
                        }
                    });
                }
            });
        }

        @Override
        public void onLoadFailed(NativeAd nativeAd, NativeAd.NativeErrorStatus nativeErrorStatus) {

            final NativeErrorCode error;
            switch (nativeErrorStatus.getErrorCode()) {
                case NativeAd.NativeErrorStatus.LOAD_TIMED_OUT:
                    error = NativeErrorCode.NETWORK_TIMEOUT;
                    break;
                case NativeAd.NativeErrorStatus.NO_NETWORK:
                    error = NativeErrorCode.CONNECTION_ERROR;
                    break;
                case NativeAd.NativeErrorStatus.UNKNOWN:
                    error = NativeErrorCode.UNSPECIFIED;
                    break;
                case NativeAd.NativeErrorStatus.LOAD_FAILED:
                case NativeAd.NativeErrorStatus.INIT_FAILED:
                    error = NativeErrorCode.UNEXPECTED_RESPONSE_CODE;
                    break;
                case NativeAd.NativeErrorStatus.ADAPTER_NOT_FOUND:
                    error = NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR;
                    break;
                case NativeAd.NativeErrorStatus.DISPLAY_FAILED:
                case NativeAd.NativeErrorStatus.EXPIRED:
                    error = NativeErrorCode.UNSPECIFIED;
                    break;
                default:
                    error = NativeErrorCode.NETWORK_NO_FILL;
            }
            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listener.onNativeAdFailed(error);

                    MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                            error.getIntCode(),
                            error);
                }
            });
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Millennial native ad failed: " + nativeErrorStatus.getDescription());
        }

        @Override
        public void onClicked(NativeAd nativeAd, NativeAd.ComponentName componentName, int i) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Millennial native ad click tracker fired.");
        }

        @Override
        public void onAdLeftApplication(NativeAd nativeAd) {
        }

        @Override
        public void onExpired(NativeAd nativeAd) {
            MoPubLog.log(EXPIRED, ADAPTER_NAME);
        }
    }
}
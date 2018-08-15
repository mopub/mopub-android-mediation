package com.mopub.nativeads;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.view.View;

import com.millennialmedia.AppInfo;
import com.millennialmedia.CreativeInfo;
import com.millennialmedia.MMException;
import com.millennialmedia.MMLog;
import com.millennialmedia.MMSDK;
import com.millennialmedia.NativeAd;
import com.millennialmedia.internal.ActivityListenerManager;
import com.mopub.common.MoPub;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.privacy.ConsentStatus;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.mobileads.MillennialUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.millennialmedia.MMSDK.setConsentData;
import static com.millennialmedia.MMSDK.setConsentRequired;
import static com.mopub.nativeads.NativeImageHelper.preCacheImages;

public class MillennialNative extends CustomEventNative {

    private static final String DCN_KEY = "dcn";
    private static final String APID_KEY = "adUnitID";
    private final static String TAG = MillennialNative.class.getSimpleName();

    MillennialStaticNativeAd staticNativeAd;

    static {
        MoPubLog.d("Millennial Media Adapter Version: " + MillennialUtils.MEDIATOR_ID);
    }

    public CreativeInfo getCreativeInfo() {
        if (staticNativeAd == null) {
            return null;
        }
        return staticNativeAd.getCreativeInfo();
    }

    @Override
    protected void loadNativeAd(final Context context, final CustomEventNativeListener customEventNativeListener,
                                Map<String, Object> localExtras, Map<String, String> serverExtras) {

        PersonalInfoManager personalInfoManager = MoPub.getPersonalInformationManager();

        if (personalInfoManager != null) {
            try {
                Boolean gdprApplies = personalInfoManager.gdprApplies();

                // Set if GDPR applies / if consent is required
                if (gdprApplies != null) {
                    setConsentRequired(gdprApplies);
                }
            } catch (NullPointerException e) {
                MoPubLog.d("GDPR applicability cannot be determined.", e);
            }

            // Pass the user consent from the MoPub SDK to One by AOL as per GDPR
            if (personalInfoManager.getPersonalInfoConsentStatus() == ConsentStatus.EXPLICIT_YES) {
                setConsentData("mopub", "1");
            }
        }

        if (context instanceof Activity) {
            try {
                MMSDK.initialize((Activity) context, ActivityListenerManager.LifecycleState.RESUMED);
            } catch (IllegalStateException e) {
                MoPubLog.d("Exception occurred initializing the MM SDK.", e);
                customEventNativeListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);

                return;
            }
        } else if (context instanceof Application) {
            try {
                MMSDK.initialize((Application) context);
            } catch (MMException e) {
                MoPubLog.d("Exception occurred initializing the MM SDK.", e);
                customEventNativeListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);

                return;
            }
        } else {
            MoPubLog.d("MM SDK must be initialized with an Activity or Application context.");
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);

            return;
        }

        String placementId = serverExtras.get(APID_KEY);
        String siteId = serverExtras.get(DCN_KEY);

        if (MillennialUtils.isEmpty(placementId)) {
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);

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
            MoPubLog.d("An exception occurred loading a native ad from MM SDK", e);
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
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
            MoPubLog.d("Millennial native ad loading.");

            nativeAd.load(context, null);
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
                MoPubLog.d("Millennial native ad impression recorded.");
            } catch (MMException e) {
                MoPubLog.d("Error tracking Millennial native ad impression", e);
            }
        }

        @Override
        public void handleClick(final View view) {
            notifyAdClicked();

            nativeClickHandler.openClickDestinationUrl(getClickDestinationUrl(), view);
            nativeAd.fireCallToActionClicked();
            MoPubLog.d("Millennial native ad clicked.");
        }

        // MM'S Native listener
        @Override
        public void onLoaded(NativeAd nativeAd) {
            CreativeInfo creativeInfo = getCreativeInfo();
            if ((creativeInfo != null) && MMLog.isDebugEnabled()) {
                MoPubLog.d("Native Creative Info: " + creativeInfo);
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
                        MoPubLog.d("Millennial native ad encountered null destination url.");
                        listener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
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
            addExtra("disclaimer", nativeAd.getDisclaimer().getText());
            if ( nativeAd.getRating() != null ) {
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
                            MoPubLog.d("Millennial native ad loaded.");
                        }

                        @Override
                        public void onImagesFailedToCache(NativeErrorCode errorCode) {
                            listener.onNativeAdFailed(errorCode);
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
                }
            });
            MoPubLog.d("Millennial native ad failed: " + nativeErrorStatus.getDescription());
        }

        @Override
        public void onClicked(NativeAd nativeAd, NativeAd.ComponentName componentName, int i) {
            MoPubLog.d("Millennial native ad click tracker fired.");
        }

        @Override
        public void onAdLeftApplication(NativeAd nativeAd) {
            MoPubLog.d("Millennial native ad has left the application.");
        }

        @Override
        public void onExpired(NativeAd nativeAd) {
            MoPubLog.d("Millennial native ad has expired!");
        }
    }
}

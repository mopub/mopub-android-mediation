package com.mopub.nativeads;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.text.TextUtils;
import android.view.View;

import com.mopub.common.MoPub;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.VerizonAdapterConfiguration;
import com.mopub.mobileads.VerizonUtils;
import com.verizon.ads.ActivityStateManager;
import com.verizon.ads.Component;
import com.verizon.ads.CreativeInfo;
import com.verizon.ads.ErrorInfo;
import com.verizon.ads.VASAds;
import com.verizon.ads.edition.StandardEdition;
import com.verizon.ads.nativeplacement.NativeAd;
import com.verizon.ads.nativeplacement.NativeAdFactory;

import org.json.JSONObject;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.DID_DISAPPEAR;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.WILL_LEAVE_APPLICATION;

public class VerizonNative extends CustomEventNative {

    private static final String ADAPTER_NAME = VerizonNative.class.getSimpleName();

    private static final String COMP_ID_RATING = "rating";
    private static final String COMP_ID_DISCLAIMER = "disclaimer";
    private static final String PLACEMENT_ID_KEY = "placementId";
    private static final String SITE_ID_KEY = "siteId";

    private VerizonStaticNativeAd verizonStaticNativeAd;
    private VerizonAdapterConfiguration verizonAdapterConfiguration;
    private CustomEventNativeListener customEventNativeListener;
    private Context context;

    static final String COMP_ID_VIDEO = "video";

    static {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "Verizon Adapter Version: " +
                VerizonAdapterConfiguration.MEDIATOR_ID);
    }

    VerizonNative() {
        super();
        verizonAdapterConfiguration = new VerizonAdapterConfiguration();
    }

    @Override
    protected void loadNativeAd(@NonNull final Context context,
                                @NonNull final CustomEventNativeListener customEventNativeListener,
                                @NonNull final Map<String, Object> localExtras,
                                @NonNull final Map<String, String> serverExtras) {

        this.customEventNativeListener = customEventNativeListener;
        this.context = context;

        if (serverExtras.isEmpty()) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Ad request to Verizon failed because " +
                    "serverExtras is null or empty");
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            customEventNativeListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);

            return;
        }

        String siteId = serverExtras.get(SITE_ID_KEY);
        String placementId = serverExtras.get(PLACEMENT_ID_KEY);
        String[] adTypes = {"inline"}; //currently only ad type.

        if (!VASAds.isInitialized()) {

            Application application = null;

            if (context instanceof Application) {
                application = (Application) context;
            } else if (context instanceof Activity) {
                application = ((Activity) context).getApplication();
            }

            if (!StandardEdition.initialize(application, siteId)) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Failed to initialize the Verizon SDK");
                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                        MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

                customEventNativeListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
            }
        }

        // Ensure that siteId is the key and cache serverExtras so siteId can be used to initialize VAS early at next launch
        if (!TextUtils.isEmpty(siteId)) {
            serverExtras.put(VerizonAdapterConfiguration.VAS_SITE_ID_KEY, siteId);
        }

        if (verizonAdapterConfiguration != null) {
            verizonAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
        }

        // The current activity must be set as resumed so VAS can track ad visibility
        ActivityStateManager activityStateManager = VASAds.getActivityStateManager();
        if (activityStateManager != null && context instanceof Activity) {
            activityStateManager.setState((Activity) context, ActivityStateManager.ActivityState.RESUMED);
        }

        if (TextUtils.isEmpty(placementId)) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Invalid server extras! Make sure placementId is set");
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            customEventNativeListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);

            return;
        }

        VASAds.setLocationEnabled(MoPub.getLocationAwareness() != MoPub.LocationAwareness.DISABLED);

        NativeAdFactory nativeAdFactory = new NativeAdFactory(context, placementId, adTypes,
                new VerizonNativeFactoryListener());

        nativeAdFactory.load(new VerizonNativeListener());
        MoPubLog.log(placementId, LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    @Override
    protected void onInvalidate() {

        VerizonUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Destroy any hanging references
                if (verizonStaticNativeAd != null) {
                    verizonStaticNativeAd.destroy();
                    verizonStaticNativeAd = null;
                }
            }
        });
    }

    static class VerizonStaticNativeAd extends StaticNativeAd {
        @NonNull
        private final Context context;
        @NonNull
        private final NativeAd nativeAd;
        @NonNull
        private final ImpressionTracker impressionTracker;
        @NonNull
        private final NativeClickHandler nativeClickHandler;


        VerizonStaticNativeAd(@NonNull final Context context,
                              @NonNull final NativeAd nativeAd,
                              @NonNull final ImpressionTracker impressionTracker,
                              @NonNull final NativeClickHandler nativeClickHandler) {

            this.context = context.getApplicationContext();
            this.nativeAd = nativeAd;
            this.impressionTracker = impressionTracker;
            this.nativeClickHandler = nativeClickHandler;
        }

        // Lifecycle Handlers
        @Override
        public void prepare(@NonNull final View view) {
            impressionTracker.addView(view, this);
            nativeClickHandler.setOnClickListener(view, this);
        }

        @Override
        public void clear(@NonNull final View view) {
            impressionTracker.removeView(view);
            nativeClickHandler.clearOnClickListener(view);
        }

        @Override
        public void destroy() {
            impressionTracker.destroy();
            super.destroy();
        }

        // Event Handlers
        @Override
        public void recordImpression(@NonNull final View view) {
            notifyAdImpressed();
            nativeAd.fireImpression();
        }

        @Override
        public void handleClick(@Nullable final View view) {
            MoPubLog.log(CLICKED, ADAPTER_NAME);
            notifyAdClicked();
            nativeAd.invokeDefaultAction(context);
        }
    }

    class VerizonNativeFactoryListener implements NativeAdFactory.NativeAdFactoryListener {

        @Override
        public void onLoaded(final NativeAdFactory nativeAdFactory, final NativeAd nativeAd) {

            VerizonUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {

                    final CreativeInfo creativeInfo = nativeAd.getCreativeInfo();
                    final Context context = VerizonNative.this.context;

                    verizonStaticNativeAd = new VerizonStaticNativeAd(context, nativeAd, new ImpressionTracker(context),
                            new NativeClickHandler(context));

                    //Populate verizonStaticNativeAd with values from nativeAd
                    populateNativeAd(nativeAd);

                    MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
                    if (creativeInfo != null) {
                        MoPubLog.log(CUSTOM, ADAPTER_NAME, "Ad Creative Info: " + creativeInfo);
                    }

                    customEventNativeListener.onNativeAdLoaded(verizonStaticNativeAd);
                }
            });
        }

        @Override
        public void onCacheLoaded(final NativeAdFactory nativeAdFactory, final int numRequested,
                                  final int numReceived) {
            //NO-OP
        }

        @Override
        public void onCacheUpdated(final NativeAdFactory nativeAdFactory, final int cacheSize) {
            //NO-OP
        }

        @Override
        public void onError(final NativeAdFactory nativeAdFactory, final ErrorInfo errorInfo) {

            VerizonUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "Error Loading: " + errorInfo);
                    NativeErrorCode errorCode = VerizonUtils.convertErrorInfoToMoPubNative(errorInfo);
                    MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, errorCode.getIntCode(), errorCode);
                }
            });
        }

        private void populateNativeAd(final NativeAd nativeAd) {

            if (nativeAd == null) {
                return;
            }

            verizonStaticNativeAd.setTitle(parseTextComponent("title", nativeAd));
            verizonStaticNativeAd.setText(parseTextComponent("body", nativeAd));
            verizonStaticNativeAd.setCallToAction(parseTextComponent("callToAction", nativeAd));
            verizonStaticNativeAd.setMainImageUrl(parseURLComponent("mainImage", nativeAd));
            verizonStaticNativeAd.setIconImageUrl(parseURLComponent("iconImage", nativeAd));

            final String ratingString = parseTextComponent("rating", nativeAd);

            if (!TextUtils.isEmpty(ratingString)) {
                final String[] ratingArray = ratingString.trim().split("\\s+");

                if (ratingArray.length > 0) {
                    try {
                        final Double rating = Double.parseDouble(ratingArray[0]);

                        verizonStaticNativeAd.setStarRating(rating);
                        verizonStaticNativeAd.addExtra(COMP_ID_RATING, ratingArray[0]);
                    } catch (NumberFormatException e) {
                        // do nothing
                    }
                }
            }

            final String disclaimer = parseTextComponent("disclaimer", nativeAd);

            if (!TextUtils.isEmpty(disclaimer)) {
                verizonStaticNativeAd.addExtra(COMP_ID_DISCLAIMER, disclaimer);
            }

            final String videoURL = parseURLComponent("video", nativeAd);

            if (!TextUtils.isEmpty(videoURL)) {
                verizonStaticNativeAd.addExtra(COMP_ID_VIDEO, videoURL);
            }
        }
    }

    private String parseTextComponent(final String key, final NativeAd nativeAd) {
        final JSONObject jsonObject = nativeAd.getJSON(key);

        if (jsonObject != null) {
            try {
                final JSONObject dataObject = jsonObject.getJSONObject("data");
                return dataObject.optString("value");
            } catch (Exception e) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unable to parse " + key);
            }
        }

        return null;
    }

    private String parseURLComponent(final String key, final NativeAd nativeAd) {
        final JSONObject jsonObject = nativeAd.getJSON(key);

        if (jsonObject != null) {
            try {
                final JSONObject dataObject = jsonObject.getJSONObject("data");
                return dataObject.optString("url");
            } catch (Exception e) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unable to parse " + key);
            }
        }

        return null;
    }

    class VerizonNativeListener implements NativeAd.NativeAdListener {

        @Override
        public void onError(final NativeAd nativeAd, final ErrorInfo errorInfo) {

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Error: " + errorInfo);
            VerizonUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    NativeErrorCode errorCode = VerizonUtils.convertErrorInfoToMoPubNative(errorInfo);
                    customEventNativeListener.onNativeAdFailed(errorCode);
                    MoPubLog.log(SHOW_FAILED, ADAPTER_NAME, errorCode.getIntCode(), errorCode);
                }
            });
        }

        @Override
        public void onClosed(final NativeAd nativeAd) {
            MoPubLog.log(DID_DISAPPEAR, ADAPTER_NAME);
        }

        @Override
        public void onClicked(final NativeAd nativeAd, final Component component) {
            MoPubLog.log(CLICKED, ADAPTER_NAME);
        }

        @Override
        public void onAdLeftApplication(final NativeAd nativeAd) {
            MoPubLog.log(WILL_LEAVE_APPLICATION, ADAPTER_NAME);
        }

        @Override
        public void onEvent(final NativeAd nativeAd, final String source, final String eventId, final Map<String, Object> arguments) {
            //NO-OP
        }
    }
}

package com.mopub.nativeads;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.VerizonAdapterConfiguration;
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
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.DID_DISAPPEAR;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.WILL_LEAVE_APPLICATION;

public class VerizonNative extends CustomEventNative {

    private static final String ADAPTER_NAME = VerizonNative.class.getSimpleName();

    private static final String COMP_ID_RATING = "rating";
    private static final String COMP_ID_DISCLAIMER = "disclaimer";
    private static final String PLACEMENT_ID_KEY = "placementId";
    private static final String SITE_ID_KEY = "siteId";
    private static final String AD_IMPRESSION_EVENT_ID = "adImpression";

    private final VerizonAdapterConfiguration verizonAdapterConfiguration;

    private VerizonStaticNativeAd verizonStaticNativeAd;
    private VerizonNativeListener verizonNativeListener;
    private CustomEventNativeListener customEventNativeListener;
    private static String mPlacementId;

    static final String COMP_ID_VIDEO = "video";

    static {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Verizon Adapter Version: " +
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
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(customEventNativeListener);
        Preconditions.checkNotNull(localExtras);
        Preconditions.checkNotNull(serverExtras);

        this.customEventNativeListener = customEventNativeListener;

        if (serverExtras.isEmpty()) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Ad request to Verizon " +
                    "failed because serverExtras is null or empty");
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            customEventNativeListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);

            return;
        }

        final String siteId = serverExtras.get(SITE_ID_KEY);
        mPlacementId = serverExtras.get(PLACEMENT_ID_KEY);
        final String[] adTypes = {"100", "simpleImage", "simpleVideo"};

        if (!VASAds.isInitialized()) {

            Application application = null;

            if (context instanceof Application) {
                application = (Application) context;
            } else if (context instanceof Activity) {
                application = ((Activity) context).getApplication();
            }

            if (!StandardEdition.initialize(application, siteId)) {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Failed to initialize " +
                        "the Verizon SDK");
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
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
        final ActivityStateManager activityStateManager = VASAds.getActivityStateManager();
        if (activityStateManager != null && context instanceof Activity) {
            activityStateManager.setState((Activity) context, ActivityStateManager.ActivityState.RESUMED);
        }

        if (TextUtils.isEmpty(mPlacementId)) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Invalid server extras! " +
                    "Make sure placementId is set");
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            customEventNativeListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);

            return;
        }

        if (!TextUtils.isEmpty(serverExtras.get(VerizonAdapterConfiguration.SERVER_EXTRAS_AD_CONTENT_KEY))) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME,
                    "Advanced Bidding for native placements is not supported at this time. " +
                            "serverExtras key '" + VerizonAdapterConfiguration.SERVER_EXTRAS_AD_CONTENT_KEY +
                            "' should have no value.");

            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            customEventNativeListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);

            return;
        }

        NativeAdFactory nativeAdFactory = new NativeAdFactory(context, mPlacementId, adTypes,
                new VerizonNativeFactoryListener());

        verizonNativeListener = new VerizonNativeListener();
        nativeAdFactory.load(verizonNativeListener);
        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    static class VerizonStaticNativeAd extends StaticNativeAd {

        @NonNull
        private final NativeAd nativeAd;

        VerizonStaticNativeAd(@NonNull final NativeAd nativeAd) {

            this.nativeAd = nativeAd;
        }

        @NonNull
        public NativeAd getNativeAd() {
            return nativeAd;
        }

        @Override
        public void destroy() {
            super.destroy();
            nativeAd.destroy();
        }
    }

    class VerizonNativeFactoryListener implements NativeAdFactory.NativeAdFactoryListener {

        @Override
        public void onLoaded(final NativeAdFactory nativeAdFactory, final NativeAd nativeAd) {

            VerizonAdapterConfiguration.postOnUiThread(new Runnable() {
                @Override
                public void run() {

                    final CreativeInfo creativeInfo = nativeAd.getCreativeInfo();

                    verizonStaticNativeAd = new VerizonStaticNativeAd(nativeAd);
                    verizonNativeListener.setVerizonStaticNativeAd(verizonStaticNativeAd);

                    //Populate verizonStaticNativeAd with values from nativeAd
                    populateNativeAd(nativeAd);

                    MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
                    if (creativeInfo != null) {
                        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Ad Creative " +
                                "Info: " + creativeInfo);
                    }

                    customEventNativeListener.onNativeAdLoaded(verizonStaticNativeAd);
                }
            });
        }


        @Override
        public void onError(final NativeAdFactory nativeAdFactory, final ErrorInfo errorInfo) {

            VerizonAdapterConfiguration.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Error Loading: " +
                            errorInfo);
                    NativeErrorCode errorCode = VerizonAdapterConfiguration.convertErrorInfoToMoPubNative(errorInfo);
                    customEventNativeListener.onNativeAdFailed(errorCode);
                    MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, errorCode.getIntCode(),
                            errorCode);
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
            verizonStaticNativeAd.setSponsored(parseURLComponent("disclaimer", nativeAd));
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
                        MoPubLog.log(CUSTOM_WITH_THROWABLE, ADAPTER_NAME, "Exception occurred" +
                                " while parsing Verizon's native ad rating.", e);
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
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Unable to parse " + key);
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
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Unable to parse " + key);
            }
        }

        return null;
    }

    private static String getAdNetworkId() {
        return mPlacementId;
    }

    class VerizonNativeListener implements NativeAd.NativeAdListener {

        private VerizonStaticNativeAd verizonStaticNativeAd;

        public void setVerizonStaticNativeAd(VerizonStaticNativeAd verizonStaticNativeAd) {
            this.verizonStaticNativeAd = verizonStaticNativeAd;
        }

        @Override
        public void onError(final NativeAd nativeAd, final ErrorInfo errorInfo) {

            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Error: " + errorInfo);
            VerizonAdapterConfiguration.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    NativeErrorCode errorCode =
                            VerizonAdapterConfiguration.convertErrorInfoToMoPubNative(errorInfo);
                    customEventNativeListener.onNativeAdFailed(errorCode);
                    MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME, errorCode.getIntCode(),
                            errorCode);
                }
            });
        }

        @Override
        public void onClosed(final NativeAd nativeAd) {
            MoPubLog.log(getAdNetworkId(), DID_DISAPPEAR, ADAPTER_NAME);
        }

        @Override
        public void onClicked(final NativeAd nativeAd, final Component component) {
            MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);

            VerizonAdapterConfiguration.postOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (verizonStaticNativeAd != null) {
                        verizonStaticNativeAd.notifyAdClicked();
                    }
                }
            });
        }

        @Override
        public void onAdLeftApplication(final NativeAd nativeAd) {
            MoPubLog.log(getAdNetworkId(), WILL_LEAVE_APPLICATION, ADAPTER_NAME);
        }

        @Override
        public void onEvent(final NativeAd nativeAd, final String source, final String eventId,
                            final Map<String, Object> arguments) {
            if (AD_IMPRESSION_EVENT_ID.equals(eventId)) {
                MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);

                VerizonAdapterConfiguration.postOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        if (verizonStaticNativeAd != null) {
                            verizonStaticNativeAd.notifyAdImpressed();
                        }
                    }
                });
            }
        }
    }
}

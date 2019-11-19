package com.mopub.nativeads;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.MediaView;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdBase;
import com.facebook.ads.NativeAdListener;
import com.facebook.ads.NativeBannerAd;
import com.mopub.common.DataKeys;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.FacebookAdapterConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;

public class FacebookNative extends CustomEventNative {
    private static final String PLACEMENT_ID_KEY = "placement_id";
    private static final String NATIVE_BANNER_KEY = "native_banner";
    private static final String ADAPTER_NAME = FacebookNative.class.getSimpleName();
    private static AtomicBoolean sIsInitialized = new AtomicBoolean(false);
    private Boolean isNativeBanner;

    @NonNull
    private FacebookAdapterConfiguration mFacebookAdapterConfiguration;

    // CustomEventNative implementation
    public FacebookNative() {
        mFacebookAdapterConfiguration = new FacebookAdapterConfiguration();
    }

    @Override
    protected void loadNativeAd(@NonNull final Context context,
                                @NonNull final CustomEventNativeListener customEventNativeListener,
                                @NonNull final Map<String, Object> localExtras,
                                @NonNull final Map<String, String> serverExtras) {

        if (!sIsInitialized.getAndSet(true)) {
            AudienceNetworkAds.initialize(context);
        }
        final String placementId;
        if (extrasAreValid(serverExtras)) {
            placementId = serverExtras.get(PLACEMENT_ID_KEY);
            mFacebookAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
        } else {
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_NO_FILL);
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, NativeErrorCode.NETWORK_NO_FILL.getIntCode(), NativeErrorCode.NETWORK_NO_FILL);
            return;
        }

        final String bid = serverExtras.get(DataKeys.ADM_KEY);
        if (!localExtras.isEmpty()) {
            final Object isNativeBannerObject = localExtras.get(NATIVE_BANNER_KEY);

            if (isNativeBannerObject instanceof Boolean) {
                isNativeBanner = (Boolean) isNativeBannerObject;
            }
        }

        // The native banner flag is not set in localExtras.
        // Fall back to retrieving the flag from the FacebookAdapterConfiguration.
        isNativeBanner = isNativeBanner == null ? FacebookAdapterConfiguration.getNativeBannerPref() : isNativeBanner;

        if (isNativeBanner != null) {
            if (isNativeBanner) {
                final FacebookNativeAd facebookNativeBannerAd =
                        new FacebookNativeAd(context,
                                new NativeBannerAd(context, placementId), customEventNativeListener, bid);
                facebookNativeBannerAd.loadAd();

                return;
            }
        }

        // The native banner flag is set to false or not set at all.
        // Request a regular native ad.
        final FacebookNativeAd facebookNativeAd =
                new FacebookNativeAd(context,
                        new NativeAd(context, placementId), customEventNativeListener, bid);
        facebookNativeAd.loadAd();
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(PLACEMENT_ID_KEY);
        return (!TextUtils.isEmpty(placementId));
    }

    private static void registerChildViewsForInteraction(final View view, final NativeAdBase nativeAdBase,
                                                         @Nullable final MediaView mediaView, final MediaView adIconView) {

        if (nativeAdBase == null) {
            return;
        }

        final List<View> clickableViews = new ArrayList<>();
        assembleChildViewsWithLimit(view, clickableViews, 10);

        if (nativeAdBase instanceof NativeAd && mediaView != null) {
            NativeAd nativeAd = (NativeAd) nativeAdBase;
            if (clickableViews.size() == 1) {
                nativeAd.registerViewForInteraction(view, mediaView, adIconView);
            } else {
                nativeAd.registerViewForInteraction(view, mediaView, adIconView, clickableViews);
            }
        } else if (nativeAdBase instanceof NativeBannerAd) {
            NativeBannerAd nativeBannerAd = (NativeBannerAd) nativeAdBase;
            if (clickableViews.size() == 1) {
                nativeBannerAd.registerViewForInteraction(view, adIconView);
            } else {
                nativeBannerAd.registerViewForInteraction(view, adIconView, clickableViews);
            }
        }
    }

    private static void assembleChildViewsWithLimit(final View view,
                                                    final List<View> clickableViews, final int limit) {
        if (view == null) {
            MoPubLog.log(CUSTOM, "View given is null. Ignoring");
            return;
        }

        if (limit <= 0) {
            MoPubLog.log(CUSTOM, "Depth limit reached; adding this view regardless of its type.");
            clickableViews.add(view);
            return;
        }

        if (view instanceof ViewGroup && ((ViewGroup) view).getChildCount() > 0) {
            final ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                assembleChildViewsWithLimit(vg.getChildAt(i), clickableViews, limit - 1);
            }
            return;
        }

        clickableViews.add(view);
    }

    static class FacebookNativeAd extends BaseNativeAd implements NativeAdListener {
        private static final String SOCIAL_CONTEXT_FOR_AD = "socialContextForAd";

        private final Context mContext;
        private final NativeAdBase mNativeAd;
        private final CustomEventNativeListener mCustomEventNativeListener;

        private final Map<String, Object> mExtras;

        private final String mBid;

        FacebookNativeAd(final Context context,
                         final NativeAdBase nativeAd,
                         final CustomEventNativeListener customEventNativeListener,
                         final String bid) {
            mContext = context.getApplicationContext();
            mNativeAd = nativeAd;
            mCustomEventNativeListener = customEventNativeListener;
            mExtras = new HashMap<String, Object>();
            mBid = bid;
        }

        void loadAd() {
            NativeAdBase.NativeAdLoadConfigBuilder nativeAdLoadConfigBuilder = mNativeAd
                    .buildLoadAdConfig()
                    .withAdListener(this);

            if (!TextUtils.isEmpty(mBid)) {
                mNativeAd.loadAd(nativeAdLoadConfigBuilder.withBid(mBid).build());
                MoPubLog.log(mBid, LOAD_ATTEMPTED, ADAPTER_NAME);
            } else {
                mNativeAd.loadAd(nativeAdLoadConfigBuilder.build());
                MoPubLog.log(LOAD_ATTEMPTED, ADAPTER_NAME);
            }
        }

        /**
         * Returns the String corresponding to the advertiser name
         */
        final public String getAdvertiserName() {
            return mNativeAd.getAdvertiserName();
        }

        /**
         * Returns the String corresponding to the ad's title.
         */
        final public String getTitle() {
            return mNativeAd.getAdHeadline();
        }

        /**
         * Returns the String corresponding to the ad's body text. May be null.
         */
        final public String getText() {
            return mNativeAd.getAdBodyText();
        }

        /**
         * Returns the Call To Action String (i.e. "Download" or "Learn More") associated with this ad.
         */
        final public String getCallToAction() {
            return mNativeAd.getAdCallToAction();
        }

        /**
         * Returns the Sponsored Label associated with this ad.
         */
        @Nullable
        final public String getSponsoredName() {
            return mNativeAd instanceof NativeBannerAd ? mNativeAd.getSponsoredTranslation() : null;
        }

        /**
         * Returns the Privacy Information click through url.
         *
         * @return String representing the Privacy Information Icon click through url, or {@code null}
         * if not set.
         */
        final public String getPrivacyInformationIconClickThroughUrl() {
            return mNativeAd.getAdChoicesLinkUrl();
        }

        // AdListener
        @Override
        public void onAdLoaded(final Ad ad) {
            // This identity check is from Facebook's Native API sample code:
            // https://developers.facebook.com/docs/audience-network/android/native-api
            if (!mNativeAd.equals(ad) || !mNativeAd.isAdLoaded()) {
                mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_NO_FILL);
                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, NativeErrorCode.NETWORK_NO_FILL.getIntCode(), NativeErrorCode.NETWORK_NO_FILL);
                return;
            }

            addExtra(SOCIAL_CONTEXT_FOR_AD, mNativeAd.getAdSocialContext());
            mCustomEventNativeListener.onNativeAdLoaded(FacebookNativeAd.this);
            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
        }

        @Override
        public void onError(final Ad ad, final AdError adError) {
            if (adError == null) {
                mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.UNSPECIFIED);
                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, NativeErrorCode.UNSPECIFIED.getIntCode(), NativeErrorCode.UNSPECIFIED);
            } else if (adError.getErrorCode() == AdError.NO_FILL.getErrorCode()) {
                mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_NO_FILL);
                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, NativeErrorCode.NETWORK_NO_FILL.getIntCode(), NativeErrorCode.NETWORK_NO_FILL);
            } else if (adError.getErrorCode() == AdError.INTERNAL_ERROR.getErrorCode()) {
                mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_INVALID_STATE);
                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, NativeErrorCode.NETWORK_INVALID_STATE.getIntCode(), NativeErrorCode.NETWORK_INVALID_STATE);
            } else {
                mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.UNSPECIFIED);
                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, NativeErrorCode.UNSPECIFIED.getIntCode(), NativeErrorCode.UNSPECIFIED);
            }
        }

        @Override
        public void onAdClicked(final Ad ad) {
            notifyAdClicked();
            MoPubLog.log(CLICKED, ADAPTER_NAME);
        }

        @Override
        public void onLoggingImpression(final Ad ad) {
            notifyAdImpressed();
            MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);
        }

        // BaseForwardingNativeAd
        @Override
        public void prepare(final View view) {
        }

        @Override
        public void clear(final View view) {
            mNativeAd.unregisterView();
        }

        @Override
        public void destroy() {
            mNativeAd.destroy();
        }

        /**
         * Given a particular String key, return the associated Object value from the ad's extras map.
         * See {@link StaticNativeAd#getExtras()} for more information.
         */
        @Nullable
        final public Object getExtra(final String key) {
            if (!Preconditions.NoThrow.checkNotNull(key, "getExtra key is not allowed to be null")) {
                return null;
            }
            return mExtras.get(key);
        }

        /**
         * Returns a copy of the extras map, reflecting additional ad content not reflected in any
         * of the above hardcoded setters. This is particularly useful for passing down custom fields
         * with MoPub's direct-sold native ads or from mediated networks that pass back additional
         * fields.
         */
        final public Map<String, Object> getExtras() {
            return new HashMap<String, Object>(mExtras);
        }

        final public void addExtra(final String key, final Object value) {
            if (!Preconditions.NoThrow.checkNotNull(key, "addExtra key is not allowed to be null")) {
                return;
            }
            mExtras.put(key, value);
        }

        void registerChildViewsForInteraction(final View view, @Nullable final MediaView mediaView,
                                              final MediaView adIconView) {
            FacebookNative.registerChildViewsForInteraction(view, mNativeAd, mediaView, adIconView);
        }

        @Override
        public void onMediaDownloaded(final Ad ad) {
        }

        NativeAdBase getFacebookNativeAd() {
            return mNativeAd;
        }
    }
}

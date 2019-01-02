package com.mopub.nativeads;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.mopub.common.MediationSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The {@link GooglePlayServicesNative} class is used to load native Google mobile ads.
 */
public class GooglePlayServicesNative extends CustomEventNative {
    protected static final String TAG = "MoPubToAdMobNative";

    /**
     * The current version of the adapter.
     */
    private static final String ADAPTER_VERSION = "0.3.1";

    /**
     * Key to obtain AdMob application ID from the server extras provided by MoPub.
     */
    private static final String KEY_EXTRA_APPLICATION_ID = "appid";

    /**
     * Key to obtain AdMob ad unit ID from the extras provided by MoPub.
     */
    private static final String KEY_EXTRA_AD_UNIT_ID = "adunit";

    /**
     * Key to set and obtain the image orientation preference.
     */
    public static final String KEY_EXTRA_ORIENTATION_PREFERENCE = "orientation_preference";

    /**
     * Key to set and obtain the AdChoices icon placement preference.
     */
    public static final String KEY_EXTRA_AD_CHOICES_PLACEMENT = "ad_choices_placement";

    /**
     * Key to set and obtain the experimental swap margins flag.
     */
    public static final String KEY_EXPERIMENTAL_EXTRA_SWAP_MARGINS = "swap_margins";

    /**
     * Key to set and obtain the content URL to be passed with AdMob's ad request.
     */
    public static final String KEY_CONTENT_URL = "contentUrl";

    /**
     * Key to set and obtain the test device ID String to be passed with AdMob's ad request.
     */
    public static final String TEST_DEVICES_KEY = "testDevices";

    /**
     * Flag to determine whether or not the adapter has been initialized.
     */
    private static AtomicBoolean sIsInitialized = new AtomicBoolean(false);

    @Override
    protected void loadNativeAd(@NonNull final Context context,
                                @NonNull final CustomEventNativeListener customEventNativeListener,
                                @NonNull Map<String, Object> localExtras,
                                @NonNull Map<String, String> serverExtras) {

        if (!sIsInitialized.getAndSet(true)) {
            Log.i(TAG, "Adapter version - " + ADAPTER_VERSION);
            if (serverExtras.containsKey(KEY_EXTRA_APPLICATION_ID)
                    && !TextUtils.isEmpty(serverExtras.get(KEY_EXTRA_APPLICATION_ID))) {
                MobileAds.initialize(context, serverExtras.get(KEY_EXTRA_APPLICATION_ID));
            } else {
                MobileAds.initialize(context);
            }
        }

        String adUnitId = serverExtras.get(KEY_EXTRA_AD_UNIT_ID);
        if (TextUtils.isEmpty(adUnitId)) {
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_INVALID_REQUEST);
            return;
        }

        GooglePlayServicesNativeAd nativeAd = new GooglePlayServicesNativeAd(customEventNativeListener);
        nativeAd.loadAd(context, adUnitId, localExtras);
    }

    /**
     * The {@link GooglePlayServicesNativeAd} class is used to load and map Google native
     * ads to MoPub native ads.
     */
    static class GooglePlayServicesNativeAd extends BaseNativeAd {

        // Native ad assets.
        private String mTitle;
        private String mText;
        private String mMainImageUrl;
        private String mIconImageUrl;
        private String mCallToAction;
        private Double mStarRating;
        private String mAdvertiser;
        private String mStore;
        private String mPrice;
        private String mMediaView;

        /**
         * Flag to determine whether or not to swap margins from actual ad view to Google native ad
         * view.
         */
        private boolean mSwapMargins;

        /**
         * A custom event native listener used to forward Google Mobile Ads SDK events to MoPub.
         */
        private CustomEventNativeListener mCustomEventNativeListener;

        /**
         * A Google unified ad.
         */
        private UnifiedNativeAd mUnifiedNativeAd;

        public GooglePlayServicesNativeAd(
                CustomEventNativeListener customEventNativeListener) {
            this.mCustomEventNativeListener = customEventNativeListener;
        }

        public String getMediaView() {
            return mMediaView;
        }

        public void setMediaView(String mediaView) {
            this.mMediaView = mediaView;

        }

        /**
         * @return the title string associated with this native ad.
         */
        public String getTitle() {
            return mTitle;
        }

        /**
         * @return the text/body string associated with the native ad.
         */
        public String getText() {
            return mText;
        }

        /**
         * @return the main image URL associated with the native ad.
         */
        public String getMainImageUrl() {
            return mMainImageUrl;
        }

        /**
         * @return the icon image URL associated with the native ad.
         */
        public String getIconImageUrl() {
            return mIconImageUrl;
        }

        /**
         * @return the call to action string associated with the native ad.
         */
        public String getCallToAction() {
            return mCallToAction;
        }

        /**
         * @return the star rating associated with the native ad.
         */
        public Double getStarRating() {
            return mStarRating;
        }

        /**
         * @return the advertiser string associated with the native ad.
         */
        public String getAdvertiser() {
            return mAdvertiser;
        }

        /**
         * @return the store string associated with the native ad.
         */
        public String getStore() {
            return mStore;
        }

        /**
         * @return the price string associated with the native ad.
         */
        public String getPrice() {
            return mPrice;
        }

        /**
         * @param title the title to be set.
         */
        public void setTitle(String title) {
            this.mTitle = title;
        }

        /**
         * @param text the text/body to be set.
         */
        public void setText(String text) {
            this.mText = text;
        }

        /**
         * @param mainImageUrl the main image URL to be set.
         */
        public void setMainImageUrl(String mainImageUrl) {
            this.mMainImageUrl = mainImageUrl;
        }

        /**
         * @param iconImageUrl the icon image URL to be set.
         */
        public void setIconImageUrl(String iconImageUrl) {
            this.mIconImageUrl = iconImageUrl;
        }

        /**
         * @param callToAction the call to action string to be set.
         */
        public void setCallToAction(String callToAction) {
            this.mCallToAction = callToAction;
        }

        /**
         * @param starRating the star rating value to be set.
         */
        public void setStarRating(Double starRating) {
            this.mStarRating = starRating;
        }

        /**
         * @param advertiser the advertiser string to be set.
         */
        public void setAdvertiser(String advertiser) {
            this.mAdvertiser = advertiser;
        }

        /**
         * @param store the store string to be set.
         */
        public void setStore(String store) {
            this.mStore = store;
        }

        /**
         * @param price the price string to be set.
         */
        public void setPrice(String price) {
            this.mPrice = price;
        }

        /**
         * @return whether or not to swap margins when rendering the ad.
         */
        public boolean shouldSwapMargins() {
            return this.mSwapMargins;
        }

        /**
         * @return The unified native ad.
         */
        public UnifiedNativeAd getUnifiedNativeAd() {
            return mUnifiedNativeAd;
        }

        /**
         * This method will load native ads from Google for the given ad unit ID.
         *
         * @param context  required to request a Google native ad.
         * @param adUnitId Google's AdMob Ad Unit ID.
         */
        public void loadAd(final Context context, String adUnitId,
                           Map<String, Object> localExtras) {
            AdLoader.Builder builder = new AdLoader.Builder(context, adUnitId);
            // Get the experimental swap margins extra.
            if (localExtras.containsKey(KEY_EXPERIMENTAL_EXTRA_SWAP_MARGINS)) {
                Object swapMarginExtra = localExtras.get(KEY_EXPERIMENTAL_EXTRA_SWAP_MARGINS);
                if (swapMarginExtra instanceof Boolean) {
                    mSwapMargins = (boolean) swapMarginExtra;
                }
            }

            NativeAdOptions.Builder optionsBuilder = new NativeAdOptions.Builder();

            // MoPub requires the images to be pre-cached using their APIs, so we do not want
            // Google to download the image assets.
            optionsBuilder.setReturnUrlsForImageAssets(true);

            // MoPub allows for only one image, so only request for one image.
            optionsBuilder.setRequestMultipleImages(false);

            optionsBuilder.setReturnUrlsForImageAssets(false);

            // Get the preferred image orientation from the local extras.
            if (localExtras.containsKey(KEY_EXTRA_ORIENTATION_PREFERENCE)
                    && isValidOrientationExtra(localExtras.get(KEY_EXTRA_ORIENTATION_PREFERENCE))) {
                optionsBuilder.setImageOrientation(
                        (int) localExtras.get(KEY_EXTRA_ORIENTATION_PREFERENCE));
            }

            // Get the preferred AdChoices icon placement from the local extras.
            if (localExtras.containsKey(KEY_EXTRA_AD_CHOICES_PLACEMENT)
                    && isValidAdChoicesPlacementExtra(
                    localExtras.get(KEY_EXTRA_AD_CHOICES_PLACEMENT))) {
                optionsBuilder.setAdChoicesPlacement(
                        (int) localExtras.get(KEY_EXTRA_AD_CHOICES_PLACEMENT));
            }

            NativeAdOptions adOptions = optionsBuilder.build();

            AdLoader adLoader =
                    builder.forUnifiedNativeAd(
                            new UnifiedNativeAd.OnUnifiedNativeAdLoadedListener() {
                                @Override
                                public void onUnifiedNativeAdLoaded(UnifiedNativeAd unifiedNativeAd) {
                                    if (!isValidUnifiedAd(unifiedNativeAd)) {
                                        Log.i(TAG, "The Google native unified ad is missing one or "
                                                + "more required assets, failing request.");
                                        mCustomEventNativeListener.onNativeAdFailed(
                                                NativeErrorCode.INVALID_RESPONSE);
                                        return;
                                    }

                                    mUnifiedNativeAd = unifiedNativeAd;
                                    List<com.google.android.gms.ads.formats.NativeAd.Image> images =
                                            unifiedNativeAd.getImages();
                                    List<String> imageUrls = new ArrayList<>();
                                    com.google.android.gms.ads.formats.NativeAd.Image mainImage =
                                            images.get(0);

                                    // Assuming that the URI provided is an URL.
                                    imageUrls.add(mainImage.getUri().toString());

                                    com.google.android.gms.ads.formats.NativeAd.Image iconImage =
                                            unifiedNativeAd.getIcon();
                                    // Assuming that the URI provided is an URL.
                                    imageUrls.add(iconImage.getUri().toString());
                                    preCacheImages(context, imageUrls);
                                }
                            }).withAdListener(new AdListener() {
                        @Override
                        public void onAdClicked() {
                            super.onAdClicked();
                            GooglePlayServicesNativeAd.this.notifyAdClicked();
                        }

                        @Override
                        public void onAdImpression() {
                            super.onAdImpression();
                            GooglePlayServicesNativeAd.this.notifyAdImpressed();
                        }

                        @Override
                        public void onAdFailedToLoad(int errorCode) {
                            super.onAdFailedToLoad(errorCode);
                            switch (errorCode) {
                                case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                                    mCustomEventNativeListener.onNativeAdFailed(
                                            NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
                                    break;
                                case AdRequest.ERROR_CODE_INVALID_REQUEST:
                                    mCustomEventNativeListener.onNativeAdFailed(
                                            NativeErrorCode.NETWORK_INVALID_REQUEST);
                                    break;
                                case AdRequest.ERROR_CODE_NETWORK_ERROR:
                                    mCustomEventNativeListener.onNativeAdFailed(
                                            NativeErrorCode.CONNECTION_ERROR);
                                    break;
                                case AdRequest.ERROR_CODE_NO_FILL:
                                    mCustomEventNativeListener.onNativeAdFailed(
                                            NativeErrorCode.NETWORK_NO_FILL);
                                    break;
                                default:
                                    mCustomEventNativeListener.onNativeAdFailed(
                                            NativeErrorCode.UNSPECIFIED);
                            }
                        }
                    }).withNativeAdOptions(adOptions).build();

            AdRequest.Builder requestBuilder = new AdRequest.Builder();
            requestBuilder.setRequestAgent("MoPub");

            // Publishers may append a content URL by passing it to the MoPubNative.setLocalExtras() call.
            if (localExtras.get(KEY_CONTENT_URL) != null) {
                String contentUrl = localExtras.get(KEY_CONTENT_URL).toString();
                if (!TextUtils.isEmpty(contentUrl)) {
                    requestBuilder.setContentUrl(contentUrl);
                }
            }

            // Publishers may request for test ads by passing test device IDs to the MoPubNative.setLocalExtras() call.
            if (localExtras.get(TEST_DEVICES_KEY) != null) {
                String testDeviceId = localExtras.get(TEST_DEVICES_KEY).toString();
                if (!TextUtils.isEmpty(testDeviceId)) {
                    requestBuilder.addTestDevice(testDeviceId);
                }
            }

            // Consent collected from the MoPub’s consent dialogue should not be used to set up
            // Google's personalization preference. Publishers should work with Google to be GDPR-compliant.
            forwardNpaIfSet(requestBuilder);

            AdRequest adRequest = requestBuilder.build();
            adLoader.loadAd(adRequest);
        }

        private void forwardNpaIfSet(AdRequest.Builder builder) {

            // Only forward the "npa" bundle if it is explicitly set. Otherwise, don't attach it with the ad request.
            if (GooglePlayServicesMediationSettings.getNpaBundle() != null &&
                    !GooglePlayServicesMediationSettings.getNpaBundle().isEmpty()) {
                builder.addNetworkExtrasBundle(AdMobAdapter.class, GooglePlayServicesMediationSettings.getNpaBundle());
            }
        }

        /**
         * This method will check whether or not the provided extra value can be mapped to
         * NativeAdOptions' orientation constants.
         *
         * @param extra to be checked if it is valid.
         * @return {@code true} if the extra can be mapped to one of {@link NativeAdOptions}
         * orientation constants, {@code false} otherwise.
         */
        private boolean isValidOrientationExtra(Object extra) {
            if (extra == null || !(extra instanceof Integer)) {
                return false;
            }
            Integer preference = (Integer) extra;
            return (preference == NativeAdOptions.ORIENTATION_ANY
                    || preference == NativeAdOptions.ORIENTATION_LANDSCAPE
                    || preference == NativeAdOptions.ORIENTATION_PORTRAIT);
        }

        /**
         * Checks whether or not the provided extra value can be mapped to NativeAdOptions'
         * AdChoices icon placement constants.
         *
         * @param extra to be checked if it is valid.
         * @return {@code true} if the extra can be mapped to one of {@link NativeAdOptions}
         * AdChoices icon placement constants, {@code false} otherwise.
         */
        private boolean isValidAdChoicesPlacementExtra(Object extra) {
            if (extra == null || !(extra instanceof Integer)) {
                return false;
            }
            Integer placement = (Integer) extra;
            return (placement == NativeAdOptions.ADCHOICES_TOP_LEFT
                    || placement == NativeAdOptions.ADCHOICES_TOP_RIGHT
                    || placement == NativeAdOptions.ADCHOICES_BOTTOM_LEFT
                    || placement == NativeAdOptions.ADCHOICES_BOTTOM_RIGHT);
        }

        /**
         * This method will check whether or not the given ad has all the required assets
         * (title, text, main image url, icon url and call to action) for it to be correctly
         * mapped to a {@link GooglePlayServicesNativeAd}.
         *
         * @param unifiedNativeAd to be checked if it is valid.
         * @return {@code true} if the given native ad has all the necessary assets to
         * create a {@link GooglePlayServicesNativeAd}, {@code false} otherwise.
         */

        private boolean isValidUnifiedAd(UnifiedNativeAd unifiedNativeAd) {
            return (unifiedNativeAd.getHeadline() != null && unifiedNativeAd.getBody() != null
                    && unifiedNativeAd.getImages() != null && unifiedNativeAd.getImages().size() > 0
                    && unifiedNativeAd.getImages().get(0) != null
                    && unifiedNativeAd.getIcon() != null
                    && unifiedNativeAd.getCallToAction() != null);
        }


        @Override
        public void prepare(@NonNull View view) {
            // Adding click and impression trackers is handled by the GooglePlayServicesRenderer,
            // do nothing here.
        }

        @Override
        public void clear(@NonNull View view) {
            // Called when an ad is no longer displayed to a user.

            mCustomEventNativeListener = null;
            mUnifiedNativeAd.cancelUnconfirmedClick();
        }

        @Override
        public void destroy() {
            // Called when the ad will never be displayed again.
            if (mUnifiedNativeAd != null) {
                mUnifiedNativeAd.destroy();
            }
        }

        /**
         * This method will try to cache images and send success/failure callbacks based on
         * whether or not the image caching succeeded.
         *
         * @param context   required to pre-cache images.
         * @param imageUrls the urls of images that need to be cached.
         */
        private void preCacheImages(Context context, List<String> imageUrls) {
            NativeImageHelper.preCacheImages(context, imageUrls,
                    new NativeImageHelper.ImageListener() {
                        @Override
                        public void onImagesCached() {
                            if (mUnifiedNativeAd != null) {
                                prepareUnifiedNativeAd(mUnifiedNativeAd);
                                mCustomEventNativeListener.onNativeAdLoaded(
                                        GooglePlayServicesNativeAd.this);
                            }
                        }

                        @Override
                        public void onImagesFailedToCache(NativeErrorCode errorCode) {
                            mCustomEventNativeListener.onNativeAdFailed(errorCode);
                        }
                    });
        }

        /**
         * This method will map the Google native ad loaded to this
         * {@link GooglePlayServicesNativeAd}.
         *
         * @param unifiedNativeAd that needs to be mapped to this native ad.
         */
        private void prepareUnifiedNativeAd(UnifiedNativeAd unifiedNativeAd) {
            List<com.google.android.gms.ads.formats.NativeAd.Image> images =
                    unifiedNativeAd.getImages();
            setMainImageUrl(images.get(0).getUri().toString());

            com.google.android.gms.ads.formats.NativeAd.Image icon = unifiedNativeAd.getIcon();
            setIconImageUrl(icon.getUri().toString());
            setCallToAction(unifiedNativeAd.getCallToAction());
            setTitle(unifiedNativeAd.getHeadline());

            setText(unifiedNativeAd.getBody());
            if (unifiedNativeAd.getStarRating() != null) {
                setStarRating(unifiedNativeAd.getStarRating());
            }
            // Add store asset if available.
            if (unifiedNativeAd.getStore() != null) {
                setStore(unifiedNativeAd.getStore());
            }
            // Add price asset if available.
            if (unifiedNativeAd.getPrice() != null) {
                setPrice(unifiedNativeAd.getPrice());
            }
        }
    }

    public static final class GooglePlayServicesMediationSettings implements MediationSettings {
        private static Bundle npaBundle;

        public GooglePlayServicesMediationSettings() {
        }

        public GooglePlayServicesMediationSettings(Bundle bundle) {
            npaBundle = bundle;
        }

        public void setNpaBundle(Bundle bundle) {
            npaBundle = bundle;
        }

        /* The MoPub Android SDK queries MediationSettings from the rewarded video code
        (MoPubRewardedVideoManager.getGlobalMediationSettings). That API might not always be
        available to publishers importing the modularized SDK(s) based on select ad formats.
        This is a workaround to statically get the "npa" Bundle passed to us via the constructor. */
        private static Bundle getNpaBundle() {
            return npaBundle;
        }
    }
}

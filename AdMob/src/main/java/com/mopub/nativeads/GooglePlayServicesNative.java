package com.mopub.nativeads;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.GooglePlayServicesAdapterConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.mobileads.GooglePlayServicesAdapterConfiguration.forwardNpaIfSet;

/**
 * The {@link GooglePlayServicesNative} class is used to load native Google mobile ads.
 */
public class GooglePlayServicesNative extends CustomEventNative {
    /**
     * Key to obtain AdMob ad unit ID from the extras provided by MoPub.
     */
    public static final String KEY_EXTRA_AD_UNIT_ID = "adunit";

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
     * Key to set and obtain the flag whether the application's content is child-directed.
     */
    public static final String TAG_FOR_CHILD_DIRECTED_KEY = "tagForChildDirectedTreatment";

    /**
     * Key to set and obtain the flag to mark ad requests to Google to receive treatment for
     * users in the European Economic Area (EEA) under the age of consent.
     */
    public static final String TAG_FOR_UNDER_AGE_OF_CONSENT_KEY = "tagForUnderAgeOfConsent";

    /**
     * Key to set and obtain the test device ID String to be passed with AdMob's ad request.
     */
    public static final String TEST_DEVICES_KEY = "testDevices";

    /**
     * String to store the simple class name for this adapter.
     */
    private static final String ADAPTER_NAME = GooglePlayServicesNative.class.getSimpleName();

    /**
     * Flag to determine whether or not the adapter has been initialized.
     */
    private static final AtomicBoolean sIsInitialized = new AtomicBoolean(false);

    /**
     * String to store the AdMob ad unit ID.
     */
    private static String mAdUnitId;

    @NonNull
    private final GooglePlayServicesAdapterConfiguration mGooglePlayServicesAdapterConfiguration;

    public GooglePlayServicesNative() {
        mGooglePlayServicesAdapterConfiguration = new GooglePlayServicesAdapterConfiguration();
    }

    @Override
    protected void loadNativeAd(@NonNull final Context context,
                                @NonNull final CustomEventNativeListener customEventNativeListener,
                                @NonNull Map<String, Object> localExtras,
                                @NonNull Map<String, String> serverExtras) {

        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(customEventNativeListener);
        Preconditions.checkNotNull(localExtras);
        Preconditions.checkNotNull(context);

        if (!sIsInitialized.getAndSet(true)) {
            MobileAds.initialize(context);
        }

        mAdUnitId = serverExtras.get(KEY_EXTRA_AD_UNIT_ID);
        if (TextUtils.isEmpty(mAdUnitId)) {
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_NO_FILL);

            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    NativeErrorCode.NETWORK_NO_FILL.getIntCode(),
                    NativeErrorCode.NETWORK_NO_FILL);
            return;
        }

        GooglePlayServicesNativeAd nativeAd = new GooglePlayServicesNativeAd(customEventNativeListener);
        nativeAd.loadAd(context, mAdUnitId, localExtras);

        mGooglePlayServicesAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
    }

    /**
     * The {@link GooglePlayServicesNativeAd} class is used to load and map Google native
     * ads to MoPub native ads.
     */
    public static class GooglePlayServicesNativeAd extends BaseNativeAd {

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
         * A Google native ad.
         */
        private com.google.android.gms.ads.nativead.NativeAd mNativeAd;

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
         * @return The native ad.
         */
        public NativeAd getNativeAd() {
            return mNativeAd;
        }

        /**
         * This method will load native ads from Google for the given ad unit ID.
         *
         * @param context  required to request a Google native ad.
         * @param adUnitId Google's AdMob Ad Unit ID.
         */
        public void loadAd(final Context context, String adUnitId,
                           Map<String, Object> localExtras) {
            final AdLoader.Builder builder = new AdLoader.Builder(context, adUnitId);
            // Get the experimental swap margins extra.
            if (localExtras.containsKey(KEY_EXPERIMENTAL_EXTRA_SWAP_MARGINS)) {
                Object swapMarginExtra = localExtras.get(KEY_EXPERIMENTAL_EXTRA_SWAP_MARGINS);
                if (swapMarginExtra instanceof Boolean) {
                    mSwapMargins = (boolean) swapMarginExtra;
                }
            }

            final NativeAdOptions.Builder optionsBuilder = new NativeAdOptions.Builder();

            // MoPub allows for only one image, so only request for one image.
            optionsBuilder.setRequestMultipleImages(false);

            // Get the preferred image orientation from the local extras.
            if (localExtras.containsKey(KEY_EXTRA_ORIENTATION_PREFERENCE)
                    && isValidOrientationExtra(localExtras.get(KEY_EXTRA_ORIENTATION_PREFERENCE))) {
                optionsBuilder.setMediaAspectRatio(
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
                    builder.forNativeAd(
                            new NativeAd.OnNativeAdLoadedListener() {
                                @Override
                                public void onNativeAdLoaded(NativeAd nativeAd) {
                                    if (!isValidNativeAd(nativeAd)) {
                                        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME,
                                                "The Google native ad is missing one or more " +
                                                        "required assets, failing request.");

                                        mCustomEventNativeListener.onNativeAdFailed(
                                                NativeErrorCode.NETWORK_NO_FILL);

                                        MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                                                NativeErrorCode.NETWORK_NO_FILL.getIntCode(),
                                                NativeErrorCode.NETWORK_NO_FILL);
                                        return;
                                    }

                                    mNativeAd = nativeAd;
                                    List<com.google.android.gms.ads.nativead.NativeAd.Image> images =
                                            nativeAd.getImages();
                                    List<String> imageUrls = new ArrayList<>();
                                    com.google.android.gms.ads.nativead.NativeAd.Image mainImage =
                                            images.get(0);

                                    // Assuming that the URI provided is an URL.
                                    imageUrls.add(mainImage.getUri().toString());

                                    if (nativeAd.getIcon() != null) {
                                        com.google.android.gms.ads.nativead.NativeAd.Image iconImage =
                                                nativeAd.getIcon();
                                        // Assuming that the URI provided is an URL.
                                        imageUrls.add(iconImage.getUri().toString());
                                    }
                                    preCacheImages(context, imageUrls);
                                }
                            }).withAdListener(new AdListener() {
                        @Override
                        public void onAdClicked() {
                            super.onAdClicked();
                            GooglePlayServicesNativeAd.this.notifyAdClicked();

                            MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
                        }

                        @Override
                        public void onAdImpression() {
                            super.onAdImpression();
                            GooglePlayServicesNativeAd.this.notifyAdImpressed();

                            MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
                        }

                        @Override
                        public void onAdFailedToLoad(LoadAdError loadAdError) {
                            super.onAdFailedToLoad(loadAdError);

                            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                                    NativeErrorCode.NETWORK_NO_FILL.getIntCode(),
                                    NativeErrorCode.NETWORK_NO_FILL);
                            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Failed to " +
                                    "load Google native ad with message: " + loadAdError.getMessage() +
                                    ". Caused by: " + loadAdError.getCause());

                            switch (loadAdError.getCode()) {
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
            final String contentUrl = (String) localExtras.get(KEY_CONTENT_URL);

            if (!TextUtils.isEmpty(contentUrl)) {
                requestBuilder.setContentUrl(contentUrl);
            }

            forwardNpaIfSet(requestBuilder);

            final RequestConfiguration.Builder requestConfigurationBuilder = new RequestConfiguration.Builder();

            // Publishers may request for test ads by passing test device IDs to the MoPubView.setLocalExtras() call.
            final String testDeviceId = (String) localExtras.get(TEST_DEVICES_KEY);

            if (!TextUtils.isEmpty(testDeviceId)) {
                requestConfigurationBuilder.setTestDeviceIds(Collections.singletonList(testDeviceId));
            }

            // Publishers may want to indicate that their content is child-directed and forward this
            // information to Google.
            final Boolean childDirected = (Boolean) localExtras.get(TAG_FOR_CHILD_DIRECTED_KEY);

            if (childDirected != null) {
                if (childDirected) {
                    requestConfigurationBuilder.setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE);
                } else {
                    requestConfigurationBuilder.setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE);
                }
            } else {
                requestConfigurationBuilder.setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED);
            }

            // Publishers may want to mark their requests to receive treatment for users in the
            // European Economic Area (EEA) under the age of consent.
            final Boolean underAgeOfConsent = (Boolean) localExtras.get(TAG_FOR_UNDER_AGE_OF_CONSENT_KEY);

            if (underAgeOfConsent != null) {
                if (underAgeOfConsent) {
                    requestConfigurationBuilder.setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE);
                } else {
                    requestConfigurationBuilder.setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE);
                }
            } else {
                requestConfigurationBuilder.setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED);
            }

            final RequestConfiguration requestConfiguration = requestConfigurationBuilder.build();
            MobileAds.setRequestConfiguration(requestConfiguration);

            final AdRequest adRequest = requestBuilder.build();
            adLoader.loadAd(adRequest);

            MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
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
            if (!(extra instanceof Integer)) {
                return false;
            }
            Integer preference = (Integer) extra;
            return (preference == NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_ANY
                    || preference == NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE
                    || preference == NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT);
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
            if (!(extra instanceof Integer)) {
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
         * (title, text, main image url and call to action) for it to be correctly
         * mapped to a {@link GooglePlayServicesNativeAd}.
         *
         * @param nativeAd to be checked if it is valid.
         * @return {@code true} if the given native ad has all the necessary assets to
         * create a {@link GooglePlayServicesNativeAd}, {@code false} otherwise.
         */

        private boolean isValidNativeAd(NativeAd nativeAd) {
            return (nativeAd.getHeadline() != null && nativeAd.getBody() != null
                    && nativeAd.getImages() != null && nativeAd.getImages().size() > 0
                    && nativeAd.getImages().get(0) != null
                    && nativeAd.getCallToAction() != null);
        }

        @Override
        public void prepare(@NonNull View view) {
            // Adding click and impression trackers is handled by the GooglePlayServicesRenderer,
            // do nothing here.
        }

        @Override
        public void clear(@NonNull View view) {
            Preconditions.checkNotNull(view);

            // Called when an ad is no longer displayed to a user.
            mCustomEventNativeListener = null;
            mNativeAd.cancelUnconfirmedClick();
        }

        @Override
        public void destroy() {
            // Called when the ad will never be displayed again.
            if (mNativeAd != null) {
                mNativeAd.destroy();
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
                            if (mNativeAd != null) {
                                prepareNativeAd(mNativeAd);
                                mCustomEventNativeListener.onNativeAdLoaded(
                                        GooglePlayServicesNativeAd.this);

                                MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
                            }
                        }

                        @Override
                        public void onImagesFailedToCache(NativeErrorCode errorCode) {
                            mCustomEventNativeListener.onNativeAdFailed(errorCode);

                            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                                    errorCode.getIntCode(),
                                    errorCode);
                        }
                    });
        }

        /**
         * This method will map the Google native ad loaded to this
         * {@link GooglePlayServicesNativeAd}.
         *
         * @param nativeAd that needs to be mapped to this native ad.
         */
        private void prepareNativeAd(NativeAd nativeAd) {
            List<com.google.android.gms.ads.nativead.NativeAd.Image> images =
                    nativeAd.getImages();
            setMainImageUrl(images.get(0).getUri().toString());

            if (nativeAd.getIcon() != null) {
                com.google.android.gms.ads.nativead.NativeAd.Image icon = nativeAd.getIcon();
                setIconImageUrl(icon.getUri().toString());
            }
            setCallToAction(nativeAd.getCallToAction());
            setTitle(nativeAd.getHeadline());

            setText(nativeAd.getBody());
            if (nativeAd.getStarRating() != null) {
                setStarRating(nativeAd.getStarRating());
            }
            // Add store asset if available.
            if (nativeAd.getStore() != null) {
                setStore(nativeAd.getStore());
            }
            // Add price asset if available.
            if (nativeAd.getPrice() != null) {
                setPrice(nativeAd.getPrice());
            }
        }
    }

    private static String getAdNetworkId() {
        return mAdUnitId;
    }
}

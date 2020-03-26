// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/
// 2020.3.3- add class HuaweiNative
// Huawei Technologies Co., Ltd.

package com.mopub.nativeads;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import com.huawei.hms.ads.AdListener;
import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.HwAds;
import com.huawei.hms.ads.Image;
import com.huawei.hms.ads.RequestOptions;
import com.huawei.hms.ads.nativead.NativeAdConfiguration;
import com.huawei.hms.ads.nativead.NativeAdLoader;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.HuaweiAdapterConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.huawei.hms.ads.TagForChild.TAG_FOR_CHILD_PROTECTION_FALSE;
import static com.huawei.hms.ads.TagForChild.TAG_FOR_CHILD_PROTECTION_TRUE;
import static com.huawei.hms.ads.TagForChild.TAG_FOR_CHILD_PROTECTION_UNSPECIFIED;
import static com.huawei.hms.ads.UnderAge.PROMISE_FALSE;
import static com.huawei.hms.ads.UnderAge.PROMISE_TRUE;
import static com.huawei.hms.ads.UnderAge.PROMISE_UNSPECIFIED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;

/**
 * The {@link HuaweiNative} class is used to load native Huawei mobile ads.
 */
public class HuaweiNative extends CustomEventNative {

    /**
     * Key to obtain Huawei application ID from the server extras provided by Huawei ads.
     */
    public static final String KEY_EXTRA_APPLICATION_ID = "appid";

    /**
     * Key to obtain Huawei ad unit ID from the extras provided by Huawei.
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
     * Key to set and obtain the content URL to be passed with Huawei's ad request.
     */
    public static final String KEY_CONTENT_URL = "contentUrl";

    /**
     * Key to set and obtain the flag whether the application's content is child-directed.
     */
    public static final String TAG_FOR_CHILD_DIRECTED_KEY = "tagForChildDirectedTreatment";

    /**
     * Key to set and obtain the flag to mark ad requests to Huawei to receive treatment for
     * users in the European Economic Area (EEA) under the age of consent.
     */
    public static final String TAG_FOR_UNDER_AGE_OF_CONSENT_KEY = "tagForUnderAgeOfConsent";

    /**
     * String to store the simple class name for this adapter.
     */
    private static final String ADAPTER_NAME = HuaweiNative.class.getSimpleName();

    /**
     * Flag to determine whether or not the adapter has been initialized.
     */
    private static AtomicBoolean sIsInitialized = new AtomicBoolean(false);

    /**
     * String to store the Huawei ad unit ID.
     */
    private static String mAdUnitId;

    @NonNull
    private HuaweiAdapterConfiguration mHuaweiAdapterConfiguration;

    public HuaweiNative() {
        mHuaweiAdapterConfiguration = new HuaweiAdapterConfiguration();
    }

    @Override
    protected void loadNativeAd(@NonNull final Context context,
                                @NonNull final CustomEventNativeListener customEventNativeListener,
                                @NonNull Map<String, Object> localExtras,
                                @NonNull Map<String, String> serverExtras) {

        if (!sIsInitialized.getAndSet(true)) {
            if (serverExtras.containsKey(KEY_EXTRA_APPLICATION_ID)
                    && !TextUtils.isEmpty(serverExtras.get(KEY_EXTRA_APPLICATION_ID))) {
                HwAds.init(context, serverExtras.get(KEY_EXTRA_APPLICATION_ID));
            } else {
                HwAds.init(context);
            }
        }

        mAdUnitId = serverExtras.get(KEY_EXTRA_AD_UNIT_ID);
        if (TextUtils.isEmpty(mAdUnitId)) {
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_NO_FILL);

            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    NativeErrorCode.NETWORK_NO_FILL.getIntCode(),
                    NativeErrorCode.NETWORK_NO_FILL);
            return;
        }

        HuaweiNativeAd nativeAd = new HuaweiNativeAd(customEventNativeListener);
        nativeAd.loadAd(context, mAdUnitId, localExtras);

        mHuaweiAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
    }

    /**
     * The {@link HuaweiNativeAd} class is used to load and map Huawei native
     * ads to Mopub native ads.
     */
    static class HuaweiNativeAd extends BaseNativeAd {

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
         * Flag to determine whether or not to swap margins from actual ad view to Huawei native ad
         * view.
         */
        private boolean mSwapMargins;

        /**
         * A custom event native listener used to forward Huawei Mobile Ads SDK events to Huawei.
         */
        private CustomEventNativeListener mCustomEventNativeListener;

        /**
         * A Huawei ad.
         */
        private com.huawei.hms.ads.nativead.NativeAd mNativeAd;

        public HuaweiNativeAd(
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
        public com.huawei.hms.ads.nativead.NativeAd getNativeAd() {
            return mNativeAd;
        }

        /**
         * This method will load native ads from Huawei for the given ad unit ID.
         *
         * @param context  required to request a Huawei native ad.
         * @param adUnitId Huawei's  Ad Unit ID.
         */
        public void loadAd(final Context context, String adUnitId,
                           Map<String, Object> localExtras) {
            final NativeAdLoader.Builder builder = new NativeAdLoader.Builder(context, adUnitId);
            // Get the experimental swap margins extra.
            if (localExtras.containsKey(KEY_EXPERIMENTAL_EXTRA_SWAP_MARGINS)) {
                Object swapMarginExtra = localExtras.get(KEY_EXPERIMENTAL_EXTRA_SWAP_MARGINS);
                if (swapMarginExtra instanceof Boolean) {
                    mSwapMargins = (boolean) swapMarginExtra;
                }
            }

            final NativeAdConfiguration.Builder optionsBuilder = new NativeAdConfiguration.Builder();

            // MoPub requires the images to be pre-cached using their APIs, so we do not want
            // Huawei to download the image assets.
            optionsBuilder.setReturnUrlsForImages(true);

            // Huawei ads allows for only one image, so only request for one image.
            optionsBuilder.setRequestMultiImages(false);

            optionsBuilder.setReturnUrlsForImages(false);

            // Get the preferred image orientation from the local extras.
            if (localExtras.containsKey(KEY_EXTRA_ORIENTATION_PREFERENCE)
                    && isValidOrientationExtra(localExtras.get(KEY_EXTRA_ORIENTATION_PREFERENCE))) {
                optionsBuilder.setMediaDirection(
                        (int) localExtras.get(KEY_EXTRA_ORIENTATION_PREFERENCE));
            }

            // Get the preferred AdChoices icon placement from the local extras.
            if (localExtras.containsKey(KEY_EXTRA_AD_CHOICES_PLACEMENT)
                    && isValidAdChoicesPlacementExtra(
                    localExtras.get(KEY_EXTRA_AD_CHOICES_PLACEMENT))) {
                optionsBuilder.setChoicesPosition(
                        (int) localExtras.get(KEY_EXTRA_AD_CHOICES_PLACEMENT));
            }

            NativeAdConfiguration adOptions = optionsBuilder.build();

            NativeAdLoader adLoader =
                    builder.setNativeAdLoadedListener(
                            new com.huawei.hms.ads.nativead.NativeAd.NativeAdLoadedListener() {
                                @Override
                                public void onNativeAdLoaded(com.huawei.hms.ads.nativead.NativeAd nativeAd) {
                                    if (!isValidNativeAd(nativeAd)) {
                                        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME,
                                                "The Huawei native ad is missing one or " +
                                                        "more required assets, failing request.");

                                        mCustomEventNativeListener.onNativeAdFailed(
                                                NativeErrorCode.NETWORK_NO_FILL);

                                        MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                                                NativeErrorCode.NETWORK_NO_FILL.getIntCode(),
                                                NativeErrorCode.NETWORK_NO_FILL);
                                        return;
                                    }

                                    mNativeAd = nativeAd;
                                    List<Image> images =
                                            nativeAd.getImages();
                                    List<String> imageUrls = new ArrayList<>();
                                    Image mainImage =
                                            images.get(0);

                                    // Assuming that the URI provided is an URL.
                                    imageUrls.add(mainImage.getUri().toString());

                                    if(nativeAd.getIcon() != null){
                                        Image iconImage =
                                                nativeAd.getIcon();
                                        // Assuming that the URI provided is an URL.
                                        imageUrls.add(iconImage.getUri().toString());
                                    }
                                    preCacheImages(context, imageUrls);
                                }
                            }).setAdListener(new AdListener() {
                        @Override
                        public void onAdClicked() {
                            super.onAdClicked();
                            HuaweiNativeAd.this.notifyAdClicked();

                            MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
                        }

                        @Override
                        public void onAdImpression() {
                            super.onAdImpression();
                            HuaweiNativeAd.this.notifyAdImpressed();

                            MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
                        }

                        @Override
                        public void onAdFailed(int errorCode) {
                            super.onAdFailed(errorCode);
                            switch (errorCode) {
                                case AdParam.ErrorCode.INNER:
                                    mCustomEventNativeListener.onNativeAdFailed(
                                            NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
                                    break;
                                case AdParam.ErrorCode.INVALID_REQUEST:
                                    mCustomEventNativeListener.onNativeAdFailed(
                                            NativeErrorCode.NETWORK_INVALID_REQUEST);
                                    break;
                                case AdParam.ErrorCode.NETWORK_ERROR:
                                    mCustomEventNativeListener.onNativeAdFailed(
                                            NativeErrorCode.CONNECTION_ERROR);
                                    break;
                                case AdParam.ErrorCode.NO_AD:
                                    mCustomEventNativeListener.onNativeAdFailed(
                                            NativeErrorCode.NETWORK_NO_FILL);
                                    break;
                                default:
                                    mCustomEventNativeListener.onNativeAdFailed(
                                            NativeErrorCode.UNSPECIFIED);
                            }
                        }
                    }
                    ).setNativeAdOptions(adOptions).build();

            AdParam.Builder requestBuilder = new AdParam.Builder();
            requestBuilder.setRequestOrigin("MoPub");

            // Publishers may append a content URL by passing it to the MoPubNative.setLocalExtras() call.
            final String contentUrl = (String) localExtras.get(KEY_CONTENT_URL);

            if (!TextUtils.isEmpty(contentUrl)) {
                requestBuilder.setTargetingContentUrl(contentUrl);
            }

            final RequestOptions.Builder requestConfigurationBuilder = new RequestOptions.Builder();

            // Publishers may want to indicate that their content is child-directed and forward this
            // information to Huawei.
            final Boolean childDirected = (Boolean) localExtras.get(TAG_FOR_CHILD_DIRECTED_KEY);

            if (childDirected != null) {
                if (childDirected) {
                    requestConfigurationBuilder.setTagForChildProtection(TAG_FOR_CHILD_PROTECTION_TRUE);
                } else {
                    requestConfigurationBuilder.setTagForChildProtection(TAG_FOR_CHILD_PROTECTION_FALSE);
                }
            } else {
                requestConfigurationBuilder.setTagForChildProtection(TAG_FOR_CHILD_PROTECTION_UNSPECIFIED );
            }

            // Publishers may want to mark their requests to receive treatment for users in the
            // European Economic Area (EEA) under the age of consent.
            final Boolean underAgeOfConsent = (Boolean) localExtras.get(TAG_FOR_UNDER_AGE_OF_CONSENT_KEY);

            if (underAgeOfConsent != null) {
                if (underAgeOfConsent) {
                    requestConfigurationBuilder.setTagForUnderAgeOfPromise(PROMISE_TRUE);
                } else {
                    requestConfigurationBuilder.setTagForUnderAgeOfPromise(PROMISE_FALSE);
                }
            } else {
                requestConfigurationBuilder.setTagForUnderAgeOfPromise(PROMISE_UNSPECIFIED);
            }

            final RequestOptions requestConfiguration = requestConfigurationBuilder.build();
            HwAds.setRequestOptions(requestConfiguration);

            final AdParam adRequest = requestBuilder.build();
            adLoader.loadAd(adRequest);

            MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
        }

        /**
         * This method will check whether or not the provided extra value can be mapped to
         * NativeAdOptions' orientation constants.
         *
         * @param extra to be checked if it is valid.
         * @return {@code true} if the extra can be mapped to one of {@link NativeAdConfiguration}
         * orientation constants, {@code false} otherwise.
         */
        private boolean isValidOrientationExtra(Object extra) {
            if (extra == null || !(extra instanceof Integer)) {
                return false;
            }
            Integer preference = (Integer) extra;
            return (preference == NativeAdConfiguration.Direction.ANY
                    || preference == NativeAdConfiguration.Direction.LANDSCAPE
                    || preference == NativeAdConfiguration.Direction.PORTRAIT);
        }

        /**
         * Checks whether or not the provided extra value can be mapped to NativeAdOptions'
         * AdChoices icon placement constants.
         *
         * @param extra to be checked if it is valid.
         * @return {@code true} if the extra can be mapped to one of {@link NativeAdConfiguration}
         * AdChoices icon placement constants, {@code false} otherwise.
         */
        private boolean isValidAdChoicesPlacementExtra(Object extra) {
            if (extra == null || !(extra instanceof Integer)) {
                return false;
            }
            Integer placement = (Integer) extra;
            return (placement == NativeAdConfiguration.ChoicesPosition.TOP_LEFT
                    || placement == NativeAdConfiguration.ChoicesPosition.TOP_RIGHT
                    || placement == NativeAdConfiguration.ChoicesPosition.BOTTOM_LEFT
                    || placement == NativeAdConfiguration.ChoicesPosition.BOTTOM_RIGHT);
        }

        /**
         * This method will check whether or not the given ad has all the required assets
         * (title, text, main image url, icon url and call to action) for it to be correctly
         * mapped to a {@link HuaweiNativeAd}.
         *
         * @param nativeAd to be checked if it is valid.
         * @return {@code true} if the given native ad has all the necessary assets to
         * create a {@link HuaweiNativeAd}, {@code false} otherwise.
         */

        private boolean isValidNativeAd(com.huawei.hms.ads.nativead.NativeAd nativeAd) {
            return (nativeAd.getTitle() != null
                    && nativeAd.getImages() != null && nativeAd.getImages().size() > 0
                    && nativeAd.getImages().get(0) != null
                    && nativeAd.getCallToAction() != null);
        }

        @Override
        public void prepare(@NonNull View view) {
            // Adding click and impression trackers is handled by the HuaweiAdRenderer,
            // do nothing here.
        }

        @Override
        public void clear(@NonNull View view) {
            // Called when an ad is no longer displayed to a user.

            mCustomEventNativeListener = null;
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
                                        HuaweiNativeAd.this);

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
         * This method will map the Huawei native ad loaded to this
         * {@link HuaweiNativeAd}.
         *
         * @param nativeAd that needs to be mapped to this native ad.
         */
        private void prepareNativeAd(com.huawei.hms.ads.nativead.NativeAd nativeAd) {
            List<Image> images =
                    nativeAd.getImages();
            setMainImageUrl(images.get(0).getUri().toString());

            Image icon = nativeAd.getIcon();
            if (icon != null) {
                setIconImageUrl(icon.getUri().toString());
            }
            setCallToAction(nativeAd.getCallToAction());
            setTitle(nativeAd.getTitle());

            setText(nativeAd.getDescription());
            if (nativeAd.getRating() != null) {
                setStarRating(nativeAd.getRating());
            }
            // Add store asset if available.
            if (nativeAd.getMarket() != null) {
                setStore(nativeAd.getMarket());
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

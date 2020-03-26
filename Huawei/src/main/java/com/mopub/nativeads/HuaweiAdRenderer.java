// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/
// 2020.3.3- add huawei ad renderer
// Huawei Technologies Co., Ltd.


package com.mopub.nativeads;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.huawei.hms.ads.ChoicesView;
import com.huawei.hms.ads.nativead.MediaView;
import com.huawei.hms.ads.nativead.NativeView;
import com.mopub.common.logging.MoPubLog;
import com.mopub.nativeads.HuaweiNative.HuaweiNativeAd;

import java.util.Map;
import java.util.WeakHashMap;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;

/**
 * The {@link HuaweiAdRenderer} class is used to render
 * HuaweiNativeAd.
 */
public class HuaweiAdRenderer implements MoPubAdRenderer<HuaweiNativeAd> {

    /**
     * Key to set and get star rating text view as an extra in the view binder.
     */
    public static final String VIEW_BINDER_KEY_STAR_RATING = "key_star_rating";

    /**
     * Key to set and get advertiser text view as an extra in the view binder.
     */
    public static final String VIEW_BINDER_KEY_ADVERTISER = "key_advertiser";

    /**
     * Key to set and get store text view as an extra in the view binder.
     */
    public static final String VIEW_BINDER_KEY_STORE = "key_store";

    /**
     * Key to set and get price text view as an extra in the view binder.
     */
    public static final String VIEW_BINDER_KEY_PRICE = "key_price";

    /**
     * Key to set and get the AdChoices icon view as an extra in the view binder.
     */
    public static final String VIEW_BINDER_KEY_AD_CHOICES_ICON_CONTAINER = "ad_choices_container";

    /**
     * ID for the frame layout that wraps the Huawei native ad view.
     */
    private static final int ID_WRAPPING_FRAME = 1001;

    /**
     * ID for the Huawei native ad view.
     */
    private static final int ID_HUAWEI_NATIVE_VIEW = 1002;

    /**
     * A view binder containing the layout resource and views to be rendered by the renderer.
     */
    private final MediaViewBinder mViewBinder;

    /**
     * A weak hash map used to keep track of view holder so that the views can be properly recycled.
     */
    private final WeakHashMap<View, HuaweiStaticNativeViewHolder> mViewHolderMap;

    /**
     * String to store the simple class name for this adapter.
     */
    private static final String ADAPTER_NAME = HuaweiAdRenderer.class.getSimpleName();

    public HuaweiAdRenderer(MediaViewBinder viewBinder) {
        this.mViewBinder = viewBinder;
        this.mViewHolderMap = new WeakHashMap<>();
    }

    @NonNull
    @Override
    public View createAdView(@NonNull Context context, @Nullable ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(mViewBinder.layoutId, parent, false);
        // Create a frame layout and add the inflated view as a child. This will allow us to add
        // the Huawei native ad view into the view hierarchy at render time.
        FrameLayout wrappingView = new FrameLayout(context);
        wrappingView.setId(ID_WRAPPING_FRAME);
        wrappingView.addView(view);

        MoPubLog.log(CUSTOM, ADAPTER_NAME, "Ad view created.");
        return wrappingView;
    }

    @Override
    public void renderAdView(@NonNull View view,
                             @NonNull HuaweiNativeAd nativeAd) {
        HuaweiStaticNativeViewHolder viewHolder = mViewHolderMap.get(view);
        if (viewHolder == null) {
            viewHolder = HuaweiStaticNativeViewHolder.fromViewBinder(view, mViewBinder);
            mViewHolderMap.put(view, viewHolder);
        }

        NativeView nativeAdView = new NativeView(view.getContext());

        updateNativeedAdview(nativeAd, viewHolder, nativeAdView);
        insertHuaweiNativeAdView(nativeAdView, view, nativeAd.shouldSwapMargins());
    }

    /**
     * This method will add the given Huawei ad view into the view hierarchy of the given
     * MoPub native ad view.
     *
     * @param nativeView Huawei's ad view to be added as a parent to the MoPub's
     *                            view.
     * @param moPubNativeAdView   MoPub's native ad view created by this renderer.
     * @param swapMargins         {@code true} if the margins need to be swapped, {@code false}
     *                            otherwise.
     */
    private static void insertHuaweiNativeAdView(NativeView nativeView,
                                                 View moPubNativeAdView,
                                                 boolean swapMargins) {

        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);

        if (moPubNativeAdView instanceof FrameLayout
                && moPubNativeAdView.getId() == ID_WRAPPING_FRAME) {

            nativeView.setId(ID_HUAWEI_NATIVE_VIEW);
            FrameLayout outerFrame = (FrameLayout) moPubNativeAdView;
            View actualView = outerFrame.getChildAt(0);

            if (swapMargins) {
                // Huawei ad view renders the AdChoices icon in one of the four corners of
                // its view. If a margin is specified on the actual ad view, the AdChoices view
                // might be rendered outside the actual ad view. Moving the margins from the
                // actual ad view to Huawei native ad view will make sure that the AdChoices icon
                // is being rendered within the bounds of the actual ad view.
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                FrameLayout.LayoutParams actualViewParams =
                        (FrameLayout.LayoutParams) actualView.getLayoutParams();
                layoutParams.setMargins(actualViewParams.leftMargin,
                        actualViewParams.topMargin,
                        actualViewParams.rightMargin,
                        actualViewParams.bottomMargin);
                nativeView.setLayoutParams(layoutParams);
                actualViewParams.setMargins(0, 0, 0, 0);
            } else {
                nativeView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }

            outerFrame.removeView(actualView);
            nativeView.addView(actualView);
            outerFrame.addView(nativeView);
        } else {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Couldn't add huawei native ad view. Wrapping view not found.");
        }
    }

    /**
     * This method will render the given native ad view using the native ad and set the views to
     * Huawei's native ad view.
     *
     * @param staticNativeAd         a static native ad object containing the required assets to
     *                               set to the native ad view.
     * @param staticNativeViewHolder a static native view holder object containing the mapped
     *                               views from the view binder.
     * @param nativeView          the Huawei ad view in the view hierarchy.
     */
    private void updateNativeedAdview(HuaweiNativeAd staticNativeAd,
                                      HuaweiStaticNativeViewHolder staticNativeViewHolder,
                                      NativeView nativeView) {
        NativeRendererHelper.addTextView(
                staticNativeViewHolder.mTitleView, staticNativeAd.getTitle());
        nativeView.setTitleView(staticNativeViewHolder.mTitleView);
        NativeRendererHelper.addTextView(
                staticNativeViewHolder.mTextView, staticNativeAd.getText());
        nativeView.setDescriptionView(staticNativeViewHolder.mTextView);
        if (staticNativeViewHolder.mMediaView != null) {
            MediaView mediaview = new MediaView(nativeView.getContext());
            staticNativeViewHolder.mMediaView.removeAllViews();
            staticNativeViewHolder.mMediaView.addView(mediaview);
            nativeView.setMediaView(mediaview);

        }
        NativeRendererHelper.addTextView(staticNativeViewHolder.mCallToActionView,
                staticNativeAd.getCallToAction());
        nativeView.setCallToActionView(staticNativeViewHolder.mCallToActionView);
        String imageUrl = staticNativeAd.getIconImageUrl() != null ? staticNativeAd.getIconImageUrl() :
                staticNativeAd.getMainImageUrl();
        NativeImageHelper.loadImageView(imageUrl, staticNativeViewHolder.mIconImageView);
        nativeView.setImageView(staticNativeViewHolder.mIconImageView);
        if (staticNativeAd.getAdvertiser() != null) {
            NativeRendererHelper.addTextView(
                    staticNativeViewHolder.mAdvertiserTextView, staticNativeAd.getAdvertiser());
            nativeView.setAdSourceView(staticNativeViewHolder.mAdvertiserTextView);
        }
        // Add the AdChoices icon to the container if one is provided by the publisher.
        if (staticNativeViewHolder.mAdChoicesIconContainer != null) {
            ChoicesView adChoicesView = new ChoicesView(nativeView.getContext());
            staticNativeViewHolder.mAdChoicesIconContainer.removeAllViews();
            staticNativeViewHolder.mAdChoicesIconContainer.addView(adChoicesView);
            nativeView.setChoicesView(adChoicesView);
        }

        // Set the privacy information icon to null as the Huawei Mobile Ads SDK automatically
        // renders the AdChoices icon.
        NativeRendererHelper.addPrivacyInformationIcon(
                staticNativeViewHolder.mPrivacyInformationIconImageView, null, null);

        nativeView.setNativeAd(staticNativeAd.getNativeAd());
    }

    @Override
    public boolean supports(@NonNull BaseNativeAd nativeAd) {
        return nativeAd instanceof HuaweiNativeAd;
    }

    private static class HuaweiStaticNativeViewHolder {
        @Nullable
        View mMainView;
        @Nullable
        TextView mTitleView;
        @Nullable
        TextView mTextView;
        @Nullable
        TextView mCallToActionView;
        @Nullable
        ImageView mIconImageView;
        @Nullable
        ImageView mPrivacyInformationIconImageView;
        @Nullable
        TextView mStarRatingTextView;
        @Nullable
        TextView mAdvertiserTextView;
        @Nullable
        TextView mStoreTextView;
        @Nullable
        TextView mPriceTextView;
        @Nullable
        FrameLayout mAdChoicesIconContainer;
        @Nullable
        MediaLayout mMediaView;

        private static final HuaweiStaticNativeViewHolder EMPTY_VIEW_HOLDER =
                new HuaweiStaticNativeViewHolder();

        @NonNull
        public static HuaweiStaticNativeViewHolder fromViewBinder(@NonNull View view,
                                                                  @NonNull MediaViewBinder
                                                                          viewBinder) {
            final HuaweiStaticNativeViewHolder viewHolder = new HuaweiStaticNativeViewHolder();
            viewHolder.mMainView = view;
            try {
                viewHolder.mTitleView =  view.findViewById(viewBinder.titleId);
                viewHolder.mTextView =  view.findViewById(viewBinder.textId);
                viewHolder.mCallToActionView =
                        view.findViewById(viewBinder.callToActionId);

                viewHolder.mIconImageView =
                         view.findViewById(viewBinder.iconImageId);
                viewHolder.mPrivacyInformationIconImageView =
                         view.findViewById(viewBinder.privacyInformationIconImageId);
                viewHolder.mMediaView =
                         view.findViewById(viewBinder.mediaLayoutId);
                Map<String, Integer> extraViews = viewBinder.extras;
                Integer starRatingTextViewId = extraViews.get(VIEW_BINDER_KEY_STAR_RATING);
                if (starRatingTextViewId != null) {
                    viewHolder.mStarRatingTextView =
                             view.findViewById(starRatingTextViewId);
                }
                Integer advertiserTextViewId = extraViews.get(VIEW_BINDER_KEY_ADVERTISER);
                if (advertiserTextViewId != null) {
                    viewHolder.mAdvertiserTextView =
                             view.findViewById(advertiserTextViewId);
                }
                Integer storeTextViewId = extraViews.get(VIEW_BINDER_KEY_STORE);
                if (storeTextViewId != null) {
                    viewHolder.mStoreTextView =  view.findViewById(storeTextViewId);
                }
                Integer priceTextViewId = extraViews.get(VIEW_BINDER_KEY_PRICE);
                if (priceTextViewId != null) {
                    viewHolder.mPriceTextView =  view.findViewById(priceTextViewId);
                }
                Integer adChoicesIconViewId =
                        extraViews.get(VIEW_BINDER_KEY_AD_CHOICES_ICON_CONTAINER);
                if (adChoicesIconViewId != null) {
                    viewHolder.mAdChoicesIconContainer =
                             view.findViewById(adChoicesIconViewId);
                }
                return viewHolder;
            } catch (ClassCastException exception) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Could not cast from id in ViewBinder to " +
                        "expected View type", exception);
                return EMPTY_VIEW_HOLDER;
            }
        }
    }

}

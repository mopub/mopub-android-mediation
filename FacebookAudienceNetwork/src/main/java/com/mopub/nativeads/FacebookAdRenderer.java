package com.mopub.nativeads;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.ads.AdOptionsView;
import com.facebook.ads.MediaView;
import com.facebook.ads.NativeAdLayout;
import com.mopub.common.Preconditions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class FacebookAdRenderer implements MoPubAdRenderer<FacebookNative.FacebookNativeAd> {
    private final FacebookViewBinder mViewBinder;

    // This is used instead of View.setTag, which causes a memory leak in 2.3
    // and earlier: https://code.google.com/p/android/issues/detail?id=18273
    @NonNull
    final WeakHashMap<View, FacebookNativeViewHolder> mViewHolderMap;

    /**
     * Constructs a native ad renderer with a view binder.
     *
     * @param viewBinder The view binder to use when inflating and rendering an ad.
     */
    public FacebookAdRenderer(final FacebookViewBinder viewBinder) {
        mViewBinder = viewBinder;
        mViewHolderMap = new WeakHashMap<View, FacebookNativeViewHolder>();
    }

    @Override
    public View createAdView(@NonNull Context context, final ViewGroup parent) {
        Preconditions.checkNotNull(context);
        return LayoutInflater
                .from(context)
                .inflate(mViewBinder.layoutId, parent, false);
    }

    @Override
    public void renderAdView(@NonNull View view,
                             @NonNull FacebookNative.FacebookNativeAd facebookNativeAd) {
        Preconditions.checkNotNull(facebookNativeAd);
        Preconditions.checkNotNull(view);

        FacebookNativeViewHolder facebookNativeViewHolder = mViewHolderMap.get(view);
        if (facebookNativeViewHolder == null) {
            facebookNativeViewHolder = FacebookNativeViewHolder.fromViewBinder(view, mViewBinder);
            mViewHolderMap.put(view, facebookNativeViewHolder);
        }

        update(facebookNativeViewHolder, facebookNativeAd);
        NativeRendererHelper.updateExtras(facebookNativeViewHolder.getMainView(),
                mViewBinder.extras,
                facebookNativeAd.getExtras());
    }

    @Override
    public boolean supports(@NonNull BaseNativeAd nativeAd) {
        Preconditions.checkNotNull(nativeAd);
        return nativeAd instanceof FacebookNative.FacebookNativeAd;
    }

    private void update(final FacebookNativeViewHolder facebookNativeViewHolder,
                        final FacebookNative.FacebookNativeAd nativeAd) {
        NativeRendererHelper.addTextView(facebookNativeViewHolder.getTitleView(),
                nativeAd.getTitle());
        NativeRendererHelper.addTextView(facebookNativeViewHolder.getTextView(), nativeAd.getText());
        NativeRendererHelper.addTextView(facebookNativeViewHolder.getCallToActionView(),
                nativeAd.getCallToAction());
        NativeRendererHelper.addTextView(facebookNativeViewHolder.getAdvertiserNameView(),
                nativeAd.getAdvertiserName());
        NativeRendererHelper.addTextView(facebookNativeViewHolder.getSponsoredLabelView(),
                nativeAd.getSponsoredName());

        final RelativeLayout adChoicesContainer =
                facebookNativeViewHolder.getAdChoicesContainer();
        nativeAd.registerChildViewsForInteraction(facebookNativeViewHolder.getMainView(),
                facebookNativeViewHolder.getMediaView(), facebookNativeViewHolder.getAdIconView());
        if (adChoicesContainer != null) {
            adChoicesContainer.removeAllViews();
            NativeAdLayout nativeAdLayout = null;
            if (facebookNativeViewHolder.mainView instanceof NativeAdLayout) {
                nativeAdLayout = (NativeAdLayout) facebookNativeViewHolder.mainView;
            }
            final AdOptionsView adOptionsView =
                    new AdOptionsView(
                            adChoicesContainer.getContext(),
                            nativeAd.getFacebookNativeAd(),
                            nativeAdLayout);
            ViewGroup.LayoutParams layoutParams = adOptionsView.getLayoutParams();
            if (layoutParams instanceof RelativeLayout.LayoutParams) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    ((RelativeLayout.LayoutParams) layoutParams).addRule(
                            RelativeLayout.ALIGN_PARENT_END);
                } else {
                    ((RelativeLayout.LayoutParams) layoutParams).addRule(
                            RelativeLayout.ALIGN_PARENT_RIGHT);
                }
            }
            adChoicesContainer.addView(adOptionsView);
        }
    }

    static class FacebookNativeViewHolder {
        @Nullable
        private View mainView;
        @Nullable
        private TextView titleView;
        @Nullable
        private TextView textView;
        @Nullable
        private TextView callToActionView;
        @Nullable
        private RelativeLayout adChoicesContainer;
        @Nullable
        private MediaView mediaView;
        @Nullable
        private MediaView adIconView;
        @Nullable
        private TextView advertiserNameView;
        @Nullable
        private TextView sponsoredLabelView;

        // Use fromViewBinder instead of a constructor
        private FacebookNativeViewHolder() {
        }

        static FacebookNativeViewHolder fromViewBinder(@Nullable final View view,
                                                       @Nullable final FacebookViewBinder facebookViewBinder) {
            if (view == null || facebookViewBinder == null) {
                return new FacebookNativeViewHolder();
            }

            final FacebookNativeViewHolder viewHolder = new FacebookNativeViewHolder();
            viewHolder.mainView = view;
            viewHolder.titleView = view.findViewById(facebookViewBinder.titleId);
            viewHolder.textView = view.findViewById(facebookViewBinder.textId);
            viewHolder.callToActionView =
                    view.findViewById(facebookViewBinder.callToActionId);
            viewHolder.adChoicesContainer =
                    view.findViewById(facebookViewBinder.adChoicesRelativeLayoutId);
            viewHolder.mediaView = view.findViewById(facebookViewBinder.mediaViewId);
            viewHolder.adIconView = view.findViewById(facebookViewBinder.adIconViewId);
            viewHolder.advertiserNameView = view.findViewById(facebookViewBinder.advertiserNameId);
            viewHolder.sponsoredLabelView = view.findViewById(facebookViewBinder.sponsoredLabelId);
            return viewHolder;
        }

        @Nullable
        public View getMainView() {
            return mainView;
        }

        @Nullable
        public TextView getTitleView() {
            return titleView;
        }

        @Nullable
        public TextView getTextView() {
            return textView;
        }

        @Nullable
        public TextView getCallToActionView() {
            return callToActionView;
        }

        @Nullable
        public RelativeLayout getAdChoicesContainer() {
            return adChoicesContainer;
        }

        @Nullable
        public MediaView getAdIconView() {
            return adIconView;
        }

        @Nullable
        public MediaView getMediaView() {
            return mediaView;
        }

        @Nullable
        public TextView getAdvertiserNameView() {
            return advertiserNameView;
        }

        @Nullable
        public TextView getSponsoredLabelView() {
            return sponsoredLabelView;
        }
    }

    public static class FacebookViewBinder {

        final int layoutId;
        final int titleId;
        final int textId;
        final int callToActionId;
        final int adChoicesRelativeLayoutId;
        @NonNull
        final Map<String, Integer> extras;
        final int mediaViewId;
        final int adIconViewId;
        final int advertiserNameId;
        final int sponsoredLabelId;

        private FacebookViewBinder(@NonNull final Builder builder) {
            Preconditions.checkNotNull(builder);

            this.layoutId = builder.layoutId;
            this.titleId = builder.titleId;
            this.textId = builder.textId;
            this.callToActionId = builder.callToActionId;
            this.adChoicesRelativeLayoutId = builder.adChoicesRelativeLayoutId;
            this.extras = builder.extras;
            this.mediaViewId = builder.mediaViewId;
            this.adIconViewId = builder.adIconViewId;
            this.advertiserNameId = builder.advertiserNameId;
            this.sponsoredLabelId = builder.sponsoredLabelId;
        }

        public static class Builder {

            private final int layoutId;
            private int titleId;
            private int textId;
            private int callToActionId;
            private int adChoicesRelativeLayoutId;
            @NonNull
            private Map<String, Integer> extras = Collections.emptyMap();
            private int mediaViewId;
            private int adIconViewId;
            private int advertiserNameId;
            private int sponsoredLabelId;

            public Builder(final int layoutId) {
                this.layoutId = layoutId;
                this.extras = new HashMap<>();
            }

            @NonNull
            public final Builder titleId(final int titleId) {
                this.titleId = titleId;
                return this;
            }

            @NonNull
            public final Builder textId(final int textId) {
                this.textId = textId;
                return this;
            }

            @NonNull
            public final Builder callToActionId(final int callToActionId) {
                this.callToActionId = callToActionId;
                return this;
            }

            @NonNull
            public final Builder adChoicesRelativeLayoutId(final int adChoicesRelativeLayoutId) {
                this.adChoicesRelativeLayoutId = adChoicesRelativeLayoutId;
                return this;
            }

            @NonNull
            public final Builder extras(final Map<String, Integer> resourceIds) {
                this.extras = new HashMap<String, Integer>(resourceIds);
                return this;
            }

            @NonNull
            public final Builder addExtra(final String key, final int resourceId) {
                this.extras.put(key, resourceId);
                return this;
            }

            @NonNull
            public Builder mediaViewId(final int mediaViewId) {
                this.mediaViewId = mediaViewId;
                return this;
            }

            @NonNull
            public Builder adIconViewId(final int adIconViewId) {
                this.adIconViewId = adIconViewId;
                return this;
            }

            @NonNull
            public Builder advertiserNameId(final int advertiserNameId) {
                this.advertiserNameId = advertiserNameId;
                return this;
            }

            @NonNull
            public Builder sponsoredNameId(final int sponsoredLabelId) {
                this.sponsoredLabelId = sponsoredLabelId;
                return this;
            }

            @NonNull
            public FacebookViewBinder build() {
                return new FacebookViewBinder(this);
            }
        }
    }
}

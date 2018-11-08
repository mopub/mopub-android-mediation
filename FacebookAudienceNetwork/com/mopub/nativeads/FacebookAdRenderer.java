package com.mopub.nativeads;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.ads.AdChoicesView;
import com.facebook.ads.AdIconView;
import com.facebook.ads.MediaView;
import com.mopub.common.Preconditions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Include this class if you want to use Facebook native video ads. This renderer handles Facebook
 * static and video native ads. This will automatically replace the main image view with the
 * Facebook MediaView that handles showing the main asset.
 */
public class FacebookAdRenderer implements MoPubAdRenderer<FacebookNative.FacebookVideoEnabledNativeAd> {
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
    public View createAdView(final Context context, final ViewGroup parent) {
        return LayoutInflater
                .from(context)
                .inflate(mViewBinder.layoutId, parent, false);
    }

    @Override
    public void renderAdView(final View view,
                             final FacebookNative.FacebookVideoEnabledNativeAd facebookVideoEnabledNativeAd) {
        FacebookNativeViewHolder facebookNativeViewHolder = mViewHolderMap.get(view);
        if (facebookNativeViewHolder == null) {
            facebookNativeViewHolder = FacebookNativeViewHolder.fromViewBinder(view, mViewBinder);
            mViewHolderMap.put(view, facebookNativeViewHolder);
        }

        update(facebookNativeViewHolder, facebookVideoEnabledNativeAd);
        NativeRendererHelper.updateExtras(facebookNativeViewHolder.getMainView(),
                mViewBinder.extras,
                facebookVideoEnabledNativeAd.getExtras());
    }

    @Override
    public boolean supports(final BaseNativeAd nativeAd) {
        Preconditions.checkNotNull(nativeAd);
        return nativeAd instanceof FacebookNative.FacebookVideoEnabledNativeAd;
    }

    private void update(final FacebookNativeViewHolder facebookNativeViewHolder,
                        final FacebookNative.FacebookVideoEnabledNativeAd nativeAd) {
        NativeRendererHelper.addTextView(facebookNativeViewHolder.getTitleView(),
                nativeAd.getTitle());
        NativeRendererHelper.addTextView(facebookNativeViewHolder.getTextView(), nativeAd.getText());
        NativeRendererHelper.addTextView(facebookNativeViewHolder.getCallToActionView(),
                nativeAd.getCallToAction());
        NativeRendererHelper.addTextView(facebookNativeViewHolder.getAdvertiserNameView(),
                nativeAd.getAdvertiserName());

        final RelativeLayout adChoicesContainer =
                facebookNativeViewHolder.getAdChoicesContainer();
        nativeAd.registerChildViewsForInteraction(facebookNativeViewHolder.getMainView(),
                facebookNativeViewHolder.getMediaView(), facebookNativeViewHolder.getAdIconView());
        if (adChoicesContainer != null) {
            adChoicesContainer.removeAllViews();
            final AdChoicesView adChoicesView = new AdChoicesView(adChoicesContainer.getContext(),
                    nativeAd.getFacebookNativeAd(), true);
            ViewGroup.LayoutParams layoutParams = adChoicesView.getLayoutParams();
            if (layoutParams instanceof RelativeLayout.LayoutParams) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    ((RelativeLayout.LayoutParams) layoutParams).addRule(RelativeLayout.ALIGN_PARENT_END);
                } else {
                    ((RelativeLayout.LayoutParams) layoutParams).addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                }
            }
            adChoicesContainer.addView(adChoicesView);
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
        private AdIconView adIconView;
        @Nullable
        private TextView advertiserNameView;

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
        public AdIconView getAdIconView() {
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

        private FacebookViewBinder(@NonNull final Builder builder) {
            this.layoutId = builder.layoutId;
            this.titleId = builder.titleId;
            this.textId = builder.textId;
            this.callToActionId = builder.callToActionId;
            this.adChoicesRelativeLayoutId = builder.adChoicesRelativeLayoutId;
            this.extras = builder.extras;
            this.mediaViewId = builder.mediaViewId;
            this.adIconViewId = builder.adIconViewId;
            this.advertiserNameId = builder.advertiserNameId;
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
            public FacebookViewBinder build() {
                return new FacebookViewBinder(this);
            }
        }
    }
}
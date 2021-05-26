// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.Preconditions;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * This is a reference adapter implementation designed for testing the MoPub mediation protocol ONLY.
 * For that purpose, it leverages a simple network SDK implementation and does not constitute any real-world
 * testing as far as a live ad experience goes.
 * <p>
 * Future MoPub adapter implementations may refer to the implementation here for best practices.
 */
public class ReferenceNativeAdRenderer implements MoPubAdRenderer<ReferenceNativeAdapter.ReferenceNativeAd> {
    private final ReferenceViewBinder mViewBinder;

    @NonNull
    final WeakHashMap<View, ReferenceNativeViewHolder> mViewHolderMap;

    public ReferenceNativeAdRenderer(final ReferenceViewBinder viewBinder) {
        mViewBinder = viewBinder;
        mViewHolderMap = new WeakHashMap<>();
    }

    /**
     * Creates a new view to be used as an ad.
     * <p/>
     * This method is called when you call {@link com.mopub.nativeads.MoPubStreamAdPlacer#getAdView}
     * and the convertView is null. You must return a valid view.
     *
     * @param context The context. Useful for creating a view. This is recommended to be an
     *                Activity. If you have custom themes defined in your Activity, not passing
     *                in that Activity will result in the default Application theme being used
     *                when creating the ad view.
     * @param parent  The parent that the view will eventually be attached to. You might use the
     *                parent to determine layout parameters, but should return the view without
     *                attaching it to the parent.
     * @return A new ad view.
     */
    @NonNull
    @Override
    public View createAdView(@NonNull Context context, final ViewGroup parent) {
        Preconditions.checkNotNull(context);

        return LayoutInflater.from(context).inflate(mViewBinder.layoutId, parent, false);
    }

    /**
     * Renders a view created by {@link #createAdView} by filling it with ad data.
     *
     * @param view              The ad {@link View}
     * @param referenceNativeAd The ad data that should be bound to the view.
     */
    @Override
    public void renderAdView(@NonNull View view,
                             @NonNull ReferenceNativeAdapter.ReferenceNativeAd referenceNativeAd) {

        // It is recommended to fail fast if required parameters are invalid
        Preconditions.checkNotNull(referenceNativeAd);
        Preconditions.checkNotNull(view);

        ReferenceNativeViewHolder referenceNativeViewHolder = mViewHolderMap.get(view);

        if (referenceNativeViewHolder == null) {
            referenceNativeViewHolder = ReferenceNativeViewHolder.fromViewBinder(view, mViewBinder);
            mViewHolderMap.put(view, referenceNativeViewHolder);
        }

        update(referenceNativeViewHolder, referenceNativeAd);
    }

    /**
     * Determines if this renderer supports the type of native ad passed in.
     *
     * @param nativeAd The native ad to render.
     * @return True if the renderer can render the native ad and false if it cannot.
     */
    @Override
    public boolean supports(@NonNull BaseNativeAd nativeAd) {
        Preconditions.checkNotNull(nativeAd);

        return nativeAd instanceof ReferenceNativeAdapter.ReferenceNativeAd;
    }

    private void update(final ReferenceNativeViewHolder referenceNativeViewHolder,
                        final ReferenceNativeAdapter.ReferenceNativeAd nativeAd) {
        NativeRendererHelper.addTextView(referenceNativeViewHolder.getTitleView(),
                nativeAd.getTitle());
        NativeRendererHelper.addTextView(referenceNativeViewHolder.getTextView(), nativeAd.getText());
        NativeRendererHelper.addTextView(referenceNativeViewHolder.getCallToActionView(),
                nativeAd.getCallToAction());
        NativeRendererHelper.addTextView(referenceNativeViewHolder.getAdvertiserNameView(),
                nativeAd.getAdvertiserName());
        NativeRendererHelper.addTextView(referenceNativeViewHolder.getSponsoredLabelView(),
                nativeAd.getSponsoredName());

        NativeImageHelper.loadImageView(nativeAd.getMainImageUrl(), referenceNativeViewHolder.mainImageView);
        NativeImageHelper.loadImageView(nativeAd.getIconImageUrl(), referenceNativeViewHolder.iconImageView);
    }

    static class ReferenceNativeViewHolder {
        @Nullable
        private TextView titleView;
        @Nullable
        private TextView textView;
        @Nullable
        private TextView callToActionView;
        @Nullable
        private RelativeLayout adChoicesContainer;
        @Nullable
        private ImageView mainImageView;
        @Nullable
        private ImageView iconImageView;
        @Nullable
        private TextView advertiserNameView;
        @Nullable
        private TextView sponsoredLabelView;

        private ReferenceNativeViewHolder() {
        }

        static ReferenceNativeViewHolder fromViewBinder(@Nullable final View view,
                                                        @Nullable final ReferenceViewBinder referenceViewBinder) {
            if (view == null || referenceViewBinder == null) {
                return new ReferenceNativeViewHolder();
            }

            final ReferenceNativeViewHolder viewHolder = new ReferenceNativeViewHolder();
            viewHolder.titleView = view.findViewById(referenceViewBinder.titleId);
            viewHolder.textView = view.findViewById(referenceViewBinder.textId);
            viewHolder.callToActionView = view.findViewById(referenceViewBinder.callToActionId);
            viewHolder.adChoicesContainer = view.findViewById(referenceViewBinder.adChoicesRelativeLayoutId);
            viewHolder.mainImageView = view.findViewById(referenceViewBinder.mainImageId);
            viewHolder.iconImageView = view.findViewById(referenceViewBinder.iconImageId);
            viewHolder.advertiserNameView = view.findViewById(referenceViewBinder.advertiserNameId);
            viewHolder.sponsoredLabelView = view.findViewById(referenceViewBinder.sponsoredLabelId);

            return viewHolder;
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
        public TextView getAdvertiserNameView() {
            return advertiserNameView;
        }

        @Nullable
        public TextView getSponsoredLabelView() {
            return sponsoredLabelView;
        }
    }

    public static class ReferenceViewBinder {
        final int layoutId;
        final int titleId;
        final int textId;
        final int callToActionId;
        final int adChoicesRelativeLayoutId;
        @NonNull
        final Map<String, Integer> extras;
        final int mainImageId;
        final int iconImageId;
        final int advertiserNameId;
        final int sponsoredLabelId;

        private ReferenceViewBinder(@NonNull final Builder builder) {
            Preconditions.checkNotNull(builder);

            this.layoutId = builder.layoutId;
            this.titleId = builder.titleId;
            this.textId = builder.textId;
            this.callToActionId = builder.callToActionId;
            this.adChoicesRelativeLayoutId = builder.adChoicesRelativeLayoutId;
            this.extras = builder.extras;
            this.mainImageId = builder.mainImageId;
            this.iconImageId = builder.iconImageId;
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
            private Map<String, Integer> extras;
            private int mainImageId;
            private int iconImageId;
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
            public Builder mainImageId(final int mainImageId) {
                this.mainImageId = mainImageId;
                return this;
            }

            @NonNull
            public Builder iconImageId(final int iconImageId) {
                this.iconImageId = iconImageId;
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
            public ReferenceViewBinder build() {
                return new ReferenceViewBinder(this);
            }
        }
    }
}

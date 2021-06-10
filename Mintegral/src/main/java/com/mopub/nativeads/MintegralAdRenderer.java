package com.mopub.nativeads;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mbridge.msdk.nativex.view.MBMediaView;
import com.mbridge.msdk.out.Campaign;
import com.mbridge.msdk.out.OnMBMediaViewListener;
import com.mbridge.msdk.widget.MBAdChoice;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class MintegralAdRenderer implements MoPubAdRenderer<MintegralNative.MBridgeNativeAd> {
    private final String ADAPTER_NAME = this.getClass().getSimpleName();
    private final ViewBinder mViewBinder;

    private final WeakHashMap<View, NativeViewHolder> mViewHolderMap;

    public MintegralAdRenderer(final ViewBinder viewBinder) {
        mViewBinder = viewBinder;
        mViewHolderMap = new WeakHashMap<>();
    }

    @NonNull
    @Override
    public View createAdView(@NonNull final Context context, @Nullable final ViewGroup parent) {
        Preconditions.checkNotNull(context);

        return LayoutInflater.from(context).inflate(mViewBinder.layoutId, parent, false);
    }

    @Override
    public void renderAdView(@NonNull View view, @NonNull MintegralNative.MBridgeNativeAd ad) {
        Preconditions.checkNotNull(view);
        Preconditions.checkNotNull(ad);

        NativeViewHolder nativeViewHolder = mViewHolderMap.get(view);

        if (nativeViewHolder == null) {
            nativeViewHolder = NativeViewHolder.fromViewBinder(view, mViewBinder);
            mViewHolderMap.put(view, nativeViewHolder);
        }

        update(nativeViewHolder, ad);
    }

    @Override
    public boolean supports(@NonNull BaseNativeAd nativeAd) {
        Preconditions.checkNotNull(nativeAd);

        return nativeAd instanceof MintegralNative.MBridgeNativeAd;
    }

    private void update(final NativeViewHolder nativeViewHolder,
                        final MintegralNative.MBridgeNativeAd nativeAd) {

        final ImageView mainImageView = nativeViewHolder.getMainImageView();

        NativeRendererHelper.addTextView(nativeViewHolder.getTitleView(), nativeAd.getTitle());
        NativeRendererHelper.addTextView(nativeViewHolder.getTextView(), nativeAd.getText());
        NativeRendererHelper.addTextView(nativeViewHolder.getCallToActionView(), nativeAd.getCallToAction());
        NativeImageHelper.loadImageView(nativeAd.getMainImageUrl(), mainImageView);
        NativeImageHelper.loadImageView(nativeAd.getIconUrl(), nativeViewHolder.getIconImageView());

        nativeAd.registerViewForInteraction(nativeViewHolder.getMainView());

        final MBMediaView mediaView = nativeViewHolder.getMediaView();

        if (mediaView != null) {
            mediaView.setNativeAd(nativeAd.mCampaign);
            mediaView.setVisibility(View.VISIBLE);

            mediaView.setOnMediaViewListener(new OnMBMediaViewListener() {
                @Override
                public void onEnterFullscreen() {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "onEnterFullscreen");
                }

                @Override
                public void onExitFullscreen() {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "onExitFullscreen");
                }

                @Override
                public void onStartRedirection(Campaign campaign, String message) {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "onStartRedirection: " + message);
                }

                @Override
                public void onFinishRedirection(Campaign campaign, String message) {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "onFinishRedirection: " + message);
                }

                @Override
                public void onRedirectionFailed(Campaign campaign, String message) {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "onRedirectionFailed: " + message);
                }

                @Override
                public void onVideoAdClicked(Campaign campaign) {
                    MoPubLog.log(CLICKED, ADAPTER_NAME);
//                    nativeAd.notifyAdClicked();
                    nativeAd.onAdClick(campaign);
                }

                @Override
                public void onVideoStart() {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "onVideoStart");
                }
            });

        }

        final Campaign campaign = nativeAd.mCampaign;
        try {
            final MBAdChoice adChoices = nativeViewHolder.getAdChoice();
            if (adChoices != null) {
                adChoices.setCampaign(campaign);
            }
        } catch (Throwable e) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "Failed to set AdChoices", e);
        }
    }

    static class NativeViewHolder {

        @Nullable
        View mainView;
        @Nullable
        TextView titleView;
        @Nullable
        TextView textView;
        @Nullable
        TextView callToActionView;
        @Nullable
        ImageView mainImageView;
        @Nullable
        ImageView iconImageView;
        @Nullable
        ImageView privacyInformationIconImageView;
        @Nullable
        MBMediaView mediaView;
        @Nullable
        MBAdChoice adChoice;

        @VisibleForTesting
        static final NativeViewHolder EMPTY_VIEW_HOLDER = new NativeViewHolder();

        private NativeViewHolder() {
        }

        static NativeViewHolder fromViewBinder(final View view, final ViewBinder viewBinder) {
            if (view == null || viewBinder == null) {
                return new NativeViewHolder();
            }
            final NativeViewHolder viewHolder = new NativeViewHolder();
            viewHolder.mainView = view;

            try {
                viewHolder.titleView = view.findViewById(viewBinder.titleId);
                viewHolder.textView = view.findViewById(viewBinder.textId);
                viewHolder.mainImageView = view.findViewById(viewBinder.mainImageId);
                viewHolder.iconImageView = view.findViewById(viewBinder.iconImageId);
                viewHolder.callToActionView = view.findViewById(viewBinder.callToActionId);
                viewHolder.adChoice = view.findViewById(viewBinder.adChoicesId);
                viewHolder.mediaView = view.findViewById(viewBinder.mediaViewId);

                return viewHolder;
            } catch (ClassCastException exception) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Could not cast from id in ViewBinder to " +
                        "expected View type", exception);
                return EMPTY_VIEW_HOLDER;
            }
        }

        @Nullable
        View getMainView() {
            return mainView;
        }

        @Nullable
        TextView getTitleView() {
            return titleView;
        }

        @Nullable
        TextView getTextView() {
            return textView;
        }

        @Nullable
        TextView getCallToActionView() {
            return callToActionView;
        }

        @Nullable
        ImageView getMainImageView() {
            return mainImageView;
        }

        @Nullable
        ImageView getIconImageView() {
            return iconImageView;
        }

        @Nullable
        ImageView getPrivacyInformationIconImageView() {
            return privacyInformationIconImageView;
        }

        @Nullable
        MBMediaView getMediaView() {
            return mediaView;
        }

        @Nullable
        public MBAdChoice getAdChoice() {
            return adChoice;
        }
    }

    public static class ViewBinder {
        public final static class Builder {
            private final int layoutId;
            private int titleId;
            private int textId;
            private int callToActionId;
            private int mainImageId;
            private int iconImageId;
            private int privacyInformationIconImageId;
            private int mediaViewId;
            private int adChoicesId;

            @NonNull
            private Map<String, Integer> extras = Collections.emptyMap();

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
            public final Builder mainImageId(final int mediaLayoutId) {
                this.mainImageId = mediaLayoutId;
                return this;
            }

            @NonNull
            public final Builder iconImageId(final int iconImageId) {
                this.iconImageId = iconImageId;
                return this;
            }

            @NonNull
            public final Builder privacyInformationIconImageId(final int privacyInformationIconImageId) {
                this.privacyInformationIconImageId = privacyInformationIconImageId;
                return this;
            }

            @NonNull
            public final Builder adChoicesId(final int adChoicesId) {
                this.adChoicesId = adChoicesId;
                return this;
            }

            public final Builder mediaViewId(final int mediaViewId) {
                this.mediaViewId = mediaViewId;
                return this;
            }

            @NonNull
            public final Builder addExtras(final Map<String, Integer> resourceIds) {
                this.extras = new HashMap<String, Integer>(resourceIds);
                return this;
            }

            @NonNull
            public final Builder addExtra(final String key, final int resourceId) {
                this.extras.put(key, resourceId);
                return this;
            }

            @NonNull
            public final ViewBinder build() {
                return new ViewBinder(this);
            }
        }

        final int layoutId;
        final int titleId;
        final int textId;
        final int callToActionId;
        final int mainImageId;
        final int iconImageId;
        final int privacyInformationIconImageId;
        final int mediaViewId;
        final int adChoicesId;
        @NonNull
        final Map<String, Integer> extras;

        private ViewBinder(@NonNull final Builder builder) {
            this.layoutId = builder.layoutId;
            this.titleId = builder.titleId;
            this.textId = builder.textId;
            this.callToActionId = builder.callToActionId;
            this.mainImageId = builder.mainImageId;
            this.iconImageId = builder.iconImageId;
            this.privacyInformationIconImageId = builder.privacyInformationIconImageId;
            this.mediaViewId = builder.mediaViewId;
            this.adChoicesId = builder.adChoicesId;
            this.extras = builder.extras;
        }
    }
}

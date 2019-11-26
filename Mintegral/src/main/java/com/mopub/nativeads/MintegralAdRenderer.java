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

import com.mintegral.msdk.nativex.view.MTGMediaView;
import com.mintegral.msdk.out.Campaign;
import com.mintegral.msdk.out.OnMTGMediaViewListener;
import com.mintegral.msdk.widget.MTGAdChoice;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import static android.view.View.VISIBLE;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class MintegralAdRenderer implements MoPubAdRenderer<MintegralNative.MintegralStaticNativeAd> {

    private static final String ADAPTER_NAME = MintegralAdRenderer.class.getSimpleName();
    private final MintegralViewBinder mViewBinder;

    private final WeakHashMap<View, MintegralNativeViewHolder> mViewHolderMap;

    public MintegralAdRenderer(final MintegralViewBinder viewBinder) {
        mViewBinder = viewBinder;
        mViewHolderMap = new WeakHashMap<>();
    }

    @NonNull
    @Override
    public View createAdView(@NonNull final Context context, @Nullable final ViewGroup parent) {

        Preconditions.checkNotNull(context);

        final View adView = LayoutInflater.from(context).inflate(mViewBinder.layoutId, parent, false);
        final View mainImageView = adView.findViewById(mViewBinder.mainImageId);

        if (mainImageView == null) {
            return adView;
        }

        final ViewGroup.LayoutParams mainImageViewLayoutParams = mainImageView.getLayoutParams();
        final MTGMediaView.LayoutParams mediaViewLayoutParams = new MTGMediaView.LayoutParams(
                mainImageViewLayoutParams.width, mainImageViewLayoutParams.height);

        if (mainImageViewLayoutParams instanceof ViewGroup.MarginLayoutParams) {
            final ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) mainImageViewLayoutParams;

            mediaViewLayoutParams.setMargins(marginParams.leftMargin,
                    marginParams.topMargin,
                    marginParams.rightMargin,
                    marginParams.bottomMargin);
        }

        if (mainImageViewLayoutParams instanceof RelativeLayout.LayoutParams) {
            mainImageView.setVisibility(View.INVISIBLE);
        } else {
            mainImageView.setVisibility(View.GONE);
        }

        final MTGMediaView mediaView = new MTGMediaView(context);
        mediaView.setLayoutParams(mainImageViewLayoutParams);

        final ViewGroup mainImageParent = (ViewGroup) mainImageView.getParent();
        final int mainImageIndex = mainImageParent.indexOfChild(mainImageView);

        // Mintegral uses a MediaView instead of the main image ImageView supplied by the publisher,
        // so we swap the main image ImageView with the MediaView using the former's layout params
        // (while still keeping the former in the view hierarchy).
        mainImageParent.addView(mediaView, mainImageIndex + 1);

        return adView;
    }

    @Override
    public void renderAdView(@NonNull final View view, @NonNull final MintegralNative.MintegralStaticNativeAd ad) {

        Preconditions.checkNotNull(view);
        Preconditions.checkNotNull(ad);

        MintegralNativeViewHolder mintegralNativeViewHolder = mViewHolderMap.get(view);

        if (mintegralNativeViewHolder == null) {
            mintegralNativeViewHolder = MintegralNativeViewHolder.fromViewBinder(view, mViewBinder);
            mViewHolderMap.put(view, mintegralNativeViewHolder);
        }

        update(mintegralNativeViewHolder, ad);

        setViewVisibility(mintegralNativeViewHolder, VISIBLE);
        ad.prepare(view);
    }

    @Override
    public boolean supports(@NonNull BaseNativeAd nativeAd) {
        return nativeAd instanceof MintegralNative.MintegralStaticNativeAd;
    }

    private void update(final MintegralNativeViewHolder mintegralNativeViewHolder,
                        final MintegralNative.MintegralStaticNativeAd nativeAd) {

        final ImageView mainImageView = mintegralNativeViewHolder.getMainImageView();

        NativeRendererHelper.addTextView(mintegralNativeViewHolder.getTitleView(),
                nativeAd.getTitle());
        NativeRendererHelper.addTextView(mintegralNativeViewHolder.getTextView(), nativeAd.getText());
        NativeRendererHelper.addTextView(mintegralNativeViewHolder.getCallToActionView(),
                nativeAd.getCallToAction());
        NativeImageHelper.loadImageView(nativeAd.getMainImageUrl(), mainImageView);
        NativeImageHelper.loadImageView(nativeAd.getIconImageUrl(),
                mintegralNativeViewHolder.getIconImageView());
        NativeRendererHelper.addPrivacyInformationIcon(
                mintegralNativeViewHolder.getPrivacyInformationIconImageView(),
                nativeAd.getPrivacyInformationIconImageUrl(),
                nativeAd.getPrivacyInformationIconClickThroughUrl());

        final MTGMediaView mediaView = mintegralNativeViewHolder.getMediaView();

        if (mediaView != null && mainImageView != null) {
            mediaView.setNativeAd(nativeAd.mCampaign);
            mediaView.setVisibility(View.VISIBLE);

            mediaView.setOnMediaViewListener(new OnMTGMediaViewListener() {
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
                    nativeAd.notifyAdClicked();
                }

                @Override
                public void onVideoStart() {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "onVideoStart");
                }
            });

            if (mintegralNativeViewHolder.isMainImageViewInRelativeView()) {
                mainImageView.setVisibility(View.INVISIBLE);
            } else {
                mainImageView.setVisibility(View.GONE);
            }
        }

        final Campaign campaign = nativeAd.mCampaign;
        final MTGAdChoice adChoices = mintegralNativeViewHolder.getAdChoices();

        try {
            final RelativeLayout view = (RelativeLayout) mintegralNativeViewHolder.getTitleView().getParent();
            final RelativeLayout.LayoutParams Params = (RelativeLayout.LayoutParams) view.getLayoutParams();

            Params.height = campaign.getAdchoiceSizeHeight();
            Params.width = campaign.getAdchoiceSizeWidth();

            if (adChoices != null) {
                adChoices.setLayoutParams(Params);
                adChoices.setCampaign(campaign);
            }
        } catch (Throwable e) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "Failed to update AdChoices layout params", e);
        }
    }

    private static void setViewVisibility(final MintegralNativeViewHolder mintegralNativeViewHolder,
                                          final int visibility) {
        if (mintegralNativeViewHolder.getMainView() != null) {
            mintegralNativeViewHolder.getMainView().setVisibility(visibility);
        }
    }

    static class MintegralNativeViewHolder {
        private MTGMediaView mMediaView;
        private boolean isMainImageViewInRelativeView;
        private MTGAdChoice adChoices;

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

        @VisibleForTesting
        static final MintegralNativeViewHolder EMPTY_VIEW_HOLDER = new MintegralNativeViewHolder();

        private MintegralNativeViewHolder() {
        }

        static MintegralNativeViewHolder fromViewBinder(final View view,
                                                        final MintegralViewBinder viewBinder) {
            final MintegralNativeViewHolder staticNativeViewHolder = new MintegralNativeViewHolder();
            staticNativeViewHolder.mainView = view;

            try {
                staticNativeViewHolder.titleView = view.findViewById(viewBinder.titleId);
                staticNativeViewHolder.textView = view.findViewById(viewBinder.textId);
                staticNativeViewHolder.callToActionView = view.findViewById(viewBinder.callToActionId);
                staticNativeViewHolder.mainImageView = view.findViewById(viewBinder.mainImageId);
                staticNativeViewHolder.iconImageView = view.findViewById(viewBinder.iconImageId);
                staticNativeViewHolder.privacyInformationIconImageView = view.findViewById(viewBinder.privacyInformationIconImageId);
                staticNativeViewHolder.adChoices = view.findViewById(viewBinder.adChoicesId);

                final View mainImageView = staticNativeViewHolder.mainImageView;
                boolean mainImageViewInRelativeView = false;
                MTGMediaView mediaView = null;

                if (mainImageView != null) {
                    final ViewGroup mainImageParent = (ViewGroup) mainImageView.getParent();

                    if (mainImageParent instanceof RelativeLayout) {
                        mainImageViewInRelativeView = true;
                    }

                    final int mainImageIndex = mainImageParent.indexOfChild(mainImageView);
                    final View viewAfterImageView = mainImageParent.getChildAt(mainImageIndex + 1);

                    if (viewAfterImageView instanceof MTGMediaView) {
                        mediaView = (MTGMediaView) viewAfterImageView;
                    }
                }

                staticNativeViewHolder.mMediaView = mediaView;
                staticNativeViewHolder.isMainImageViewInRelativeView = mainImageViewInRelativeView;

                return staticNativeViewHolder;
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
        MTGMediaView getMediaView() {
            return mMediaView;
        }

        boolean isMainImageViewInRelativeView() {
            return isMainImageViewInRelativeView;
        }

        @Nullable
        MTGAdChoice getAdChoices() {
            return adChoices;
        }
    }

    public static class MintegralViewBinder {
        public final static class Builder {
            private final int layoutId;
            private int titleId;
            private int textId;
            private int callToActionId;
            private int mainImageId;
            private int iconImageId;
            private int privacyInformationIconImageId;
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
            public final MintegralViewBinder build() {
                return new MintegralViewBinder(this);
            }
        }

        final int layoutId;
        final int titleId;
        final int textId;
        final int callToActionId;
        final int mainImageId;
        final int iconImageId;
        final int privacyInformationIconImageId;
        final int adChoicesId;
        @NonNull
        final Map<String, Integer> extras;

        private MintegralViewBinder(@NonNull final Builder builder) {
            this.layoutId = builder.layoutId;
            this.titleId = builder.titleId;
            this.textId = builder.textId;
            this.callToActionId = builder.callToActionId;
            this.mainImageId = builder.mainImageId;
            this.iconImageId = builder.iconImageId;
            this.privacyInformationIconImageId = builder.privacyInformationIconImageId;
            this.adChoicesId = builder.adChoicesId;
            this.extras = builder.extras;
        }
    }
}

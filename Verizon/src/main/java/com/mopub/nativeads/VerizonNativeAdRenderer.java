package com.mopub.nativeads;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.VerizonAdapterConfiguration;
import com.verizon.ads.VideoPlayerView;
import com.verizon.ads.verizonnativecontroller.NativeViewComponent;
import com.verizon.ads.videoplayer.VerizonVideoPlayer;

import java.util.Map;
import java.util.WeakHashMap;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;

public class VerizonNativeAdRenderer implements MoPubAdRenderer<VerizonNative.VerizonStaticNativeAd> {

    // This is used instead of View.setTag, which causes a memory leak in 2.3
    // and earlier: https://code.google.com/p/android/issues/detail?id=18273
    @NonNull
    private final WeakHashMap<View, VerizonNativeViewHolder> viewHolderMap;
    @NonNull
    private final ViewBinder viewBinder;
    @Nullable
    private VerizonVideoPlayer verizonVideoPlayer;

    /**
     * Constructs a native ad renderer with a view binder.
     *
     * @param viewBinder The view binder to use when inflating and rendering an ad.
     */
    public VerizonNativeAdRenderer(@NonNull final ViewBinder viewBinder) {
        this.viewBinder = viewBinder;
        viewHolderMap = new WeakHashMap<>();
    }

    @NonNull
    @Override
    public View createAdView(@NonNull final Context context, @Nullable final ViewGroup parent) {
        return LayoutInflater
                .from(context)
                .inflate(viewBinder.layoutId, parent, false);
    }

    @Override
    public void renderAdView(@NonNull final View view,
                             @NonNull final VerizonNative.VerizonStaticNativeAd verizonStaticNativeAd) {
        Preconditions.checkNotNull(view);
        Preconditions.checkNotNull(verizonStaticNativeAd);

        VerizonNativeViewHolder verizonNativeViewHolder = viewHolderMap.get(view);
        if (verizonNativeViewHolder == null) {
            verizonNativeViewHolder = VerizonNativeViewHolder.fromViewBinder(view, viewBinder);
            viewHolderMap.put(view, verizonNativeViewHolder);
        }

        updateViews(verizonNativeViewHolder, verizonStaticNativeAd, view.getContext());
        updateVideoView(verizonNativeViewHolder, verizonStaticNativeAd.getExtras());
        verizonStaticNativeAd.getNativeAd().registerContainerView((ViewGroup) view);
        NativeRendererHelper.updateExtras(view, viewBinder.extras, verizonStaticNativeAd.getExtras());
    }

    @Override
    public boolean supports(@NonNull final BaseNativeAd nativeAd) {
        Preconditions.checkNotNull(nativeAd);
        return nativeAd instanceof VerizonNative.VerizonStaticNativeAd;
    }

    private void updateViews(@NonNull final VerizonNativeViewHolder verizonNativeViewHolder,
                             @NonNull final VerizonNative.VerizonStaticNativeAd nativeAd,
                             @NonNull final Context context) {
        Preconditions.checkNotNull(verizonNativeViewHolder);
        Preconditions.checkNotNull(nativeAd);

        NativeRendererHelper.addTextView(verizonNativeViewHolder.titleView, nativeAd.getTitle());
        ((NativeViewComponent) nativeAd.getNativeAd().getComponent(context, "title")).prepareView(verizonNativeViewHolder.titleView);
        NativeRendererHelper.addTextView(verizonNativeViewHolder.textView, nativeAd.getText());
        ((NativeViewComponent) nativeAd.getNativeAd().getComponent(context, "body")).prepareView(verizonNativeViewHolder.textView);
        NativeRendererHelper.addTextView(verizonNativeViewHolder.callToActionView, nativeAd.getCallToAction());
        ((NativeViewComponent) nativeAd.getNativeAd().getComponent(context, "callToAction")).prepareView(verizonNativeViewHolder.callToActionView);
        NativeRendererHelper.addTextView(verizonNativeViewHolder.sponsoredTextView, nativeAd.getSponsored());
        ((NativeViewComponent) nativeAd.getNativeAd().getComponent(context, "disclaimer")).prepareView(verizonNativeViewHolder.sponsoredTextView);

        NativeImageHelper.loadImageView(nativeAd.getMainImageUrl(), verizonNativeViewHolder.mainImageView);
        ((NativeViewComponent) nativeAd.getNativeAd().getComponent(context, "mainImage")).prepareView(verizonNativeViewHolder.mainImageView);
        NativeImageHelper.loadImageView(nativeAd.getIconImageUrl(), verizonNativeViewHolder.iconImageView);
        ((NativeViewComponent) nativeAd.getNativeAd().getComponent(context, "iconImage")).prepareView(verizonNativeViewHolder.iconImageView);
    }

    private void updateVideoView(@NonNull final VerizonNativeViewHolder verizonNativeViewHolder,
                                 @Nullable final Map<String, Object> extras) {
        try {
            Preconditions.checkNotNull(verizonNativeViewHolder);

            if (verizonVideoPlayer != null) {
                verizonVideoPlayer.unload(); //stops multiple videos from playing.
            }

            if (extras != null && verizonNativeViewHolder.videoView != null) {

                verizonVideoPlayer = new VerizonVideoPlayer(verizonNativeViewHolder.videoView.getContext());
                VideoPlayerView videoPlayerView = new VideoPlayerView(verizonNativeViewHolder.videoView.getContext());
                final FrameLayout.LayoutParams videoParams = new FrameLayout.LayoutParams(FrameLayout
                        .LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

                videoPlayerView.bindPlayer(verizonVideoPlayer);
                verizonNativeViewHolder.videoView.addView(videoPlayerView, videoParams);

                final String url = (String) extras.get(VerizonNative.COMP_ID_VIDEO);

                if (url != null) {
                    verizonNativeViewHolder.videoView.setVisibility(View.VISIBLE);
                    verizonVideoPlayer.load(url);

                    VerizonAdapterConfiguration.postOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            verizonVideoPlayer.play();
                        }
                    });
                } else {
                    verizonNativeViewHolder.videoView.setVisibility(View.GONE);
                }
            }
        } catch (Exception e) {
            MoPubLog.log(CUSTOM, "Unable to render view: " + e.getMessage());
        }
    }
}

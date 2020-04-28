package com.mopub.nativeads;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.VerizonAdapterConfiguration;
import com.verizon.ads.VideoPlayerView;
import com.verizon.ads.videoplayer.VerizonVideoPlayerView;

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
    private VideoPlayerView videoPlayerView;

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

        VerizonNativeViewHolder verizonNativeViewHolder = viewHolderMap.get(view);
        if (verizonNativeViewHolder == null) {
            verizonNativeViewHolder = VerizonNativeViewHolder.fromViewBinder(view, viewBinder);
            viewHolderMap.put(view, verizonNativeViewHolder);
        }

        updateViews(verizonNativeViewHolder, verizonStaticNativeAd);
        updateVideoView(verizonNativeViewHolder, verizonStaticNativeAd.getExtras());
        NativeRendererHelper.updateExtras(view, viewBinder.extras, verizonStaticNativeAd.getExtras());
    }

    @Override
    public boolean supports(@NonNull final BaseNativeAd nativeAd) {
        return nativeAd instanceof VerizonNative.VerizonStaticNativeAd;
    }

    private void updateViews(@NonNull final VerizonNativeViewHolder verizonNativeViewHolder,
                             @NonNull final VerizonNative.VerizonStaticNativeAd nativeAd) {

        NativeRendererHelper.addTextView(verizonNativeViewHolder.titleView, nativeAd.getTitle());
        NativeRendererHelper.addTextView(verizonNativeViewHolder.textView, nativeAd.getText());
        NativeRendererHelper.addTextView(verizonNativeViewHolder.callToActionView, nativeAd.getCallToAction());

        NativeImageHelper.loadImageView(nativeAd.getMainImageUrl(), verizonNativeViewHolder.mainImageView);
        NativeImageHelper.loadImageView(nativeAd.getIconImageUrl(), verizonNativeViewHolder.iconImageView);
    }

    private void updateVideoView(@NonNull final VerizonNativeViewHolder verizonNativeViewHolder,
                                 @Nullable final Map<String, Object> extras) {
        try {
            if (videoPlayerView != null) {
                videoPlayerView.unload(); //stops multiple videos from playing.
            }

            if (extras != null && verizonNativeViewHolder.videoView != null) {

                videoPlayerView = new VerizonVideoPlayerView(verizonNativeViewHolder.videoView.getContext());
                final FrameLayout.LayoutParams videoParams = new FrameLayout.LayoutParams(FrameLayout
                        .LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

                verizonNativeViewHolder.videoView.addView(videoPlayerView, videoParams);

                final String url = (String) extras.get(VerizonNative.COMP_ID_VIDEO);

                if (url != null) {
                    verizonNativeViewHolder.videoView.setVisibility(View.VISIBLE);
                    videoPlayerView.load(url);

                    VerizonAdapterConfiguration.postOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            videoPlayerView.play();
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

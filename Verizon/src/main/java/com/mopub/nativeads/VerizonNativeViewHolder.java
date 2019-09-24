package com.mopub.nativeads;

import androidx.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.mopub.common.logging.MoPubLog;
import com.verizon.ads.videoplayer.VideoView;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.ERROR;

class VerizonNativeViewHolder {
    @Nullable
    TextView titleView;
    @Nullable
    TextView textView;
    @Nullable
    TextView callToActionView;
    @Nullable
    VideoView videoView;
    @Nullable
    ImageView mainImageView;
    @Nullable
    ImageView iconImageView;

    // Use fromViewBinder instead of a constructor
    private VerizonNativeViewHolder() {
    }

    static VerizonNativeViewHolder fromViewBinder(@Nullable final View view, @Nullable final ViewBinder viewBinder) {

        final VerizonNativeViewHolder viewHolder = new VerizonNativeViewHolder();

        if (view == null || viewBinder == null) {
            return viewHolder;
        }

        try {
            viewHolder.titleView = view.findViewById(viewBinder.titleId);
            viewHolder.textView = view.findViewById(viewBinder.textId);
            viewHolder.callToActionView = view.findViewById(viewBinder.callToActionId);
            viewHolder.mainImageView = view.findViewById(viewBinder.mainImageId);
            viewHolder.iconImageView = view.findViewById(viewBinder.iconImageId);

            if (viewBinder.extras.get(VerizonNative.COMP_ID_VIDEO) != null) {
                viewHolder.videoView = view.findViewById(viewBinder.extras.get(VerizonNative.COMP_ID_VIDEO));
            }

            return viewHolder;
        } catch (Exception exception) {
            MoPubLog.log(ERROR, "Could not cast from id in ViewBinder to expected View type", exception);

            return new VerizonNativeViewHolder();
        }
    }
}

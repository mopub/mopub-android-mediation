package com.mopub.nativeads;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;

import android.widget.ImageView;
import android.widget.TextView;

import com.bytedance.sdk.openadsdk.adapter.MediaView;
import com.mopub.common.logging.MoPubLog;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;

/**
 * created by wuzejian on 2020/5/12
 */
public class PangleAdNativeViewHolder {

    @Nullable
    public TextView titleView;
    @Nullable
    public TextView description;
    @Nullable
    public ImageView icon;
    @Nullable
    public TextView advertiserNameView;
    @Nullable
    public TextView callToActionView;

    public ImageView logoView;

    /**
     * video View
     */
    public MediaView mediaView;


    private static PangleAdNativeViewHolder EMPTY_VIEW_HOLDER = new PangleAdNativeViewHolder();

    private PangleAdNativeViewHolder() {
    }

    static PangleAdNativeViewHolder fromViewBinder(@NonNull final View view,
                                                   @NonNull final PangleAdViewBinder pangleAdViewBinder) {
        final PangleAdNativeViewHolder adViewHolder = new PangleAdNativeViewHolder();
        try {

            /** common ui */
            adViewHolder.titleView = view.findViewById(pangleAdViewBinder.titleId);
            adViewHolder.description = view.findViewById(pangleAdViewBinder.descriptionTextId);
            adViewHolder.callToActionView = view.findViewById(pangleAdViewBinder.callToActionId);
            adViewHolder.advertiserNameView = view.findViewById(pangleAdViewBinder.sourceId);
            adViewHolder.icon = view.findViewById(pangleAdViewBinder.iconImageId);
            adViewHolder.logoView = view.findViewById(pangleAdViewBinder.logoViewId);
            adViewHolder.mediaView = view.findViewById(pangleAdViewBinder.mediaViewId);
            return adViewHolder;
        } catch (ClassCastException exception) {
            MoPubLog.log(CUSTOM, "Could not cast from id in pangleAdViewBinder to expected View type",
                    exception);
            return EMPTY_VIEW_HOLDER;
        }
    }


}

package com.mopub.nativeads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.mopub.common.logging.MoPubLog;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;
/**
 * created by wuzejian on 2020/5/12
 */
public class PangolinNativeViewHolder {

    @Nullable
    public TextView titleView;
    @Nullable
    public TextView description;
    @Nullable
    public ImageView icon;
    @Nullable
    public ImageView dislike;
    @Nullable
    public TextView advertiserNameView;
    @Nullable
    public TextView callToActionView;

    public ImageView logoView;

    //video
    public FrameLayout mediaView;

    //main Image
    public ImageView mLargeImage;

    //small Image
    public ImageView mSmallImage;

    //vertical Image
    public ImageView mVerticalImage;

    //group Image
    public ImageView mGroupImage1;
    public ImageView mGroupImage2;
    public ImageView mGroupImage3;

    private static PangolinNativeViewHolder EMPTY_VIEW_HOLDER = new PangolinNativeViewHolder();

    private PangolinNativeViewHolder() {
    }

    static PangolinNativeViewHolder fromViewBinder(@NonNull final View view,
                                                   @NonNull final PangolinViewBinder pangolinViewBinder) {
        final PangolinNativeViewHolder adViewHolder = new PangolinNativeViewHolder();
        try {

            //common ui
            adViewHolder.titleView = view.findViewById(pangolinViewBinder.titleId);
            adViewHolder.description = view.findViewById(pangolinViewBinder.descriptionTextId);
            adViewHolder.callToActionView = view.findViewById(pangolinViewBinder.callToActionId);
            adViewHolder.dislike = view.findViewById(pangolinViewBinder.dislikeId);
            adViewHolder.advertiserNameView = view.findViewById(pangolinViewBinder.sourceId);
            adViewHolder.icon = view.findViewById(pangolinViewBinder.iconImageId);
            adViewHolder.logoView = view.findViewById(pangolinViewBinder.logoViewId);

            if (pangolinViewBinder.layoutType == PangolinViewBinder.IMAGE_MODE_LARGE_IMG) {
                adViewHolder.mLargeImage = view.findViewById(pangolinViewBinder.mainImageId);
            } else if (pangolinViewBinder.layoutType == PangolinViewBinder.IMAGE_MODE_VIDEO) {
                adViewHolder.mediaView = view.findViewById(pangolinViewBinder.mediaViewId);
            } else if (pangolinViewBinder.layoutType == PangolinViewBinder.IMAGE_MODE_GROUP_IMG) {
                adViewHolder.mGroupImage1 = view.findViewById(pangolinViewBinder.groupImage1Id);
                adViewHolder.mGroupImage2 = view.findViewById(pangolinViewBinder.groupImage2Id);
                adViewHolder.mGroupImage3 = view.findViewById(pangolinViewBinder.groupImage3Id);
            } else if (pangolinViewBinder.layoutType == PangolinViewBinder.IMAGE_MODE_SMALL_IMG) {
                adViewHolder.mSmallImage = view.findViewById(pangolinViewBinder.smallImageId);
            } else if (pangolinViewBinder.layoutType == PangolinViewBinder.IMAGE_MODE_VERTICAL_IMG) {
                adViewHolder.mVerticalImage = view.findViewById(pangolinViewBinder.verticalImageId);
            }

            return adViewHolder;
        } catch (ClassCastException exception) {
            MoPubLog.log(CUSTOM, "Could not cast from id in pangolinViewBinder to expected View type",
                    exception);
            return EMPTY_VIEW_HOLDER;
        }
    }


}

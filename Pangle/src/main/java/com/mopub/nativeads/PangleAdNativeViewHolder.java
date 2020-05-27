package com.mopub.nativeads;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
    public ImageView dislike;
    @Nullable
    public TextView advertiserNameView;
    @Nullable
    public TextView callToActionView;

    public ImageView logoView;

    /**video View */
    public FrameLayout mediaView;

    /** main Image */
    public ImageView mLargeImage;

    /** small Image */
    public ImageView mSmallImage;

    /** vertical Image */
    public ImageView mVerticalImage;

    /** group Image */
    public ImageView mGroupImage1;
    public ImageView mGroupImage2;
    public ImageView mGroupImage3;

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
            adViewHolder.dislike = view.findViewById(pangleAdViewBinder.dislikeId);
            adViewHolder.advertiserNameView = view.findViewById(pangleAdViewBinder.sourceId);
            adViewHolder.icon = view.findViewById(pangleAdViewBinder.iconImageId);
            adViewHolder.logoView = view.findViewById(pangleAdViewBinder.logoViewId);

            if (pangleAdViewBinder.layoutType == PangleAdViewBinder.IMAGE_MODE_LARGE_IMG) {
                adViewHolder.mLargeImage = view.findViewById(pangleAdViewBinder.mainImageId);
            } else if (pangleAdViewBinder.layoutType == PangleAdViewBinder.IMAGE_MODE_VIDEO) {
                adViewHolder.mediaView = view.findViewById(pangleAdViewBinder.mediaViewId);
            } else if (pangleAdViewBinder.layoutType == PangleAdViewBinder.IMAGE_MODE_GROUP_IMG) {
                adViewHolder.mGroupImage1 = view.findViewById(pangleAdViewBinder.groupImage1Id);
                adViewHolder.mGroupImage2 = view.findViewById(pangleAdViewBinder.groupImage2Id);
                adViewHolder.mGroupImage3 = view.findViewById(pangleAdViewBinder.groupImage3Id);
            } else if (pangleAdViewBinder.layoutType == PangleAdViewBinder.IMAGE_MODE_SMALL_IMG) {
                adViewHolder.mSmallImage = view.findViewById(pangleAdViewBinder.smallImageId);
            } else if (pangleAdViewBinder.layoutType == PangleAdViewBinder.IMAGE_MODE_VERTICAL_IMG) {
                adViewHolder.mVerticalImage = view.findViewById(pangleAdViewBinder.verticalImageId);
            }

            return adViewHolder;
        } catch (ClassCastException exception) {
            MoPubLog.log(CUSTOM, "Could not cast from id in pangleAdViewBinder to expected View type",
                    exception);
            return EMPTY_VIEW_HOLDER;
        }
    }


}

package com.mopub.nativeads;


import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bytedance.sdk.openadsdk.TTImage;
import com.bytedance.sdk.openadsdk.TTNativeAd;
import com.mopub.common.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/**
 * created by wuzejian on 2020/5/12
 */
public class PangolinAdRenderer implements MoPubAdRenderer<PangolinNativeAdapter.PangolinNativeAd> {

    private final PangolinViewBinder mViewBinder;

    private final WeakHashMap<View, PangolinNativeViewHolder> mViewHolderMap;

    public PangolinAdRenderer(PangolinViewBinder viewBinder) {
        this.mViewBinder = viewBinder;
        this.mViewHolderMap = new WeakHashMap();
    }

    /**
     * 这个是创建的listView Item 布局
     *
     * @param context
     * @param parent
     * @return
     */
    @Override
    public View createAdView(Context context, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(this.mViewBinder.layoutId, parent, false);
    }

    /**
     * listView 会调用该方法渲染广告view，需要在该方法中处理广告数据的显示操作
     *
     * @param view
     * @param ad
     */
    @Override
    public void renderAdView(View view, PangolinNativeAdapter.PangolinNativeAd ad) {
        PangolinNativeViewHolder pangolinNativeViewHolder = mViewHolderMap.get(view);
        if (pangolinNativeViewHolder == null) {
            pangolinNativeViewHolder = PangolinNativeViewHolder.fromViewBinder(view, mViewBinder);
            mViewHolderMap.put(view, pangolinNativeViewHolder);
        }
        this.updateAdUI(pangolinNativeViewHolder, ad, view);
    }

    private void updateAdUI(PangolinNativeViewHolder pangolinNativeViewHolder, final PangolinNativeAdapter.PangolinNativeAd ad, View convertView) {
        if (ad == null || convertView == null) return;

        if (!TextUtils.isEmpty(ad.getTitle()) && pangolinNativeViewHolder.titleView != null) {
            pangolinNativeViewHolder.titleView.setText(ad.getTitle());
        }

        if (!TextUtils.isEmpty(ad.getAdvertiserName()) && pangolinNativeViewHolder.advertiserNameView != null) {
            pangolinNativeViewHolder.advertiserNameView.setText(ad.getAdvertiserName());
        }

        if (!TextUtils.isEmpty(ad.getDecriptionText()) && pangolinNativeViewHolder.description != null) {
            pangolinNativeViewHolder.description.setText(ad.getDecriptionText());
        }

        if (!TextUtils.isEmpty(ad.getCallToAction()) && pangolinNativeViewHolder.callToActionView != null) {
            pangolinNativeViewHolder.callToActionView.setText(ad.getCallToAction());
        }

        if (ad.getIcon() != null && !TextUtils.isEmpty(ad.getIcon().getImageUrl()) && pangolinNativeViewHolder.icon != null) {
            NativeImageHelper.loadImageView(ad.getIcon().getImageUrl(), pangolinNativeViewHolder.icon);
        }

        if (ad.getAdLogo() != null && pangolinNativeViewHolder.logoView != null) {
            pangolinNativeViewHolder.logoView.setImageBitmap(ad.getAdLogo());
        }

        if (mViewBinder.layoutType == PangolinViewBinder.IMAGE_MODE_LARGE_IMG) {//  big image
            if (ad.getImageList() != null && !ad.getImageList().isEmpty()) {
                TTImage image = ad.getImageList().get(0);
                if (image != null && image.isValid()) {
                    NativeImageHelper.loadImageView(image.getImageUrl(), pangolinNativeViewHolder.mLargeImage);
                }
            }
        } else if (mViewBinder.layoutType == PangolinViewBinder.IMAGE_MODE_VIDEO) { // video
            if (pangolinNativeViewHolder.mediaView != null) {
                View video = ad.getAdView();
                if (video != null) {
                    if (video.getParent() == null) {
                        pangolinNativeViewHolder.mediaView.removeAllViews();
                        pangolinNativeViewHolder.mediaView.addView(video, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    }
                }
            }
        } else if (mViewBinder.layoutType == PangolinViewBinder.IMAGE_MODE_GROUP_IMG) {// 3 Image
            if (ad.getImageList() != null && ad.getImageList().size() >= 3) {
                TTImage image1 = ad.getImageList().get(0);
                TTImage image2 = ad.getImageList().get(1);
                TTImage image3 = ad.getImageList().get(2);

                if (image1 != null && image1.isValid()) {
                    NativeImageHelper.loadImageView(image1.getImageUrl(), pangolinNativeViewHolder.mGroupImage1);
                }

                if (image2 != null && image2.isValid()) {
                    NativeImageHelper.loadImageView(image2.getImageUrl(), pangolinNativeViewHolder.mGroupImage2);
                }

                if (image3 != null && image3.isValid()) {
                    NativeImageHelper.loadImageView(image3.getImageUrl(), pangolinNativeViewHolder.mGroupImage3);
                }
            }

        } else if (mViewBinder.layoutType == PangolinViewBinder.IMAGE_MODE_SMALL_IMG) {// small image
            if (ad.getImageList() != null && ad.getImageList().size() > 0) {
                TTImage image = ad.getImageList().get(0);
                if (image != null && image.isValid()) {
                    NativeImageHelper.loadImageView(image.getImageUrl(), pangolinNativeViewHolder.mSmallImage);
                }
            }
        } else if (mViewBinder.layoutType == PangolinViewBinder.IMAGE_MODE_VERTICAL_IMG) {// vertical image
            if (ad.getImageList() != null && ad.getImageList().size() > 0) {
                TTImage image = ad.getImageList().get(0);
                if (image != null && image.isValid()) {
                    NativeImageHelper.loadImageView(image.getImageUrl(), pangolinNativeViewHolder.mVerticalImage);
                }
            }
        }


        //the views that can be clicked
        List<View> clickViewList = new ArrayList<>();
        clickViewList.add(convertView);
        //The views that can trigger the creative action (like download app)
        List<View> creativeViewList = new ArrayList<>();
        creativeViewList.add(pangolinNativeViewHolder.callToActionView);
        //notice! This involves advertising billing and must be called correctly. convertView must use ViewGroup.
        ad.registerViewForInteraction((ViewGroup) convertView, clickViewList, creativeViewList, new TTNativeAd.AdInteractionListener() {
            @Override
            public void onAdClicked(View view, TTNativeAd pangolinAd) {
                ad.onAdClicked(view, pangolinAd);
            }

            @Override
            public void onAdCreativeClick(View view, TTNativeAd pangolinAd) {
                ad.onAdCreativeClick(view, pangolinAd);

            }

            @Override
            public void onAdShow(TTNativeAd pangolinAd) {
                ad.onAdShow(pangolinAd);

            }
        });


    }


    @Override
    public boolean supports(BaseNativeAd nativeAd) {
        Preconditions.checkNotNull(nativeAd);
        return nativeAd instanceof PangolinNativeAdapter.PangolinNativeAd;
    }


}

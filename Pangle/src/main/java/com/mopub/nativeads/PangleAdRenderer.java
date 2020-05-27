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
public class PangleAdRenderer implements MoPubAdRenderer<PangleAdNative.PangolinNativeAd> {

    private final PangleAdViewBinder mViewBinder;

    private final WeakHashMap<View, PangleAdNativeViewHolder> mViewHolderMap;

    public PangleAdRenderer(PangleAdViewBinder viewBinder) {
        this.mViewBinder = viewBinder;
        this.mViewHolderMap = new WeakHashMap();
    }

    /**
     * create listView Item layout
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
     * listView item layout render
     *
     * @param view
     * @param ad
     */
    @Override
    public void renderAdView(View view, PangleAdNative.PangolinNativeAd ad) {
        PangleAdNativeViewHolder pangleAdNativeViewHolder = mViewHolderMap.get(view);
        if (pangleAdNativeViewHolder == null) {
            pangleAdNativeViewHolder = PangleAdNativeViewHolder.fromViewBinder(view, mViewBinder);
            mViewHolderMap.put(view, pangleAdNativeViewHolder);
        }
        this.updateAdUI(pangleAdNativeViewHolder, ad, view);
    }

    private void updateAdUI(PangleAdNativeViewHolder pangleAdNativeViewHolder, final PangleAdNative.PangolinNativeAd ad, View convertView) {
        if (ad == null || convertView == null) return;

        if (pangleAdNativeViewHolder.dislike != null) {
            pangleAdNativeViewHolder.dislike.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(ad.getTitle()) && pangleAdNativeViewHolder.titleView != null) {
            pangleAdNativeViewHolder.titleView.setText(ad.getTitle());
        }

        if (!TextUtils.isEmpty(ad.getAdvertiserName()) && pangleAdNativeViewHolder.advertiserNameView != null) {
            pangleAdNativeViewHolder.advertiserNameView.setText(ad.getAdvertiserName());
        }

        if (!TextUtils.isEmpty(ad.getDecriptionText()) && pangleAdNativeViewHolder.description != null) {
            pangleAdNativeViewHolder.description.setText(ad.getDecriptionText());
        }

        if (!TextUtils.isEmpty(ad.getCallToAction()) && pangleAdNativeViewHolder.callToActionView != null) {
            pangleAdNativeViewHolder.callToActionView.setText(ad.getCallToAction());
        }

        if (ad.getIcon() != null && !TextUtils.isEmpty(ad.getIcon().getImageUrl()) && pangleAdNativeViewHolder.icon != null) {
            NativeImageHelper.loadImageView(ad.getIcon().getImageUrl(), pangleAdNativeViewHolder.icon);
        }

        if (ad.getAdLogo() != null && pangleAdNativeViewHolder.logoView != null) {
            pangleAdNativeViewHolder.logoView.setImageBitmap(ad.getAdLogo());
        }

        if (mViewBinder.layoutType == PangleAdViewBinder.IMAGE_MODE_LARGE_IMG) {//  big image
            if (ad.getImageList() != null && !ad.getImageList().isEmpty()) {
                TTImage image = ad.getImageList().get(0);
                if (image != null && image.isValid()) {
                    NativeImageHelper.loadImageView(image.getImageUrl(), pangleAdNativeViewHolder.mLargeImage);
                }
            }
        } else if (mViewBinder.layoutType == PangleAdViewBinder.IMAGE_MODE_VIDEO) { // video
            if (pangleAdNativeViewHolder.mediaView != null) {
                View video = ad.getAdView();
                if (video != null) {
                    if (video.getParent() == null) {
                        pangleAdNativeViewHolder.mediaView.removeAllViews();
                        pangleAdNativeViewHolder.mediaView.addView(video, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    }
                }
            }
        } else if (mViewBinder.layoutType == PangleAdViewBinder.IMAGE_MODE_GROUP_IMG) {// 3 Image
            if (ad.getImageList() != null && ad.getImageList().size() >= 3) {
                TTImage image1 = ad.getImageList().get(0);
                TTImage image2 = ad.getImageList().get(1);
                TTImage image3 = ad.getImageList().get(2);

                if (image1 != null && image1.isValid()) {
                    NativeImageHelper.loadImageView(image1.getImageUrl(), pangleAdNativeViewHolder.mGroupImage1);
                }

                if (image2 != null && image2.isValid()) {
                    NativeImageHelper.loadImageView(image2.getImageUrl(), pangleAdNativeViewHolder.mGroupImage2);
                }

                if (image3 != null && image3.isValid()) {
                    NativeImageHelper.loadImageView(image3.getImageUrl(), pangleAdNativeViewHolder.mGroupImage3);
                }
            }

        } else if (mViewBinder.layoutType == PangleAdViewBinder.IMAGE_MODE_SMALL_IMG) {/** small image */
            if (ad.getImageList() != null && ad.getImageList().size() > 0) {
                TTImage image = ad.getImageList().get(0);
                if (image != null && image.isValid()) {
                    NativeImageHelper.loadImageView(image.getImageUrl(), pangleAdNativeViewHolder.mSmallImage);
                }
            }
        } else if (mViewBinder.layoutType == PangleAdViewBinder.IMAGE_MODE_VERTICAL_IMG) {/** vertical image */
            if (ad.getImageList() != null && ad.getImageList().size() > 0) {
                TTImage image = ad.getImageList().get(0);
                if (image != null && image.isValid()) {
                    NativeImageHelper.loadImageView(image.getImageUrl(), pangleAdNativeViewHolder.mVerticalImage);
                }
            }
        }


        /**  the views that can be clicked */
        List<View> clickViewList = new ArrayList<>();
        clickViewList.add(convertView);
        /** The views that can trigger the creative action (like download app) */
        List<View> creativeViewList = new ArrayList<>();
        if (pangleAdNativeViewHolder.callToActionView != null) {
            creativeViewList.add(pangleAdNativeViewHolder.callToActionView);
        }
        /**  notice! This involves advertising billing and must be called correctly. convertView must use ViewGroup. */
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
        return nativeAd instanceof PangleAdNative.PangolinNativeAd;
    }


}

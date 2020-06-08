package com.mopub.nativeads;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bytedance.sdk.openadsdk.TTNativeAd;
import com.bytedance.sdk.openadsdk.adapter.MediationAdapterUtil;
import com.mopub.common.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

public class PangleAdRenderer implements MoPubAdRenderer<PangleAdNative.PangolinNativeAd> {

    private final PangleAdViewBinder mViewBinder;

    private final WeakHashMap<View, PangleAdNativeViewHolder> mViewHolderMap;

    public PangleAdRenderer(PangleAdViewBinder viewBinder) {
        this.mViewBinder = viewBinder;
        this.mViewHolderMap = new WeakHashMap();
    }

    @Override
    public View createAdView(Context context, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(this.mViewBinder.mLayoutId, parent, false);
    }

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
        if (ad == null || convertView == null) {
            return;
        }

        if (!TextUtils.isEmpty(ad.getTitle()) && pangleAdNativeViewHolder.mTitleView != null) {
            pangleAdNativeViewHolder.mTitleView.setText(ad.getTitle());
        }

        if (!TextUtils.isEmpty(ad.getAdvertiserName()) && pangleAdNativeViewHolder.mAdvertiserNameView != null) {
            pangleAdNativeViewHolder.mAdvertiserNameView.setText(ad.getAdvertiserName());
        }

        if (!TextUtils.isEmpty(ad.getDescriptionText()) && pangleAdNativeViewHolder.mDescription != null) {
            pangleAdNativeViewHolder.mDescription.setText(ad.getDescriptionText());
        }

        if (!TextUtils.isEmpty(ad.getCallToAction()) && pangleAdNativeViewHolder.mCallToActionView != null) {
            pangleAdNativeViewHolder.mCallToActionView.setText(ad.getCallToAction());
        }

        if (ad.getIcon() != null && !TextUtils.isEmpty(ad.getIcon().getImageUrl()) && pangleAdNativeViewHolder.mIcon != null) {
            NativeImageHelper.loadImageView(ad.getIcon().getImageUrl(), pangleAdNativeViewHolder.mIcon);
        }

        if (ad.getAdLogo() != null && pangleAdNativeViewHolder.mLogoView != null) {
            pangleAdNativeViewHolder.mLogoView.setImageBitmap(ad.getAdLogo());
            pangleAdNativeViewHolder.mLogoView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ad.showPrivacyActivity();
                }
            });
        }

        /** Add Native Feed Main View */
        MediationAdapterUtil.addNativeFeedMainView(convertView.getContext(), ad.getImageMode(), pangleAdNativeViewHolder.mMediaView, ad.getAdView(), ad.getImageList());

        /** The views that can be clicked */
        List<View> clickViewList = new ArrayList<>();
        clickViewList.add(convertView);

        /** The views that can trigger the creative action (like download app) */
        List<View> creativeViewList = new ArrayList<>();
        if (pangleAdNativeViewHolder.mCallToActionView != null) {
            creativeViewList.add(pangleAdNativeViewHolder.mCallToActionView);
        }

        /** Notice! This involves advertising billing and must be called correctly. convertView must use ViewGroup. */
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

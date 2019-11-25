package com.mopub.nativeads;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdBase;
import com.facebook.ads.NativeAdView;
import com.facebook.ads.NativeAdViewAttributes;
import com.mopub.common.Preconditions;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class FacebookTemplateRenderer implements MoPubAdRenderer<FacebookNative.FacebookNativeAd> {

    @Nullable
    private NativeAdViewAttributes mTemplateAttributes;

    public FacebookTemplateRenderer(@Nullable NativeAdViewAttributes attributes) {
        mTemplateAttributes = attributes;
    }

    @NonNull
    @Override
    public View createAdView(@NonNull Context context, @Nullable ViewGroup parent) {
        return new FrameLayout(context);
    }

    @Override
    public void renderAdView(@NonNull View parentView, @NonNull FacebookNative.FacebookNativeAd ad) {
        Preconditions.checkNotNull(parentView);
        Preconditions.checkNotNull(ad);

        NativeAdBase nativeAdBase = ad.getFacebookNativeAd();

        if (nativeAdBase instanceof NativeAd) {
            View adView = NativeAdView.render(parentView.getContext(), (NativeAd) nativeAdBase, mTemplateAttributes);

            FrameLayout.LayoutParams adViewParams = new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            ((FrameLayout) parentView).addView(adView, adViewParams);
        }
    }

    @Override
    public boolean supports(@NonNull BaseNativeAd nativeAd) {
        Preconditions.checkNotNull(nativeAd);
        return nativeAd instanceof FacebookNative.FacebookNativeAd;
    }
}

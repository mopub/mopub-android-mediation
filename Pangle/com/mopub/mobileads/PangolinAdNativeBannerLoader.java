package com.mopub.mobileads;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTImage;
import com.bytedance.sdk.openadsdk.TTNativeAd;
import com.mopub.common.logging.MoPubLog;
import com.mopub.nativeads.NativeImageHelper;
import com.union_test.toutiao.R;
import com.union_test.toutiao.utils.TToast;

import java.util.ArrayList;
import java.util.List;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.mobileads.ErrorCode.mapErrorCode;
import static com.mopub.mobileads.PangolinAudienceAdBannerAdapter.ADAPTER_NAME;

/**
 * created by wuzejian on 2020/5/11
 * native banner
 */
public class PangolinAdNativeBannerLoader {

    private Context mContext;

    private CustomEventBanner.CustomEventBannerListener mCustomEventBannerListener;


    PangolinAdNativeBannerLoader(Context context, CustomEventBanner.CustomEventBannerListener customEventBannerListener) {
        this.mCustomEventBannerListener = customEventBannerListener;
        this.mContext = context;
    }


    public void loadAdNativeBanner(AdSlot adSlot, TTAdNative ttAdNative) {
        if (mContext == null || adSlot == null || ttAdNative == null || TextUtils.isEmpty(adSlot.getCodeId()))
            return;
        //request ad
        ttAdNative.loadNativeAd(adSlot, mNativeAdListener);

    }

    private TTAdNative.NativeAdListener mNativeAdListener = new TTAdNative.NativeAdListener() {
        @Override
        public void onError(int code, String message) {
            Log.e(ADAPTER_NAME, "native banner ad  onBannerFailed.-code=" + code + "," + message);
            if (mCustomEventBannerListener != null) {
                mCustomEventBannerListener.onBannerFailed(mapErrorCode(code));
            }
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);
        }

        @Override
        public void onNativeAdLoad(List<TTNativeAd> ads) {
            if (ads.get(0) == null) {
                return;
            }

            View bannerView = LayoutInflater.from(mContext).inflate(R.layout.native_ad, null, false);
            if (bannerView == null) {
                return;
            }

            //bind ad data and interaction
            setAdData(bannerView, ads.get(0));

            if (mCustomEventBannerListener != null) {
                //load success add view to mMoPubView
                mCustomEventBannerListener.onBannerLoaded(bannerView);
            }

        }
    };

    private void setAdData(View nativeView, TTNativeAd nativeAd) {
        ((TextView) nativeView.findViewById(R.id.tv_native_ad_title)).setText(nativeAd.getTitle());
        ((TextView) nativeView.findViewById(R.id.tv_native_ad_desc)).setText(nativeAd.getDescription());
        ImageView imgDislike = nativeView.findViewById(R.id.img_native_dislike);
        bindDislikeAction(nativeAd, imgDislike);
        if (nativeAd.getImageList() != null && !nativeAd.getImageList().isEmpty()) {
            TTImage image = nativeAd.getImageList().get(0);
            if (image != null && image.isValid()) {
                ImageView im = nativeView.findViewById(R.id.iv_native_image);
                NativeImageHelper.loadImageView(image.getImageUrl(), im);
            }
        }
        TTImage icon = nativeAd.getIcon();
        if (icon != null && icon.isValid()) {
            ImageView im = nativeView.findViewById(R.id.iv_native_icon);
            NativeImageHelper.loadImageView(icon.getImageUrl(), im);
        }
        Button mCreativeButton = (Button) nativeView.findViewById(R.id.btn_native_creative);
        switch (nativeAd.getInteractionType()) {
            case TTAdConstant.INTERACTION_TYPE_DOWNLOAD:
                mCreativeButton.setVisibility(View.VISIBLE);
                mCreativeButton.setText(mContext.getString(R.string.tt_native_banner_download));
                break;
            case TTAdConstant.INTERACTION_TYPE_DIAL:
                mCreativeButton.setVisibility(View.VISIBLE);
                mCreativeButton.setText(mContext.getString(R.string.tt_native_banner_call));
                break;
            case TTAdConstant.INTERACTION_TYPE_LANDING_PAGE:
            case TTAdConstant.INTERACTION_TYPE_BROWSER:
                mCreativeButton.setVisibility(View.VISIBLE);
                mCreativeButton.setText(mContext.getString(R.string.tt_native_banner_view));
                break;
            default:
                mCreativeButton.setVisibility(View.GONE);
                TToast.show(mContext, "error");
        }

        //the views that can be clicked
        List<View> clickViewList = new ArrayList<>();
        clickViewList.add(nativeView);

        //The views that can trigger the creative action (like download app)
        List<View> creativeViewList = new ArrayList<>();
        creativeViewList.add(mCreativeButton);

        //notice! This involves advertising billing and must be called correctly. convertView must use ViewGroup.
        nativeAd.registerViewForInteraction((ViewGroup) nativeView, clickViewList, creativeViewList, imgDislike, mAdInteractionListener);

    }

    //todo：set dislike how can get Activity Instance from mopub ？？？？？
    private void bindDislikeAction(TTNativeAd ad, View dislikeView) {
//        final TTAdDislike ttAdDislike = ad.getDislikeDialog(mContext);
//        if (ttAdDislike != null) {
//            ttAdDislike.setDislikeInteractionCallback(new TTAdDislike.DislikeInteractionCallback() {
//                @Override
//                public void onSelected(int position, String value) {
//
//                }
//
//                @Override
//                public void onCancel() {
//
//                }
//            });
//        }
//        dislikeView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (ttAdDislike != null)
//                    ttAdDislike.showDislikeDialog();
//            }
//        });
    }


    TTNativeAd.AdInteractionListener mAdInteractionListener = new TTNativeAd.AdInteractionListener() {
        @Override
        public void onAdClicked(View view, TTNativeAd ad) {
            Log.e(ADAPTER_NAME, "banner native Ad clicked");
            if (mCustomEventBannerListener != null) {
                mCustomEventBannerListener.onBannerClicked();
            }
        }

        @Override
        public void onAdCreativeClick(View view, TTNativeAd ad) {
            Log.e(ADAPTER_NAME, "banner native Ad clicked");
            if (mCustomEventBannerListener != null) {
                mCustomEventBannerListener.onBannerClicked();
            }
        }

        @Override
        public void onAdShow(TTNativeAd ad) {
            Log.e(ADAPTER_NAME, "banner native Ad showed");

            if (mCustomEventBannerListener != null) {
                mCustomEventBannerListener.onBannerImpression();
            }
        }
    };

    public void destroy() {
        mContext = null;
        mCustomEventBannerListener = null;
        mNativeAdListener = null;
    }

}

package com.mopub.mobileads;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdDislike;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTNativeExpressAd;
import com.mopub.common.logging.MoPubLog;
import com.union_test.toutiao.config.TTAdManagerHolder;
import com.union_test.toutiao.utils.TToast;

import java.util.List;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.mobileads.ErrorCode.mapErrorCode;
import static com.mopub.mobileads.PangolinAudienceAdBannerAdapter.ADAPTER_NAME;

/**
 * created by wuzejian on 2020/5/11
 */
public class PangolinAdExpressBannerLoader {

    private TTNativeExpressAd mTTNativeExpressAd;

    private Context mContext;

    private CustomEventBanner.CustomEventBannerListener mCustomEventBannerListener;


    PangolinAdExpressBannerLoader(Context context, CustomEventBanner.CustomEventBannerListener customEventBannerListener) {
        this.mCustomEventBannerListener = customEventBannerListener;
        this.mContext = context;
    }

    /**
     * load ad
     *
     * @param adSlot
     */
    public void loadAdExpressBanner(AdSlot adSlot, TTAdNative ttAdNative) {
        if (mContext == null || adSlot == null || ttAdNative == null || TextUtils.isEmpty(adSlot.getCodeId()))
            return;
        ttAdNative.loadBannerExpressAd(adSlot, mTTNativeExpressAdListener);
    }


    /**
     * banner 广告加载回调监听
     */
    private TTAdNative.NativeExpressAdListener mTTNativeExpressAdListener = new TTAdNative.NativeExpressAdListener() {
        @SuppressLint("LongLogTag")
        @Override
        public void onError(int code, String message) {
            Log.e(ADAPTER_NAME, "express banner ad  onBannerFailed.-code=" + code + "," + message);
            if (mCustomEventBannerListener != null) {
                mCustomEventBannerListener.onBannerFailed(mapErrorCode(code));
            }
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);
        }

        @Override
        public void onNativeExpressAdLoad(List<TTNativeExpressAd> ads) {
            if (ads == null || ads.size() == 0) {
                return;
            }
            mTTNativeExpressAd = ads.get(0);
            mTTNativeExpressAd.setSlideIntervalTime(30 * 1000);
            mTTNativeExpressAd.setExpressInteractionListener(mExpressAdInteractionListener);
            bindDislike(mTTNativeExpressAd);
            mTTNativeExpressAd.render();
        }
    };

    private void bindDislike(TTNativeExpressAd ad) {
        //dislike function, maybe you can use custom dialog, please refer to the access document by yourself
        if (mContext instanceof Activity) {
            ad.setDislikeCallback((Activity) mContext, new TTAdDislike.DislikeInteractionCallback() {
                @Override
                public void onSelected(int position, String value) {
                    TToast.show(mContext, "click " + value);
                }

                @Override
                public void onCancel() {
                    TToast.show(mContext, "Cancel click ");
                }
            });
        }
    }

    /**
     * banner 渲染回调监听
     */
    private TTNativeExpressAd.ExpressAdInteractionListener mExpressAdInteractionListener = new TTNativeExpressAd.ExpressAdInteractionListener() {
        @Override
        public void onAdClicked(View view, int type) {
            if (mCustomEventBannerListener != null) {
                mCustomEventBannerListener.onBannerClicked();
            }
        }

        @Override
        public void onAdShow(View view, int type) {
            if (mCustomEventBannerListener != null) {
                mCustomEventBannerListener.onBannerImpression();
            }
        }

        @Override
        public void onRenderFail(View view, String msg, int code) {
            TToast.show(mContext, "banner ad onRenderFail msg = " + msg + "，code=" + code);
            if (mCustomEventBannerListener != null) {
                mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.RENDER_PROCESS_GONE_UNSPECIFIED);
            }
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.RENDER_PROCESS_GONE_UNSPECIFIED.getIntCode(),
                    MoPubErrorCode.RENDER_PROCESS_GONE_UNSPECIFIED);
        }

        @Override
        public void onRenderSuccess(View view, float width, float height) {
            if (mCustomEventBannerListener != null) {
                //render success add view to mMoPubView
                TToast.show(mContext, "banner ad onRenderSuccess ");
                mCustomEventBannerListener.onBannerLoaded(view);
            }
        }
    };

    public void destroy() {
        if (mTTNativeExpressAd != null) {
            mTTNativeExpressAd.destroy();
            mTTNativeExpressAd = null;
        }

        this.mContext = null;
        this.mCustomEventBannerListener = null;
        this.mExpressAdInteractionListener = null;
        this.mTTNativeExpressAdListener = null;
    }
}

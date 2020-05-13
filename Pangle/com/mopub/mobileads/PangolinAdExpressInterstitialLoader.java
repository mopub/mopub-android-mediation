package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTNativeExpressAd;
import com.mopub.common.logging.MoPubLog;
import com.union_test.toutiao.utils.TToast;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.mobileads.PangolinAudienceAdAdapterInterstitial.ADAPTER_NAME;

/**
 * created by wuzejian on 2020/5/11
 */
public class PangolinAdExpressInterstitialLoader {
    private Context mContext;
    private CustomEventInterstitial.CustomEventInterstitialListener mInterstitialListener;
    private TTNativeExpressAd mTTInterstitialExpressAd;
    private AtomicBoolean isRenderLoaded = new AtomicBoolean(false);


    PangolinAdExpressInterstitialLoader(Context context, CustomEventInterstitial.CustomEventInterstitialListener interstitialListener) {
        this.mContext = context;
        this.mInterstitialListener = interstitialListener;
    }

    void loadExpressInterstitialAd(AdSlot adSlot, TTAdNative ttAdNative) {
        if (ttAdNative == null || mContext == null || adSlot == null || TextUtils.isEmpty(adSlot.getCodeId())) {
            return;
        }
        ttAdNative.loadInteractionExpressAd(adSlot, mInterstitialAdExpressAdListener);
    }

    private TTAdNative.NativeExpressAdListener mInterstitialAdExpressAdListener = new TTAdNative.NativeExpressAdListener() {
        @Override
        public void onError(int code, String message) {
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, ErrorCode.mapErrorCode(code), message);
            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialFailed(ErrorCode.mapErrorCode(code));
            }
        }

        @Override
        public void onNativeExpressAdLoad(List<TTNativeExpressAd> ads) {
            if (ads == null || ads.size() == 0) {
                return;
            }
            mTTInterstitialExpressAd = ads.get(0);
            mTTInterstitialExpressAd.setExpressInteractionListener(mInterstitialExpressAdInteractionListener);
            mTTInterstitialExpressAd.render();
        }
    };

    /**
     * 渲染回调监听器
     */
    private TTNativeExpressAd.AdInteractionListener mInterstitialExpressAdInteractionListener = new TTNativeExpressAd.AdInteractionListener() {
        @Override
        public void onAdDismiss() {
            TToast.show(mContext, "广告关闭");
            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialDismissed();
            }
        }

        @Override
        public void onAdClicked(View view, int type) {
            TToast.show(mContext, "广告被点击");
            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialClicked();
            }
        }

        @Override
        public void onAdShow(View view, int type) {
            TToast.show(mContext, "广告展示");
            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialImpression();
            }
        }

        @Override
        public void onRenderFail(View view, String msg, int code) {
            TToast.show(mContext, msg + " code:" + code);
            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.RENDER_PROCESS_GONE_UNSPECIFIED.getIntCode(),
                    msg);
            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialFailed(MoPubErrorCode.RENDER_PROCESS_GONE_UNSPECIFIED);
            }
        }

        @Override
        public void onRenderSuccess(View view, float width, float height) {
            //返回view的宽高 单位 dp
            TToast.show(mContext, "渲染成功");
            isRenderLoaded.set(true);
            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialLoaded();
            }
        }
    };


    public void showInterstitial(Activity activity) {
        if (mTTInterstitialExpressAd != null && isRenderLoaded.get()) {
            mTTInterstitialExpressAd.showInteractionExpressAd(activity);
        } else {
            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
        }
    }

    public void destroy() {
        if (mTTInterstitialExpressAd != null) {
            mTTInterstitialExpressAd.destroy();
            mTTInterstitialExpressAd = null;
        }
        mInterstitialExpressAdInteractionListener = null;
        mInterstitialListener = null;
        mInterstitialAdExpressAdListener = null;
        mContext = null;
    }

}

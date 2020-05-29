package com.mopub.mobileads;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.adapter.MopubAdapterUtil;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdDislike;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTNativeAd;
import com.bytedance.sdk.openadsdk.TTNativeExpressAd;
import com.mopub.common.DataKeys;
import com.mopub.common.logging.MoPubLog;

import java.util.List;
import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;

/**
 * created by wuzejian on 2019-11-29
 */
public class PangleAdBanner extends CustomEventBanner {

    protected static final String ADAPTER_NAME = "PangleAdBanner";

    /**
     * pangolin network banner ad unit ID.
     */
    private String placementId = null;

    public final static String GDPR_RESULT = "gdpr_result";
    public final static String COPPA_VALUE = "coppa_value";
    public final static String AD_BANNER_WIDTH = "ad_banner_width";
    public final static String AD_BANNER_HEIGHT = "ad_banner_height";

    private PangleAdapterConfiguration mPangleAdapterConfiguration;

    private Context mContext;

    private PangleAdBannerExpressLoader mAdExpressBannerLoader;
    private PangleAdBannerNativeLoader mAdNativeBannerLoader;

    private float bannerWidth;
    private float bannerHeight;


    public PangleAdBanner() {
        mPangleAdapterConfiguration = new PangleAdapterConfiguration();
    }


    @Override
    protected void loadBanner(Context context, CustomEventBannerListener customEventBannerListener, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        mContext = context;
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "loadBanner mContext =" + mContext);
        /** cache data from server */
        mPangleAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);


        TTAdManager adManager = null;
        String adm = null;

        boolean isExpressAd = false;

        if (serverExtras != null) {
            /** obtain adunit from server by mopub */
            String adunit = serverExtras.get(PangleAdapterConfiguration.KEY_EXTRA_AD_UNIT_ID);
            if (!TextUtils.isEmpty(adunit)) {
                this.placementId = adunit;
            }
            adm = serverExtras.get(DataKeys.ADM_KEY);
            /** init pangolin SDK */
            String appId = serverExtras.get(PangleAdapterConfiguration.PANGLE_APP_ID_KEY);
            String appName = serverExtras.get(PangleAdapterConfiguration.PANGLE_APP_NAME_KEY);
            PangleAdapterConfiguration.pangleSdkInit(context, appId, appName);
            adManager = PangleAdapterConfiguration.getPangleSdkManager();

            mPangleAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
        }


        /** set GDPR */
        if (localExtras != null && !localExtras.isEmpty()) {
            if (localExtras.containsKey(GDPR_RESULT)) {
                int gdpr = (int) localExtras.get(GDPR_RESULT);
                if (adManager != null && (gdpr == 0 || gdpr == 1)) {
                    adManager.setGdpr(gdpr);
                }
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "banner receive gdpr=" + gdpr);
            }

            if (placementId == null && localExtras.containsKey(PangleAdapterConfiguration.KEY_EXTRA_AD_UNIT_ID)) {
                placementId = (String) localExtras.get(PangleAdapterConfiguration.KEY_EXTRA_AD_UNIT_ID);
            }

            if (adManager != null) {
                isExpressAd = adManager.getAdRequetTypeByRit(placementId) == TTAdConstant.REQUEST_AD_TYPE_EXPRESS;
            }


            if (isExpressAd) {
                float[] bannerAdSizeAdapterSafely = PangleSharedUtil.getBannerAdSizeAdapterSafely(localExtras, AD_BANNER_WIDTH, AD_BANNER_HEIGHT);
                bannerWidth = bannerAdSizeAdapterSafely[0];
                bannerHeight = bannerAdSizeAdapterSafely[1];
            } else {
                /** obtain extra parameters */
                if (localExtras.containsKey(AD_BANNER_WIDTH)) {
                    bannerWidth = Float.valueOf(String.valueOf(localExtras.get(AD_BANNER_WIDTH)));
                }

                if (localExtras.containsKey(AD_BANNER_HEIGHT)) {
                    bannerHeight = Float.valueOf(String.valueOf(localExtras.get(AD_BANNER_HEIGHT)));
                }

            }
            checkSize(isExpressAd);

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "bannerWidth =" + bannerWidth + "，bannerHeight=" + bannerHeight + ",placementId=" + placementId);

        }


        MoPubLog.log(CUSTOM, ADAPTER_NAME, "loadBanner method placementId：" + placementId);

        /** create request parameters for AdSlot */
        AdSlot.Builder adSlotBuilder = new AdSlot.Builder()
                .setCodeId(placementId)
                .setSupportDeepLink(true)
                .setAdCount(1)
                .setImageAcceptedSize((int) bannerWidth, (int) bannerHeight)
                .withBid(adm);

        if (isExpressAd) {
            adSlotBuilder.setExpressViewAcceptedSize(bannerWidth, bannerHeight);
        } else {
            adSlotBuilder.setNativeAdType(AdSlot.TYPE_BANNER);
        }

        /** request ad express banner */
        if (isExpressAd) {
            mAdExpressBannerLoader = new PangleAdBannerExpressLoader(mContext, customEventBannerListener);
            mAdExpressBannerLoader.loadAdExpressBanner(adSlotBuilder.build(), adManager.createAdNative(mContext));
        } else {
            mAdNativeBannerLoader = new PangleAdBannerNativeLoader(mContext, bannerWidth, bannerHeight, customEventBannerListener);
            mAdNativeBannerLoader.loadAdNativeBanner(adSlotBuilder.build(), adManager.createAdNative(mContext));
        }

    }

    private void checkSize(boolean isExpressAd) {
        if (isExpressAd) {
            if (bannerWidth <= 0) {
                bannerWidth = 320;
                bannerHeight = 0;
            }
            if (bannerHeight < 0) {
                bannerHeight = 0;
            }
        } else {
            /** default value */
            if (bannerWidth <= 0 || bannerHeight <= 0) {
                bannerWidth = 320;
                bannerHeight = 50;
            }
        }
    }


    @Override
    protected void onInvalidate() {
        if (mAdExpressBannerLoader != null) {
            mAdExpressBannerLoader.destroy();
            mAdExpressBannerLoader = null;
        }

        if (mAdNativeBannerLoader != null) {
            mAdNativeBannerLoader.destroy();
            mAdNativeBannerLoader = null;
        }
    }


    /**
     * created by wuzejian on 2020/5/11
     * pangle native ad banner
     */
    public static class PangleAdBannerNativeLoader {

        private Context mContext;

        private CustomEventBanner.CustomEventBannerListener mCustomEventBannerListener;

        private View mBannerView;


        private float mBannerWidth;
        private float mBannerHeight;

        PangleAdBannerNativeLoader(Context context, float bannerWidth, float bannerHeight, CustomEventBanner.CustomEventBannerListener customEventBannerListener) {
            this.mCustomEventBannerListener = customEventBannerListener;
            this.mContext = context;
            this.mBannerWidth = bannerWidth;
            this.mBannerHeight = bannerHeight;
        }


        void loadAdNativeBanner(AdSlot adSlot, TTAdNative ttAdNative) {
            if (mContext == null || adSlot == null || ttAdNative == null || TextUtils.isEmpty(adSlot.getCodeId()))
                return;
            ttAdNative.loadNativeAd(adSlot, mNativeAdListener);

        }

        private TTAdNative.NativeAdListener mNativeAdListener = new TTAdNative.NativeAdListener() {
            @Override
            public void onError(int code, String message) {
                if (mCustomEventBannerListener != null) {
                    mCustomEventBannerListener.onBannerFailed(PangleSharedUtil.mapErrorCode(code));
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

                mBannerView = MopubAdapterUtil.setAdDataAndBuildBannerView(mContext, ads.get(0), mAdInteractionListener, mBannerWidth, mBannerHeight);

                if (mBannerView == null) {
                    return;
                }

                if (mCustomEventBannerListener != null) {
                    /** load success add view to mMoPubView */
                    mCustomEventBannerListener.onBannerLoaded(mBannerView);
                }

            }
        };

        TTNativeAd.AdInteractionListener mAdInteractionListener = new TTNativeAd.AdInteractionListener() {
            @Override
            public void onAdClicked(View view, TTNativeAd ad) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "banner native Ad clicked");

                if (mCustomEventBannerListener != null) {
                    mCustomEventBannerListener.onBannerClicked();
                }
            }

            @Override
            public void onAdCreativeClick(View view, TTNativeAd ad) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "banner native Ad clicked");
                if (mCustomEventBannerListener != null) {
                    mCustomEventBannerListener.onBannerClicked();
                }
            }

            @Override
            public void onAdShow(TTNativeAd ad) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "banner native Ad showed");

                if (mCustomEventBannerListener != null) {
                    mCustomEventBannerListener.onBannerImpression();
                }
            }
        };

        public void destroy() {
//            mContext = null;
            mCustomEventBannerListener = null;
            mNativeAdListener = null;
        }

    }


    /**
     * created by wuzejian on 2020/5/11
     * pangle express ad banner
     */
    public static class PangleAdBannerExpressLoader {

        private TTNativeExpressAd mTTNativeExpressAd;

        private Context mContext;

        private CustomEventBanner.CustomEventBannerListener mCustomEventBannerListener;


        PangleAdBannerExpressLoader(Context context, CustomEventBanner.CustomEventBannerListener customEventBannerListener) {
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
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "express banner ad  onBannerFailed.-code=" + code + "," + message);

                if (mCustomEventBannerListener != null) {
                    mCustomEventBannerListener.onBannerFailed(PangleSharedUtil.mapErrorCode(code));
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
            /** dislike function, maybe you can use custom dialog, please refer to the access document by yourself */
            if (mContext instanceof Activity) {
                ad.setDislikeCallback((Activity) mContext, new TTAdDislike.DislikeInteractionCallback() {
                    @Override
                    public void onSelected(int position, String value) {
                        MoPubLog.log(CUSTOM, ADAPTER_NAME, "DislikeInteractionCallback=click-value=" + value);
                    }

                    @Override
                    public void onCancel() {
                        MoPubLog.log(CUSTOM, ADAPTER_NAME, "DislikeInteractionCallbac Cancel click=");
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
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "banner ad onRenderFail msg = " + msg + "，code=" + code);
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
                    /** render success add view to mMoPubView */
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "banner ad onRenderSuccess ");
                    mCustomEventBannerListener.onBannerLoaded(view);
                }
            }
        };

        public void destroy() {
            if (mTTNativeExpressAd != null) {
                mTTNativeExpressAd.destroy();
                mTTNativeExpressAd = null;
            }

//            this.mContext = null;
            this.mCustomEventBannerListener = null;
            this.mExpressAdInteractionListener = null;
            this.mTTNativeExpressAdListener = null;
        }
    }

}

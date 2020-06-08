package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdDislike;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTNativeAd;
import com.bytedance.sdk.openadsdk.TTNativeExpressAd;
import com.bytedance.sdk.openadsdk.adapter.MediationAdapterUtil;
import com.mopub.common.DataKeys;
import com.mopub.common.logging.MoPubLog;

import java.util.List;
import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;

public class PangleAdBanner extends CustomEventBanner {

    private static final String ADAPTER_NAME = PangleAdBanner.class.getSimpleName();

    /**
     * Pangle network banner ad unit ID.
     */
    private static String mPlacementId;
    private PangleAdapterConfiguration mPangleAdapterConfiguration;

    private Context mContext;

    private PangleAdBannerExpressLoader mAdExpressBannerLoader;
    private PangleAdBannerNativeLoader mAdTraditionalBannerLoader;

    private float mBannerWidth;
    private float mBannerHeight;


    public PangleAdBanner() {
        mPangleAdapterConfiguration = new PangleAdapterConfiguration();
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "PangleAdBanner has been create ....");
    }


    @Override
    protected void loadBanner(Context context, CustomEventBannerListener customEventBannerListener, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        mContext = context;
        /** cache data from server */
        TTAdManager adManager = null;
        String adm = null;

        boolean isExpressAd = false;

        if (serverExtras != null) {
            mPangleAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
            /** Obtain ad placement id from MoPub UI */
            mPlacementId = serverExtras.get(PangleAdapterConfiguration.KEY_EXTRA_AD_PLACEMENT_ID);
            if (TextUtils.isEmpty(mPlacementId)) {
                if (customEventBannerListener != null) {
                    customEventBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                    MoPubLog.log(CUSTOM, ADAPTER_NAME,
                            "Invalid Pangle placement ID. Failing ad request. " +
                                    "Ensure the ad placement id is valid on the MoPub dashboard.");
                }
                return;
            }
            adm = serverExtras.get(DataKeys.ADM_KEY);

            /** Init Pangle SDK if fail to initialize in the adapterConfiguration */
            String appId = serverExtras.get(PangleAdapterConfiguration.KEY_EXTRA_APP_ID);
            PangleAdapterConfiguration.pangleSdkInit(context, appId);
            adManager = PangleAdapterConfiguration.getPangleSdkManager();

            mPangleAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
        }


        if (adManager != null) {
            isExpressAd = adManager.isExpressAd(mPlacementId, adm);
        }

        if (isExpressAd) {
            float[] bannerAdSizeAdapterSafely = PangleSharedUtil.getBannerAdSizeAdapterSafely(localExtras, DataKeys.AD_WIDTH, DataKeys.AD_HEIGHT);
            mBannerWidth = bannerAdSizeAdapterSafely[0];
            mBannerHeight = bannerAdSizeAdapterSafely[1];
        } else {
            /** obtain extra parameters */
            if (localExtras.containsKey(DataKeys.AD_WIDTH)) {
                mBannerWidth = Float.valueOf((Integer) localExtras.get(DataKeys.AD_WIDTH));
            }
            if (localExtras.containsKey(DataKeys.AD_HEIGHT)) {
                mBannerHeight = Float.valueOf((Integer) localExtras.get(DataKeys.AD_HEIGHT));
            }
        }

        checkSize(isExpressAd);

        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "BannerWidth =" + mBannerWidth + "，bannerHeight=" + mBannerHeight + ",placementId=" + mPlacementId);
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "LoadBanner method placementId：" + mPlacementId);

        if (TextUtils.isEmpty(mPlacementId)) {
            if (customEventBannerListener != null) {
                customEventBannerListener.onBannerFailed(MoPubErrorCode.MISSING_AD_UNIT_ID);
            }
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.MISSING_AD_UNIT_ID.getIntCode(),
                    MoPubErrorCode.MISSING_AD_UNIT_ID);
            return;
        }

        if (adManager == null) {
            if (customEventBannerListener != null) {
                customEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
            }
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_INVALID_STATE.getIntCode(),
                    MoPubErrorCode.NETWORK_INVALID_STATE);
            return;
        }

        AdSlot.Builder adSlotBuilder = new AdSlot.Builder()
                .setCodeId(mPlacementId)
                .setSupportDeepLink(true)
                .setAdCount(1)
                .setImageAcceptedSize((int) mBannerWidth, (int) mBannerHeight)
                .withBid(adm);

        if (isExpressAd) {
            adSlotBuilder.setExpressViewAcceptedSize(mBannerWidth, mBannerHeight);
        } else {
            adSlotBuilder.setNativeAdType(AdSlot.TYPE_BANNER);
        }

        /** request ad express banner */
        if (isExpressAd) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Loading Pangle express banner ad");
            MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
            mAdExpressBannerLoader = new PangleAdBannerExpressLoader(mContext, customEventBannerListener);
            mAdExpressBannerLoader.loadAdExpressBanner(adSlotBuilder.build(), adManager.createAdNative(mContext));
        } else {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Loading Pangle traditional banner ad");
            MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
            mAdTraditionalBannerLoader = new PangleAdBannerNativeLoader(mContext, mBannerWidth, mBannerHeight, customEventBannerListener);
            mAdTraditionalBannerLoader.loadAdNativeBanner(adSlotBuilder.build(), adManager.createAdNative(mContext));
        }

    }

    private static String getAdNetworkId() {
        return mPlacementId;
    }

    private void checkSize(boolean isExpressAd) {
        if (isExpressAd) {
            if (mBannerWidth <= 0) {
                mBannerWidth = 320;
                mBannerHeight = 0;
            }
            if (mBannerHeight < 0) {
                mBannerHeight = 0;
            }
        } else {
            if (mBannerWidth <= 0 || mBannerHeight <= 0) {
                mBannerWidth = 320;
                mBannerHeight = 50;
            }
        }
    }


    @Override
    protected void onInvalidate() {
        if (mAdExpressBannerLoader != null) {
            mAdExpressBannerLoader.destroy();
            mAdExpressBannerLoader = null;
        }

        if (mAdTraditionalBannerLoader != null) {
            mAdTraditionalBannerLoader.destroy();
            mAdTraditionalBannerLoader = null;
        }
    }


    /**
     * Pangle traditional banner callback interface
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
            if (mContext == null || adSlot == null || ttAdNative == null || TextUtils.isEmpty(adSlot.getCodeId())) {
                if (mCustomEventBannerListener != null) {
                    mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
                return;
            }
            ttAdNative.loadNativeAd(adSlot, mNativeAdListener);

        }

        private TTAdNative.NativeAdListener mNativeAdListener = new TTAdNative.NativeAdListener() {
            @Override
            public void onError(int code, String message) {
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                        MoPubErrorCode.NETWORK_NO_FILL);
                if (mCustomEventBannerListener != null) {
                    mCustomEventBannerListener.onBannerFailed(PangleSharedUtil.mapErrorCode(code));
                }
            }

            @Override
            public void onNativeAdLoad(List<TTNativeAd> ads) {
                if (ads.get(0) == null) {
                    MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                            MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                            MoPubErrorCode.NETWORK_NO_FILL);
                    if (mCustomEventBannerListener != null) {
                        mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.NO_FILL);
                    }
                    return;
                }

                mBannerView = MediationAdapterUtil.setAdDataAndBuildBannerView(mContext, ads.get(0), mAdInteractionListener, mBannerWidth, mBannerHeight);

                if (mBannerView == null) {
                    if (mCustomEventBannerListener != null) {
                        mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.NO_FILL);
                    }
                    return;
                }

                MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
                if (mCustomEventBannerListener != null) {
                    mCustomEventBannerListener.onBannerLoaded(mBannerView);
                }

            }
        };

        TTNativeAd.AdInteractionListener mAdInteractionListener = new TTNativeAd.AdInteractionListener() {
            @Override
            public void onAdClicked(View view, TTNativeAd ad) {
                MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
                if (mCustomEventBannerListener != null) {
                    mCustomEventBannerListener.onBannerClicked();
                }
            }

            @Override
            public void onAdCreativeClick(View view, TTNativeAd ad) {
                MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
                if (mCustomEventBannerListener != null) {
                    mCustomEventBannerListener.onBannerClicked();
                }
            }

            @Override
            public void onAdShow(TTNativeAd ad) {
                MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
                if (mCustomEventBannerListener != null) {
                    mCustomEventBannerListener.onBannerImpression();
                }
            }
        };

        public void destroy() {
            mCustomEventBannerListener = null;
            mNativeAdListener = null;
        }

    }


    /**
     * Pangle express banner callback interface
     */
    public static class PangleAdBannerExpressLoader {

        private TTNativeExpressAd mTTNativeExpressAd;

        private Context mContext;

        private CustomEventBanner.CustomEventBannerListener mCustomEventBannerListener;


        PangleAdBannerExpressLoader(Context context, CustomEventBanner.CustomEventBannerListener customEventBannerListener) {
            this.mCustomEventBannerListener = customEventBannerListener;
            this.mContext = context;
        }

        public void loadAdExpressBanner(AdSlot adSlot, TTAdNative ttAdNative) {
            if (mContext == null || adSlot == null || ttAdNative == null || TextUtils.isEmpty(adSlot.getCodeId())) {
                if (mCustomEventBannerListener != null) {
                    mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
                return;
            }
            ttAdNative.loadBannerExpressAd(adSlot, mTTNativeExpressAdListener);
        }


        private TTAdNative.NativeExpressAdListener mTTNativeExpressAdListener = new TTAdNative.NativeExpressAdListener() {
            @Override
            public void onError(int code, String message) {
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, "express banner ad  onBannerFailed.-code=" + code + "," + message);
                if (mCustomEventBannerListener != null) {
                    mCustomEventBannerListener.onBannerFailed(PangleSharedUtil.mapErrorCode(code));
                }
            }

            @Override
            public void onNativeExpressAdLoad(List<TTNativeExpressAd> ads) {
                if (ads == null || ads.size() == 0) {
                    MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                            MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                            MoPubErrorCode.NETWORK_NO_FILL);
                    if (mCustomEventBannerListener != null) {
                        mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.NO_FILL);
                    }
                    return;
                }
                MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
                mTTNativeExpressAd = ads.get(0);
                mTTNativeExpressAd.setExpressInteractionListener(mExpressAdInteractionListener);
                bindDislike(mTTNativeExpressAd);
                mTTNativeExpressAd.render();
            }
        };

        private void bindDislike(TTNativeExpressAd ad) {
            /** dislike function, maybe you can use custom dialog, please refer to the access document from Pangle */
            if (mContext instanceof Activity) {
                ad.setDislikeCallback((Activity) mContext, new TTAdDislike.DislikeInteractionCallback() {
                    @Override
                    public void onSelected(int position, String value) {
                        MoPubLog.log(CUSTOM, ADAPTER_NAME, "Pangle DislikeInteractionCallback click value=" + value);
                    }

                    @Override
                    public void onCancel() {
                        MoPubLog.log(CUSTOM, ADAPTER_NAME, "Pangle DislikeInteractionCallbac cancel click...");
                    }
                });
            }
        }

        /**
         * Pangle express banner render callback interface
         */
        private TTNativeExpressAd.ExpressAdInteractionListener mExpressAdInteractionListener = new TTNativeExpressAd.ExpressAdInteractionListener() {
            @Override
            public void onAdClicked(View view, int type) {
                MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
                if (mCustomEventBannerListener != null) {
                    mCustomEventBannerListener.onBannerClicked();
                }
            }

            @Override
            public void onAdShow(View view, int type) {
                MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
                if (mCustomEventBannerListener != null) {
                    mCustomEventBannerListener.onBannerImpression();
                }
            }

            @Override
            public void onRenderFail(View view, String msg, int code) {
                MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME);
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME,
                        "banner ad onRenderFail msg = " + msg + "，code=" + code);

                if (mCustomEventBannerListener != null) {
                    mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.RENDER_PROCESS_GONE_UNSPECIFIED);
                }
            }

            @Override
            public void onRenderSuccess(View view, float width, float height) {
                MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
                MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
                if (mCustomEventBannerListener != null) {
                    mCustomEventBannerListener.onBannerLoaded(view);
                }
            }
        };

        public void destroy() {
            if (mTTNativeExpressAd != null) {
                mTTNativeExpressAd.destroy();
                mTTNativeExpressAd = null;
            }

            this.mCustomEventBannerListener = null;
            this.mExpressAdInteractionListener = null;
            this.mTTNativeExpressAdListener = null;
        }
    }

}

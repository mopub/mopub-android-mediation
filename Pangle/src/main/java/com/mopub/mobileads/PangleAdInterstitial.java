package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.view.View;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTFullScreenVideoAd;
import com.bytedance.sdk.openadsdk.TTNativeAd;
import com.bytedance.sdk.openadsdk.TTNativeExpressAd;
import com.bytedance.sdk.openadsdk.adapter.PangleAdInterstitialActivity;
import com.mopub.common.DataKeys;
import com.mopub.common.logging.MoPubLog;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;

public class PangleAdInterstitial extends CustomEventInterstitial {
    private static final String ADAPTER_NAME = PangleAdInterstitial.class.getSimpleName();
    /**
     * ad size
     */
    public final static String AD_WIDTH = "ad_width";
    public final static String AD_HEIGHT = "ad_height";


    /**
     * pangolin network Interstitial ad unit ID.
     */
    private String mPlacementId;
    private boolean mIsFullVideoAd;
    private Context mContext;
    private boolean isExpressAd = false;
    private float adWidth = 0;
    private float adHeight = 0;
    private PangleAdapterConfiguration mPangleAdapterConfiguration;
    private PangleAdInterstitialExpressLoader mExpressInterstitialLoader;
    private PangleAdInterstitialNativeLoader mNativeInterstitialLoader;
    private PangleAdInterstitialFullVideoLoader mFullVideoLoader;

    public PangleAdInterstitial() {
        mPangleAdapterConfiguration = new PangleAdapterConfiguration();
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "PangolinAdapterInterstitial has been create ....");
    }

    @Override
    protected void loadInterstitial(
            final Context context,
            final CustomEventInterstitialListener customEventInterstitialListener,
            final Map<String, Object> localExtras,
            final Map<String, String> serverExtras) {
        this.mContext = context;
        int mOrientation = mContext.getResources().getConfiguration().orientation;
        mPangleAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
        setAutomaticImpressionAndClickTracking(false);

        String adm = null;

        TTAdManager ttAdManager = null;
        TTAdNative ttAdNative = null;


        if (serverExtras != null) {
            /** obtain adunit from server by mopub */
            String adunit = serverExtras.get(PangleAdapterConfiguration.KEY_EXTRA_AD_PLACEMENT_ID);
            if (!TextUtils.isEmpty(adunit)) {
                this.mPlacementId = adunit;
            }
            adm = serverExtras.get(DataKeys.ADM_KEY);
            /** init pangolin SDK */
            String appId = serverExtras.get(PangleAdapterConfiguration.KEY_EXTRA_APP_ID);
            PangleAdapterConfiguration.pangleSdkInit(context, appId);
            ttAdManager = PangleAdapterConfiguration.getPangleSdkManager();
            ttAdNative = ttAdManager.createAdNative(context.getApplicationContext());
            mPangleAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);

            if (ttAdManager != null) {
                isExpressAd = ttAdManager.getAdRequetTypeByRit(mPlacementId) == TTAdConstant.REQUEST_AD_TYPE_EXPRESS;
                mIsFullVideoAd = ttAdManager.isFullScreenVideoAd(mPlacementId);
            }

            /** obtain extra parameters */
            float[] adSize = PangleSharedUtil.getAdSizeSafely(localExtras, AD_WIDTH, AD_HEIGHT);
            adWidth = adSize[0];
            adHeight = adSize[1];

            checkSize(isExpressAd);
        }

        MoPubLog.log(CUSTOM, ADAPTER_NAME, "adWidth =" + adWidth + "，adHeight=" + adHeight + ",placementId=" + mPlacementId + ",isExpressAd=" + isExpressAd);

        AdSlot.Builder adSlotBuilder = new AdSlot.Builder()
                .setCodeId(mPlacementId)
                .setSupportDeepLink(true)
                .setAdCount(1)
                .withBid(adm);

        if (!mIsFullVideoAd) {
            /** request Interstitial */
            if (isExpressAd) {
                adSlotBuilder.setExpressViewAcceptedSize(adWidth, adHeight);
                mExpressInterstitialLoader = new PangleAdInterstitialExpressLoader(mContext, customEventInterstitialListener);
                mExpressInterstitialLoader.loadExpressInterstitialAd(adSlotBuilder.build(), ttAdNative);
            } else {
                if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
                    /** ORIENTATION_PORTRAIT 2:3 */
                    adWidth = PangleSharedUtil.getScreenWidth(mContext);
                    adHeight = (3 * adWidth) / 2;
                } else {
                    /**  3:2 = w:h */
                    adHeight = PangleSharedUtil.getScreenHeight(mContext);
                    adWidth = (3 * adHeight) / 2;
                }
                adSlotBuilder.setNativeAdType(AdSlot.TYPE_INTERACTION_AD);
                adSlotBuilder.setImageAcceptedSize((int) adWidth, (int) adHeight);
                mNativeInterstitialLoader = new PangleAdInterstitialNativeLoader(mContext, mOrientation, customEventInterstitialListener);
                mNativeInterstitialLoader.loadAdNativeInterstitial(adSlotBuilder.build(), ttAdNative);
            }
        } else {
            /**  request FullVideoAd */
            adSlotBuilder.setImageAcceptedSize(1080, 1920);
            mFullVideoLoader = new PangleAdInterstitialFullVideoLoader(mContext, customEventInterstitialListener);
            mFullVideoLoader.loadAdFullVideoListener(adSlotBuilder.build(), ttAdNative);
        }
    }


    private void checkSize(boolean isExpressAd) {
        if (isExpressAd) {
            if (adWidth <= 0) {
                adWidth = 300;
                adWidth = 450;
            }
            if (adHeight < 0) {
                adHeight = 0;
            }
        } else {
            if (adWidth <= 0 || adHeight <= 0) {
                adWidth = 600;
                adHeight = 900;
            }
        }
    }


    @Override
    protected void showInterstitial() {
        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);
        if (!mIsFullVideoAd) {
            if (isExpressAd) {
                if (mExpressInterstitialLoader != null && mContext instanceof Activity) {
                    mExpressInterstitialLoader.showInterstitial((Activity) mContext);
                }
            } else {
                if (mNativeInterstitialLoader != null && mNativeInterstitialLoader.isReady()) {
                    mNativeInterstitialLoader.showNativeInterstitialActiviy();
                }
            }
        } else {
            if (mFullVideoLoader != null && mContext instanceof Activity) {
                mFullVideoLoader.showFullVideo((Activity) mContext);
            }
        }
    }

    @Override
    protected void onInvalidate() {
        if (!mIsFullVideoAd) {
            if (isExpressAd) {
                if (mExpressInterstitialLoader != null) {
                    mExpressInterstitialLoader.destroy();
                }
            } else {
                if (mNativeInterstitialLoader != null) {
                    mNativeInterstitialLoader.destroy();
                }
            }
        } else {
            if (mFullVideoLoader != null) {
                mFullVideoLoader.destroy();
            }
        }
    }


    /**
     * Native Interstitial Ad Loader
     */
    public static class PangleAdInterstitialNativeLoader {
        private static final String TAG = "AdNativeInterLoader";
        private Context mContext;
        private CustomEventInterstitial.CustomEventInterstitialListener mInterstitialListener;
        private boolean mIsLoading;
        private TTNativeAd mTTNativeAd;
        private int mOrientation;

        PangleAdInterstitialNativeLoader(Context context, int orientation, CustomEventInterstitial.CustomEventInterstitialListener interstitialListener) {
            this.mContext = context;
            this.mInterstitialListener = interstitialListener;
            this.mOrientation = orientation;
        }

        void loadAdNativeInterstitial(AdSlot adSlot, TTAdNative ttAdNative) {
            if (ttAdNative == null || mContext == null || adSlot == null || TextUtils.isEmpty(adSlot.getCodeId())) {
                return;
            }
            ttAdNative.loadNativeAd(adSlot, mNativeAdListener);
        }

        TTAdNative.NativeAdListener mNativeAdListener = new TTAdNative.NativeAdListener() {
            @Override
            public void onError(int code, String message) {
                mIsLoading = false;
                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, PangleSharedUtil.mapErrorCode(code), message);
                if (mInterstitialListener != null) {
                    mInterstitialListener.onInterstitialFailed(PangleSharedUtil.mapErrorCode(code));
                }
            }

            @Override
            public void onNativeAdLoad(List<TTNativeAd> ads) {
                mIsLoading = true;
                if (ads.get(0) == null) {
                    return;
                }
                mTTNativeAd = ads.get(0);
                if (mInterstitialListener != null) {
                    mInterstitialListener.onInterstitialLoaded();
                }
            }
        };

        private boolean isReady() {
            return mIsLoading;
        }

        private void showNativeInterstitialActiviy() {
            PangleAdInterstitialActivity.showAd(mContext, mTTNativeAd, mOrientation == Configuration.ORIENTATION_PORTRAIT ? PangleAdInterstitialActivity.INTENT_TYPE_IMAGE_2_3 : PangleAdInterstitialActivity.INTENT_TYPE_IMAGE_3_2, new com.bytedance.sdk.openadsdk.CustomEventInterstitialListener() {
                @Override
                public void onInterstitialShown() {
                    if (mInterstitialListener != null) {
                        mInterstitialListener.onInterstitialShown();
                    }
                }

                @Override
                public void onInterstitialClicked() {
                    if (mInterstitialListener != null) {
                        mInterstitialListener.onInterstitialClicked();
                    }
                }

                @Override
                public void onInterstitialImpression() {
                    if (mInterstitialListener != null) {
                        mInterstitialListener.onInterstitialImpression();
                    }
                }

                @Override
                public void onLeaveApplication() {
                    if (mInterstitialListener != null) {
                        mInterstitialListener.onLeaveApplication();
                    }
                }

                @Override
                public void onInterstitialDismissed() {
                    if (mInterstitialListener != null) {
                        mInterstitialListener.onInterstitialDismissed();
                    }
                }
            });
        }

        public void destroy() {
            mContext = null;
            mInterstitialListener = null;
            mNativeAdListener = null;
            mTTNativeAd = null;
        }
    }


    /**
     * Express interstitial ad
     */
    public static class PangleAdInterstitialExpressLoader {
        private Context mContext;
        private CustomEventInterstitial.CustomEventInterstitialListener mInterstitialListener;
        private TTNativeExpressAd mTTInterstitialExpressAd;
        private AtomicBoolean isRenderLoaded = new AtomicBoolean(false);


        PangleAdInterstitialExpressLoader(Context context, CustomEventInterstitial.CustomEventInterstitialListener interstitialListener) {
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
                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, PangleSharedUtil.mapErrorCode(code), message);
                if (mInterstitialListener != null) {
                    mInterstitialListener.onInterstitialFailed(PangleSharedUtil.mapErrorCode(code));
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
         * render callback
         */
        private TTNativeExpressAd.AdInteractionListener mInterstitialExpressAdInteractionListener = new TTNativeExpressAd.AdInteractionListener() {
            @Override
            public void onAdDismiss() {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "TTNativeExpressAd-AdInteractionListener ad onAdDismiss");
                if (mInterstitialListener != null) {
                    mInterstitialListener.onInterstitialDismissed();
                }
            }

            @Override
            public void onAdClicked(View view, int type) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "TTNativeExpressAd-AdInteractionListener ad onAdClicked");
                if (mInterstitialListener != null) {
                    mInterstitialListener.onInterstitialClicked();
                }
            }

            @Override
            public void onAdShow(View view, int type) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "TTNativeExpressAd-AdInteractionListener ad show");
                if (mInterstitialListener != null) {
                    mInterstitialListener.onInterstitialImpression();
                }
            }

            @Override
            public void onRenderFail(View view, String msg, int code) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "onInterstitialFailed ad onRenderFail msg = " + msg + "，code=" + code);

                MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.RENDER_PROCESS_GONE_UNSPECIFIED.getIntCode(),
                        msg);
                if (mInterstitialListener != null) {
                    mInterstitialListener.onInterstitialFailed(MoPubErrorCode.RENDER_PROCESS_GONE_UNSPECIFIED);
                }
            }

            @Override
            public void onRenderSuccess(View view, float width, float height) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "render success");
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


    /**
     * Pangle full-video ad
     */
    public static class PangleAdInterstitialFullVideoLoader {
        private Context mContext;
        private CustomEventInterstitial.CustomEventInterstitialListener mFullVideoListener;
        private boolean mIsLoaded;
        private TTFullScreenVideoAd mTTFullScreenVideoAd;

        PangleAdInterstitialFullVideoLoader(Context context, CustomEventInterstitial.CustomEventInterstitialListener fullVideoListener) {
            this.mContext = context;
            this.mFullVideoListener = fullVideoListener;
        }

        void loadAdFullVideoListener(AdSlot adSlot, TTAdNative ttAdNative) {
            if (ttAdNative == null || mContext == null || adSlot == null || TextUtils.isEmpty(adSlot.getCodeId())) {
                return;
            }
            ttAdNative.loadFullScreenVideoAd(adSlot, mLoadFullVideoAdListener);
        }

        void showFullVideo(Activity activity) {
            if (mTTFullScreenVideoAd != null && mIsLoaded) {
                mTTFullScreenVideoAd.showFullScreenVideoAd(activity);
            }
        }

        public void destroy() {
            mContext = null;
            mFullVideoListener = null;
            mTTFullScreenVideoAd = null;
            mLoadFullVideoAdListener = null;
            mFullScreenVideoAdInteractionListener = null;
        }


        private TTAdNative.FullScreenVideoAdListener mLoadFullVideoAdListener = new TTAdNative.FullScreenVideoAdListener() {
            @Override
            public void onError(int code, String message) {
                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, "Loading Full Video creative encountered an error: " + PangleSharedUtil.mapErrorCode(code).toString() + ",error message:" + message);
                if (mFullVideoListener != null) {
                    mFullVideoListener.onInterstitialFailed(PangleSharedUtil.mapErrorCode(code));
                }
            }

            @Override
            public void onFullScreenVideoAdLoad(TTFullScreenVideoAd ad) {
                if (ad != null) {
                    mIsLoaded = true;
                    mTTFullScreenVideoAd = ad;
                    mTTFullScreenVideoAd.setFullScreenVideoAdInteractionListener(mFullScreenVideoAdInteractionListener);
                    if (mFullVideoListener != null) {
                        mFullVideoListener.onInterstitialLoaded();
                    }
                    MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);

                } else {
                    if (mFullVideoListener != null) {
                        mFullVideoListener.onInterstitialFailed(PangleSharedUtil.mapErrorCode(PangleSharedUtil.NO_AD));
                    }
                    MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, " mTTFullScreenVideoAd is null !");
                }
            }

            @Override
            public void onFullScreenVideoCached() {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, " mTTFullScreenVideoAd onFullScreenVideoCached invoke !");
            }
        };

        private TTFullScreenVideoAd.FullScreenVideoAdInteractionListener mFullScreenVideoAdInteractionListener = new TTFullScreenVideoAd.FullScreenVideoAdInteractionListener() {

            @Override
            public void onAdShow() {
                if (mFullVideoListener != null) {
                    mFullVideoListener.onInterstitialShown();
                }
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "TTFullScreenVideoAd onAdShow...");
            }

            @Override
            public void onAdVideoBarClick() {
                if (mFullVideoListener != null) {
                    mFullVideoListener.onInterstitialClicked();
                }
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "TTFullScreenVideoAd onAdVideoBarClick...");
            }

            @Override
            public void onAdClose() {
                if (mFullVideoListener != null) {
                    mFullVideoListener.onInterstitialDismissed();
                }
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "TTFullScreenVideoAd onAdClose...");
            }

            @Override
            public void onVideoComplete() {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "TTFullScreenVideoAd onVideoComplete...");
            }

            @Override
            public void onSkippedVideo() {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "TTFullScreenVideoAd onSkippedVideo...");
            }
        };

    }
}

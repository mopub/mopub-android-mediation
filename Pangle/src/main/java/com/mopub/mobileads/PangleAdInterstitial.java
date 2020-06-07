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

import static com.mopub.common.logging.MoPubLog.AdLogEvent.DID_DISAPPEAR;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.WILL_LEAVE_APPLICATION;

public class PangleAdInterstitial extends CustomEventInterstitial {
    private static final String ADAPTER_NAME = PangleAdInterstitial.class.getSimpleName();
    /**
     * ad size
     */
    private static final String AD_WIDTH = "ad_width";
    private static final String AD_HEIGHT = "ad_height";


    /**
     * pangolin network Interstitial ad unit ID.
     */
    private static String mPlacementId;
    private boolean mIsFullVideoAd;
    private Context mContext;
    private boolean mIsExpressAd = false;
    private float mAdWidth = 0;
    private float mAdHeight = 0;
    private PangleAdapterConfiguration mPangleAdapterConfiguration;
    private PangleAdInterstitialExpressLoader mExpressInterstitialLoader;
    private PangleAdInterstitialNativeLoader mNativeInterstitialLoader;
    private PangleAdInterstitialFullVideoLoader mFullVideoLoader;
    private CustomEventInterstitial.CustomEventInterstitialListener customEventInterstitialListener;

    public PangleAdInterstitial() {
        mPangleAdapterConfiguration = new PangleAdapterConfiguration();
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "PangolinAdapterInterstitial has been create ....");
    }

    @Override
    protected void loadInterstitial(
            final Context context,
            final CustomEventInterstitialListener customEventInterstitialListener,
            final Map<String, Object> localExtras,
            final Map<String, String> serverExtras) {
        this.mContext = context;
        this.customEventInterstitialListener = customEventInterstitialListener;
        int mOrientation = mContext.getResources().getConfiguration().orientation;
        mPangleAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
        setAutomaticImpressionAndClickTracking(false);

        String adm = null;

        TTAdManager ttAdManager = null;
        TTAdNative ttAdNative = null;


        if (serverExtras != null) {
            /** Obtain ad placement id from MoPub UI */
            String adunit = serverExtras.get(PangleAdapterConfiguration.KEY_EXTRA_AD_PLACEMENT_ID);
            mPlacementId = adunit;

            if (TextUtils.isEmpty(mPlacementId)) {
                if (customEventInterstitialListener != null) {
                    customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
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
            ttAdManager = PangleAdapterConfiguration.getPangleSdkManager();
            ttAdNative = ttAdManager.createAdNative(context.getApplicationContext());
            mPangleAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);

            if (ttAdManager != null) {
                mIsExpressAd = ttAdManager.isExpressAd(mPlacementId, adm);
                mIsFullVideoAd = ttAdManager.isFullScreenVideoAd(mPlacementId, adm);
            }

            /** obtain traditional or express interstitial extra parameters */
            if (!mIsFullVideoAd) {
                float[] adSize = PangleSharedUtil.getAdSizeSafely(localExtras, AD_WIDTH, AD_HEIGHT);
                mAdWidth = adSize[0];
                mAdHeight = adSize[1];
            }

            checkSize(mIsExpressAd);
        }

        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "adWidth =" + mAdWidth + "，adHeight=" + mAdHeight + ",placementId=" + mPlacementId + ",isExpressAd=" + mIsExpressAd);

        if (ttAdManager == null) {
            if (customEventInterstitialListener != null) {
                customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
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
                .withBid(adm);
        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
        if (!mIsFullVideoAd) {
            /** request Interstitial */
            if (mIsExpressAd) {
                MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME, "Loading Pangle express interstitial ad");
                adSlotBuilder.setExpressViewAcceptedSize(mAdWidth, mAdHeight);
                mExpressInterstitialLoader = new PangleAdInterstitialExpressLoader(mContext, customEventInterstitialListener);
                mExpressInterstitialLoader.loadExpressInterstitialAd(adSlotBuilder.build(), ttAdNative);
            } else {
                if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
                    /** ORIENTATION_PORTRAIT 2:3 */
                    mAdWidth = PangleSharedUtil.getScreenWidth(mContext);
                    mAdHeight = (3 * mAdWidth) / 2;
                } else {
                    /**  3:2 = w:h */
                    mAdHeight = PangleSharedUtil.getScreenHeight(mContext);
                    mAdWidth = (3 * mAdHeight) / 2;
                }
                MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME, "Loading Pangle traditional interstitial ad");
                adSlotBuilder.setNativeAdType(AdSlot.TYPE_INTERACTION_AD);
                adSlotBuilder.setImageAcceptedSize((int) mAdWidth, (int) mAdHeight);
                mNativeInterstitialLoader = new PangleAdInterstitialNativeLoader(mContext, mOrientation, customEventInterstitialListener);
                mNativeInterstitialLoader.loadAdNativeInterstitial(adSlotBuilder.build(), ttAdNative);
            }
        } else {
            MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME, "Loading Pangle FullVideoAd ad");
            /**  request FullVideoAd */
            adSlotBuilder.setImageAcceptedSize(1080, 1920);
            mFullVideoLoader = new PangleAdInterstitialFullVideoLoader(mContext, customEventInterstitialListener);
            mFullVideoLoader.loadAdFullVideoListener(adSlotBuilder.build(), ttAdNative);
        }
    }


    private void checkSize(boolean isExpressAd) {
        if (isExpressAd) {
            if (mAdWidth <= 0) {
                mAdWidth = 300;
                mAdWidth = 450;
            }
            if (mAdHeight < 0) {
                mAdHeight = 0;
            }
        } else {
            if (mAdWidth <= 0 || mAdHeight <= 0) {
                mAdWidth = 600;
                mAdHeight = 900;
            }
        }
    }


    @Override
    protected void showInterstitial() {
        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
        boolean hasShow = false;
        if (!mIsFullVideoAd) {
            if (mIsExpressAd) {
                if (mExpressInterstitialLoader != null && mContext instanceof Activity) {
                    mExpressInterstitialLoader.showInterstitial((Activity) mContext);
                    hasShow = true;
                }
            } else {
                if (mNativeInterstitialLoader != null && mNativeInterstitialLoader.isReady()) {
                    mNativeInterstitialLoader.showNativeInterstitialActiviy();
                    hasShow = true;
                }
            }
        } else {
            if (mFullVideoLoader != null && mContext instanceof Activity) {
                mFullVideoLoader.showFullVideo((Activity) mContext);
                hasShow = true;
            }
        }

        if (!hasShow && customEventInterstitialListener != null) {
            MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME);
            customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.UNSPECIFIED);
        }
    }

    private static String getAdNetworkId() {
        return mPlacementId;
    }

    @Override
    protected void onInvalidate() {
        if (!mIsFullVideoAd) {
            if (mIsExpressAd) {
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
                if (mInterstitialListener != null) {
                    mInterstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
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
                    MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                            MoPubErrorCode.NETWORK_NO_FILL);
                    if (mInterstitialListener != null) {
                        mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NO_FILL);
                    }
                    return;
                }
                MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
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
                    MoPubLog.log(getAdNetworkId(), MoPubLog.AdLogEvent.SHOW_SUCCESS, ADAPTER_NAME);
                    if (mInterstitialListener != null) {
                        mInterstitialListener.onInterstitialShown();
                    }
                }

                @Override
                public void onInterstitialClicked() {
                    MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
                    if (mInterstitialListener != null) {
                        mInterstitialListener.onInterstitialClicked();
                    }
                }

                @Override
                public void onInterstitialImpression() {
                    MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
                    if (mInterstitialListener != null) {
                        mInterstitialListener.onInterstitialImpression();
                    }
                }

                @Override
                public void onLeaveApplication() {
                    MoPubLog.log(getAdNetworkId(), WILL_LEAVE_APPLICATION, ADAPTER_NAME);
                    if (mInterstitialListener != null) {
                        mInterstitialListener.onLeaveApplication();
                    }
                }

                @Override
                public void onInterstitialDismissed() {
                    MoPubLog.log(getAdNetworkId(), DID_DISAPPEAR, ADAPTER_NAME);
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
                if (mInterstitialListener != null) {
                    mInterstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
                return;
            }
            ttAdNative.loadInteractionExpressAd(adSlot, mInterstitialAdExpressAdListener);
        }

        private TTAdNative.NativeExpressAdListener mInterstitialAdExpressAdListener = new TTAdNative.NativeExpressAdListener() {
            @Override
            public void onError(int code, String message) {
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, PangleSharedUtil.mapErrorCode(code), message);
                if (mInterstitialListener != null) {
                    mInterstitialListener.onInterstitialFailed(PangleSharedUtil.mapErrorCode(code));
                }
            }

            @Override
            public void onNativeExpressAdLoad(List<TTNativeExpressAd> ads) {
                if (ads == null || ads.size() == 0) {
                    if (mInterstitialListener != null) {
                        mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NO_FILL);
                    }
                    return;
                }
                MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
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
                MoPubLog.log(getAdNetworkId(), DID_DISAPPEAR, ADAPTER_NAME);
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "TTNativeExpressAd-AdInteractionListener ad onAdDismiss");
                if (mInterstitialListener != null) {
                    mInterstitialListener.onInterstitialDismissed();
                }
            }

            @Override
            public void onAdClicked(View view, int type) {
                MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "TTNativeExpressAd-AdInteractionListener ad onAdClicked");
                if (mInterstitialListener != null) {
                    mInterstitialListener.onInterstitialClicked();
                }
            }

            @Override
            public void onAdShow(View view, int type) {
                MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME, "Pangle adInterstitial express ad show");
                if (mInterstitialListener != null) {
                    mInterstitialListener.onInterstitialImpression();
                }
            }

            @Override
            public void onRenderFail(View view, String msg, int code) {
                MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME, "onInterstitialFailed ad onRenderFail msg = " + msg + "，code=" + code);
                MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.RENDER_PROCESS_GONE_UNSPECIFIED.getIntCode(),
                        msg);
                if (mInterstitialListener != null) {
                    mInterstitialListener.onInterstitialFailed(MoPubErrorCode.RENDER_PROCESS_GONE_UNSPECIFIED);
                }
            }

            @Override
            public void onRenderSuccess(View view, float width, float height) {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "render success");
                isRenderLoaded.set(true);
                if (mInterstitialListener != null) {
                    mInterstitialListener.onInterstitialLoaded();
                }
            }
        };


        public void showInterstitial(Activity activity) {
            if (mTTInterstitialExpressAd != null && isRenderLoaded.get()) {
                MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
                mTTInterstitialExpressAd.showInteractionExpressAd(activity);
            } else {
                MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME,
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
                if (mFullVideoListener != null) {
                    mFullVideoListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
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
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, "Loading Full Video creative encountered an error: " + PangleSharedUtil.mapErrorCode(code).toString() + ",error message:" + message);
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
                    MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);

                } else {
                    if (mFullVideoListener != null) {
                        mFullVideoListener.onInterstitialFailed(PangleSharedUtil.mapErrorCode(PangleSharedUtil.NO_AD));
                    }
                    MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME);
                }
            }

            @Override
            public void onFullScreenVideoCached() {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, " Pangle onFullScreenVideoCached invoke !");
            }
        };

        private TTFullScreenVideoAd.FullScreenVideoAdInteractionListener mFullScreenVideoAdInteractionListener = new TTFullScreenVideoAd.FullScreenVideoAdInteractionListener() {

            @Override
            public void onAdShow() {
                MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
                if (mFullVideoListener != null) {
                    mFullVideoListener.onInterstitialShown();
                }
            }

            @Override
            public void onAdVideoBarClick() {
                MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
                if (mFullVideoListener != null) {
                    mFullVideoListener.onInterstitialClicked();
                }
            }

            @Override
            public void onAdClose() {
                MoPubLog.log(getAdNetworkId(), DID_DISAPPEAR, ADAPTER_NAME);
                if (mFullVideoListener != null) {
                    mFullVideoListener.onInterstitialDismissed();
                }
            }

            @Override
            public void onVideoComplete() {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Pangle TTFullScreenVideoAd onVideoComplete...");
            }

            @Override
            public void onSkippedVideo() {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Pangle TTFullScreenVideoAd onSkippedVideo...");
            }
        };

    }
}

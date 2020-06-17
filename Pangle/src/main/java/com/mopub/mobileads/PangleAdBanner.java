package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdDislike;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTNativeAd;
import com.bytedance.sdk.openadsdk.TTNativeExpressAd;
import com.bytedance.sdk.openadsdk.adapter.MediationAdapterUtil;
import com.mopub.common.DataKeys;
import com.mopub.common.LifecycleListener;
import com.mopub.common.Preconditions;
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

public class PangleAdBanner extends BaseAd {

    private static final String ADAPTER_NAME = PangleAdBanner.class.getSimpleName();

    private static String mPlacementId;
    private PangleAdapterConfiguration mPangleAdapterConfiguration;

    private Context mContext;

    private PangleAdBannerExpressLoader mAdExpressBannerLoader;
    private PangleAdBannerTraditionalLoader mAdTraditionalBannerLoader;

    private float mBannerWidth;
    private float mBannerHeight;
    private View mBannerView;

    public PangleAdBanner() {
        mPangleAdapterConfiguration = new PangleAdapterConfiguration();
    }

    @Override
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        mContext = context;
        setAutomaticImpressionAndClickTracking(false);
        TTAdManager adManager = null;
        String adm = null;
        boolean isExpressAd = false;

        final Map<String, String> extras = adData.getExtras();

        if (extras != null && !extras.isEmpty()) {
            mPangleAdapterConfiguration.setCachedInitializationParameters(context, extras);
            /** Obtain ad placement id from MoPub UI */
            mPlacementId = extras.get(PangleAdapterConfiguration.KEY_EXTRA_AD_PLACEMENT_ID);

            if (TextUtils.isEmpty(mPlacementId)) {
                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME,
                        "Invalid Pangle placement ID. Failing ad request. " +
                                "Ensure the ad placement ID is valid on the MoPub dashboard.");
                return;
            }
            adm = extras.get(DataKeys.ADM_KEY);

            /** Init Pangle SDK if fail to initialize in the adapterConfiguration */
            final String appId = extras.get(PangleAdapterConfiguration.KEY_EXTRA_APP_ID);
            PangleAdapterConfiguration.pangleSdkInit(context, appId);
            adManager = PangleAdapterConfiguration.getPangleSdkManager();
        }

        if (adManager != null) {
            isExpressAd = adManager.isExpressAd(mPlacementId, adm);
        } else {
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
            }
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_INVALID_STATE.getIntCode(),
                    MoPubErrorCode.NETWORK_INVALID_STATE);
            return;
        }

        float[] bannerAdSizeAdapterSafely = PangleAdapterConfiguration.getBannerAdSizeAdapter(adData);
        mBannerWidth = bannerAdSizeAdapterSafely[0];
        mBannerHeight = bannerAdSizeAdapterSafely[1];

        resolveBannerAdSize(isExpressAd);

        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "BannerWidth =" + mBannerWidth + "，bannerHeight=" + mBannerHeight + ",placementId=" + mPlacementId);
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "LoadBanner method placementId：" + mPlacementId);

        final AdSlot.Builder adSlotBuilder = new AdSlot.Builder()
                .setCodeId(mPlacementId)
                .setSupportDeepLink(true)
                .setAdCount(1) /* Mediation doesn't support multiple ad responses within the single ad request. */
                .setImageAcceptedSize((int) mBannerWidth, (int) mBannerHeight)
                .withBid(adm);

        if (isExpressAd) {
            adSlotBuilder.setExpressViewAcceptedSize(mBannerWidth, mBannerHeight);
        } else {
            adSlotBuilder.setNativeAdType(AdSlot.TYPE_BANNER);
        }

        /**
         *  Request ad express banner @link to and see the difference between express banner and traditional banner
         */
        if (isExpressAd) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Loading Pangle express banner ad");
            mAdExpressBannerLoader = new PangleAdBannerExpressLoader(mContext);
            mAdExpressBannerLoader.loadAdExpressBanner(adSlotBuilder.build(), adManager.createAdNative(mContext));
        } else {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Loading Pangle traditional banner ad");
            mAdTraditionalBannerLoader = new PangleAdBannerTraditionalLoader(mContext, mBannerWidth, mBannerHeight);
            mAdTraditionalBannerLoader.loadAdTraditionalBanner(adSlotBuilder.build(), adManager.createAdNative(mContext));
        }
        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    @NonNull
    public String getAdNetworkId() {
        return mPlacementId == null ? "" : mPlacementId;
    }

    private void resolveBannerAdSize(boolean isExpressAd) {
        if (isExpressAd) {
            if (mBannerWidth <= 0) {
                mBannerWidth = 600;
                mBannerHeight = 0;
            }
            if (mBannerHeight < 0) {
                mBannerHeight = 0;
            }
        } else {
            if (mBannerWidth <= 0 || mBannerHeight <= 0) {
                mBannerWidth = 600;
                mBannerHeight = 90;
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

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull AdData adData) {
        return false;
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @Override
    public View getAdView() {
        return mBannerView;
    }


    /**
     * Pangle traditional banner callback interface
     */
    public class PangleAdBannerTraditionalLoader {

        private Context mContext;
        private float mBannerWidth;
        private float mBannerHeight;

        PangleAdBannerTraditionalLoader(Context context, float bannerWidth, float bannerHeight) {
            this.mContext = context;
            this.mBannerWidth = bannerWidth;
            this.mBannerHeight = bannerHeight;
        }

        void loadAdTraditionalBanner(AdSlot adSlot, TTAdNative adInstance) {
            if (mContext == null || adSlot == null || adInstance == null || TextUtils.isEmpty(adSlot.getCodeId())) {
                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
                return;
            }
            adInstance.loadNativeAd(adSlot, mNativeAdListener);
        }

        private TTAdNative.NativeAdListener mNativeAdListener = new TTAdNative.NativeAdListener() {
            @Override
            public void onError(int code, String message) {
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                        MoPubErrorCode.NETWORK_NO_FILL);
                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(PangleAdapterConfiguration.mapErrorCode(code));
                }
            }

            @Override
            public void onNativeAdLoad(List<TTNativeAd> ads) {
                if (ads == null || ads.get(0) == null) {
                    MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                            MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                            MoPubErrorCode.NETWORK_NO_FILL);
                    if (mLoadListener != null) {
                        mLoadListener.onAdLoadFailed(MoPubErrorCode.NO_FILL);
                    }
                    return;
                }

                mBannerView = MediationAdapterUtil.setAdDataAndBuildBannerView(mContext, ads.get(0), mAdInteractionListener, mBannerWidth, mBannerHeight);

                if (mBannerView == null) {
                    if (mLoadListener != null) {
                        mLoadListener.onAdLoadFailed(MoPubErrorCode.NO_FILL);
                    }
                    return;
                }

                MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
                if (mLoadListener != null) {
                    mLoadListener.onAdLoaded();
                }
            }
        };

        TTNativeAd.AdInteractionListener mAdInteractionListener = new TTNativeAd.AdInteractionListener() {
            @Override
            public void onAdClicked(View view, TTNativeAd ad) {
                MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
                if (mInteractionListener != null) {
                    mInteractionListener.onAdClicked();
                }
            }

            @Override
            public void onAdCreativeClick(View view, TTNativeAd ad) {
                MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
                if (mInteractionListener != null) {
                    mInteractionListener.onAdClicked();
                }
            }

            @Override
            public void onAdShow(TTNativeAd ad) {
                MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
                if (mInteractionListener != null) {
                    mInteractionListener.onAdImpression();
                }
            }
        };

        public void destroy() {
            mLoadListener = null;
            mNativeAdListener = null;
        }
    }


    /**
     * Pangle express banner callback interface
     */
    public class PangleAdBannerExpressLoader {

        private TTNativeExpressAd mTTNativeExpressAd;
        private Context mContext;

        PangleAdBannerExpressLoader(Context context) {
            this.mContext = context;
        }

        public void loadAdExpressBanner(AdSlot adSlot, TTAdNative adInstance) {
            if (mContext == null || adSlot == null || adInstance == null || TextUtils.isEmpty(adSlot.getCodeId())) {
                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
                return;
            }
            adInstance.loadBannerExpressAd(adSlot, mTTNativeExpressAdListener);
        }


        private TTAdNative.NativeExpressAdListener mTTNativeExpressAdListener = new TTAdNative.NativeExpressAdListener() {
            @Override
            public void onError(int code, String message) {
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, "express banner ad  onAdLoadFailed.-code=" + code + "," + message);
                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(PangleAdapterConfiguration.mapErrorCode(code));
                }
            }

            @Override
            public void onNativeExpressAdLoad(List<TTNativeExpressAd> ads) {
                if (ads == null || ads.size() == 0) {
                    MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                            MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                            MoPubErrorCode.NETWORK_NO_FILL);
                    if (mLoadListener != null) {
                        mLoadListener.onAdLoadFailed(MoPubErrorCode.NO_FILL);
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
                        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Pangle DislikeInteractionCallback click value=" + value);
                    }

                    @Override
                    public void onCancel() {
                        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Pangle DislikeInteractionCallbac cancel click...");
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
                if (mInteractionListener != null) {
                    mInteractionListener.onAdClicked();
                }
            }

            @Override
            public void onAdShow(View view, int type) {
                MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
                if (mInteractionListener != null) {
                    mInteractionListener.onAdImpression();
                }
            }

            @Override
            public void onRenderFail(View view, String msg, int code) {
                MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME);
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME,
                        "banner ad onRenderFail msg = " + msg + "，code=" + code);

                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(MoPubErrorCode.RENDER_PROCESS_GONE_UNSPECIFIED);
                }
            }

            @Override
            public void onRenderSuccess(View view, float width, float height) {
                MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
                MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
                mBannerView = view;
                if (mLoadListener != null) {
                    mLoadListener.onAdLoaded();
                }
            }
        };

        public void destroy() {
            if (mTTNativeExpressAd != null) {
                mTTNativeExpressAd.destroy();
                mTTNativeExpressAd = null;
            }

            this.mExpressAdInteractionListener = null;
            this.mTTNativeExpressAdListener = null;
        }
    }
}

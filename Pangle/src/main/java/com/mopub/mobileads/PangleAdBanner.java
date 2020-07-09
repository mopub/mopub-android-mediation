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
import com.bytedance.sdk.openadsdk.TTNativeExpressAd;
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

    private int mBannerWidth;
    private int mBannerHeight;
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

        final Map<String, String> extras = adData.getExtras();

        if (extras != null && !extras.isEmpty()) {
            mPlacementId = extras.get(PangleAdapterConfiguration.AD_PLACEMENT_ID_EXTRA_KEY);

            if (TextUtils.isEmpty(mPlacementId)) {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME,
                        "Invalid Pangle placement ID. Failing ad request. " +
                                "Ensure the ad placement ID is valid on the MoPub dashboard.");

                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
                return;
            }

            adm = extras.get(DataKeys.ADM_KEY);

            /** Init Pangle SDK if fail to initialize in the PangleAdapterConfiguration */
            final String appId = extras.get(PangleAdapterConfiguration.APP_ID_EXTRA_KEY);
            PangleAdapterConfiguration.pangleSdkInit(context, appId);
            adManager = PangleAdapterConfiguration.getPangleSdkManager();
            mPangleAdapterConfiguration.setCachedInitializationParameters(context, extras);
        }

        if (adManager != null) {
            if (!adManager.isExpressAd(mPlacementId, adm)) {
                MoPubLog.log(getAdNetworkId(), CUSTOM, "Invalid Pangle placement ID. " +
                        "Make sure the ad placement ID is Express format in Pangle UI.");

                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
                return;
            }
        } else {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_INVALID_STATE.getIntCode(),
                    MoPubErrorCode.NETWORK_INVALID_STATE);

            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
            }
            return;
        }

        int[] safeBannerSizes = getAdSize(adData);
        mBannerWidth = safeBannerSizes[0];
        mBannerHeight = safeBannerSizes[1];

        resolveAdSize();

        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "BannerWidth = " + mBannerWidth +
                ", BannerHeight = " + mBannerHeight);

        final AdSlot.Builder adSlotBuilder = new AdSlot.Builder()
                .setCodeId(mPlacementId)
                .setSupportDeepLink(true)
                .setImageAcceptedSize(mBannerWidth, mBannerHeight)
                .withBid(adm);

        adSlotBuilder.setExpressViewAcceptedSize((float) mBannerWidth, (float) mBannerHeight);

        mAdExpressBannerLoader = new PangleAdBannerExpressLoader(mContext);
        mAdExpressBannerLoader.loadAdExpressBanner(adSlotBuilder.build(), adManager.createAdNative(mContext));
        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    @NonNull
    public String getAdNetworkId() {
        return mPlacementId == null ? "" : mPlacementId;
    }

    /**
     * Pangle banner support size:
     * 600*300, 600*400, 600*500, 600*260, 600*90, 600*150, 640*100, 690*388
     * Please refer to our documentation for Pangle size mapping.
     * https://developers.mopub.com//publishers/mediation/networks/pangle/#set-up-banner-express-ad-size-in-pangle-ui
     *
     * @param adData for the banner information
     * @return Array of desire banner size in safe area.
     *
     * Banner size mapping according to the incoming size in adapter
     * and selected size on Pangle platform.
     * Pangle will return the banner ads with appropriate size.
     *
     */
    public static int[] getAdSize(AdData adData) {
        int[] adSize = new int[]{0, 0};

        if (adData == null) {
            adSize = new int[]{600, 90};
            return adSize;
        }

        final Object oWidth = adData.getAdWidth();
        if (oWidth instanceof Integer) {
            adSize[0] = (Integer) oWidth;
        }

        final Object oHeight = adData.getAdHeight();
        if (oHeight instanceof Integer) {
            adSize[1] = (Integer) oHeight;
        }

        if (oWidth != null) {
            final float ratio = adSize[0] / adSize[1];

            if (ratio == 600f / 500f || ratio == 600f / 400f ||
                    ratio == 690f / 388f || ratio == 600f / 300f ||
                    ratio == 600f / 260f || ratio == 600f / 150f ||
                    ratio == 640f / 100f || ratio == 600f / 90f
            ) {
                return adSize;
            }

            final float factor = 0.25f;
            float widthRatio = adSize[0] / 600f;
            if (widthRatio <= 0.5f + factor) {
                widthRatio = 0.5f;
            } else if (widthRatio <= 1f + factor) {
                widthRatio = 1f;
            } else if (widthRatio <= 1.5f + factor) {
                widthRatio = 1.5f;
            } else {
                widthRatio = 2f;
            }

            if (ratio < 600f / 500f) { //1.2f
                adSize[0] = (int) (600f * widthRatio);
                adSize[1] = (int) (500f * widthRatio);
            } else if (ratio < 600f / 400f) {//1.5f
                adSize[0] = (int) (600f * widthRatio);
                adSize[1] = (int) (400f * widthRatio);
            } else if (ratio < 690f / 388f) { //1.77f
                widthRatio = adSize[0] / 690f;
                if (widthRatio < 0.5f + factor) {
                    widthRatio = 0.5f;
                } else if (widthRatio < 1f + factor) {
                    widthRatio = 1f;
                } else if (widthRatio < 1.5f + factor) {
                    widthRatio = 1.5f;
                } else {
                    widthRatio = 2f;
                }
                adSize[0] = (int) (690f * widthRatio);
                adSize[1] = (int) (388f * widthRatio);
            } else if (ratio < 600f / 300f) { // 2f
                adSize[0] = (int) (600f * widthRatio);
                adSize[1] = (int) (300f * widthRatio);
            } else if (ratio < 600f / 260f) {//2.3f
                adSize[0] = (int) (600f * widthRatio);
                adSize[1] = (int) (260f * widthRatio);
            } else if (ratio < 600f / 150f) {// 4.0f
                adSize[0] = (int) (600f * widthRatio);
                adSize[1] = (int) (150f * widthRatio);
            } else if (ratio < 640f / 100f) { //6.4f
                widthRatio = adSize[0] / 640f;
                if (widthRatio < 0.5f + factor) {
                    widthRatio = 0.5f;
                } else if (widthRatio < 1f + factor) {
                    widthRatio = 1f;
                } else if (widthRatio < 1.5f + factor) {
                    widthRatio = 1.5f;
                } else {
                    widthRatio = 2f;
                }
                adSize[0] = (int) (640f * widthRatio);
                adSize[1] = (int) (100f * widthRatio);

            } else if (ratio < 600f / 90f || ratio > 600f / 90f) {//6.67f
                adSize[0] = (int) (600f * widthRatio);
                adSize[1] = (int) (90f * widthRatio);
            }
        }

        return adSize;
    }

    private void resolveAdSize() {
        if (mBannerWidth <= 0) {
            mBannerWidth = 600;
            mBannerHeight = 0;
        }

        if (mBannerHeight < 0) {
            mBannerHeight = 0;
        }
    }

    @Override
    protected void onInvalidate() {
        if (mAdExpressBannerLoader != null) {
            mAdExpressBannerLoader.destroy();
            mAdExpressBannerLoader = null;
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
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                        "onAdLoadFailed() error code: " + code + ", " + message);

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
                        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Pangle DislikeInteractionCallback onSelected(): " + value);
                    }

                    @Override
                    public void onCancel() {
                        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Pangle DislikeInteractionCallback onCancel()");
                    }
                });
            }
        }

        /**
         * Pangle express banner render callback interface
         */
        private TTNativeExpressAd.ExpressAdInteractionListener mExpressAdInteractionListener =
                new TTNativeExpressAd.ExpressAdInteractionListener() {
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
                            mInteractionListener.onAdShown();
                            mInteractionListener.onAdImpression();
                        }
                    }

                    @Override
                    public void onRenderFail(View view, String msg, int code) {
                        MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME);
                        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME,
                                "Banner ad onRenderFail() called with message: " + msg
                                        + ", and code: " + code);

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
            mBannerView = null;
        }
    }
}

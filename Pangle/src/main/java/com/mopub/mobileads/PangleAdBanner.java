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

        setAutomaticImpressionAndClickTracking(false);

        mContext = context;
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

            mPangleAdapterConfiguration.setCachedInitializationParameters(context, extras);
        }

        final TTAdManager adManager = PangleAdapterConfiguration.getPangleSdkManager();
        if (adManager == null) {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_INVALID_STATE.getIntCode(),
                    MoPubErrorCode.NETWORK_INVALID_STATE);

            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
            }
            return;
        }

        final int[] safeBannerSizes = getAdSize(adData);
        mBannerWidth = safeBannerSizes[0];
        mBannerHeight = safeBannerSizes[1];

        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "BannerWidth = " + mBannerWidth +
                ", BannerHeight = " + mBannerHeight);

        final AdSlot.Builder adSlotBuilder = new AdSlot.Builder()
                .setCodeId(mPlacementId)
                .isExpressAd(true)
                .setExpressViewAcceptedSize(mBannerWidth, mBannerHeight)
                .withBid(adm);

        mAdExpressBannerLoader = new PangleAdBannerExpressLoader(mContext);
        mAdExpressBannerLoader.loadAdExpressBanner(adSlotBuilder.build(), adManager.createAdNative(mContext));

        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    @NonNull
    public String getAdNetworkId() {
        return mPlacementId == null ? "" : mPlacementId;
    }

    /**
     * Banner size ratio mapping according to the incoming size in adapter and selected size on Pangle platform.
     * Pangle will return the banner ads with appropriate size.
     * <p>
     * Please refer to our documentation for Pangle size mapping.
     * https://developers.mopub.com//publishers/mediation/networks/pangle/#set-up-banner-express-ad-size-in-pangle-ui
     *
     * @param adData for the banner information
     * @return Array of desire banner size in safe area.
     */
    public static int[] getAdSize(AdData adData) {
        int[] adSize = new int[]{0, 0};

        if (adData == null || adData.getAdWidth() == null || adData.getAdHeight() == null) {
            adSize = new int[]{600, 500};
            return adSize;
        }

        int expectWidth = adData.getAdWidth();
        int expectHeight = adData.getAdHeight();
        adSize[0] = adData.getAdWidth();
        adSize[1] = adData.getAdHeight();

        final float dimensionRatio = 1.0f * adSize[0] / adSize[1];

        if (dimensionRatio == 600f / 500f || dimensionRatio == 640f / 100f) {
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

        if (dimensionRatio < 600f / 500f) { // 1.2f
            adSize[0] = (int) (600f * widthRatio);
            adSize[1] = (int) (500f * widthRatio);
        } else { // 6.4f = 640f / 100f
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
        }

        // Ensure that the width and height of the ad will not exceed the actual container width and height
        if (adSize[0] > expectWidth) {
            adSize[1] = (int) Math.floor(adSize[1] / (1.0f * adSize[0] / expectWidth));
            adSize[0] = expectWidth;
        }
        if (adSize[1] > expectHeight) {
            adSize[0] = (int) Math.floor(adSize[0] / (1.0f * adSize[1] / expectHeight));
            adSize[1] = expectHeight;
        }

        if (adSize[0] <= 0) {
            adSize[0] = 600;
            adSize[1] = 0;
        }

        if (adSize[1] < 0) {
            adSize[1] = 0;
        }

        return adSize;
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
            /** Pangle provided a dislike callback, it is an optional callback method when user click they dislike the ad.
             *  Please reach out to Pangle team if you want to implement it.
             */
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

                    @Override
                    public void onRefuse() {
                        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Pangle DislikeInteractionCallback onRefuse()");
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
                        MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME);
                        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME,
                                "Pangle banner ad failed to render with message: " + msg
                                        + ", and code: " + code);

                        if (mLoadListener != null) {
                            mLoadListener.onAdLoadFailed(MoPubErrorCode.INLINE_LOAD_ERROR);
                        }
                    }

                    @Override
                    public void onRenderSuccess(View view, float width, float height) {
                        mBannerView = view;

                        MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
                        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);

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

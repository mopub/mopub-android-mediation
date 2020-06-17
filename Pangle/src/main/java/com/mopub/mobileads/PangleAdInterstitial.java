package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTFullScreenVideoAd;
import com.bytedance.sdk.openadsdk.TTNativeAd;
import com.bytedance.sdk.openadsdk.TTNativeExpressAd;
import com.bytedance.sdk.openadsdk.adapter.PangleAdInterstitialActivity;
import com.mopub.common.DataKeys;
import com.mopub.common.LifecycleListener;
import com.mopub.common.Preconditions;
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

public class PangleAdInterstitial extends BaseAd {
    private static final String ADAPTER_NAME = PangleAdInterstitial.class.getSimpleName();
    private static final String KEY_EXTRA_AD_WIDTH = "ad_width";
    private static final String KEY_EXTRA_AD_HEIGHT = "ad_height";

    private static String mPlacementId;
    private Context mContext;
    private PangleAdapterConfiguration mPangleAdapterConfiguration;
    private PangleAdInterstitialExpressLoader mExpressInterstitialLoader;
    private PangleAdInterstitialNativeLoader mTraditionalInterstitialLoader;
    private PangleAdInterstitialFullVideoLoader mFullVideoLoader;
    private boolean mIsExpressAd = false;
    private boolean mIsFullVideoAd;
    private float mAdWidth = 0;
    private float mAdHeight = 0;

    public PangleAdInterstitial() {
        mPangleAdapterConfiguration = new PangleAdapterConfiguration();
    }

    @Override
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        mContext = context;
        setAutomaticImpressionAndClickTracking(false);
        final Map<String, String> extras = adData.getExtras();
        int mOrientation = mContext.getResources().getConfiguration().orientation;
        mPangleAdapterConfiguration.setCachedInitializationParameters(context, extras);

        String adm = null;

        TTAdManager adManager = null;
        TTAdNative adInstance = null;

        if (extras != null && !extras.isEmpty()) {
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

            if (adManager != null) {
                mIsExpressAd = adManager.isExpressAd(mPlacementId, adm);
                mIsFullVideoAd = adManager.isFullScreenVideoAd(mPlacementId, adm);
            }

            /** Init Pangle SDK if fail to initialize in the adapterConfiguration */
            final String appId = extras.get(PangleAdapterConfiguration.KEY_EXTRA_APP_ID);
            PangleAdapterConfiguration.pangleSdkInit(context, appId);
            adManager = PangleAdapterConfiguration.getPangleSdkManager();
            adInstance = adManager.createAdNative(context.getApplicationContext());
            mPangleAdapterConfiguration.setCachedInitializationParameters(context, extras);

            /** Obtain traditional or express interstitial extra parameters */
            if (!mIsFullVideoAd) {
                final float[] adSize = PangleAdapterConfiguration.getAdSizeSafely(extras, KEY_EXTRA_AD_WIDTH, KEY_EXTRA_AD_HEIGHT);
                mAdWidth = adSize[0];
                mAdHeight = adSize[1];
            }

            resolveIntersititalAdSize(mIsExpressAd);
        }

        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "adWidth =" + mAdWidth + "，adHeight="
                + mAdHeight + ",placementId=" + mPlacementId + ",isExpressAd=" + mIsExpressAd);

        if (adManager == null) {
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
            }
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_INVALID_STATE.getIntCode(),
                    MoPubErrorCode.NETWORK_INVALID_STATE);
            return;
        }

        final AdSlot.Builder adSlotBuilder = new AdSlot.Builder()
                .setCodeId(mPlacementId)
                .setSupportDeepLink(true)
                .setAdCount(1)  /* Mediation doesn't support multiple ad responses within the single ad request. */
                .withBid(adm);
        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);

        if (!mIsFullVideoAd) {
            /** request Interstitial */
            if (mIsExpressAd) {
                MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME, "Loading Pangle express interstitial ad");
                adSlotBuilder.setExpressViewAcceptedSize(mAdWidth, mAdHeight);
                mExpressInterstitialLoader = new PangleAdInterstitialExpressLoader(mContext);
                mExpressInterstitialLoader.loadExpressInterstitialAd(adSlotBuilder.build(), adInstance);
            } else {
                if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
                    /** ORIENTATION_PORTRAIT 2:3 */
                    mAdWidth = PangleAdapterConfiguration.getScreenWidth(mContext);
                    mAdHeight = (3 * mAdWidth) / 2;
                } else {
                    /**  3:2 = w:h */
                    mAdHeight = PangleAdapterConfiguration.getScreenHeight(mContext);
                    mAdWidth = (3 * mAdHeight) / 2;
                }
                MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME, "Loading Pangle traditional interstitial ad");
                adSlotBuilder.setNativeAdType(AdSlot.TYPE_INTERACTION_AD);
                adSlotBuilder.setImageAcceptedSize((int) mAdWidth, (int) mAdHeight);
                mTraditionalInterstitialLoader = new PangleAdInterstitialNativeLoader(mContext, mOrientation);
                mTraditionalInterstitialLoader.loadAdNativeInterstitial(adSlotBuilder.build(), adInstance);
            }
        } else {
            MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME, "Loading Pangle FullVideoAd ad");
            /**  request FullVideoAd */
            adSlotBuilder.setImageAcceptedSize(1080, 1920);
            mFullVideoLoader = new PangleAdInterstitialFullVideoLoader(mContext);
            mFullVideoLoader.loadAdFullVideoListener(adSlotBuilder.build(), adInstance);
        }
    }

    private void resolveIntersititalAdSize(boolean isExpressAd) {
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
    protected void show() {
        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
        boolean hasShow = false;
        if (!mIsFullVideoAd) {
            if (mIsExpressAd) {
                if (mExpressInterstitialLoader != null && mContext instanceof Activity) {
                    mExpressInterstitialLoader.showInterstitial((Activity) mContext);
                    hasShow = true;
                }
            } else {
                if (mTraditionalInterstitialLoader != null && mTraditionalInterstitialLoader.isReady()) {
                    mTraditionalInterstitialLoader.showTraditionalInterstitialActivity();
                    hasShow = true;
                }
            }
        } else {
            if (mFullVideoLoader != null && mContext instanceof Activity) {
                mFullVideoLoader.showFullVideo((Activity) mContext);
                hasShow = true;
            }
        }

        if (!hasShow) {
            MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME);
            if (mInteractionListener != null) {
                mInteractionListener.onAdFailed(MoPubErrorCode.FULLSCREEN_SHOW_ERROR);
            }
        }
    }

    @NonNull
    public String getAdNetworkId() {
        return mPlacementId == null ? "" : mPlacementId;
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull AdData adData) throws Exception {
        return false;
    }

    @Override
    protected void onInvalidate() {
        if (!mIsFullVideoAd) {
            if (mIsExpressAd) {
                if (mExpressInterstitialLoader != null) {
                    mExpressInterstitialLoader.destroy();
                }
            } else {
                if (mTraditionalInterstitialLoader != null) {
                    mTraditionalInterstitialLoader.destroy();
                }
            }
        } else {
            if (mFullVideoLoader != null) {
                mFullVideoLoader.destroy();
            }
        }
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }


    /**
     * Traditional Interstitial Ad Loader
     */
    public class PangleAdInterstitialNativeLoader {
        private Context mContext;
        private boolean mIsLoading;
        private TTNativeAd mTTNativeAd;
        private int mOrientation;

        PangleAdInterstitialNativeLoader(Context context,
                                         int orientation) {
            this.mContext = context;
            this.mOrientation = orientation;
        }

        void loadAdNativeInterstitial(AdSlot adSlot, TTAdNative adInstance) {
            if (adInstance == null || mContext == null || adSlot == null || TextUtils.isEmpty(adSlot.getCodeId())) {
                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
                return;
            }
            adInstance.loadNativeAd(adSlot, mNativeAdListener);
        }

        TTAdNative.NativeAdListener mNativeAdListener = new TTAdNative.NativeAdListener() {
            @Override
            public void onError(int code, String message) {
                mIsLoading = false;
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, PangleAdapterConfiguration.mapErrorCode(code), message);
                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(PangleAdapterConfiguration.mapErrorCode(code));
                }
            }

            @Override
            public void onNativeAdLoad(List<TTNativeAd> ads) {
                mIsLoading = true;
                if (ads == null || ads.get(0) == null) {
                    MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                            MoPubErrorCode.NETWORK_NO_FILL);
                    if (mLoadListener != null) {
                        mLoadListener.onAdLoadFailed(MoPubErrorCode.NO_FILL);
                    }
                    return;
                }
                MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
                mTTNativeAd = ads.get(0);
                if (mLoadListener != null) {
                    mLoadListener.onAdLoaded();
                }
            }
        };

        private boolean isReady() {
            return mIsLoading;
        }

        private void showTraditionalInterstitialActivity() {
            PangleAdInterstitialActivity.showAd(mContext, mTTNativeAd,
                    mOrientation == Configuration.ORIENTATION_PORTRAIT ?
                            PangleAdInterstitialActivity.INTENT_TYPE_IMAGE_2_3 :
                            PangleAdInterstitialActivity.INTENT_TYPE_IMAGE_3_2,
                    new com.bytedance.sdk.openadsdk.CustomEventInterstitialListener() {
                        @Override
                        public void onInterstitialShown() {
                            MoPubLog.log(getAdNetworkId(), MoPubLog.AdLogEvent.SHOW_SUCCESS, ADAPTER_NAME);
                            if (mInteractionListener != null) {
                                mInteractionListener.onAdShown();
                            }
                        }

                        @Override
                        public void onInterstitialShowFail() {
                            MoPubLog.log(getAdNetworkId(), MoPubLog.AdLogEvent.SHOW_FAILED, ADAPTER_NAME);
                        }

                        @Override
                        public void onInterstitialClicked() {
                            MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
                            if (mInteractionListener != null) {
                                mInteractionListener.onAdClicked();
                            }
                        }

                        @Override
                        public void onInterstitialImpression() {
                            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Pangle traditional interstitial " +
                                    "logged impression.");
                            if (mInteractionListener != null) {
                                mInteractionListener.onAdImpression();
                            }
                        }

                        @Override
                        public void onLeaveApplication() {
                            MoPubLog.log(getAdNetworkId(), WILL_LEAVE_APPLICATION, ADAPTER_NAME);
                        }

                        @Override
                        public void onInterstitialDismissed() {
                            MoPubLog.log(getAdNetworkId(), DID_DISAPPEAR, ADAPTER_NAME);
                            if (mInteractionListener != null) {
                                mInteractionListener.onAdDismissed();
                            }
                        }
                    });
        }

        public void destroy() {
            mContext = null;
            mNativeAdListener = null;
            mTTNativeAd = null;
        }
    }


    /**
     * Express interstitial ad
     */
    public class PangleAdInterstitialExpressLoader {
        private Context mContext;
        private TTNativeExpressAd mTTInterstitialExpressAd;
        private AtomicBoolean isRenderLoaded = new AtomicBoolean(false);


        PangleAdInterstitialExpressLoader(Context context) {
            this.mContext = context;
        }

        void loadExpressInterstitialAd(AdSlot adSlot, TTAdNative adInstance) {
            if (adInstance == null || mContext == null || adSlot == null || TextUtils.isEmpty(adSlot.getCodeId())) {
                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
                return;
            }
            adInstance.loadInteractionExpressAd(adSlot, mInterstitialAdExpressAdListener);
        }

        private TTAdNative.NativeExpressAdListener mInterstitialAdExpressAdListener = new TTAdNative.NativeExpressAdListener() {
            @Override
            public void onError(int code, String message) {
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, PangleAdapterConfiguration.mapErrorCode(code), message);
                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(PangleAdapterConfiguration.mapErrorCode(code));
                }
            }

            @Override
            public void onNativeExpressAdLoad(List<TTNativeExpressAd> ads) {
                if (ads == null || ads.size() == 0) {
                    if (mLoadListener != null) {
                        mLoadListener.onAdLoadFailed(MoPubErrorCode.NO_FILL);
                    }
                    return;
                }
                mTTInterstitialExpressAd = ads.get(0);
                mTTInterstitialExpressAd.setExpressInteractionListener(mInterstitialExpressAdInteractionListener);

                /** The ad instance need to attach to the view which is loaded.*/
                mTTInterstitialExpressAd.render();
            }
        };

        /**
         * render callback
         */
        private TTNativeExpressAd.AdInteractionListener mInterstitialExpressAdInteractionListener =
                new TTNativeExpressAd.AdInteractionListener() {
                    @Override
                    public void onRenderSuccess(View view, float width, float height) {
                        MoPubLog.log(getAdNetworkId(), MoPubLog.AdLogEvent.LOAD_SUCCESS, ADAPTER_NAME);
                        isRenderLoaded.set(true);
                        if (mLoadListener != null) {
                            mLoadListener.onAdLoaded();
                        }
                    }

                    @Override
                    public void onRenderFail(View view, String msg, int code) {
                        MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME, "Express Ad onRenderFail msg = " + msg + "，code=" + code);
                        if (mInteractionListener != null) {
                            mInteractionListener.onAdFailed(MoPubErrorCode.INTERNAL_ERROR);
                        }
                    }

                    @Override
                    public void onAdDismiss() {
                        MoPubLog.log(getAdNetworkId(), DID_DISAPPEAR, ADAPTER_NAME);
                        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Express Ad onAdDismiss");
                        if (mInteractionListener != null) {
                            mInteractionListener.onAdDismissed();
                        }
                    }

                    @Override
                    public void onAdClicked(View view, int type) {
                        MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
                        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Express Ad onAdClicked");
                        if (mInteractionListener != null) {
                            mInteractionListener.onAdClicked();
                        }
                    }

                    @Override
                    public void onAdShow(View view, int type) {
                        MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME, "Express Ad onAdShow");
                        if (mInteractionListener != null) {
                            mInteractionListener.onAdShown();
                            mInteractionListener.onAdImpression();
                        }
                    }
                };


        public void showInterstitial(Activity activity) {
            if (mTTInterstitialExpressAd != null && isRenderLoaded.get()) {
                mTTInterstitialExpressAd.showInteractionExpressAd(activity);
            } else {
                MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                        MoPubErrorCode.NETWORK_NO_FILL);

                if (mInteractionListener != null) {
                    mInteractionListener.onAdFailed(MoPubErrorCode.NETWORK_NO_FILL);
                }
            }
        }

        public void destroy() {
            if (mTTInterstitialExpressAd != null) {
                mTTInterstitialExpressAd.destroy();
                mTTInterstitialExpressAd = null;
            }
            mInterstitialExpressAdInteractionListener = null;
            mInterstitialAdExpressAdListener = null;
            mContext = null;
        }

    }

    /**
     * Pangle full-video ad
     */
    public class PangleAdInterstitialFullVideoLoader {
        private Context mContext;
        private boolean mIsLoaded;
        private TTFullScreenVideoAd mTTFullScreenVideoAd;

        PangleAdInterstitialFullVideoLoader(Context context) {
            this.mContext = context;
        }

        void loadAdFullVideoListener(AdSlot adSlot, TTAdNative adInstance) {
            if (adInstance == null || mContext == null || adSlot == null || TextUtils.isEmpty(adSlot.getCodeId())) {
                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
                return;
            }
            adInstance.loadFullScreenVideoAd(adSlot, mLoadFullVideoAdListener);
        }

        void showFullVideo(Activity activity) {
            if (mTTFullScreenVideoAd != null && mIsLoaded) {
                mTTFullScreenVideoAd.showFullScreenVideoAd(activity);
            }
        }

        public void destroy() {
            mContext = null;
            mTTFullScreenVideoAd = null;
            mLoadFullVideoAdListener = null;
            mFullScreenVideoAdInteractionListener = null;
        }


        private TTAdNative.FullScreenVideoAdListener mLoadFullVideoAdListener = new TTAdNative.FullScreenVideoAdListener() {
            @Override
            public void onError(int code, String message) {
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, "Loading Full Video creative encountered an error: " + PangleAdapterConfiguration.mapErrorCode(code).toString() + ",error message:" + message);
                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(PangleAdapterConfiguration.mapErrorCode(code));
                }
            }

            @Override
            public void onFullScreenVideoAdLoad(TTFullScreenVideoAd ad) {
                if (ad != null) {
                    mIsLoaded = true;
                    mTTFullScreenVideoAd = ad;
                    mTTFullScreenVideoAd.setFullScreenVideoAdInteractionListener(mFullScreenVideoAdInteractionListener);
                    if (mLoadListener != null) {
                        mLoadListener.onAdLoaded();
                    }
                    MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);

                } else {
                    if (mLoadListener != null) {
                        mLoadListener.onAdLoadFailed(MoPubErrorCode.NO_FILL);
                    }
                    MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME);
                }
            }

            @Override
            public void onFullScreenVideoCached() {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, " Pangle onFullScreenVideoCached invoke !");
            }
        };

        private TTFullScreenVideoAd.FullScreenVideoAdInteractionListener mFullScreenVideoAdInteractionListener
                = new TTFullScreenVideoAd.FullScreenVideoAdInteractionListener() {

            @Override
            public void onAdShow() {
                MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
                if (mInteractionListener != null) {
                    mInteractionListener.onAdShown();
                    mInteractionListener.onAdImpression();
                }
            }

            @Override
            public void onAdVideoBarClick() {
                MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
                if (mInteractionListener != null) {
                    mInteractionListener.onAdClicked();
                }
            }

            @Override
            public void onAdClose() {
                MoPubLog.log(getAdNetworkId(), DID_DISAPPEAR, ADAPTER_NAME);
                if (mInteractionListener != null) {
                    mInteractionListener.onAdDismissed();
                }
            }

            @Override
            public void onVideoComplete() {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Pangle FullScreenVideoAd onVideoComplete...");
            }

            @Override
            public void onSkippedVideo() {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Pangle FullScreenVideoAd onSkippedVideo...");
            }
        };
    }
}

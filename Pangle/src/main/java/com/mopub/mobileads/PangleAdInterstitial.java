package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTFullScreenVideoAd;
import com.mopub.common.DataKeys;
import com.mopub.common.LifecycleListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdLogEvent.DID_DISAPPEAR;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;

public class PangleAdInterstitial extends BaseAd {
    private static final String ADAPTER_NAME = PangleAdInterstitial.class.getSimpleName();

    private String mPlacementId;
    private Context mContext;
    private PangleAdapterConfiguration mPangleAdapterConfiguration;
    private PangleAdInterstitialFullVideoLoader mFullVideoLoader;

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

        String adm = null;

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

            /** Init Pangle SDK if fail to initialize in the adapterConfiguration */
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

        final AdSlot.Builder adSlotBuilder = new AdSlot.Builder()
                .setCodeId(mPlacementId)
                .withBid(adm);

        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);

        /** Default value for Full Screen Video */
        adSlotBuilder.setImageAcceptedSize(1080, 1920);
        mFullVideoLoader = new PangleAdInterstitialFullVideoLoader(mContext);
        mFullVideoLoader.loadAdFullVideoListener(adSlotBuilder.build(),
                adManager.createAdNative(context.getApplicationContext()));
    }

    @Override
    protected void show() {
        if (mFullVideoLoader != null && mContext instanceof Activity) {
            MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);

            mFullVideoLoader.showFullVideo((Activity) mContext);
        } else {
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
        if (mFullVideoLoader != null) {
            mFullVideoLoader.destroy();
        }
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
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
            } else {
                MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME);

                if (mInteractionListener != null) {
                    mInteractionListener.onAdFailed(MoPubErrorCode.FULLSCREEN_SHOW_ERROR);
                }
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
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                        "Loading Full Video creative encountered an error: "
                                + PangleAdapterConfiguration.mapErrorCode(code).toString()
                                + ", error message:" + message);

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

                    MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);

                    if (mLoadListener != null) {
                        mLoadListener.onAdLoaded();
                    }
                } else {
                    MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME);

                    if (mLoadListener != null) {
                        mLoadListener.onAdLoadFailed(MoPubErrorCode.NO_FILL);
                    }
                }
            }

            @Override
            public void onFullScreenVideoCached() {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onFullScreenVideoCached: The full screen video is cached.");
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
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Pangle FullScreenVideoAd onVideoComplete.");
            }

            @Override
            public void onSkippedVideo() {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Pangle FullScreenVideoAd onSkippedVideo.");
            }
        };
    }
}

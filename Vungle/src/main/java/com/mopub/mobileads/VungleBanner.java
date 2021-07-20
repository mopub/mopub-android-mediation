package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.DataKeys;
import com.mopub.common.LifecycleListener;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Views;
import com.vungle.warren.AdConfig;
import com.vungle.warren.AdConfig.AdSize;
import com.vungle.warren.BannerAdConfig;
import com.vungle.warren.error.VungleException;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.mopub.common.DataKeys.ADUNIT_FORMAT;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.vungle.warren.AdConfig.AdSize.BANNER;
import static com.vungle.warren.AdConfig.AdSize.BANNER_LEADERBOARD;
import static com.vungle.warren.AdConfig.AdSize.BANNER_SHORT;
import static com.vungle.warren.AdConfig.AdSize.VUNGLE_MREC;

@Keep
public class VungleBanner extends BaseAd {

    private static final String ADAPTER_NAME = VungleBanner.class.getSimpleName();
    /*
     * APP_ID_KEY is intended for MoPub internal use. Do not modify.
     */
    private static final String APP_ID_KEY = "appId";
    private static final String PLACEMENT_ID_KEY = "pid";
    private static final String PLACEMENT_IDS_KEY = "pids";

    private static VungleRouter sVungleRouter;
    private final Handler mHandler;
    private String mAppId;
    private String mPlacementId;
    private VungleBannerRouterListener mVungleRouterListener;
    private boolean mIsPlaying;
    private com.vungle.warren.VungleBanner mVungleBannerAd;
    private Context mContext;
    @NonNull
    private final VungleAdapterConfiguration mVungleAdapterConfiguration;
    private final AtomicBoolean mPendingRequestBanner = new AtomicBoolean(false);
    private final AdConfig mAdConfig = new AdConfig();
    @Nullable
    private View mAdView;
    @Nullable
    private String mAdMarkup;

    public VungleBanner() {
        mHandler = new Handler(Looper.getMainLooper());
        sVungleRouter = VungleRouter.getInstance();
        mVungleAdapterConfiguration = new VungleAdapterConfiguration();
    }

    @Override
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {
        this.mContext = context;
        mPendingRequestBanner.set(true);
        setAutomaticImpressionAndClickTracking(false);

        final Map<String, String> extras = adData.getExtras();
        if (!validateIdsInServerExtras(extras)) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                            MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                            MoPubErrorCode.NETWORK_NO_FILL);
                    if (mLoadListener != null) {
                        mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);
                    }
                }
            });

            return;
        }

        if (mVungleRouterListener == null) {
            mVungleRouterListener = new VungleBannerRouterListener();
        }

        if (!sVungleRouter.isVungleInitialized()) {
            // No longer passing the placement IDs (pids) param per Vungle 6.3.17
            sVungleRouter.initVungle(context, mAppId);
            mVungleAdapterConfiguration.setCachedInitializationParameters(context, extras);
        }

        AdSize vungleAdSize = getVungleAdSize(adData);
        if (vungleAdSize == null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                            MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                            "Banner size is not valid.");
                    if (mLoadListener != null) {
                        mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);
                    }
                }
            });

            return;
        }

        mAdConfig.setAdSize(vungleAdSize);

        VungleMediationConfiguration.adConfigWithExtras(mAdConfig, extras, true);

        mAdMarkup = extras.get(DataKeys.ADM_KEY);
        if (TextUtils.isEmpty(mAdMarkup)) {
            mAdMarkup = null;
        }

        if (AdSize.isBannerAdSize(vungleAdSize)) {
            sVungleRouter.loadBannerAd(mPlacementId, mAdMarkup, vungleAdSize, mVungleRouterListener);
            MoPubLog.log(mPlacementId, LOAD_ATTEMPTED, ADAPTER_NAME);
        } else {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, "Unsupported Banner/Medium rectangle Ad size:  " +
                            "Placement ID:" + mPlacementId);
                    if (mLoadListener != null) {
                        mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                    }
                }
            });
        }
    }

    private AdSize getVungleAdSize(@NonNull final AdData adData) {
        final Map<String, String> extras = adData.getExtras();

        AdSize adSizeType = null;
        int adWidthInDp = adData.getAdWidth() != null ? adData.getAdWidth() : 0;
        int adHeightInDp = adData.getAdHeight() != null ? adData.getAdHeight() : 0;

        String adUnitFormat = extras.get(ADUNIT_FORMAT);
        if (!TextUtils.isEmpty(adUnitFormat)) {
            adUnitFormat = adUnitFormat.toLowerCase();
        }
        final boolean isMRECFormat = "medium_rectangle".equals(adUnitFormat);
        if (isMRECFormat) {
            if (adWidthInDp >= VUNGLE_MREC.getWidth() && adHeightInDp >= VUNGLE_MREC.getHeight()) {
                adSizeType = VUNGLE_MREC;
            }
        } else {
            if (adWidthInDp >= BANNER_LEADERBOARD.getWidth() && adHeightInDp >= BANNER_LEADERBOARD.getHeight()) {
                adSizeType = BANNER_LEADERBOARD;
            } else if (adWidthInDp >= BANNER.getWidth() && adHeightInDp >= BANNER.getHeight()) {
                adSizeType = BANNER;
            } else if (adWidthInDp >= BANNER_SHORT.getWidth() && adHeightInDp >= BANNER_SHORT.getHeight()) {
                adSizeType = BANNER_SHORT;
            }
        }

        if (adSizeType == null) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "No size found that matches the requested size:" + adWidthInDp
                    + "x" + adHeightInDp + " adUnitFormat is:" + adUnitFormat);
        } else {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "matched ad size:" + adSizeType + " for requesting ad size:"
                    + adWidthInDp + "x" + adHeightInDp + " adUnitFormat is:" + adUnitFormat);
        }

        return adSizeType;
    }

    @Override
    protected void onInvalidate() {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "onInvalidate is called for Placement ID:" + mPlacementId);
        mPendingRequestBanner.set(false);

        if (mVungleBannerAd != null) {
            Views.removeFromParent(mVungleBannerAd);
            mVungleBannerAd.destroyAd();
            mVungleBannerAd = null;
        }

        if (sVungleRouter != null) {
            sVungleRouter.removeRouterListener(mPlacementId);
        }

        mVungleRouterListener = null;
        mAdMarkup = null;
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    private boolean validateIdsInServerExtras(Map<String, String> serverExtras) {
        boolean isAllDataValid = true;

        if (serverExtras.containsKey(APP_ID_KEY)) {
            mAppId = serverExtras.get(APP_ID_KEY);

            if (TextUtils.isEmpty(mAppId)) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "App ID is empty.");

                isAllDataValid = false;
            }
        } else {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "AppID is not in serverExtras.");
            isAllDataValid = false;
        }

        if (serverExtras.containsKey(PLACEMENT_ID_KEY)) {
            mPlacementId = serverExtras.get(PLACEMENT_ID_KEY);
            if (TextUtils.isEmpty(mPlacementId)) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Placement ID for this Ad Unit is empty.");
                isAllDataValid = false;
            }
        } else {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Placement ID for this Ad Unit is not in serverExtras.");
            isAllDataValid = false;
        }

        if (serverExtras.containsKey(PLACEMENT_IDS_KEY)) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "No need to set placement IDs " +
                    "in MoPub dashboard with Vungle SDK version " +
                    com.vungle.warren.BuildConfig.VERSION_NAME);
        }

        return isAllDataValid;
    }

    @NonNull
    public String getAdNetworkId() {
        return mPlacementId != null ? mPlacementId : "";
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull final Activity activity,
                                            @NonNull final AdData adData) {
        return false;
    }

    @Nullable
    public View getAdView() {
        return mAdView;
    }

    private class VungleBannerRouterListener implements VungleRouterListener {

        @Override
        public void onAdEnd(String placementId) {
            if (mPlacementId.equals(placementId)) {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onAdEnd placement id: " + placementId);
                mIsPlaying = false;
                sVungleRouter.removeRouterListener(mPlacementId);
                mVungleRouterListener = null;
            }
        }

        @Override
        public void onAdClick(String placementId) {
            if (mPlacementId.equals(placementId)) {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onAdClick placement id: " + placementId);
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        if (mInteractionListener != null) {
                            mInteractionListener.onAdClicked();
                        }
                        MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
                    }
                });
            }
        }

        @Override
        public void onAdRewarded(String placementId) {
            //nothing to do
        }

        @Override
        public void onAdLeftApplication(String placementId) {
            //Nothing to do. If we invoke mCustomEventBannerListener.onLeaveApplication() it will cause
            // onBannerClicked() event to be called twice.
        }

        @Override
        public void onAdStart(@NonNull String placementReferenceId) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onAdStart placement id" +
                    placementReferenceId);
            if (mPlacementId.equals(placementReferenceId)) {
                mIsPlaying = true;
                //Let's load it again to mimic auto-cache
                if (AdSize.isBannerAdSize(mAdConfig.getAdSize()) && mAdMarkup == null) {
                    sVungleRouter.loadBannerAd(mPlacementId, null, mAdConfig.getAdSize(), mVungleRouterListener);
                }
            }
        }


        @Override
        public void onAdViewed(@NonNull String placementReferenceId) {

            if (mPlacementId.equals(placementReferenceId)) {
                MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME, "onAdViewed, Vungle banner ad logged impression. Placement id - Placement ID: " + placementReferenceId);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mInteractionListener != null) {
                            mInteractionListener.onAdImpression();
                        }
                    }
                });
            }
        }

        @Override
        public void onAdPlayError(@NonNull String placementId, VungleException exception) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onUnableToPlayAd - Placement ID: " +
                    placementId + ", reason: " + exception.getLocalizedMessage());
            if (mPlacementId.equals(placementId)) {

                mIsPlaying = false;
                final MoPubErrorCode errorCode = VungleRouter.mapErrorCode(exception.getExceptionCode());

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mInteractionListener != null) {
                            mInteractionListener.onAdFailed(errorCode);
                        }

                        MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME, errorCode.getIntCode(), errorCode);
                    }
                });
            }
        }

        @Override
        public void onAdLoadError(@NonNull String placementId, VungleException exception) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onAdLoadError placement id: " + placementId);
            if (mPlacementId.equals(placementId)) {
                if (!mIsPlaying) {
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME,
                            "banner ad is not loaded - Placement ID: " + placementId);
                    final MoPubErrorCode errorCode = VungleRouter.mapErrorCode(exception.getExceptionCode());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mLoadListener != null) {
                                mLoadListener.onAdLoadFailed(errorCode);
                            }
                            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, errorCode.getIntCode(), errorCode);
                        }
                    });
                }
            }
        }

        @Override
        public void onAdLoaded(@NonNull final String placementId) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onAdLoaded placement id: " + placementId);
            if (mPlacementId.equals(placementId)) {
                if (!mIsPlaying) {
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME,
                            "banner ad successfully loaded - Placement ID: " + placementId);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (!mPendingRequestBanner.getAndSet(false)) {
                                return;
                            }

                            final RelativeLayout layout = new RelativeLayout(mContext) {
                                @Override
                                protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
                                    super.onVisibilityChanged(changedView, visibility);
                                    if (mVungleBannerAd != null) {
                                        mVungleBannerAd.setAdVisibility(visibility == VISIBLE);
                                    }
                                }
                            };

                            //Fix for Unity Player that can't render a view with a state changed from INVISIBLE to VISIBLE.
                            //TODO: Remove once it's fixed in MoPub Unity plugin.
                            layout.setBackgroundColor(Color.TRANSPARENT);
                            boolean loadSucceeded = false;

                            if (AdSize.isBannerAdSize(mAdConfig.getAdSize())) {
                                mVungleBannerAd = sVungleRouter.getVungleBannerAd(placementId,
                                        mAdMarkup, new BannerAdConfig(mAdConfig));
                                if (mVungleBannerAd != null) {
                                    loadSucceeded = true;
                                    layout.addView(mVungleBannerAd);
                                }
                            }
                            if (loadSucceeded) {
                                mAdView = layout;
                                if (mLoadListener != null) {
                                    mLoadListener.onAdLoaded();
                                }
                                MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);

                            } else {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mLoadListener != null) {
                                            mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);
                                        }
                                        MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                                                MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                                                MoPubErrorCode.NETWORK_NO_FILL);
                                    }
                                });
                            }
                        }
                    });
                }
            }
        }

    }
}

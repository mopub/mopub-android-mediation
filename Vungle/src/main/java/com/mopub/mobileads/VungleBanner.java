package com.mopub.mobileads;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import android.text.TextUtils;
import android.view.View;
import android.widget.RelativeLayout;

import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Views;
import com.vungle.warren.AdConfig;
import com.vungle.warren.VungleNativeAd;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.mopub.common.DataKeys.AD_HEIGHT;
import static com.mopub.common.DataKeys.AD_WIDTH;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;

@Keep
public class VungleBanner extends CustomEventBanner {

    private static final String ADAPTER_NAME = VungleBanner.class.getSimpleName();
    /*
     * APP_ID_KEY is intended for MoPub internal use. Do not modify.
     */
    private static final String APP_ID_KEY = "appId";
    private static final String PLACEMENT_ID_KEY = "pid";
    private static final String PLACEMENT_IDS_KEY = "pids";
    
    private CustomEventBannerListener mCustomEventBannerListener;
    private final Handler mHandler;
    private String mAppId;
    private static String mPlacementId;
    private VungleBannerRouterListener mVungleRouterListener;
    private static VungleRouter sVungleRouter;
    private boolean mIsPlaying;
    private VungleNativeAd vungleBannerAd;
    private Context mContext;
    @NonNull
    private VungleAdapterConfiguration mVungleAdapterConfiguration;
    private AtomicBoolean pendingRequestBanner = new AtomicBoolean(false);
    private AdConfig adConfig = new AdConfig();
    private int adWidthInDp, adHeightInDp;

    public VungleBanner() {
        this.mHandler = new Handler(Looper.getMainLooper());
        sVungleRouter = VungleRouter.getInstance();
        mVungleAdapterConfiguration = new VungleAdapterConfiguration();
    }

    @Override
    protected void loadBanner(Context context, CustomEventBannerListener customEventBannerListener, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        this.mContext = context;
        mCustomEventBannerListener = customEventBannerListener;
        pendingRequestBanner.set(true);

        setAutomaticImpressionAndClickTracking(false);

        if (context == null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                            MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                            MoPubErrorCode.NETWORK_NO_FILL);
                    mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
                }
            });
            return;
        }

        if (!validateIdsInServerExtras(serverExtras)) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                            MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                            MoPubErrorCode.NETWORK_NO_FILL);
                    mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
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
            mVungleAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
        }

        AdConfig.AdSize vungleAdSize = getVungleAdSize(localExtras);
        if (vungleAdSize != null) {
            adConfig.setAdSize(vungleAdSize);
        } else {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                            MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                            "Banner size is not valid.");
                    mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
                }
            });

            return;
        }

        //currently we only support MREC for banners. This will require to be reworked once we add other sizes
        if (adConfig.getAdSize() != AdConfig.AdSize.VUNGLE_MREC) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, "Unsupported Ad size, only MREC supported:  Placement ID:" + mPlacementId);
                    mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
            });
        } else {
            sVungleRouter.loadAdForPlacement(mPlacementId, mVungleRouterListener);
            MoPubLog.log(mPlacementId, LOAD_ATTEMPTED, ADAPTER_NAME);
        }
    }

    private AdConfig.AdSize getVungleAdSize(Map<String, Object> localExtras) {
        Preconditions.checkNotNull(localExtras);

        final Object adWidthObject = localExtras.get(AD_WIDTH);
        if (adWidthObject instanceof Integer) {
            adWidthInDp = (int) adWidthObject;
        }

        final Object adHeightObject = localExtras.get(AD_HEIGHT);
        if (adHeightObject instanceof Integer) {
            adHeightInDp = (int) adHeightObject;
        }

        if ((adWidthInDp == 300 && adHeightInDp == 250) || (adWidthInDp == 336 && adHeightInDp == 280)) {
            return AdConfig.AdSize.VUNGLE_MREC;
        } else {
            return null;
        }
    }

    @Override
    protected void onInvalidate() {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "onInvalidate is called for Placement ID:" + mPlacementId);
        pendingRequestBanner.set(false);

        if (vungleBannerAd != null) {
            Views.removeFromParent(vungleBannerAd.renderNativeView());
            vungleBannerAd.finishDisplayingAd();
        }

        if (sVungleRouter != null) {
            sVungleRouter.removeRouterListener(mPlacementId);
        }

        mVungleRouterListener = null;
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

    private static String getAdNetworkId() {
        return mPlacementId;
    }

    private class VungleBannerRouterListener implements VungleRouterListener {

        @Override
        public void onAdEnd(@NonNull String placementReferenceId, boolean wasSuccessfulView, final boolean wasCallToActionClicked) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onAdEnd placement id" + placementReferenceId);
            if (mPlacementId.equals(placementReferenceId)) {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onAdEnd - Placement ID: " + placementReferenceId + ", wasSuccessfulView: " + wasSuccessfulView + ", wasCallToActionClicked: " + wasCallToActionClicked);
                mIsPlaying = false;
                sVungleRouter.removeRouterListener(mPlacementId);
                mVungleRouterListener = null;
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        if (wasCallToActionClicked && mCustomEventBannerListener != null) {
                            mCustomEventBannerListener.onBannerClicked();
                            MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
                        }
                    }
                });
            }
        }

        @Override
        public void onAdStart(@NonNull String placementReferenceId) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onAdStart placement id" + placementReferenceId);
            if (mPlacementId.equals(placementReferenceId)) {
                mIsPlaying = true;
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Vungle banner ad logged impression. Placement id" + placementReferenceId);
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        if (mCustomEventBannerListener != null) {
                            mCustomEventBannerListener.onBannerImpression();
                        }
                    }
                });

                //Let's load it again to mimic auto-cache
                sVungleRouter.loadAdForPlacement(mPlacementId, mVungleRouterListener);
            }
        }

        @Override
        public void onUnableToPlayAd(@NonNull String placementReferenceId, String reason) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onUnableToPlayAd - Placement ID: " + placementReferenceId + ", reason: " + reason);
            if (mPlacementId.equals(placementReferenceId)) {
                mIsPlaying = false;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
                        MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                                MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                                MoPubErrorCode.NETWORK_NO_FILL);
                    }
                });
            }
        }

        @Override
        public void onAdAvailabilityUpdate(@NonNull final String placementReferenceId, boolean isAdAvailable) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onAdAvailabilityUpdate placement id" + placementReferenceId + " isAdAvailable " + isAdAvailable);
            if (mPlacementId.equals(placementReferenceId)) {
                if (!mIsPlaying) {
                    if (isAdAvailable) {
                        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "banner ad successfully loaded - Placement ID: " + placementReferenceId);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (!pendingRequestBanner.getAndSet(false))
                                    return;

                                boolean isSuccess = false;
                                final RelativeLayout layout = new RelativeLayout(mContext) {
                                    @Override
                                    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
                                        super.onVisibilityChanged(changedView, visibility);
                                        if (vungleBannerAd != null) {
                                            vungleBannerAd.setAdVisibility(visibility == VISIBLE);
                                        }
                                    }
                                };
                                vungleBannerAd = sVungleRouter.getVungleBannerAd(placementReferenceId, adConfig);
                                if (vungleBannerAd != null) {
                                    final View adView = vungleBannerAd.renderNativeView();
                                    if (adView != null) {
                                        isSuccess = true;
                                        layout.addView(adView);
                                        mCustomEventBannerListener.onBannerLoaded(layout);
                                        MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
                                    }
                                }
                                if (!isSuccess) {
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
                                            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                                                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                                                    MoPubErrorCode.NETWORK_NO_FILL);
                                        }
                                    });
                                }
                            }
                        });
                    } else {
                        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "banner ad is not loaded - Placement ID: " + placementReferenceId);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
                                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                                        MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                                        MoPubErrorCode.NETWORK_NO_FILL);
                            }
                        });
                    }
                }
            }
        }
    }
}

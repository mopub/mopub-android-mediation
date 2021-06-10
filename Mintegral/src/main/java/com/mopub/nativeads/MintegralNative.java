package com.mopub.nativeads;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import com.mbridge.msdk.MBridgeConstans;
import com.mbridge.msdk.out.Campaign;
import com.mbridge.msdk.out.Frame;
import com.mbridge.msdk.out.MBBidNativeHandler;
import com.mbridge.msdk.out.MBNativeHandler;
import com.mbridge.msdk.out.MBridgeSDKFactory;
import com.mbridge.msdk.out.NativeListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.MintegralAdapterConfiguration;
import com.mopub.mobileads.MintegralSdkManager;

import java.util.List;
import java.util.Map;

import static com.mopub.common.DataKeys.ADM_KEY;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.nativeads.NativeErrorCode.CONNECTION_ERROR;
import static com.mopub.nativeads.NativeErrorCode.NETWORK_NO_FILL;

public class MintegralNative extends CustomEventNative {
    private final String ADAPTER_NAME = this.getClass().getName();

    private static CustomEventNativeListener mCustomEventNativeListener;

    private String mAdUnitId;
    private String mAppId;
    private String mAppKey;
    private String mPlacementId;

    @Override
    protected void loadNativeAd(@NonNull final Context context,
                                @NonNull final CustomEventNativeListener customEventNativeListener,
                                @NonNull final Map<String, Object> localExtras,
                                @NonNull final Map<String, String> serverExtras) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(customEventNativeListener);
        Preconditions.checkNotNull(localExtras);
        Preconditions.checkNotNull(serverExtras);

        mCustomEventNativeListener = customEventNativeListener;

        if (!serverDataIsValid(serverExtras)) {
            failAdapter(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR, "One or more " +
                    "keys used for Mintegral's ad requests are empty. Failing adapter. Please ensure " +
                    "you have populated all the required keys on the MoPub dashboard.");
            return;
        }

        MintegralAdapterConfiguration.configureMintegralSdk(mAppId, mAppKey, context,
                new MintegralSdkManager.MBSDKInitializeListener() {
                    @Override
                    public void onInitializeSuccess(String appKey, String appID) {
                        loadNative(serverExtras, context, customEventNativeListener);
                    }

                    @Override
                    public void onInitializeFailure(String message) {
                        failAdapter(CONNECTION_ERROR, "Mintegral SDK init failed: " + message);
                    }
                });
    }

    private void loadNative(@NonNull Map<String, String> serverExtras, @NonNull Context context,
                            @NonNull CustomEventNativeListener customEventNativeListener) {
        final String bid = serverExtras.get(ADM_KEY);
        final MBridgeNativeAd MBridgeNativeAd = new MBridgeNativeAd(context, customEventNativeListener, mAdUnitId);
        MBridgeNativeAd.setBid(bid);
        MBridgeNativeAd.loadAd();
    }

    public class MBridgeNativeAd extends BaseNativeAd implements NativeListener.NativeAdListener,
            NativeListener.NativeTrackingListener {

        private String mBid;
        private final String mUnitId;

        MBNativeHandler mNativeAd;
        MBBidNativeHandler mBiddingNativeAd;
        Context mContext;
        Campaign mCampaign;

        MBridgeNativeAd(final Context context,
                        final CustomEventNativeListener customEventNativeListener,
                        final String adUnitId) {
            mUnitId = adUnitId;
            mCustomEventNativeListener = customEventNativeListener;
            this.mContext = context;
        }

        void loadAd() {
            MintegralAdapterConfiguration.setTargeting(MBridgeSDKFactory.getMBridgeSDK());

            final Map<String, Object> properties = MBNativeHandler.getNativeProperties(mPlacementId, mUnitId);
            properties.put(MBridgeConstans.PROPERTIES_AD_NUM, 1);
            properties.put(MBridgeConstans.NATIVE_VIDEO_WIDTH, 720);
            properties.put(MBridgeConstans.NATIVE_VIDEO_HEIGHT, 480);
            properties.put(MBridgeConstans.NATIVE_VIDEO_SUPPORT, true);

            if (TextUtils.isEmpty(mBid)) {
                mNativeAd = new MBNativeHandler(properties, mContext);
                mNativeAd.setAdListener(this);
                mNativeAd.setTrackingListener(this);
                mNativeAd.load();
            } else {
                mBiddingNativeAd = new MBBidNativeHandler(properties, mContext);
                mBiddingNativeAd.setAdListener(this);
                mBiddingNativeAd.setTrackingListener(this);
                mBiddingNativeAd.bidLoad(mBid);
            }

            MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
        }

        void setBid(String bid) {
            this.mBid = bid;
        }

        @Override
        public void onStartRedirection(Campaign campaign, String url) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onStartRedirection: " + url);
        }

        @Override
        public void onRedirectionFailed(Campaign campaign, String url) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onRedirectionFailed: " + url);
        }

        @Override
        public void onFinishRedirection(Campaign campaign, String url) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onFinishRedirection: " + url);
        }

        @Override
        public void onDownloadStart(Campaign campaign) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onDownloadStart");
        }

        @Override
        public void onDownloadFinish(Campaign campaign) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onDownloadFinish");
        }

        @Override
        public void onDownloadProgress(int progress) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onDownloadProgress");
        }

        @Override
        public boolean onInterceptDefaultLoadingDialog() {
            return false;
        }

        @Override
        public void onShowLoading(Campaign campaign) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onShowLoading");
        }

        @Override
        public void onDismissLoading(Campaign campaign) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onDismissLoading");
        }

        @Override
        public void onAdLoaded(List<Campaign> campaigns, int template) {
            if (campaigns == null || campaigns.size() == 0) {
                failAdapter(NETWORK_NO_FILL, "No Mintegral native ad campaign active. " +
                        "Failing adapter.");

                return;
            }

            mCampaign = campaigns.get(0);
            if (mCustomEventNativeListener != null) {
                mCustomEventNativeListener.onNativeAdLoaded(MBridgeNativeAd.this);
            }

            MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
        }

        @Override
        public void onAdLoadError(String errorMsg) {
            failAdapter(NETWORK_NO_FILL, errorMsg);
        }

        @Override
        public void onAdClick(Campaign campaign) {
            this.notifyAdClicked();
            MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
        }

        @Override
        public void onAdFramesLoaded(final List<Frame> list) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onAdFramesLoaded");
        }

        @Override
        public void onLoggingImpression(int adSourceType) {
            this.notifyAdImpressed();
            MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
        }

        @Override
        public void prepare(@NonNull View view) {
        }

        @Override
        public void clear(@NonNull View view) {
            Preconditions.checkNotNull(view);

            if (mNativeAd != null) {
                mNativeAd.unregisterView(view, mCampaign);
            }
            if (mBiddingNativeAd != null) {
                mBiddingNativeAd.unregisterView(view, mCampaign);
            }
        }

        @Override
        public void destroy() {

            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Finished showing Mintegral " +
                    "native ads. Invalidating adapter...");

            if (mNativeAd != null) {
                mNativeAd.release();
                mNativeAd.setAdListener(null);
                mNativeAd = null;
            } else if (mBiddingNativeAd != null) {
                mBiddingNativeAd.bidRelease();
                mBiddingNativeAd.setAdListener(null);
            }

            mCustomEventNativeListener = null;
        }

        void registerViewForInteraction(View view) {
            if (mNativeAd != null) {
                mNativeAd.registerView(view, mCampaign);
            } else if (mBiddingNativeAd != null) {
                mBiddingNativeAd.registerView(view, mCampaign);
            }
        }

        final public String getTitle() {
            return mCampaign.getAppName();
        }

        final public String getText() {
            return mCampaign.getAppDesc();
        }

        final public String getCallToAction() {
            return mCampaign.getAdCall();
        }

        final public String getMainImageUrl() {
            return mCampaign.getImageUrl();
        }

        final public String getIconUrl() {
            return mCampaign.getIconUrl();
        }

        final public int getStarRating() {
            return (int) mCampaign.getRating();
        }
    }

    private boolean serverDataIsValid(final Map<String, String> serverExtras) {
        if (serverExtras != null && !serverExtras.isEmpty()) {
            mAdUnitId = serverExtras.get(MintegralAdapterConfiguration.UNIT_ID_KEY);
            mAppId = serverExtras.get(MintegralAdapterConfiguration.APP_ID_KEY);
            mAppKey = serverExtras.get(MintegralAdapterConfiguration.APP_KEY);
            mPlacementId = serverExtras.get(MintegralAdapterConfiguration.PLACEMENT_ID_KEY);

            return !TextUtils.isEmpty(mAppId) && !TextUtils.isEmpty(mAppKey) && !TextUtils.isEmpty(mAdUnitId);
        }

        return false;
    }

    private void failAdapter(final NativeErrorCode errorCode, final String errorMsg) {
        MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, errorCode.getIntCode(), errorCode);

        if (!TextUtils.isEmpty(errorMsg)) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, errorMsg);
        }

        if (mCustomEventNativeListener != null) {
            mCustomEventNativeListener.onNativeAdFailed(errorCode);
        }
    }

    private String getAdNetworkId() {
        return mAdUnitId;
    }
}

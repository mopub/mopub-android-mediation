package com.mopub.nativeads;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import com.mintegral.msdk.MIntegralConstans;
import com.mintegral.msdk.out.Campaign;
import com.mintegral.msdk.out.Frame;
import com.mintegral.msdk.out.MIntegralSDKFactory;
import com.mintegral.msdk.out.MtgBidNativeHandler;
import com.mintegral.msdk.out.MtgNativeHandler;
import com.mintegral.msdk.out.NativeListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.MintegralAdapterConfiguration;

import java.util.List;
import java.util.Map;

import static com.mopub.common.DataKeys.ADM_KEY;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.nativeads.NativeErrorCode.NETWORK_NO_FILL;

public class MintegralNative extends CustomEventNative {

    private static final String ADAPTER_NAME = MintegralNative.class.getName();
    private static boolean isInitialized = false;
    private static CustomEventNativeListener mCustomEventNativeListener;

    private static String mAdUnitId;

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

        if (!serverDataIsValid(serverExtras, context)) {
            failAdapter(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR,
                    "One or more keys used for Mintegral's ad requests are empty. Failing " +
                            "adapter. Please ensure you have populated all the required keys on the " +
                            "MoPub dashboard.");

            return;
        }

        final String bid = serverExtras.get(ADM_KEY);

        final MintegralNativeAd mintegralNativeAd = new MintegralNativeAd(context,
                customEventNativeListener, mAdUnitId, bid);
        mintegralNativeAd.loadAd();
    }

    public static class MintegralNativeAd extends BaseNativeAd implements
            NativeListener.NativeAdListener, NativeListener.NativeTrackingListener {

        private final String mBid;
        private final String mUnitid;

        MtgNativeHandler mNativeHandler;
        MtgBidNativeHandler mtgBidNativeHandler;
        Context mContext;
        Campaign mCampaign;

        MintegralNativeAd(final Context context,
                          final CustomEventNativeListener customEventNativeListener,
                          final String adUnitId,
                          final String bid) {
            mBid = bid;
            mUnitid = adUnitId;
            mCustomEventNativeListener = customEventNativeListener;
            this.mContext = context;
        }

        void loadAd() {
            final Map<String, Object> properties = MtgNativeHandler.getNativeProperties(mUnitid);
            properties.put(MIntegralConstans.PROPERTIES_AD_NUM, 1);
            properties.put(MIntegralConstans.NATIVE_VIDEO_WIDTH, 720);
            properties.put(MIntegralConstans.NATIVE_VIDEO_HEIGHT, 480);
            properties.put(MIntegralConstans.NATIVE_VIDEO_SUPPORT, true);

            MintegralAdapterConfiguration.setTargeting(MIntegralSDKFactory.getMIntegralSDK());

            if (TextUtils.isEmpty(mBid)) {
                mNativeHandler = new MtgNativeHandler(properties, mContext);
                mNativeHandler.setAdListener(this);
                mNativeHandler.setTrackingListener(this);
                mNativeHandler.load();
            } else {
                mtgBidNativeHandler = new MtgBidNativeHandler(properties, mContext);
                mtgBidNativeHandler.setAdListener(this);
                mtgBidNativeHandler.setTrackingListener(this);
                mtgBidNativeHandler.bidLoad(mBid);
            }

            MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
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
                failAdapter(NETWORK_NO_FILL, "No Mintegral campaign active. Failing " +
                        "adapter.");

                return;
            }

            mCampaign = campaigns.get(0);
            mCustomEventNativeListener.onNativeAdLoaded(MintegralNativeAd.this);

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

            if (mNativeHandler != null) {
                mNativeHandler.unregisterView(view, mCampaign);
            }
            if (mtgBidNativeHandler != null) {
                mtgBidNativeHandler.unregisterView(view, mCampaign);
            }
        }

        @Override
        public void destroy() {

            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Finished showing Mintegral " +
                    "native ads. Invalidating adapter...");

            if (mNativeHandler != null) {
                mNativeHandler.release();
                mNativeHandler.clearVideoCache();
            } else if (mtgBidNativeHandler != null) {
                mtgBidNativeHandler.bidRelease();
            }

            mCustomEventNativeListener = null;
        }

        void registerViewForInteraction(View view) {
            if (mNativeHandler != null) {
                mNativeHandler.registerView(view, mCampaign);
            } else if (mtgBidNativeHandler != null) {
                mtgBidNativeHandler.registerView(view, mCampaign);
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

    private boolean serverDataIsValid(final Map<String, String> serverExtras, Context context) {

        if (serverExtras != null && !serverExtras.isEmpty()) {
            mAdUnitId = serverExtras.get(MintegralAdapterConfiguration.UNIT_ID_KEY);
            final String appId = serverExtras.get(MintegralAdapterConfiguration.APP_ID_KEY);
            final String appKey = serverExtras.get(MintegralAdapterConfiguration.APP_KEY);

            if (!TextUtils.isEmpty(appId) && !TextUtils.isEmpty(appKey) && !TextUtils.isEmpty(mAdUnitId)) {
                if (!isInitialized) {
                    MintegralAdapterConfiguration.configureMintegral(appId, appKey, context);
                    isInitialized = true;
                }

                return true;
            }
        }
        return false;
    }

    private static void failAdapter(final NativeErrorCode errorCode, final String errorMsg) {

        MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, errorCode.getIntCode(), errorCode);

        if (!TextUtils.isEmpty(errorMsg)) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, errorMsg);
        }

        if (mCustomEventNativeListener != null) {
            mCustomEventNativeListener.onNativeAdFailed(errorCode);
        }
    }

    private static String getAdNetworkId() {
        return mAdUnitId;
    }
}

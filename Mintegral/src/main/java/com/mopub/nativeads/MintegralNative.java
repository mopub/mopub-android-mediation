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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mopub.common.DataKeys.ADM_KEY;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.nativeads.NativeErrorCode.EMPTY_AD_RESPONSE;
import static com.mopub.nativeads.NativeImageHelper.preCacheImages;

public class MintegralNative extends CustomEventNative {

    private static final String ADAPTER_NAME = MintegralNative.class.getName();
    private static boolean isInitialized = false;
    private static CustomEventNativeListener mCustomEventNativeListener;

    private String mAdUnitId;

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

        new MintegralStaticNativeAd(mAdUnitId, context, new ImpressionTracker(context),
                new NativeClickHandler(context), customEventNativeListener, serverExtras, localExtras);
    }

    public static class MintegralStaticNativeAd extends StaticNativeAd
            implements NativeListener.NativeAdListener, NativeListener.NativeTrackingListener {

        MtgNativeHandler mNativeHandle;
        MtgBidNativeHandler mtgBidNativeHandler;
        NativeClickHandler mNativeClickHandler;
        ImpressionTracker mImpressionTracker;
        Context mContext;
        Campaign mCampaign;

        MintegralStaticNativeAd(final String adUnitId, final Context context,
                                final ImpressionTracker impressionTracker,
                                @NonNull final NativeClickHandler nativeClickHandler,
                                final CustomEventNativeListener customEventNativeListener,
                                @NonNull final Map<String, String> serverExtras,
                                @NonNull final Map<String, Object> localExtras) {

            mCustomEventNativeListener = customEventNativeListener;
            this.mImpressionTracker = impressionTracker;
            this.mNativeClickHandler = nativeClickHandler;
            this.mContext = context;

            final Map<String, Object> properties = MtgNativeHandler.getNativeProperties(adUnitId);
            properties.put(MIntegralConstans.PROPERTIES_AD_NUM, 1);
            properties.put(MIntegralConstans.NATIVE_VIDEO_WIDTH, 720);
            properties.put(MIntegralConstans.NATIVE_VIDEO_HEIGHT, 480);
            properties.put(MIntegralConstans.NATIVE_VIDEO_SUPPORT, true);

            MintegralAdapterConfiguration.setTargeting(MIntegralSDKFactory.getMIntegralSDK());

            final String adm = serverExtras.get(ADM_KEY);

            if (TextUtils.isEmpty(adm)) {
                mNativeHandle = new MtgNativeHandler(properties, mContext);

                mNativeHandle.setAdListener(this);
                mNativeHandle.setTrackingListener(this);
                mNativeHandle.load();
            } else {
                mtgBidNativeHandler = new MtgBidNativeHandler(properties, mContext);

                mtgBidNativeHandler.setAdListener(this);
                mtgBidNativeHandler.setTrackingListener(this);
                mtgBidNativeHandler.bidLoad(adm);
            }

            MoPubLog.log(LOAD_ATTEMPTED, ADAPTER_NAME);
        }

        @Override
        public void onStartRedirection(Campaign campaign, String url) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "onStartRedirection: " + url);
        }

        @Override
        public void onRedirectionFailed(Campaign campaign, String url) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "onRedirectionFailed: " + url);
        }

        @Override
        public void onFinishRedirection(Campaign campaign, String url) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "onFinishRedirection: " + url);
        }

        @Override
        public void onDownloadStart(Campaign campaign) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "onDownloadStart");
        }

        @Override
        public void onDownloadFinish(Campaign campaign) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "onDownloadFinish");
        }

        @Override
        public void onDownloadProgress(int progress) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "onDownloadProgress");
        }

        @Override
        public boolean onInterceptDefaultLoadingDialog() {
            return false;
        }

        @Override
        public void onShowLoading(Campaign campaign) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "onShowLoading");

        }

        @Override
        public void onDismissLoading(Campaign campaign) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "onDismissLoading");
        }

        @Override
        public void onAdLoaded(List<Campaign> campaigns, int template) {

            final List<String> imageUrls = new ArrayList<>();

            if (campaigns != null && campaigns.size() > 0) {
                for (Campaign campaign : campaigns) {
                    setMainImageUrl(campaign.getImageUrl());

                    if (!TextUtils.isEmpty(campaign.getImageUrl())) {
                        imageUrls.add(campaign.getImageUrl());
                    }

                    setIconImageUrl(campaign.getIconUrl());

                    if (!TextUtils.isEmpty(campaign.getIconUrl())) {
                        imageUrls.add(campaign.getIconUrl());
                    }

                    setStarRating(campaign.getRating());
                    setCallToAction(campaign.getAdCall());
                    setTitle(campaign.getAppName());
                    setText(campaign.getAppDesc());

                    mCampaign = campaign;
                }
            } else {
                failAdapter(EMPTY_AD_RESPONSE, "Fail to load Mintegral native ad. Campaign " +
                        "doesn't exist.");
            }

            preCacheImages(mContext, imageUrls, new NativeImageHelper.ImageListener() {
                @Override
                public void onImagesCached() {
                    if (mCustomEventNativeListener != null) {
                        mCustomEventNativeListener.onNativeAdLoaded(MintegralStaticNativeAd.this);
                    }

                    MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
                }

                @Override
                public void onImagesFailedToCache(NativeErrorCode errorCode) {
                    failAdapter(errorCode, "Failed to cache one or more of Mintegral native " +
                            "ad images.");
                }
            });
        }

        @Override
        public void onAdLoadError(String errorMsg) {
            failAdapter(NativeErrorCode.NETWORK_NO_FILL, errorMsg);
        }

        @Override
        public void onAdClick(Campaign campaign) {
            notifyAdClicked();

            MoPubLog.log(CLICKED, ADAPTER_NAME);
        }

        @Override
        public void onAdFramesLoaded(final List<Frame> list) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "onAdFramesLoaded");
        }

        @Override
        public void onLoggingImpression(int adSourceType) {
            notifyAdImpressed();

            MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);
        }

        @Override
        public void recordImpression(@NonNull View view) {
            Preconditions.checkNotNull(view);

            super.recordImpression(view);
        }

        @Override
        public void prepare(@NonNull View view) {
            Preconditions.checkNotNull(view);

            if (mNativeHandle != null) {
                mNativeHandle.registerView(view, mCampaign);
            } else if (mtgBidNativeHandler != null) {
                mtgBidNativeHandler.registerView(view, mCampaign);
            }
        }

        @Override
        public void clear(@NonNull View view) {
            Preconditions.checkNotNull(view);

            mImpressionTracker.removeView(view);
        }

        @Override
        public void destroy() {
            super.destroy();

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Finished showing Mintegral native ads. " +
                    "Invalidating adapter...");

            if (mNativeHandle != null) {
                mNativeHandle.release();
                mNativeHandle.clearVideoCache();
            } else if (mtgBidNativeHandler != null) {
                mtgBidNativeHandler.bidRelease();
            }

            mCustomEventNativeListener = null;
        }

        @Override
        public void handleClick(@NonNull View view) {
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

        MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, errorCode.getIntCode(), errorCode);

        if (!TextUtils.isEmpty(errorMsg)) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, errorMsg);
        }

        if (mCustomEventNativeListener != null) {
            mCustomEventNativeListener.onNativeAdFailed(errorCode);
        }
    }
}

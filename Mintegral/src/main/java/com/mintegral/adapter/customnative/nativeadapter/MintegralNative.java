package com.mintegral.adapter.customnative.nativeadapter;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.mintegral.adapter.common.AdapterCommonUtil;
import com.mintegral.adapter.common.AdapterTools;
import com.mintegral.msdk.MIntegralConstans;
import com.mintegral.msdk.MIntegralSDK;
import com.mintegral.msdk.out.Campaign;
import com.mintegral.msdk.out.Frame;
import com.mintegral.msdk.out.MIntegralSDKFactory;
import com.mintegral.msdk.out.MTGBidRewardVideoHandler;
import com.mintegral.msdk.out.MtgBidNativeHandler;
import com.mintegral.msdk.out.MtgNativeHandler;
import com.mintegral.msdk.out.NativeListener;


import com.mopub.nativeads.CustomEventNative;
import com.mopub.nativeads.ImpressionTracker;
import com.mopub.nativeads.NativeClickHandler;
import com.mopub.nativeads.NativeErrorCode;
import com.mopub.nativeads.NativeImageHelper;
import com.mopub.nativeads.StaticNativeAd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mopub.nativeads.NativeImageHelper.preCacheImages;


/**
 * Created by songjunjun on 16/11/15.
 */

public class MintegralNative extends CustomEventNative {

    private static final String ADAPTER_NAME = "MintegralNative";

    @Override
    protected void loadNativeAd(@NonNull Context context, @NonNull CustomEventNativeListener customEventNativeListener, @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras) {


        final String unit_id;


        if (extrasAreValid(serverExtras, context, localExtras)) {
            unit_id = serverExtras.get("unitId");
        } else {
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
            return;
        }
        String adm = (String) serverExtras.get("adm");
//        adm="45ba6646-6243-4260-890b-a499a5b4151e_hk";
        new MintegralStaticNativeAd(unit_id,
                context,
                new ImpressionTracker(context),
                new NativeClickHandler(context),
                customEventNativeListener, adm);

    }


    private boolean extrasAreValid(final Map<String, String> serverExtras, Context mContext, Map<String, Object> localExtras) {
        final String placementId = serverExtras.get("appId");
        final String appKey = serverExtras.get("appKey");

        AdapterCommonUtil.addChannel();
        if (placementId != null && placementId.length() > 0 && appKey != null && appKey.length() > 0) {
            MIntegralSDK sdk = MIntegralSDKFactory.getMIntegralSDK();
            if (!AdapterTools.canCollectPersonalInformation()) {
                sdk.setUserPrivateInfoType(mContext, MIntegralConstans.AUTHORITY_ALL_INFO, MIntegralConstans.IS_SWITCH_OFF);
            } else {
                sdk.setUserPrivateInfoType(mContext, MIntegralConstans.AUTHORITY_ALL_INFO, MIntegralConstans.IS_SWITCH_ON);
            }

            Map<String, String> map = sdk.getMTGConfigurationMap(placementId,
                    appKey);
            if (mContext instanceof Activity) {
                sdk.init(map, ((Activity) mContext).getApplication());
            } else if (mContext instanceof Application) {
                sdk.init(map, mContext);
            }
            AdapterCommonUtil.parseLocalExtras(localExtras, sdk);
            return true;
        }

        return false;
    }


    public static class MintegralStaticNativeAd extends StaticNativeAd implements NativeListener.NativeAdListener, NativeListener.NativeTrackingListener {

        MtgNativeHandler mNativeHandle;
        MtgBidNativeHandler mtgBidNativeHandler;
        NativeClickHandler mNativeClickHandler;
        ImpressionTracker mImpressionTracker;
        CustomEventNativeListener mCustomEventNativeListener;
        Context mContext;
        public Campaign mCampaign;

        public MintegralStaticNativeAd(String unit_id, final Context context, final ImpressionTracker impressionTracker,
                                       @NonNull final NativeClickHandler nativeClickHandler, final CustomEventNativeListener customEventNativeListener, String adm) {


            this.mCustomEventNativeListener = customEventNativeListener;
            this.mImpressionTracker = impressionTracker;
            this.mNativeClickHandler = nativeClickHandler;
            this.mContext = context;

            Map<String, Object> properties = MtgNativeHandler.getNativeProperties(unit_id);
            properties.put(MIntegralConstans.PROPERTIES_AD_NUM, 1);
            properties.put(MIntegralConstans.NATIVE_VIDEO_WIDTH, 720);
            properties.put(MIntegralConstans.NATIVE_VIDEO_HEIGHT, 480);
            properties.put(MIntegralConstans.NATIVE_VIDEO_SUPPORT, true);
            mNativeHandle = null;
            mtgBidNativeHandler = null;
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


        }


        @Override
        public void onStartRedirection(Campaign campaign, String url) {

        }

        @Override
        public void onRedirectionFailed(Campaign campaign, String url) {

        }

        @Override
        public void onFinishRedirection(Campaign campaign, String url) {

        }

        @Override
        public void onDownloadStart(Campaign campaign) {

        }

        @Override
        public void onDownloadFinish(Campaign campaign) {

        }

        @Override
        public void onDownloadProgress(int progress) {

        }

        @Override
        public boolean onInterceptDefaultLoadingDialog() {

            return false;
        }

        @Override
        public void onShowLoading(Campaign campaign) {

        }

        @Override
        public void onDismissLoading(Campaign campaign) {

        }

        @Override
        public void onAdLoaded(List<Campaign> campaigns, int template) {

            final List<String> imageUrls = new ArrayList<String>();
            if (campaigns != null && campaigns.size() > 0) {
                for (Campaign campaign : campaigns) {//将返回的所有广告给赋值
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
                mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.EMPTY_AD_RESPONSE);
            }

            preCacheImages(mContext, imageUrls, new NativeImageHelper.ImageListener() {
                @Override
                public void onImagesCached() {
                    mCustomEventNativeListener.onNativeAdLoaded(MintegralStaticNativeAd.this);
                }

                @Override
                public void onImagesFailedToCache(NativeErrorCode errorCode) {
                    mCustomEventNativeListener.onNativeAdFailed(errorCode);
                }
            });

        }

        @Override
        public void onAdLoadError(String msg) {
            mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.UNSPECIFIED);

        }

        @Override
        public void onAdClick(Campaign campaign) {
            Log.e("", "onAdClick");
            notifyAdClicked();
        }


        @Override
        public void onAdFramesLoaded(final List<Frame> list) {

        }

        @Override
        public void onLoggingImpression(int adsourceType) {
            Log.e(ADAPTER_NAME, "onLoggingImpression adsourceType:" + adsourceType);
            notifyAdImpressed();
        }


        @Override
        public void recordImpression(@NonNull View view) {
            super.recordImpression(view);


        }


        @Override
        public void prepare(@NonNull View view) {
            Log.e(ADAPTER_NAME, "registerView");
            if (mNativeHandle != null) {
                mNativeHandle.registerView(view, mCampaign);
            } else if (mtgBidNativeHandler != null) {
                mtgBidNativeHandler.registerView(view, mCampaign);
            }

        }

        @Override
        public void clear(@NonNull View view) {

            mImpressionTracker.removeView(view);
            // mNativeClickHandler.clearOnClickListener(view);
        }

        @Override
        public void destroy() {
            super.destroy();
            if (mNativeHandle != null) {
                mNativeHandle.release();
            } else if (mtgBidNativeHandler != null) {
                mtgBidNativeHandler.bidRelease();
            }
        }

        @Override
        public void handleClick(@NonNull View view) {
//            super.handleClick(view);


        }

//        @Override
//        public void setNativeEventListener(@Nullable NativeEventListener nativeEventListener) {
//
//        }

    }
}

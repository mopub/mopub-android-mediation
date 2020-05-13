package com.mopub.mobileads;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.mopub.common.DataKeys;
import com.mopub.common.logging.MoPubLog;
import com.union_test.toutiao.config.TTAdManagerHolder;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;

/**
 * created by wuzejian on 2019-11-29
 */
public class PangolinAudienceAdBannerAdapter extends CustomEventBanner {

    protected static final String ADAPTER_NAME = "pangolinAdBannerAdapter";

    /**
     * Key to obtain Pangolin ad unit ID from the extras provided by MoPub.
     */
    public static final String KEY_EXTRA_AD_UNIT_ID = "adunit";


    /**
     * pangolin network banner ad unit ID.
     */
    private String mCodeId = null;

    public final static String GDPR_RESULT = "gdpr_result";
    public final static String AD_BANNER_WIDTH = "ad_banner_width";
    public final static String AD_BANNER_HEIGHT = "ad_banner_height";

    private PangolinAdapterConfiguration mPangolinAdapterConfiguration;

    private Context mContext;

    private PangolinAdExpressBannerLoader mAdExpressBannerLoader;
    private PangolinAdNativeBannerLoader mAdNativeBannerLoader;

    private float bannerWidth;
    private float bannerHeight;

    public PangolinAudienceAdBannerAdapter() {
        mPangolinAdapterConfiguration = new PangolinAdapterConfiguration();
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "TiktokAudienceAdBannerAdapter has been create ....");
    }


    @Override
    protected void loadBanner(Context context, CustomEventBannerListener customEventBannerListener, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        mContext = context;
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "loadBanner method execute ......");

        //cache data from server
        mPangolinAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);

        //init pangolin SDK
        TTAdManager mTTAdManager = TTAdManagerHolder.get();

        String adm = null;

        boolean isExpressAd = false;

        int bannerAdRefreshTime = 0;

        //set GDPR
        if (localExtras != null && !localExtras.isEmpty()) {
            if (localExtras.containsKey(GDPR_RESULT)) {
                int gdpr = (int) localExtras.get(GDPR_RESULT);
                mTTAdManager.setGdpr(gdpr);
                Log.e("banner", "banner receive gdpr=" + gdpr);
            }

            if (localExtras.containsKey(KEY_EXTRA_AD_UNIT_ID)) {
                mCodeId = (String) localExtras.get(KEY_EXTRA_AD_UNIT_ID);
            }

            isExpressAd = mTTAdManager.getAdRequetTypeByRit(mCodeId) == TTAdConstant.REQUEST_AD_TYPE_EXPRESS;

            //obtain extra parameters
            float[] adSize = AdapterUtil.getBannerAdSizeAdapterSafely(localExtras, AD_BANNER_WIDTH, AD_BANNER_HEIGHT);
            bannerWidth = adSize[0];
            bannerHeight = adSize[1];

            //check size
            checkSize(isExpressAd);

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "bannerWidth =" + bannerWidth + "，bannerHeight=" + bannerHeight + ",mCodeId=" + mCodeId);

        }

        //obtain adunit from server by mopub
        if (serverExtras != null) {
            String adunit = serverExtras.get(KEY_EXTRA_AD_UNIT_ID);
            if (!TextUtils.isEmpty(adunit)) {
                this.mCodeId = adunit;
            }
            adm = serverExtras.get(DataKeys.ADM_KEY);
        }

        MoPubLog.log(CUSTOM, ADAPTER_NAME, "loadBanner method mCodeId：" + mCodeId);

        //create request parameters for AdSlot
        AdSlot.Builder adSlotBuilder = new AdSlot.Builder()
                .setCodeId(mCodeId) //广告位id
                .setSupportDeepLink(true)
                .setAdCount(1) //请求广告数量为1到3条
                .setImageAcceptedSize((int) bannerWidth, (int) bannerHeight)
                .withBid(adm);//adm不为空时候，传入adm数据

        if (isExpressAd) {
            adSlotBuilder.setExpressViewAcceptedSize(bannerWidth, bannerHeight); //期望模板广告view的size,单位dp
        } else {
            adSlotBuilder.setNativeAdType(AdSlot.TYPE_BANNER);//原生banner
        }

        //request ad express banner
        if (isExpressAd) {
            mAdExpressBannerLoader = new PangolinAdExpressBannerLoader(mContext, customEventBannerListener);
            mAdExpressBannerLoader.loadAdExpressBanner(adSlotBuilder.build(), mTTAdManager.createAdNative(mContext));
        } else {
            mAdNativeBannerLoader = new PangolinAdNativeBannerLoader(mContext, customEventBannerListener);
            mAdNativeBannerLoader.loadAdNativeBanner(adSlotBuilder.build(), mTTAdManager.createAdNative(mContext));
        }

    }

    private void checkSize(boolean isExpressAd) {
        if (isExpressAd) {
            if (bannerWidth <= 0) {
                bannerWidth = 320;
                bannerHeight = 0;//0自适应
            }
            if (bannerHeight < 0) {
                bannerHeight = 0;
            }
        } else {
            //default value
            if (bannerWidth <= 0 || bannerHeight <= 0) {
                bannerWidth = 320;
                bannerHeight = 50;
            }
        }
    }


    @Override
    protected void onInvalidate() {
        if (mAdExpressBannerLoader != null) {
            mAdExpressBannerLoader.destroy();
            mAdExpressBannerLoader = null;
        }

        if (mAdNativeBannerLoader != null) {
            mAdNativeBannerLoader.destroy();
            mAdNativeBannerLoader = null;
        }
    }
}

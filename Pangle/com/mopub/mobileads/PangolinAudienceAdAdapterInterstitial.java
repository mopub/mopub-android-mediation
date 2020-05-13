package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.mopub.common.DataKeys;
import com.mopub.common.logging.MoPubLog;
import com.union_test.toutiao.config.TTAdManagerHolder;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;

/**
 * created by wuzejian on 2020/5/11
 */
public class PangolinAudienceAdAdapterInterstitial extends CustomEventInterstitial {
    public static final String ADAPTER_NAME = "PangolinAdapterInterstitial";

    /**
     * Key to obtain Pangolin ad unit ID from the extras provided by MoPub.
     */
    public static final String KEY_EXTRA_AD_UNIT_ID = "adunit";

    /**
     * GDRP value if need : 0 close GDRP Privacy protection ，1: open GDRP Privacy protection
     */
    public final static String GDPR_RESULT = "gdpr_result";

    /**
     * ad size
     */
    public final static String AD_WIDTH = "ad_width";
    public final static String AD_HEIGHT = "ad_height";


    /**
     * obtain value of ad type is full-video ad
     */
    public final static String AD_TYPE_FULL_VIDEO = "ad_type_full_video";


    /**
     * activity param
     */
    public final static String EXPRESS_ACTIVITY_PARAM = "activity_param";

    /**
     * pangolin network Interstitial ad unit ID.
     */
    private String mCodeId;

    private boolean mIsFullVideoAd;

    private PangolinAdapterConfiguration mPangolinAdapterConfiguration;


    private Context mContext;
    private Activity mActivity;
    private boolean isExpressAd = false;
    private float adWidth = 0;
    private float adHeight = 0;


    private PangolinAdExpressInterstitialLoader mExpressInterstitialLoader;
    private PangolinAdNativeInterstitialLoader mNativeInterstitialLoader;
    private PangolinAdIFullVideoLoader mFullVideoLoader;

    public PangolinAudienceAdAdapterInterstitial() {
        mPangolinAdapterConfiguration = new PangolinAdapterConfiguration();
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "TiktokAudienceAdAdapterInterstitial has been create ....");
    }

    @Override
    protected void loadInterstitial(
            final Context context,
            final CustomEventInterstitialListener customEventInterstitialListener,
            final Map<String, Object> localExtras,
            final Map<String, String> serverExtras) {
        mPangolinAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
        setAutomaticImpressionAndClickTracking(false);
        this.mContext = context;
        //创建TTAdManager

        String adm = null;

        TTAdManager ttAdManager = TTAdManagerHolder.get();
        TTAdNative ttAdNative = ttAdManager.createAdNative(context.getApplicationContext());

        if (localExtras != null && !localExtras.isEmpty()) {
            if (localExtras.containsKey(GDPR_RESULT)) {
                int gdpr = (int) localExtras.get(GDPR_RESULT);
                //set GDPR
                ttAdManager.setGdpr(gdpr);
            }

            this.mIsFullVideoAd = localExtras.get(AD_TYPE_FULL_VIDEO) != null && (boolean) localExtras.get(AD_TYPE_FULL_VIDEO);
            this.mActivity = (Activity) localExtras.get(EXPRESS_ACTIVITY_PARAM);
            this.mCodeId = (String) localExtras.get(KEY_EXTRA_AD_UNIT_ID);

            isExpressAd = ttAdManager.getAdRequetTypeByRit(mCodeId) == TTAdConstant.REQUEST_AD_TYPE_EXPRESS;

            //obtain extra parameters
            float[] adSize = AdapterUtil.getAdSizeSafely(localExtras, AD_WIDTH, AD_HEIGHT);
            adWidth = adSize[0];
            adHeight = adSize[1];

            //check size
            checkSize(isExpressAd);

        }


        //obtain adunit from server by mopub
        if (serverExtras != null) {
            String adunit = serverExtras.get(KEY_EXTRA_AD_UNIT_ID);
            if (!TextUtils.isEmpty(adunit)) {
                this.mCodeId = adunit;
            }
            adm = serverExtras.get(DataKeys.ADM_KEY);
        }
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "adWidth =" + adWidth + "，adHeight=" + adHeight + ",mCodeId=" + mCodeId + ",isExpressAd=" + isExpressAd);

        AdSlot.Builder adSlotBuilder = new AdSlot.Builder()
                .setCodeId(mCodeId) //广告位id
                .setSupportDeepLink(true)
                .setAdCount(1) //请求广告数量为1到3条
                .withBid(adm); //adm不为空时候，传入adm数据


        if (!mIsFullVideoAd) {
            //request Interstitial
            if (isExpressAd) {
                adSlotBuilder.setExpressViewAcceptedSize(adWidth, adHeight); //期望模板广告view的size,单位dp
                mExpressInterstitialLoader = new PangolinAdExpressInterstitialLoader(mContext, customEventInterstitialListener);
                mExpressInterstitialLoader.loadExpressInterstitialAd(adSlotBuilder.build(), ttAdNative);
            } else {
                adSlotBuilder.setImageAcceptedSize((int) adWidth, (int) adHeight);
                adSlotBuilder.setNativeAdType(AdSlot.TYPE_INTERACTION_AD);
                mNativeInterstitialLoader = new PangolinAdNativeInterstitialLoader(mContext, customEventInterstitialListener);
                mNativeInterstitialLoader.loadAdNativeInterstitial(adSlotBuilder.build(), ttAdNative);
            }
        } else {
            //request FullVideoAd
            adSlotBuilder.setImageAcceptedSize(1080, 1920);
            mFullVideoLoader = new PangolinAdIFullVideoLoader(mContext, customEventInterstitialListener);
            mFullVideoLoader.loadAdFullVideoListener(adSlotBuilder.build(), ttAdNative);
        }
    }


    private void checkSize(boolean isExpressAd) {
        if (isExpressAd) {
            if (adWidth <= 0) {
                adWidth = 300;
                adWidth = 450;//0自适应
            }
            if (adHeight < 0) {
                adHeight = 0;
            }
        } else {
            //default value
            if (adWidth <= 0 || adHeight <= 0) {
                adWidth = 600;
                adHeight = 900;
            }
        }
    }


    @Override
    protected void showInterstitial() {
        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);
        if (!mIsFullVideoAd) {
            if (isExpressAd) {
                if (mExpressInterstitialLoader != null) {
                    mExpressInterstitialLoader.showInterstitial(mActivity);
                }
            } else {
                if (mNativeInterstitialLoader != null) {
                    mNativeInterstitialLoader.showNativeInterstitial(mActivity);
                }
            }
        } else {
            if (mFullVideoLoader != null) {
                mFullVideoLoader.showFullVideo(mActivity);
            }
        }
    }

    @Override
    protected void onInvalidate() {
        if (!mIsFullVideoAd) {
            if (isExpressAd) {
                if (mExpressInterstitialLoader != null) {
                    mExpressInterstitialLoader.destroy();
                }
            } else {
                if (mNativeInterstitialLoader != null) {
                    mNativeInterstitialLoader.destroy();
                }
            }
        } else {
            if (mFullVideoLoader != null) {
                mFullVideoLoader.destroy();
            }
        }
    }

}

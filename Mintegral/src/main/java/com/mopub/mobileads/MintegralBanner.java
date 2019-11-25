package com.mopub.mobileads;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import com.mopub.common.MoPub;
import com.mintegral.msdk.MIntegralConstans;
import com.mintegral.msdk.MIntegralSDK;
import com.mintegral.msdk.out.BannerAdListener;
import com.mintegral.msdk.out.BannerSize;
import com.mintegral.msdk.out.MIntegralSDKFactory;
import com.mintegral.msdk.out.MTGBannerView;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.mintegral.BuildConfig;

import java.util.Map;

import static com.mopub.common.DataKeys.AD_HEIGHT;
import static com.mopub.common.DataKeys.AD_WIDTH;

public class MintegralBanner extends CustomEventBanner implements BannerAdListener {
    private final String ADAPTER_NAME = "MintegralBanner";
    private CustomEventBannerListener mBannerListener;
    private MTGBannerView mtgBannerView;

    @Override
    protected void loadBanner(Context context, CustomEventBannerListener customEventBannerListener, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        Log.e(ADAPTER_NAME, "loadBanner: ");
        mBannerListener = customEventBannerListener;

        final String adUnitId;

        if (extrasAreValid(serverExtras, context, localExtras)) {
            adUnitId = serverExtras.get("unitId");
            MoPubLog.log(adUnitId, MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED, new Object[]{ADAPTER_NAME});
        } else {
            customEventBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        if (localExtras.containsKey(AD_WIDTH) && localExtras.containsKey(AD_HEIGHT)) {
            int adwidth = (int) localExtras.get(AD_WIDTH);
            int adheight = (int) localExtras.get(AD_HEIGHT);
            Log.e(ADAPTER_NAME, "loadBanner: adwidth:" + adwidth + "- adheight:" + adheight);

            final int w = dip2px(context, adwidth);
            final int h = dip2px(context, adheight);
            String adm = serverExtras.get("adm");
            mtgBannerView = new MTGBannerView(context);
            mtgBannerView.setVisibility(View.GONE);
            mtgBannerView.init(new BannerSize(BannerSize.DEV_SET_TYPE, adwidth, adheight), adUnitId);

            mtgBannerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    MoPubView.LayoutParams lp = (MoPubView.LayoutParams) mtgBannerView.getLayoutParams();
                    lp.width = w;
                    lp.height = h;
                    mtgBannerView.setLayoutParams(lp);
                }
            });

            mtgBannerView.setBannerAdListener(this);
            if (TextUtils.isEmpty(adm)) {
                mtgBannerView.load();
            } else {
                mtgBannerView.loadFromBid(adm);
            }


        }


    }

    @Override
    protected void onInvalidate() {
        Log.e(ADAPTER_NAME, "onInvalidate: ");
        if (mtgBannerView != null) {
            mtgBannerView.release();
        }
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras, Context mContext, Map<String, Object> localExtras) {
        final String appId = serverExtras.get("appId");
        final String appKey = serverExtras.get("appKey");
        final String unitId = serverExtras.get("unitId");

        BuildConfig.addChannel();
        if (!TextUtils.isEmpty(appId) && !TextUtils.isEmpty(appKey) && !TextUtils.isEmpty(unitId)) {
            MIntegralSDK sdk = MIntegralSDKFactory.getMIntegralSDK();
            if (!MoPub.canCollectPersonalInformation()) {
                sdk.setUserPrivateInfoType(mContext, MIntegralConstans.AUTHORITY_ALL_INFO, MIntegralConstans.IS_SWITCH_OFF);
            } else {
                sdk.setUserPrivateInfoType(mContext, MIntegralConstans.AUTHORITY_ALL_INFO, MIntegralConstans.IS_SWITCH_ON);
            }
            Map<String, String> map = sdk.getMTGConfigurationMap(appId,
                    appKey);
            if (mContext instanceof Activity) {
                sdk.init(map, ((Activity) mContext).getApplication());
            } else if (mContext instanceof Application) {
                sdk.init(map, mContext);
            }
            BuildConfig.parseLocalExtras(localExtras, sdk);
            return true;
        }

        return false;
    }


    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;

        return (int) (dipValue * scale + 0.5f);
    }

    @Override
    public void onLoadFailed(String s) {

        if (mBannerListener != null) {
            mBannerListener.onBannerFailed(MoPubErrorCode.NO_FILL);
        }
        Log.e(ADAPTER_NAME, "onLoadFailed: " + s);
        MoPubLog.log(MoPubLog.AdapterLogEvent.LOAD_FAILED, new Object[]{ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL});
    }

    @Override
    public void onLoadSuccessed() {
        MoPubLog.log(MoPubLog.AdapterLogEvent.CUSTOM, new Object[]{ADAPTER_NAME, "mintegral banner ad loaded successfully. Showing ad..."});
        if (mBannerListener != null && mtgBannerView != null) {
            mBannerListener.onBannerLoaded(mtgBannerView);
            mtgBannerView.setVisibility(View.VISIBLE);
            MoPubLog.log(MoPubLog.AdapterLogEvent.LOAD_SUCCESS, new Object[]{ADAPTER_NAME});
            MoPubLog.log(MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED, new Object[]{ADAPTER_NAME});
            MoPubLog.log(MoPubLog.AdapterLogEvent.SHOW_SUCCESS, new Object[]{ADAPTER_NAME});
        }
        Log.d(ADAPTER_NAME, "onLoadSuccessed: ");
    }

    @Override
    public void onLogImpression() {
        if (mBannerListener != null) {
            mBannerListener.onBannerImpression();
        }
        Log.d(ADAPTER_NAME, "onLogImpression: ");
        MoPubLog.log(MoPubLog.AdapterLogEvent.SHOW_SUCCESS, new Object[]{ADAPTER_NAME});
    }


    @Override
    public void onClick() {
        if (mBannerListener != null) {
            mBannerListener.onBannerClicked();
        }
        Log.d(ADAPTER_NAME, "onClick: ");
        MoPubLog.log(MoPubLog.AdapterLogEvent.CLICKED, new Object[]{ADAPTER_NAME});
    }

    @Override
    public void onLeaveApp() {
        if (mBannerListener != null) {
            mBannerListener.onLeaveApplication();
        }
        Log.d(ADAPTER_NAME, "onLeaveApp: ");
    }

    @Override
    public void showFullScreen() {

        Log.d(ADAPTER_NAME, "showFullScreen: ");
    }

    @Override
    public void closeFullScreen() {

        Log.d(ADAPTER_NAME, "closeFullScreen: ");
    }


}

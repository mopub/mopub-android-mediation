package com.mopub.mobileads;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewTreeObserver;

import com.mintegral.msdk.out.BannerAdListener;
import com.mintegral.msdk.out.BannerSize;
import com.mintegral.msdk.out.MIntegralSDKFactory;
import com.mintegral.msdk.out.MTGBannerView;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

import static com.mopub.common.DataKeys.ADM_KEY;
import static com.mopub.common.DataKeys.AD_HEIGHT;
import static com.mopub.common.DataKeys.AD_WIDTH;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.WILL_LEAVE_APPLICATION;
import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_NO_FILL;

public class MintegralBanner extends CustomEventBanner implements BannerAdListener {

    private static final String ADAPTER_NAME = MintegralBanner.class.getSimpleName();

    private CustomEventBannerListener mBannerListener;
    private MTGBannerView mBannerAd;

    private static String mAdUnitId;
    private int mAdWidth, mAdHeight;

    @Override
    protected void loadBanner(final Context context,
                              final CustomEventBannerListener customEventBannerListener,
                              final Map<String, Object> localExtras, Map<String, String> serverExtras) {

        mBannerListener = customEventBannerListener;

        if (!serverDataIsValid(serverExtras, context)) {
            failAdapter(ADAPTER_CONFIGURATION_ERROR, "One or " +
                    "more keys used for Mintegral's ad requests are empty. Failing adapter. Please " +
                    "ensure you have populated all the required keys on the MoPub dashboard.");

            return;
        }

        if (!adSizesAreValid(localExtras)) {
            failAdapter(ADAPTER_CONFIGURATION_ERROR, "Either the ad width " +
                    "or the ad height is less than or equal to 0. Failing adapter. Please ensure " +
                    "you have supplied the MoPub SDK non-zero ad width and height.");

            return;
        }

        mBannerAd = new MTGBannerView(context);
        mBannerAd.setVisibility(View.GONE);
        mBannerAd.init(new BannerSize(BannerSize.DEV_SET_TYPE, mAdWidth, mAdHeight), mAdUnitId);
        mBannerAd.setBannerAdListener(this);

        mBannerAd.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (mBannerAd != null) {
                    final int width = dip2px(context, mAdWidth);
                    final int height = dip2px(context, mAdHeight);

                    MoPubView.LayoutParams lp = (MoPubView.LayoutParams) mBannerAd.getLayoutParams();
                    lp.width = width;
                    lp.height = height;
                    mBannerAd.setLayoutParams(lp);
                }
            }
        });

        MintegralAdapterConfiguration.addChannel();
        MintegralAdapterConfiguration.setTargeting(MIntegralSDKFactory.getMIntegralSDK());

        final String adm = serverExtras.get(ADM_KEY);
        if (TextUtils.isEmpty(adm)) {
            mBannerAd.load();
        } else {
            mBannerAd.loadFromBid(adm);
        }

        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Requesting Mintegral banner " +
                "with width " + mAdWidth + " and height " + mAdHeight);

        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    @Override
    protected void onInvalidate() {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Finished showing Mintegral " +
                "banner. Invalidating adapter...");

        if (mBannerAd != null) {
            mBannerAd.release();
            mBannerAd = null;
        }

        mBannerListener = null;
    }

    private boolean adSizesAreValid(Map<String, Object> localExtras) {
        if (localExtras != null && !localExtras.isEmpty()) {
            final Object widthObj = localExtras.get(AD_WIDTH);

            if (widthObj instanceof Integer) {
                mAdWidth = (int) widthObj;
            }

            final Object heightObj = localExtras.get(AD_HEIGHT);

            if (heightObj instanceof Integer) {
                mAdHeight = (int) heightObj;
            }

            return mAdWidth > 0 && mAdHeight > 0;
        }

        return false;
    }

    private boolean serverDataIsValid(final Map<String, String> serverExtras, Context context) {

        if (serverExtras != null && !serverExtras.isEmpty()) {
            mAdUnitId = serverExtras.get(MintegralAdapterConfiguration.UNIT_ID_KEY);
            final String appId = serverExtras.get(MintegralAdapterConfiguration.APP_ID_KEY);
            final String appKey = serverExtras.get(MintegralAdapterConfiguration.APP_KEY);

            if (!TextUtils.isEmpty(appId) && !TextUtils.isEmpty(appKey) && !TextUtils.isEmpty(mAdUnitId)) {
                MintegralAdapterConfiguration.configureMintegral(appId, appKey, context);

                return true;
            }
        }
        return false;
    }

    private void failAdapter(final MoPubErrorCode errorCode, final String errorMsg) {

        MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, errorCode.getIntCode(), errorCode);

        if (!TextUtils.isEmpty(errorMsg)) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, errorMsg);
        }

        if (mBannerListener != null) {
            mBannerListener.onBannerFailed(errorCode);
        }
    }

    private static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;

        return (int) (dipValue * scale + 0.5f);
    }

    private static String getAdNetworkId() {
        return mAdUnitId;
    }

    @Override
    public void onLoadFailed(String errorMsg) {
        failAdapter(NETWORK_NO_FILL, errorMsg);
    }

    @Override
    public void onLoadSuccessed() {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Mintegral banner ad loaded " +
                "successfully. Showing ad...");

        if (mBannerListener != null && mBannerAd != null) {
            mBannerListener.onBannerLoaded(mBannerAd);
            mBannerAd.setVisibility(View.VISIBLE);

            MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
            MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
        }
    }

    @Override
    public void onLogImpression() {
        if (mBannerListener != null) {
            mBannerListener.onBannerImpression();
        }

        MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
    }


    @Override
    public void onClick() {
        if (mBannerListener != null) {
            mBannerListener.onBannerClicked();
        }

        MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
    }

    @Override
    public void onLeaveApp() {
        if (mBannerListener != null) {
            mBannerListener.onLeaveApplication();
        }

        MoPubLog.log(getAdNetworkId(), WILL_LEAVE_APPLICATION, ADAPTER_NAME);
    }

    @Override
    public void showFullScreen() {
    }

    @Override
    public void closeFullScreen() {
    }
}

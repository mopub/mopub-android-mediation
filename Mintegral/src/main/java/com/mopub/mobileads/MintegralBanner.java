package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mintegral.msdk.out.BannerAdListener;
import com.mintegral.msdk.out.BannerSize;
import com.mintegral.msdk.out.MIntegralSDKFactory;
import com.mintegral.msdk.out.MTGBannerView;
import com.mopub.common.LifecycleListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

import static com.mopub.common.DataKeys.ADM_KEY;
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

public class MintegralBanner extends BaseAd implements BannerAdListener {

    private final String ADAPTER_NAME = this.getClass().getSimpleName();

    private MTGBannerView mBannerAd;
    private String mAdUnitId;
    private String mPlacementId;
    private int mAdWidth, mAdHeight;

    @Override
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        setAutomaticImpressionAndClickTracking(false);

        final Map<String, String> extras = adData.getExtras();
        if (!serverDataIsValid(extras, context)) {
            failAdapter(ADAPTER_CONFIGURATION_ERROR, "One or " +
                    "more keys used for Mintegral's ad requests are empty. Failing adapter. Please " +
                    "ensure you have populated all the required keys on the MoPub dashboard.");

            return;
        }

        if (!adSizesAreValid(adData)) {
            failAdapter(ADAPTER_CONFIGURATION_ERROR, "Either the ad width " +
                    "or the ad height is less than or equal to 0. Failing adapter. Please ensure " +
                    "you have supplied the MoPub SDK non-zero ad width and height.");

            return;
        }

        mBannerAd = new MTGBannerView(context);
        mBannerAd.setVisibility(View.GONE);
        mBannerAd.init(new BannerSize(BannerSize.DEV_SET_TYPE, mAdWidth, mAdHeight), mPlacementId,
                mAdUnitId);
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

        final String adMarkup = extras.get(ADM_KEY);
        if (TextUtils.isEmpty(adMarkup)) {
            mBannerAd.load();
        } else {
            mBannerAd.loadFromBid(adMarkup);
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
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    private boolean adSizesAreValid(@NonNull final AdData adData) {
        mAdWidth = adData.getAdWidth() != null ? adData.getAdWidth() : 0;
        mAdHeight = adData.getAdHeight() != null ? adData.getAdHeight() : 0;

        return mAdWidth > 0 && mAdHeight > 0;
    }

    private boolean serverDataIsValid(final Map<String, String> extras, Context context) {

        if (extras != null && !extras.isEmpty()) {
            mAdUnitId = extras.get(MintegralAdapterConfiguration.UNIT_ID_KEY);
            mPlacementId = extras.get(MintegralAdapterConfiguration.PLACEMENT_ID_KEY);

            final String appId = extras.get(MintegralAdapterConfiguration.APP_ID_KEY);
            final String appKey = extras.get(MintegralAdapterConfiguration.APP_KEY);

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

        if (mLoadListener != null) {
            mLoadListener.onAdLoadFailed(errorCode);
        }

    }

    private static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;

        return (int) (dipValue * scale + 0.5f);
    }

    @NonNull
    public String getAdNetworkId() {
        return mAdUnitId != null ? mAdUnitId : "";
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull final Activity activity,
                                            @NonNull final AdData adData) {
        return false;
    }

    @Override
    public void onLoadFailed(String errorMsg) {
        failAdapter(NETWORK_NO_FILL, errorMsg);
    }

    @Override
    public void onLoadSuccessed() {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Mintegral banner ad loaded " +
                "successfully. Showing ad...");

        if (mLoadListener != null && mBannerAd != null) {
            mLoadListener.onAdLoaded();
            mBannerAd.setVisibility(View.VISIBLE);

            MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
            MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
        }
    }

    @Override
    protected View getAdView() {
        return mBannerAd;
    }

    @Override
    public void onLogImpression() {
        if (mInteractionListener != null) {
            mInteractionListener.onAdImpression();
        }

        MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
    }


    @Override
    public void onClick() {
        if (mInteractionListener != null) {
            mInteractionListener.onAdClicked();
        }

        MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
    }

    @Override
    public void onLeaveApp() {
        MoPubLog.log(getAdNetworkId(), WILL_LEAVE_APPLICATION, ADAPTER_NAME);
    }

    @Override
    public void showFullScreen() {
    }

    @Override
    public void closeFullScreen() {
    }

    @Override
    public void onCloseBanner() {
    }
}

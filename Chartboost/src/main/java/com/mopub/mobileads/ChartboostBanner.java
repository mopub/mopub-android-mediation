package com.mopub.mobileads;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.chartboost.sdk.Banner.BannerSize;
import com.chartboost.sdk.ChartboostBannerListener;
import com.chartboost.sdk.Events.ChartboostCacheError;
import com.chartboost.sdk.Events.ChartboostCacheEvent;
import com.chartboost.sdk.Events.ChartboostClickError;
import com.chartboost.sdk.Events.ChartboostClickEvent;
import com.chartboost.sdk.Events.ChartboostShowError;
import com.chartboost.sdk.Events.ChartboostShowEvent;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

import static com.mopub.common.DataKeys.AD_HEIGHT;
import static com.mopub.common.DataKeys.AD_WIDTH;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;

public class ChartboostBanner extends CustomEventBanner {

    private static final String ADAPTER_NAME = ChartboostBanner.class.getSimpleName();

    @NonNull
    private String mLocation = ChartboostShared.LOCATION_DEFAULT;

    private String getAdNetworkId() {
        return mLocation;
    }

    @NonNull
    private ChartboostAdapterConfiguration mChartboostAdapterConfiguration;

    private com.chartboost.sdk.ChartboostBanner mChartboostBanner;
    private CustomEventBannerListener mCustomEventBannerListener;
    private int mAdWith, mAdHeight;
    private FrameLayout mInternalView;

    public ChartboostBanner() {
        mChartboostAdapterConfiguration = new ChartboostAdapterConfiguration();
    }

    @Override
    protected void loadBanner(Context context, final CustomEventBannerListener customEventBannerListener, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(customEventBannerListener);
        Preconditions.checkNotNull(localExtras);
        Preconditions.checkNotNull(serverExtras);

        if (serverExtras.containsKey(ChartboostShared.LOCATION_KEY)) {
            String location = serverExtras.get(ChartboostShared.LOCATION_KEY);
            mLocation = TextUtils.isEmpty(location) ? mLocation : location;
        }

        try {
            ChartboostShared.initializeSdk(context, serverExtras);
            mCustomEventBannerListener = customEventBannerListener;
            mChartboostAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
        } catch (NullPointerException e) {
            reportBannerInvalidStateError(customEventBannerListener);
            return;
        } catch (IllegalStateException e) {
            reportBannerInvalidStateError(customEventBannerListener);
            return;
        }

        prepareLayout(context);
        createBanner(context, localExtras);
        attachBannerToLayout();
        mChartboostBanner.show();
    }

    private void prepareLayout(Context context) {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
        mInternalView = new FrameLayout(context);
        mInternalView.setLayoutParams(layoutParams);
    }

    private void createBanner(Context context, Map<String, Object> localExtras) {
        final BannerSize bannerSize = chartboostAdSizeFromLocalExtras(localExtras);
        mChartboostBanner = new com.chartboost.sdk.ChartboostBanner(context, mLocation, bannerSize, chartboostBannerListener);
    }

    private void attachBannerToLayout() {
        mChartboostBanner.removeAllViews();
        mInternalView.addView(mChartboostBanner);
    }

    private void reportBannerInvalidStateError(final CustomEventBannerListener customEventBannerListener) {
        if (customEventBannerListener != null) {
            customEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
        }

        MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                MoPubErrorCode.NETWORK_INVALID_STATE.getIntCode(),
                MoPubErrorCode.NETWORK_INVALID_STATE);
    }

    @Override
    protected void onInvalidate() {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Finished showing Chartboost " +
                "banner. Invalidating adapter...");

        mInternalView.removeAllViews();
        mInternalView = null;
        if (mChartboostBanner != null) {
            mChartboostBanner.detachBanner();
        }
        mChartboostBanner = null;
        mCustomEventBannerListener = null;
    }

    private BannerSize chartboostAdSizeFromLocalExtras(final Map<String, Object> localExtras) {
        if (localExtras != null && !localExtras.isEmpty()) {
            try {
                final Object adHeightObject = localExtras.get(AD_HEIGHT);
                if (adHeightObject instanceof Integer) {
                    mAdHeight = (int) adHeightObject;
                }

                final Object adWidthObject = localExtras.get(AD_WIDTH);
                if (adWidthObject instanceof Integer) {
                    mAdWith = (int) adWidthObject;
                }

                int LEADERBOARD_HEIGHT = BannerSize.getHeight(BannerSize.LEADERBOARD);
                int LEADERBOARD_WIDTH = BannerSize.getWidth(BannerSize.LEADERBOARD);
                int MEDIUM_HEIGHT = BannerSize.getHeight(BannerSize.MEDIUM);
                int MEDIUM_WIDTH = BannerSize.getWidth(BannerSize.MEDIUM);

                if (mAdHeight >= LEADERBOARD_HEIGHT && mAdWith >= LEADERBOARD_WIDTH) {
                    return BannerSize.LEADERBOARD;
                } else if (mAdHeight >= MEDIUM_HEIGHT && mAdWith >= MEDIUM_WIDTH) {
                    return BannerSize.MEDIUM;
                } else {
                    return BannerSize.STANDARD;
                }
            } catch (Exception e) {
                MoPubLog.log(getAdNetworkId(), CUSTOM_WITH_THROWABLE, ADAPTER_NAME, e);
            }
        }

        return BannerSize.STANDARD;
    }

    private ChartboostBannerListener chartboostBannerListener = new ChartboostBannerListener() {
        @Override
        public void onAdCached(ChartboostCacheEvent chartboostCacheEvent, ChartboostCacheError chartboostCacheError) {
            if (chartboostCacheError != null) {
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                        chartboostCacheError.code,
                        chartboostCacheError.toString());
                mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            } else {
                mCustomEventBannerListener.onBannerLoaded(mInternalView);
            }
        }

        @Override
        public void onAdShown(ChartboostShowEvent chartboostShowEvent, ChartboostShowError chartboostShowError) {
            if (chartboostShowError != null) {
                MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME,
                        chartboostShowError.code,
                        chartboostShowError.toString());
                mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
        }

        @Override
        public void onAdClicked(ChartboostClickEvent chartboostClickEvent, ChartboostClickError chartboostClickError) {
            if (chartboostClickError != null) {
                MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME,
                        chartboostClickError.code,
                        chartboostClickError.toString());
                mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            } else {
                mCustomEventBannerListener.onBannerClicked();
            }
        }
    };
}

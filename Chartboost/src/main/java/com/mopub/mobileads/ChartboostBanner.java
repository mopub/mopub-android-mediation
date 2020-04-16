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
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;

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
    protected void loadBanner(@NonNull Context context,
                              @NonNull final CustomEventBannerListener customEventBannerListener,
                              @NonNull Map<String, Object> localExtras,
                              @NonNull Map<String, String> serverExtras) {
        try {
            Preconditions.checkNotNull(context);
            Preconditions.checkNotNull(customEventBannerListener);
            Preconditions.checkNotNull(localExtras);
            Preconditions.checkNotNull(serverExtras);

            setAutomaticImpressionAndClickTracking(false);

            final String location = serverExtras.get(ChartboostShared.LOCATION_KEY);

            if (!TextUtils.isEmpty(location)) {
                mLocation = location;
            }

            ChartboostShared.initializeSdk(context, serverExtras);
            mCustomEventBannerListener = customEventBannerListener;
            mChartboostAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
        } catch (NullPointerException | IllegalStateException error) {
            logAndNotifyBannerFailed(customEventBannerListener, LOAD_FAILED, NETWORK_INVALID_STATE,
                    null, null);
            return;
        }

        logAndNotifyBannerFailed(customEventBannerListener, LOAD_FAILED, NETWORK_INVALID_STATE,
                null, null);

        prepareLayout(context);
        createBanner(context, localExtras);
        attachBannerToLayout();
        mChartboostBanner.show();
    }

    private void prepareLayout(Context context) {
        final FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );

        layoutParams.gravity = Gravity.CENTER_HORIZONTAL;

        mInternalView = new FrameLayout(context);
        mInternalView.setLayoutParams(layoutParams);
    }

    private void createBanner(Context context, Map<String, Object> localExtras) {
        final BannerSize bannerSize = chartboostAdSizeFromLocalExtras(localExtras);

        mChartboostBanner = new com.chartboost.sdk.ChartboostBanner(context, mLocation,
                bannerSize, chartboostBannerListener);
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "Requested ad size is: Chartboost " + bannerSize);
    }

    private void attachBannerToLayout() {
        if (mChartboostBanner != null && mInternalView != null) {
            mChartboostBanner.removeAllViews();
            mInternalView.addView(mChartboostBanner);
        }
    }

    private void logAndNotifyBannerFailed(final CustomEventBannerListener listener,
                                          MoPubLog.AdapterLogEvent event,
                                          MoPubErrorCode moPubErrorCode,
                                          String chartboostErrorName,
                                          Integer chartboostErrorCode) {
        if (chartboostErrorName != null && chartboostErrorCode != null) {
            ChartboostAdapterConfiguration.logChartboostError(getAdNetworkId(), ADAPTER_NAME, event,
                    chartboostErrorName, chartboostErrorCode);
        }

        MoPubLog.log(getAdNetworkId(), event, ADAPTER_NAME, moPubErrorCode.getIntCode(), moPubErrorCode);
        if (listener != null) {
            listener.onBannerFailed(moPubErrorCode);
        }
    }

    @Override
    protected void onInvalidate() {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Finished showing Chartboost " +
                "banner. Invalidating adapter...");

        if (mInternalView != null) {
            mInternalView.removeAllViews();
            mInternalView = null;
        }

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

                final int LEADERBOARD_HEIGHT = BannerSize.getHeight(BannerSize.LEADERBOARD);
                final int LEADERBOARD_WIDTH = BannerSize.getWidth(BannerSize.LEADERBOARD);
                final int MEDIUM_HEIGHT = BannerSize.getHeight(BannerSize.MEDIUM);
                final int MEDIUM_WIDTH = BannerSize.getWidth(BannerSize.MEDIUM);

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
                logAndNotifyBannerFailed(mCustomEventBannerListener,
                        LOAD_FAILED, MoPubErrorCode.NO_FILL,
                        chartboostCacheError.toString(), chartboostCacheError.code);
            } else {
                MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
                if (mCustomEventBannerListener != null) {
                    mCustomEventBannerListener.onBannerLoaded(mInternalView);
                }
            }
        }

        @Override
        public void onAdShown(ChartboostShowEvent chartboostShowEvent, ChartboostShowError chartboostShowError) {
            if (chartboostShowError != null) {
                logAndNotifyBannerFailed(mCustomEventBannerListener,
                        SHOW_FAILED, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR,
                        chartboostShowError.toString(), chartboostShowError.code);
            } else {
                MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
                MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
                if (mCustomEventBannerListener != null) {
                    mCustomEventBannerListener.onBannerImpression();
                }
            }
        }

        @Override
        public void onAdClicked(ChartboostClickEvent chartboostClickEvent, ChartboostClickError chartboostClickError) {
            if (chartboostClickError != null) {
                logAndNotifyBannerFailed(mCustomEventBannerListener,
                        CLICKED, MoPubErrorCode.UNSPECIFIED,
                        chartboostClickError.toString(), chartboostClickError.code);
            } else {
                MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
                if (mCustomEventBannerListener != null) {
                    mCustomEventBannerListener.onBannerClicked();
                }
            }
        }
    };
}

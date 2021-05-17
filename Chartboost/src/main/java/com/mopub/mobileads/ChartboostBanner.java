package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chartboost.sdk.Banner.BannerSize;
import com.chartboost.sdk.Chartboost;
import com.chartboost.sdk.ChartboostBannerListener;
import com.chartboost.sdk.Events.ChartboostCacheError;
import com.chartboost.sdk.Events.ChartboostCacheEvent;
import com.chartboost.sdk.Events.ChartboostClickError;
import com.chartboost.sdk.Events.ChartboostClickEvent;
import com.chartboost.sdk.Events.ChartboostShowError;
import com.chartboost.sdk.Events.ChartboostShowEvent;
import com.mopub.common.LifecycleListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.INTERNAL_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;

public class ChartboostBanner extends BaseAd {

    private static final String ADAPTER_NAME = ChartboostBanner.class.getSimpleName();

    @NonNull
    private String mLocation = ChartboostShared.LOCATION_DEFAULT;

    @NonNull
    private ChartboostAdapterConfiguration mChartboostAdapterConfiguration;

    private com.chartboost.sdk.ChartboostBanner mChartboostBanner;
    private int mAdWidth, mAdHeight;
    private FrameLayout mInternalView;
    private boolean loadTracked;
    private boolean impressionTracked;
    private boolean clickTracked;

    public ChartboostBanner() {
        mChartboostAdapterConfiguration = new ChartboostAdapterConfiguration();
    }

    @NonNull
    public String getAdNetworkId() {
        return mLocation;
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull AdData adData) throws Exception {
        return false;
    }

    @Override
    protected void load(@NonNull final Context context,
                        @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Chartboost Banners are not compatible with Android API < 21. " +
                    "Will fail the request prematurely.");
            logAndNotifyBannerFailed(true, LOAD_FAILED, INTERNAL_ERROR,
                    null, null);
            return;
        }

        try {
            setAutomaticImpressionAndClickTracking(false);

            loadTracked = false;
            impressionTracked = false;
            clickTracked = false;

            final Map<String, String> extras = adData.getExtras();
            final String location = extras.get(ChartboostShared.LOCATION_KEY);

            if (!TextUtils.isEmpty(location)) {
                mLocation = location;
            }

            mChartboostAdapterConfiguration.setCachedInitializationParameters(context, extras);

            try {
                ChartboostAdapterConfiguration.initializeChartboostSdk(context, extras);
            } catch (Exception initializationError) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, ADAPTER_NAME,
                        "Chartboost initialization called by adapter " + ADAPTER_NAME +
                                " has failed because of an exception", initializationError.getMessage());
            }

        } catch (NullPointerException | IllegalStateException error) {
            logAndNotifyBannerFailed(true, LOAD_FAILED, NETWORK_INVALID_STATE,
                    null, null);
            return;
        }

        prepareLayout(context);
        createBanner(context, adData);
        attachBannerToLayout();
        mChartboostBanner.cache();
    }

    @Nullable
    @Override
    protected View getAdView() {
        return mInternalView;
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

    private void createBanner(final Context context, final AdData adData) {
        final BannerSize bannerSize = chartboostAdSizeFromAdData(adData);
        mChartboostBanner = new com.chartboost.sdk.ChartboostBanner(context, mLocation,
                bannerSize, chartboostBannerListener);
        mChartboostBanner.setAutomaticallyRefreshesContent(false);
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "Requested ad size is: Chartboost " + bannerSize);
    }

    private void attachBannerToLayout() {
        if (mChartboostBanner != null && mInternalView != null) {
            mChartboostBanner.removeAllViews();
            mInternalView.addView(mChartboostBanner);
        }
    }

    private void logAndNotifyBannerFailed(boolean isLoad,
                                          MoPubLog.AdapterLogEvent event,
                                          MoPubErrorCode moPubErrorCode,
                                          String chartboostErrorName,
                                          Integer chartboostErrorCode) {
        if (chartboostErrorName != null && chartboostErrorCode != null) {
            ChartboostAdapterConfiguration.logChartboostError(getAdNetworkId(), ADAPTER_NAME, event,
                    chartboostErrorName, chartboostErrorCode);
        }

        MoPubLog.log(getAdNetworkId(), event, ADAPTER_NAME, moPubErrorCode.getIntCode(), moPubErrorCode);

        if (isLoad && mLoadListener != null) {
            mLoadListener.onAdLoadFailed(moPubErrorCode);
        } else if (!isLoad && mInteractionListener != null) {
            mInteractionListener.onAdFailed(moPubErrorCode);
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
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    private BannerSize chartboostAdSizeFromAdData(final AdData adData) {
        if (adData != null) {
            try {
                final Integer adHeight = adData.getAdHeight();
                if (adHeight != null) {
                    mAdHeight = adHeight;
                }

                final Integer adWidth = adData.getAdWidth();
                if (adWidth != null) {
                    mAdWidth = adWidth;
                }

                final int LEADERBOARD_HEIGHT = BannerSize.getHeight(BannerSize.LEADERBOARD);
                final int LEADERBOARD_WIDTH = BannerSize.getWidth(BannerSize.LEADERBOARD);
                final int MEDIUM_HEIGHT = BannerSize.getHeight(BannerSize.MEDIUM);
                final int MEDIUM_WIDTH = BannerSize.getWidth(BannerSize.MEDIUM);

                if (mAdHeight >= LEADERBOARD_HEIGHT && mAdWidth >= LEADERBOARD_WIDTH) {
                    return BannerSize.LEADERBOARD;
                } else if (mAdHeight >= MEDIUM_HEIGHT && mAdWidth >= MEDIUM_WIDTH) {
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
            if (chartboostCacheError == null) {
                if (!loadTracked) {
                    if (mLoadListener != null) {
                        mLoadListener.onAdLoaded();
                        loadTracked = true;
                    }
                }
                mChartboostBanner.show();
            } else {
                logAndNotifyBannerFailed(true, LOAD_FAILED, MoPubErrorCode.NO_FILL,
                        chartboostCacheError.toString(), chartboostCacheError.code.getErrorCode());
            }
        }

        @Override
        public void onAdShown(ChartboostShowEvent chartboostShowEvent, ChartboostShowError chartboostShowError) {
            if (chartboostShowError == null) {
                if (!impressionTracked) {
                    if (mInteractionListener != null) {
                        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
                        MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
                        mInteractionListener.onAdImpression();
                        impressionTracked = true;
                    }
                }
            } else {
                logAndNotifyBannerFailed(false, SHOW_FAILED, MoPubErrorCode.INLINE_SHOW_ERROR,
                        chartboostShowError.toString(), chartboostShowError.code.getErrorCode());
            }
        }

        @Override
        public void onAdClicked(ChartboostClickEvent chartboostClickEvent, ChartboostClickError chartboostClickError) {
            if (chartboostClickError != null) {
                logAndNotifyBannerFailed(false, CLICKED, MoPubErrorCode.UNSPECIFIED,
                        chartboostClickError.toString(), chartboostClickError.code.getErrorCode());
            } else {
                if (!clickTracked) {
                    if (mInteractionListener != null) {
                        MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
                        mInteractionListener.onAdClicked();
                        clickTracked = true;
                    }
                }
            }
        }
    };
}

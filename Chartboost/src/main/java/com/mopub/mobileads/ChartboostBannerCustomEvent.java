package com.mopub.mobileads;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.chartboost.sdk.Banner.BannerSize;
import com.chartboost.sdk.ChartboostBanner;
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
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;

public class ChartboostBannerCustomEvent extends CustomEventBanner {

    private static final String ADAPTER_NAME = ChartboostBannerCustomEvent.class.getSimpleName();

    @NonNull
    private String mLocation = ChartboostShared.LOCATION_DEFAULT;

    @NonNull
    private ChartboostAdapterConfiguration mChartboostAdapterConfiguration;

    private ChartboostBanner banner;
    private CustomEventBannerListener mCustomEventBannerListener;
    private int adWidth, adHeight;
    private FrameLayout internalView;

    public ChartboostBannerCustomEvent() {
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
            customEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);

            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);
            return;
        } catch (IllegalStateException e) {
            customEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);

            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);
            return;
        }


        final BannerSize bannerSize = chartboostAdSizeFromLocalExtras(localExtras);

        internalView = new FrameLayout(context);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        internalView.setLayoutParams(lp);

        banner = new ChartboostBanner(context, mLocation, bannerSize, bannerListener);
        banner.removeAllViews();
        internalView.addView(banner);
        mCustomEventBannerListener.onBannerLoaded(internalView);
        banner.show();
    }

    @Override
    protected void onInvalidate() {
        internalView.removeAllViews();
        if(banner != null) {
            banner.detachBanner();
        }
        banner = null;
        mCustomEventBannerListener = null;
    }

    private BannerSize chartboostAdSizeFromLocalExtras(final Map<String, Object> localExtras) {
        try {
            final Object adWidthObject = localExtras.get(AD_WIDTH);
            if (adWidthObject instanceof Integer) {
                adWidth = (int) adWidthObject;
            }

            final Object adHeightObject = localExtras.get(AD_HEIGHT);
            if (adHeightObject instanceof Integer) {
                adHeight = (int) adHeightObject;
            }

            if (adWidth >= 728 && adHeight >= 90) {
                return BannerSize.LEADERBOARD;
            } else if (adWidth >= 300 && adHeight >= 250) {
                return BannerSize.MEDIUM;
            } else {
                return BannerSize.STANDARD;
            }
        } catch (Exception e) {
            MoPubLog.log(getAdNetworkId(), CUSTOM_WITH_THROWABLE, ADAPTER_NAME, e);
        }

        return BannerSize.STANDARD;
    }

    private String getAdNetworkId() {
        return mLocation;
    }

    private ChartboostBannerListener bannerListener = new ChartboostBannerListener() {
        @Override
        public void onAdCached(ChartboostCacheEvent chartboostCacheEvent, ChartboostCacheError chartboostCacheError) {
            if(chartboostCacheError!=null) {
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                        chartboostCacheError.code,
                        chartboostCacheError.toString());
                mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
        }

        @Override
        public void onAdShown(ChartboostShowEvent chartboostShowEvent, ChartboostShowError chartboostShowError) {
            if(chartboostShowError!=null) {
                MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME,
                        chartboostShowError.code,
                        chartboostShowError.toString());
                mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
        }

        @Override
        public void onAdClicked(ChartboostClickEvent chartboostClickEvent, ChartboostClickError chartboostClickError) {
            if(chartboostClickError != null) {
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

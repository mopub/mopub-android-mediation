package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.LifecycleListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Views;
import com.snap.adkit.external.BannerView;
import com.snap.adkit.external.SnapAdClicked;
import com.snap.adkit.external.SnapAdDismissed;
import com.snap.adkit.external.SnapAdEventListener;
import com.snap.adkit.external.SnapAdKitEvent;
import com.snap.adkit.external.SnapAdLoadFailed;
import com.snap.adkit.external.SnapAdLoadSucceeded;
import com.snap.adkit.external.SnapAdSize;
import com.snap.adkit.external.SnapAdVisible;
import com.snap.adkit.external.SnapBannerAdImpressionRecorded;

import java.util.Map;

import static com.mopub.common.DataKeys.ADUNIT_FORMAT;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.DID_DISAPPEAR;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.mobileads.MoPubErrorCode.INLINE_LOAD_ERROR;

public class SnapAdBanner extends BaseAd {
    private static final String ADAPTER_NAME = SnapAdBanner.class.getSimpleName();
    private static final String SLOT_ID_KEY = "slotId";

    private static String mSlotId;
    private BannerView mBannerView;

    private final SnapAdAdapterConfiguration mSnapAdAdapterConfiguration;

    public SnapAdBanner() {
        mSnapAdAdapterConfiguration = new SnapAdAdapterConfiguration();
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity,
                                            @NonNull AdData adData) {
        return false;
    }

    @Override
    protected void load(@NonNull Context context, @NonNull AdData adData) throws Exception {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        setAutomaticImpressionAndClickTracking(false);

        final Map<String, String> extras = adData.getExtras();

        if (extras == null || extras.isEmpty()) {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }

            return;
        }

        mSnapAdAdapterConfiguration.setCachedInitializationParameters(context, extras);

        String adUnitFormat = extras.get(ADUNIT_FORMAT);

        if (!TextUtils.isEmpty(adUnitFormat)) {
            adUnitFormat = adUnitFormat.toLowerCase();
        }

        final SnapAdSize adSize = getAdSize(adUnitFormat);

        if (adSize == null) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME,
                    "SnapAudienceNetwork only supports ad sizes 320*50 and 300*250. " +
                            "Please ensure your MoPub ad unit format is Banner or Medium Rectangle.");
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return;
        }

        mBannerView = new BannerView(context);
        mBannerView.setAdSize(adSize);

        mSlotId = extras.get(SLOT_ID_KEY);

        mBannerView.setupListener(new SnapAdEventListener() {
            @Override
            public void onEvent(SnapAdKitEvent snapAdKitEvent, String slotId) {
                if (snapAdKitEvent instanceof SnapAdLoadSucceeded) {
                    MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);

                    if (mLoadListener != null) {
                        mLoadListener.onAdLoaded();
                    }
                } else if (snapAdKitEvent instanceof SnapAdLoadFailed) {
                    MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, INLINE_LOAD_ERROR.getIntCode(),
                            INLINE_LOAD_ERROR);

                    if (mLoadListener != null) {
                        mLoadListener.onAdLoadFailed(INLINE_LOAD_ERROR);
                    }
                } else if (snapAdKitEvent instanceof SnapAdVisible) {
                    MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);

                    if (mInteractionListener != null) {
                        mInteractionListener.onAdShown();
                    }
                } else if (snapAdKitEvent instanceof SnapAdClicked) {
                    MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);

                    if (mInteractionListener != null) {
                        mInteractionListener.onAdClicked();
                    }
                } else if (snapAdKitEvent instanceof SnapBannerAdImpressionRecorded) {
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Snap recorded " +
                            "impression: " + snapAdKitEvent.toString());

                    if (mInteractionListener != null) {
                        mInteractionListener.onAdImpression();
                    }
                } else if (snapAdKitEvent instanceof SnapAdDismissed) {
                    MoPubLog.log(getAdNetworkId(), DID_DISAPPEAR, ADAPTER_NAME);

                    if (mInteractionListener != null) {
                        mInteractionListener.onAdDismissed();
                    }
                } else {
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Received event " +
                            "from Snap Ad Kit: " + snapAdKitEvent.toString());
                }
            }
        });

        mBannerView.loadAd(mSlotId, null);
    }

    private SnapAdSize getAdSize(String adUnitFormat) {
        if ("banner".equals(adUnitFormat)) {
            return SnapAdSize.BANNER;
        } else if ("medium_rectangle".equals(adUnitFormat)) {
            return SnapAdSize.MEDIUM_RECTANGLE;
        } else {
            return null;
        }
    }

    @Override
    protected void onInvalidate() {
        Views.removeFromParent(mBannerView);

        if (mBannerView != null) {
            mBannerView.setupListener(null);
            mBannerView.destroy();

            mBannerView = null;
        }
    }

    @Nullable
    @Override
    protected View getAdView() {
        return mBannerView;
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return TextUtils.isEmpty(mSlotId) ? "" : mSlotId;
    }
}

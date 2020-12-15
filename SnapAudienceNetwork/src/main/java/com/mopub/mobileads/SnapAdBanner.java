package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubReward;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Views;
import com.snap.adkit.dagger.AdKitApplication;
import com.snap.adkit.external.BannerView;
import com.snap.adkit.external.SnapAdClicked;
import com.snap.adkit.external.SnapAdDismissed;
import com.snap.adkit.external.SnapAdEventListener;
import com.snap.adkit.external.SnapAdKit;
import com.snap.adkit.external.SnapAdKitEvent;
import com.snap.adkit.external.SnapAdLoadFailed;
import com.snap.adkit.external.SnapAdLoadSucceeded;
import com.snap.adkit.external.SnapAdRewardEarned;
import com.snap.adkit.external.SnapAdSize;
import com.snap.adkit.external.SnapAdVisible;
import com.snap.adkit.external.SnapBannerAdImpressionRecorded;

import static com.mopub.common.DataKeys.ADUNIT_FORMAT;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.DID_DISAPPEAR;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOULD_REWARD;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.mobileads.MoPubErrorCode.FULLSCREEN_LOAD_ERROR;

class SnapAdBanner extends BaseAd {

    private static final String ADAPTER_NAME = SnapAdBanner.class.getSimpleName();
    private static final String SLOT_ID_KEY = "slotId";

    private static String mSlotId;
    private BannerView bannerView;

    private final SnapAdAdapterConfiguration mSnapAdAdapterConfiguration;

    public SnapAdBanner() {
        mSnapAdAdapterConfiguration = new SnapAdAdapterConfiguration();
    }

    @NonNull
    private final SnapAdKit snapAdKit = AdKitApplication.getSnapAdKit();

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull AdData adData) throws Exception {
        return false;
    }

    @Override
    protected void load(@NonNull Context context, @NonNull AdData adData) throws Exception {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        setAutomaticImpressionAndClickTracking(false);

        final java.util.Map<String, String> extras = adData.getExtras();

        if (extras.isEmpty()) {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return;
        }

        String adUnitFormat = extras.get(ADUNIT_FORMAT);
        if (!TextUtils.isEmpty(adUnitFormat)) {
            adUnitFormat = adUnitFormat.toLowerCase();
        }

        SnapAdSize adSize;
        if ("banner".equals(adUnitFormat)) {
            adSize = SnapAdSize.BANNER;
        } else if ("medium_rectangle".equals(adUnitFormat)) {
            adSize = SnapAdSize.MEDIUM_RECTANGLE;
        } else {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME,
                    "SnapAudienceNetwork only supports ad sizes 320*50 and 300*250. " +
                            "Please ensure your MoPub adunit format is Banner or Medium Rectangle.");
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return;
        }

        bannerView = new BannerView(context);
        bannerView.setAdSize(adSize);
        mSlotId = extras.get(SLOT_ID_KEY);
        if (!TextUtils.isEmpty(mSlotId)) {
            bannerView.updateSlotId(mSlotId);
        }

        bannerView.setupListener(new SnapAdEventListener() {
            @Override
            public void onEvent(SnapAdKitEvent snapAdKitEvent, String slotId) {
                if (snapAdKitEvent instanceof SnapAdLoadSucceeded) {
                    MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);

                    if (mLoadListener != null) {
                        mLoadListener.onAdLoaded();
                    }
                } else if (snapAdKitEvent instanceof SnapAdLoadFailed) {
                    MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, FULLSCREEN_LOAD_ERROR.getIntCode(),
                            FULLSCREEN_LOAD_ERROR);

                    if (mLoadListener != null) {
                        mLoadListener.onAdLoadFailed(FULLSCREEN_LOAD_ERROR);
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
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Snap recorded impression: " +
                            snapAdKitEvent.toString());

                    if (mInteractionListener != null) {
                        mInteractionListener.onAdImpression();
                    }
                } else if (snapAdKitEvent instanceof SnapAdDismissed) {
                    MoPubLog.log(getAdNetworkId(), DID_DISAPPEAR, ADAPTER_NAME);

                    if (mInteractionListener != null) {
                        mInteractionListener.onAdDismissed();
                    }
                } else if (snapAdKitEvent instanceof SnapAdRewardEarned) {
                    MoPubLog.log(getAdNetworkId(), SHOULD_REWARD, ADAPTER_NAME,
                            MoPubReward.DEFAULT_REWARD_AMOUNT, MoPubReward.NO_REWARD_LABEL);

                    if (mInteractionListener != null) {
                        mInteractionListener.onAdComplete(MoPubReward.success(MoPubReward.NO_REWARD_LABEL,
                                MoPubReward.DEFAULT_REWARD_AMOUNT));
                    }
                } else {
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Received event from Snap " +
                            "Ad Kit: " + snapAdKitEvent.toString());
                }
            }
        });

        bannerView.loadAd();
    }

    @Override
    protected void onInvalidate() {
        Views.removeFromParent(bannerView);

        if (bannerView != null) {
            bannerView.setupListener(null);
            bannerView.destroy();
        }
    }

    @Nullable
    @Override
    protected android.view.View getAdView() {
        return bannerView;
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return TextUtils.isEmpty(mSlotId) ? "" : mSlotId;
    }
}

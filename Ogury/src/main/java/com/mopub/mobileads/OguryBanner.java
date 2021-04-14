package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.LifecycleListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.ogury.core.OguryError;
import com.ogury.ed.OguryBannerAdListener;
import com.ogury.ed.OguryBannerAdSize;
import com.ogury.ed.OguryBannerAdView;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;

public class OguryBanner extends BaseAd implements OguryBannerAdListener {

    private static final String ADAPTER_NAME = OguryBanner.class.getSimpleName();

    private String mAdUnitId;
    private OguryBannerAdView mBanner;
    private OguryAdListenerHelper mListenerHelper;

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        if (mAdUnitId == null) {
            return "";
        }
        return mAdUnitId;
    }

    @Override
    protected boolean checkAndInitializeSdk(
            @NonNull Activity launcherActivity,
            @NonNull AdData adData) {
        return false;
    }

    @Override
    protected void load(
            @NonNull Context context,
            @NonNull AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        setAutomaticImpressionAndClickTracking(false);

        OguryInitializer.startOgurySDKIfNecessary(context, adData.getExtras());
        OguryInitializer.updateConsent();

        mAdUnitId = OguryConfigurationParser.getAdUnitId(adData.getExtras());
        OguryBannerAdSize adSize = getBannerAdSize(adData.getAdWidth(), adData.getAdHeight());

        if (!OguryConfigurationParser.isAdUnitIdValid(mAdUnitId) || adSize == null) {
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
            }
            return;
        }

        mBanner = new OguryBannerAdView(context);
        mBanner.setAdUnit(mAdUnitId);
        mBanner.setAdSize(adSize);
        mListenerHelper = new OguryAdListenerHelper(ADAPTER_NAME, mAdUnitId);

        mListenerHelper.setLoadListener(mLoadListener);
        mBanner.setListener(this);
        mBanner.loadAd();

        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    public static OguryBannerAdSize getBannerAdSize(Integer iAdWidth, Integer iAdHeight) {
        int adWidth = (iAdWidth != null) ? iAdWidth : 0;
        int adHeight = (iAdHeight != null) ? iAdHeight : 0;
        if (canIncludeSize(OguryBannerAdSize.SMALL_BANNER_320x50, adWidth, adHeight)) {
            return OguryBannerAdSize.SMALL_BANNER_320x50;
        } else if (canIncludeSize(OguryBannerAdSize.MPU_300x250, adWidth, adHeight)) {
            return OguryBannerAdSize.MPU_300x250;
        }
        return null;
    }

    private static boolean canIncludeSize(OguryBannerAdSize oguryBannerAdSize, int mopubWidth, int mopubHeight) {
        if ((mopubHeight < oguryBannerAdSize.getHeight()) || (mopubWidth < oguryBannerAdSize.getWidth())) {
            return false;
        }
        float maxRatio = 1.5f;
        return (!(mopubHeight >= oguryBannerAdSize.getHeight() * maxRatio)) && (!(mopubWidth >= oguryBannerAdSize.getWidth() * maxRatio));
    }

    @Nullable
    @Override
    protected View getAdView() {
        return mBanner;
    }

    @Override
    protected void show() {
        mListenerHelper.setInteractionListener(mInteractionListener);
    }

    @Override
    protected void onInvalidate() {
        if (mBanner != null) {
            mBanner.destroy();
            mListenerHelper = null;
            mBanner = null;
        }
    }

    /**
     * OguryBannerAdListener implementation
     */

    @Override
    public void onAdLoaded() {
        mListenerHelper.onAdLoaded();
    }

    @Override
    public void onAdDisplayed() {
        mListenerHelper.onAdDisplayed();
    }

    @Override
    public void onAdClicked() {
        mListenerHelper.onAdClicked();
    }

    @Override
    public void onAdClosed() {
        mListenerHelper.onAdClosed();
    }

    @Override
    public void onAdError(OguryError oguryError) {
        mListenerHelper.onAdError(oguryError);
    }
}

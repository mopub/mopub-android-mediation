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
import com.ogury.core.OguryError;
import com.ogury.ed.OguryAdImpressionListener;
import com.ogury.ed.OguryBannerAdListener;
import com.ogury.ed.OguryBannerAdSize;
import com.ogury.ed.OguryBannerAdView;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;

public class OguryBanner extends BaseAd implements OguryBannerAdListener, OguryAdImpressionListener {
    private static final String ADAPTER_NAME = OguryBanner.class.getSimpleName();

    private final OguryAdapterConfiguration mOguryAdapterConfiguration;

    private String mAdUnitId;
    private OguryBannerAdView mBanner;
    private OguryAdListenerHelper mListenerHelper;

    public OguryBanner() {
        mOguryAdapterConfiguration = new OguryAdapterConfiguration();
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return mAdUnitId != null ? mAdUnitId : "";
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull AdData adData) {
        Preconditions.checkNotNull(launcherActivity);
        Preconditions.checkNotNull(adData);

        return OguryAdapterConfiguration.startOgurySDKIfNecessary(launcherActivity, adData.getExtras());
    }

    @Override
    protected void load(@NonNull Context context, @NonNull AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        setAutomaticImpressionAndClickTracking(false);

        final Map<String, String> extras = adData.getExtras();
        final OguryBannerAdSize adSize = getBannerAdSize(adData.getAdWidth(), adData.getAdHeight());

        mAdUnitId = OguryAdapterConfiguration.getAdUnitId(extras);

        if (TextUtils.isEmpty(mAdUnitId) || adSize == null) {
            if (adSize == null) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "The requested banner size does not fit " +
                        "because Ogury only supports 320x50 and 300x250 sizes. Failing ad request.");
            }

            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return;
        }

        mOguryAdapterConfiguration.setCachedInitializationParameters(context, extras);

        mBanner = new OguryBannerAdView(context);
        mBanner.setAdUnit(mAdUnitId);
        mBanner.setAdSize(adSize);

        mListenerHelper = new OguryAdListenerHelper(ADAPTER_NAME, mAdUnitId);
        mListenerHelper.setLoadListener(mLoadListener);

        mBanner.setListener(this);
        mBanner.setAdImpressionListener(this);
        mBanner.loadAd();

        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    @Nullable
    @Override
    protected View getAdView() {
        return mBanner;
    }

    @Override
    protected void onInvalidate() {
        if (mBanner != null) {
            mBanner.destroy();
            mBanner = null;
        }

        mListenerHelper = null;
    }

    public static OguryBannerAdSize getBannerAdSize(Integer iAdWidth, Integer iAdHeight) {
        final int adWidth = (iAdWidth != null) ? iAdWidth : 0;
        final int adHeight = (iAdHeight != null) ? iAdHeight : 0;

        if (canIncludeSize(OguryBannerAdSize.SMALL_BANNER_320x50, adWidth, adHeight)) {
            return OguryBannerAdSize.SMALL_BANNER_320x50;
        } else if (canIncludeSize(OguryBannerAdSize.MPU_300x250, adWidth, adHeight)) {
            return OguryBannerAdSize.MPU_300x250;
        }

        return null;
    }

    /**
     * This function determine if a banner of the provided size can be fitted in the space of the
     * parent banner.
     * <p>
     * It will return false if:
     * - either the width or height is smaller than the provided Ogury's size.
     * - the padding around the Ogury's banner is too large (more than 50% of the actual size of the banner).
     *
     * @param oguryBannerAdSize One of Ogury's supported banner size.
     * @param requestedWidth    Width of the parent banner. Acts as a ceiling.
     * @param requestedHeight   Height of the parent banner. Acts as a ceiling.
     * @return true if a banner of the provided size can fit in requestedWidth x requestedHeight, false otherwise.
     */
    private static boolean canIncludeSize(OguryBannerAdSize oguryBannerAdSize, int requestedWidth,
                                          int requestedHeight) {
        float maxRatio = 1.5f;
        return requestedHeight <= oguryBannerAdSize.getHeight() * maxRatio &&
                requestedWidth <= oguryBannerAdSize.getWidth() * maxRatio;
    }

    @Override
    public void onAdLoaded() {
        mListenerHelper.onAdLoaded();
    }

    @Override
    public void onAdDisplayed() {
        mListenerHelper.setInteractionListener(mInteractionListener);
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

    @Override
    public void onAdImpression() {
        mListenerHelper.onAdImpression();
    }
}

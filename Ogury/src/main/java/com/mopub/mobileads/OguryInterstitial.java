package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.LifecycleListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.ogury.core.OguryError;
import com.ogury.ed.OguryInterstitialAd;
import com.ogury.ed.OguryInterstitialAdListener;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;

public class OguryInterstitial extends BaseAd implements OguryInterstitialAdListener {

    private static final String ADAPTER_NAME = OguryInterstitial.class.getSimpleName();

    private String mAdUnitId;
    private OguryInterstitialAd mInterstitial;
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
        if (!OguryConfigurationParser.isAdUnitIdValid(mAdUnitId)) {
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
            }
            return;
        }

        mInterstitial = new OguryInterstitialAd(context, mAdUnitId);
        mListenerHelper = new OguryAdListenerHelper(ADAPTER_NAME, mAdUnitId);

        mListenerHelper.setLoadListener(mLoadListener);
        mInterstitial.setListener(this);
        mInterstitial.load();

        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    @Override
    protected void show() {
        if (mInterstitial == null || !mInterstitial.isLoaded()) {
            MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
            if (mInteractionListener != null) {
                mInteractionListener.onAdFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
            return;
        }
        mListenerHelper.setInteractionListener(mInteractionListener);
        mInterstitial.show();

        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
    }

    @Override
    protected void onInvalidate() {
        mListenerHelper = null;
        mInterstitial = null;
    }

    /**
     * OguryInterstitialAdListener implementation
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

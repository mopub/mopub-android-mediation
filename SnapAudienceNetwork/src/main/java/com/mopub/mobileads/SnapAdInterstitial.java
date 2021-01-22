package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.LifecycleListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.snap.adkit.dagger.AdKitApplication;
import com.snap.adkit.external.SnapAdClicked;
import com.snap.adkit.external.SnapAdDismissed;
import com.snap.adkit.external.SnapAdEventListener;
import com.snap.adkit.external.SnapAdImpressionHappened;
import com.snap.adkit.external.SnapAdKit;
import com.snap.adkit.external.SnapAdKitEvent;
import com.snap.adkit.external.SnapAdLoadFailed;
import com.snap.adkit.external.SnapAdLoadSucceeded;
import com.snap.adkit.external.SnapAdVisible;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.DID_DISAPPEAR;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.mobileads.MoPubErrorCode.FULLSCREEN_LOAD_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_NO_FILL;

public class SnapAdInterstitial extends BaseAd {
    private static final String ADAPTER_NAME = SnapAdInterstitial.class.getSimpleName();
    private static final String SLOT_ID_KEY = "slotId";

    private static String mSlotId;

    private final SnapAdAdapterConfiguration mSnapAdAdapterConfiguration;

    public SnapAdInterstitial() {
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
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity,
                                            @NonNull AdData adData) {
        return false;
    }

    @Override
    protected void load(@NonNull Context context, @NonNull AdData adData) {
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

        mSlotId = extras.get(SLOT_ID_KEY);
        if (!TextUtils.isEmpty(mSlotId)) {
            snapAdKit.updateSlotId(mSlotId);
        }

        snapAdKit.setupListener(new SnapAdEventListener() {
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
                } else if (snapAdKitEvent instanceof SnapAdImpressionHappened) {
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
                } else {
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Received event from Snap " +
                            "Ad Kit: " + snapAdKitEvent.toString());
                }
            }
        });

        mSnapAdAdapterConfiguration.setCachedInitializationParameters(context, extras);
        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);

        snapAdKit.loadInterstitial();
    }

    @Override
    protected void show() {
        try {
            MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);

            snapAdKit.playAd();
        } catch (Exception exception) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Failed to show Snap " +
                    "Audience Network Ads");
            MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            if (mInteractionListener != null) {
                mInteractionListener.onAdFailed((NETWORK_NO_FILL));
            }
        }
    }

    @Override
    protected void onInvalidate() {
        // no-op
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return TextUtils.isEmpty(mSlotId) ? "" : mSlotId;
    }
}

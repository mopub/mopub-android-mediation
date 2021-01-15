package com.mopub.mobileads;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

public abstract class AdColonyBaseAd extends BaseAd {

    @NonNull
    protected AdColonyAdapterConfiguration mAdColonyAdapterConfiguration;

    protected final Handler mHandler;

    @NonNull
    protected String mZoneId = AdColonyAdapterConfiguration.DEFAULT_ZONE_ID;

    @NonNull
    public String getAdNetworkId() {
        return mZoneId;
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull AdData adData) throws Exception {
        return false;
    }

    public AdColonyBaseAd() {
        mHandler = new Handler(Looper.getMainLooper());
        mAdColonyAdapterConfiguration = new AdColonyAdapterConfiguration();
    }
}

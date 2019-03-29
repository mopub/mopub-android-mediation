package com.mopub.mobileads;

import com.mopub.common.logging.MoPubLog;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;

@SuppressWarnings("unused")
public class MillennialInterstitial extends VerizonInterstitial {

    private static final String ADAPTER_NAME = MillennialInterstitial.class.getSimpleName();

    public MillennialInterstitial() {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "Verizon Adapter Version: " + VerizonAdapterConfiguration.MEDIATOR_ID);
    }

    @Override
    protected String getPlacementIdKey() {
        return "adUnitID";
    }

    @Override
    protected String getSiteIdKey() {
        return "dcn";
    }
}
package com.mopub.mobileads;

import com.mopub.common.logging.MoPubLog;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;

@SuppressWarnings("unused")
public class MillennialBanner extends VerizonBanner {

    private static final String ADAPTER_NAME = MillennialBanner.class.getSimpleName();

    public MillennialBanner() {
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

    @Override
    protected String getWidthKey() {
        return "adWidth";
    }

    @Override
    protected String getHeightKey() {
        return "adHeight";
    }
}
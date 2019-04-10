package com.mopub.mobileads;

@SuppressWarnings("unused")
public class MillennialInterstitial extends VerizonInterstitial {

    private static final String ADAPTER_NAME = MillennialInterstitial.class.getSimpleName();
    private static final String PLACEMENT_ID_KEY = "adUnitID";
    private static final String SITE_ID_KEY = "dcn";

    public MillennialInterstitial() {
    }

    @Override
    protected String getPlacementIdKey() {
        return PLACEMENT_ID_KEY;
    }

    @Override
    protected String getSiteIdKey() {
        return SITE_ID_KEY;
    }
}

package com.mopub.mobileads;

@SuppressWarnings("unused")
public class MillennialBanner extends VerizonBanner {

    private static final String ADAPTER_NAME = MillennialBanner.class.getSimpleName();
    private static final String PLACEMENT_ID_KEY = "adUnitID";
    private static final String SITE_ID_KEY = "dcn";
    private static final String WIDTH_KEY = "adWidth";
    private static final String HEIGHT_KEY = "adHeight";

    public MillennialBanner() {
    }

    @Override
    protected String getPlacementIdKey() {
        return PLACEMENT_ID_KEY;
    }

    @Override
    protected String getSiteIdKey() {
        return SITE_ID_KEY;
    }

    @Override
    protected String getWidthKey() {
        return WIDTH_KEY;
    }

    @Override
    protected String getHeightKey() {
        return HEIGHT_KEY;
    }
}

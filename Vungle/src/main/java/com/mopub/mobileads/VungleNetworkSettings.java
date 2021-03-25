package com.mopub.mobileads;

import com.vungle.warren.VungleSettings;

/**
 * To apply the Vungle network settings during initialization.
 */
class VungleNetworkSettings {

    /**
     * Minimum Space in Bytes
     */
    private static long sMinimumSpaceForInit = 50 << 20;
    private static long sMinimumSpaceForAd = 51 << 20;
    private static boolean sAndroidIdOptedOut;
    private static VungleSettings sVungleSettings;

    static void setMinSpaceForInit(long spaceForInit) {
        sMinimumSpaceForInit = spaceForInit;
        applySettings();
    }

    static void setMinSpaceForAdLoad(long spaceForAd) {
        sMinimumSpaceForAd = spaceForAd;
        applySettings();
    }

    static void setAndroidIdOptOut(boolean isOptedOut) {
        sAndroidIdOptedOut = isOptedOut;
        applySettings();
    }

    static VungleSettings getVungleSettings() {
        if (sVungleSettings == null) {
            sVungleSettings = new VungleSettings.Builder().disableBannerRefresh().build();
        }
        return sVungleSettings;
    }

    /**
     * To pass Vungle network setting to SDK. this method must be called before first loadAd.
     * if called after first loading an ad, settings will not be applied.
     */
    private static void applySettings() {
        sVungleSettings = new VungleSettings.Builder()
                .setMinimumSpaceForInit(sMinimumSpaceForInit)
                .setMinimumSpaceForAd(sMinimumSpaceForAd)
                .setAndroidIdOptOut(sAndroidIdOptedOut)
                .disableBannerRefresh()
                .build();
    }
}

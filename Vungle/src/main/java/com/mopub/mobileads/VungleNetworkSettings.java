package com.mopub.mobileads;

import com.vungle.warren.VungleSettings;

/**
 * To apply the Vungle network settings during initialization.
 */
class VungleNetworkSettings {

    /**
     * Minimum Space in Bytes
     */
    private static long minimumSpaceForInit = 50 << 20;
    private static long minimumSpaceForAd = 51 << 20;
    private static boolean androidIdOptedOut;
    private static VungleSettings vungleSettings;

    static void setMinSpaceForInit(long spaceForInit){
        minimumSpaceForInit = spaceForInit;
        applySettings();
    }

    static void setMinSpaceForAdLoad(long spaceForAd){
        minimumSpaceForAd = spaceForAd;
        applySettings();
    }

    static void setAndroidIdOptOut(boolean isOptedOut){
        androidIdOptedOut = isOptedOut;
        applySettings();
    }

    /**
     * To pass Vungle network setting to SDK. this method must be called before first loadAd.
     * if called after first loading an ad, settings will not be applied.
     */
    private static void applySettings() {
        vungleSettings = new VungleSettings.Builder()
                .setMinimumSpaceForInit(minimumSpaceForInit)
                .setMinimumSpaceForAd(minimumSpaceForAd)
                .setAndroidIdOptOut(androidIdOptedOut)
                .build();
    }

    static VungleSettings getVungleSettings(){
        return vungleSettings;
    }
}

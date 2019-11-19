package com.mopub.mobileads;

import com.vungle.warren.AdConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * A helper class for creating a network extras map that can be passed to the adapter to make
 * customizations specific to Vungle.
 */
public final class VungleExtrasBuilder {

    private static final String EXTRA_START_MUTED = "startMuted";
    private static final String EXTRA_SOUND_ENABLED = VungleInterstitial.SOUND_ENABLED_KEY;
    private static final String EXTRA_FLEXVIEW_CLOSE_TIME = VungleInterstitial.FLEX_VIEW_CLOSE_TIME_KEY;
    private static final String EXTRA_ORDINAL_VIEW_COUNT = VungleInterstitial.ORDINAL_VIEW_COUNT_KEY;
    private static final String EXTRA_ORIENTATION = VungleInterstitial.AD_ORIENTATION_KEY;

    private final Map<String, Object> extrasMap = new HashMap<>();

    public VungleExtrasBuilder setStartMuted(boolean muted) {
        extrasMap.put(EXTRA_START_MUTED, muted);
        return this;
    }

    public VungleExtrasBuilder setFlexViewCloseTimeInSec(int flexViewCloseTimeInSec) {
        extrasMap.put(EXTRA_FLEXVIEW_CLOSE_TIME, flexViewCloseTimeInSec);
        return this;
    }

    public VungleExtrasBuilder setOrdinalViewCount(int ordinalViewCount) {
        extrasMap.put(EXTRA_ORDINAL_VIEW_COUNT, ordinalViewCount);
        return this;
    }

    public VungleExtrasBuilder setAdOrientation(int adOrientation) {
        extrasMap.put(EXTRA_ORIENTATION, adOrientation);
        return this;
    }

    public Map<String, Object> build() {
        return extrasMap;
    }

    public static void adConfigWithLocalExtras(AdConfig adConfig, Map<String, Object> localExtras) {
        if (localExtras.containsKey(EXTRA_START_MUTED)) {
            Object isStartMuted = localExtras.get(EXTRA_START_MUTED);
            if (isStartMuted instanceof Boolean)
                adConfig.setMuted((Boolean) isStartMuted);
        } else {
            Object isSoundEnabled = localExtras.get(EXTRA_SOUND_ENABLED);
            if (isSoundEnabled instanceof Boolean)
                adConfig.setMuted(!(Boolean) isSoundEnabled);
        }
        Object flexViewCloseTimeInSec = localExtras.get(EXTRA_FLEXVIEW_CLOSE_TIME);
        if (flexViewCloseTimeInSec instanceof Integer)
            adConfig.setFlexViewCloseTime((Integer) flexViewCloseTimeInSec);
        Object ordinalViewCount = localExtras.get(EXTRA_ORDINAL_VIEW_COUNT);
        if (ordinalViewCount instanceof Integer)
            adConfig.setOrdinal((Integer) ordinalViewCount);
        Object adOrientation = localExtras.get(EXTRA_ORIENTATION);
        if (adOrientation instanceof Integer)
            adConfig.setAdOrientation((Integer) adOrientation);
    }

    static boolean isStartMutedNotConfigured(Map<String, Object> localExtras) {

        return !localExtras.containsKey(EXTRA_START_MUTED);
    }
}

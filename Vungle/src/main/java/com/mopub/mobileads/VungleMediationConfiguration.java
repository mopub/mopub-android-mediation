package com.mopub.mobileads;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.MediationSettings;
import com.vungle.warren.AdConfig;

import java.util.HashMap;
import java.util.Map;

public class VungleMediationConfiguration implements MediationSettings {
    @Nullable
    private final String userId;
    @Nullable
    private final String title;
    @Nullable
    private final String body;
    @Nullable
    private final String closeButtonText;
    @Nullable
    private final String keepWatchingButtonText;

    private final boolean isStartMuted;
    private final int flexViewCloseTimeInSec;
    private final int ordinalViewCount;
    private final int adOrientation;
    private final Map<String, Object> extras;

    @Nullable
    public String getUserId() {
        return userId;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    @Nullable
    public String getBody() {
        return body;
    }

    @Nullable
    public String getCloseButtonText() {
        return closeButtonText;
    }

    @Nullable
    public String getKeepWatchingButtonText() {
        return keepWatchingButtonText;
    }

    public boolean isStartMuted() {
        return isStartMuted;
    }

    public int getFlexViewCloseTimeInSec() {
        return flexViewCloseTimeInSec;
    }

    public int getOrdinalViewCount() {
        return ordinalViewCount;
    }

    public int getAdOrientation() {
        return adOrientation;
    }

    public Map<String, Object> getExtrasMap() {
        return extras;
    }

    static void adConfigWithLocalExtras(AdConfig adConfig, Map<String, Object> localExtras) {

        if (localExtras != null && !localExtras.isEmpty()) {

            if (localExtras.containsKey(Builder.EXTRA_START_MUTED_KEY)) {
                final Object isStartMuted = localExtras.get(Builder.EXTRA_START_MUTED_KEY);

                if (isStartMuted instanceof Boolean) {
                    adConfig.setMuted((Boolean) isStartMuted);
                }
            } else {
                final Object isSoundEnabled = localExtras.get(Builder.EXTRA_SOUND_ENABLED_KEY);
                if (isSoundEnabled instanceof Boolean) {
                    adConfig.setMuted(!(Boolean) isSoundEnabled);
                }
            }

            final Object flexViewCloseTimeInSec = localExtras.get(Builder.EXTRA_FLEXVIEW_CLOSE_TIME_KEY);

            if (flexViewCloseTimeInSec instanceof Integer) {
                adConfig.setFlexViewCloseTime((Integer) flexViewCloseTimeInSec);
            }
            final Object ordinalViewCount = localExtras.get(Builder.EXTRA_ORDINAL_VIEW_COUNT_KEY);

            if (ordinalViewCount instanceof Integer) {
                adConfig.setOrdinal((Integer) ordinalViewCount);
            }

            final Object adOrientation = localExtras.get(Builder.EXTRA_ORIENTATION_KEY);

            if (adOrientation instanceof Integer) {
                adConfig.setAdOrientation((Integer) adOrientation);
            }
        }
    }

    static boolean isStartMutedNotConfigured(Map<String, Object> localExtras) {
        return !localExtras.containsKey(Builder.EXTRA_START_MUTED_KEY) &&
                !localExtras.containsKey(Builder.EXTRA_SOUND_ENABLED_KEY);
    }

    public static class Builder {
        private static final String EXTRA_START_MUTED_KEY = "startMuted";
        private static final String EXTRA_SOUND_ENABLED_KEY = VungleInterstitial.SOUND_ENABLED_KEY;
        private static final String EXTRA_FLEXVIEW_CLOSE_TIME_KEY = VungleInterstitial.FLEX_VIEW_CLOSE_TIME_KEY;
        private static final String EXTRA_ORDINAL_VIEW_COUNT_KEY = VungleInterstitial.ORDINAL_VIEW_COUNT_KEY;
        private static final String EXTRA_ORIENTATION_KEY = VungleInterstitial.AD_ORIENTATION_KEY;

        @Nullable
        private String userId;
        @Nullable
        private String title;
        @Nullable
        private String body;
        @Nullable
        private String closeButtonText;
        @Nullable
        private String keepWatchingButtonText;

        private boolean isStartMuted = false;
        private int flexViewCloseTimeInSec = 0;
        private int ordinalViewCount = 0;
        private int adOrientation = AdConfig.AUTO_ROTATE;
        private final Map<String, Object> extras = new HashMap<>();

        public Builder withUserId(@NonNull final String userId) {
            this.userId = userId;
            return this;
        }

        public Builder withCancelDialogTitle(@NonNull final String title) {
            this.title = title;
            return this;
        }

        public Builder withCancelDialogBody(@NonNull final String body) {
            this.body = body;
            return this;
        }

        public Builder withCancelDialogCloseButton(@NonNull final String buttonText) {
            this.closeButtonText = buttonText;
            return this;
        }

        public Builder withCancelDialogKeepWatchingButton(@NonNull final String buttonText) {
            this.keepWatchingButtonText = buttonText;
            return this;
        }

        @Deprecated
        public Builder withSoundEnabled(boolean isSoundEnabled) {
            return withStartMuted(!isSoundEnabled);
        }

        public Builder withStartMuted(boolean isStartMuted) {
            this.isStartMuted = isStartMuted;
            extras.put(EXTRA_START_MUTED_KEY, isStartMuted);
            return this;
        }

        public Builder withFlexViewCloseTimeInSec(int flexViewCloseTimeInSec) {
            this.flexViewCloseTimeInSec = flexViewCloseTimeInSec;
            extras.put(EXTRA_FLEXVIEW_CLOSE_TIME_KEY, flexViewCloseTimeInSec);
            return this;
        }

        public Builder withOrdinalViewCount(int ordinalViewCount) {
            this.ordinalViewCount = ordinalViewCount;
            extras.put(EXTRA_ORDINAL_VIEW_COUNT_KEY, ordinalViewCount);
            return this;
        }

        public Builder withAutoRotate(@AdConfig.Orientation int adOrientation) {
            this.adOrientation = adOrientation;
            extras.put(EXTRA_ORIENTATION_KEY, adOrientation);
            return this;
        }

        public VungleMediationConfiguration build() {
            return new VungleMediationConfiguration(this);
        }
    }

    VungleMediationConfiguration(@NonNull final Builder builder) {
        this.userId = builder.userId;
        this.title = builder.title;
        this.body = builder.body;
        this.closeButtonText = builder.closeButtonText;
        this.keepWatchingButtonText = builder.keepWatchingButtonText;
        this.isStartMuted = builder.isStartMuted;
        this.flexViewCloseTimeInSec = builder.flexViewCloseTimeInSec;
        this.ordinalViewCount = builder.ordinalViewCount;
        this.adOrientation = builder.adOrientation;
        this.extras = builder.extras;
    }
}

package com.mopub.mobileads;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.MediationSettings;
import com.vungle.warren.AdConfig;

import java.util.HashMap;
import java.util.Map;

public class VungleMediationConfiguration implements MediationSettings {
    @Nullable
    private final String mUserId;
    @Nullable
    private final String mTitle;
    @Nullable
    private final String mBody;
    @Nullable
    private final String mCloseButtonText;
    @Nullable
    private final String mKeepWatchingButtonText;

    private final boolean mIsStartMuted;
    private final int mOrdinalViewCount;
    private final int mAdOrientation;
    private final Map<String, Object> mExtras;

    @Nullable
    public String getUserId() {
        return mUserId;
    }

    @Nullable
    public String getTitle() {
        return mTitle;
    }

    @Nullable
    public String getBody() {
        return mBody;
    }

    @Nullable
    public String getCloseButtonText() {
        return mCloseButtonText;
    }

    @Nullable
    public String getKeepWatchingButtonText() {
        return mKeepWatchingButtonText;
    }

    public boolean isStartMuted() {
        return mIsStartMuted;
    }

    public int getOrdinalViewCount() {
        return mOrdinalViewCount;
    }

    public int getAdOrientation() {
        return mAdOrientation;
    }

    public Map<String, Object> getExtrasMap() {
        return mExtras;
    }

    static void adConfigWithExtras(@NonNull final AdConfig adConfig,
                                   @NonNull final Map<String, String> extras,
                                   boolean defaultMuteState) {
        adConfig.setMuted(defaultMuteState);

        if (extras.containsKey(Builder.EXTRA_START_MUTED_KEY)) {
            final String isStartMuted = extras.get(Builder.EXTRA_START_MUTED_KEY);
            adConfig.setMuted(Boolean.parseBoolean(isStartMuted));
        }

        final String ordinalViewCount = extras.get(Builder.EXTRA_ORDINAL_VIEW_COUNT_KEY);

        if (!TextUtils.isEmpty(ordinalViewCount)) {
            try {
                adConfig.setOrdinal(Integer.parseInt(ordinalViewCount));

            } catch (NumberFormatException e) {
                // ignore and don't set ordinal view count
            }
        }

        String adOrientation = extras.get(Builder.EXTRA_ORIENTATION_KEY);

        if (TextUtils.isEmpty(adOrientation)) {
            adOrientation = VungleAdapterConfiguration.getWithAutoRotate();
        }

        if (!TextUtils.isEmpty(adOrientation)) {
            try {
                adConfig.setAdOrientation(Integer.parseInt(adOrientation));

            } catch (NumberFormatException e) {
                // ignore and don't set ad orientation
            }
        }
    }

    public static class Builder {
        private static final String EXTRA_START_MUTED_KEY = "startMuted";
        private static final String EXTRA_ORDINAL_VIEW_COUNT_KEY = "vungleOrdinalViewCount";
        private static final String EXTRA_ORIENTATION_KEY = "vungleAdOrientation";

        @Nullable
        private String mUserId;
        @Nullable
        private String mTitle;
        @Nullable
        private String mBody;
        @Nullable
        private String mCloseButtonText;
        @Nullable
        private String mKeepWatchingButtonText;

        private boolean mIsStartMuted = false;
        private int mOrdinalViewCount = 0;
        private int mAdOrientation = AdConfig.AUTO_ROTATE;
        private Map<String, Object> mExtras = new HashMap<>();

        public Builder withUserId(@NonNull final String userId) {
            this.mUserId = userId;
            return this;
        }

        public Builder withCancelDialogTitle(@NonNull final String title) {
            this.mTitle = title;
            return this;
        }

        public Builder withCancelDialogBody(@NonNull final String body) {
            this.mBody = body;
            return this;
        }

        public Builder withCancelDialogCloseButton(@NonNull final String buttonText) {
            this.mCloseButtonText = buttonText;
            return this;
        }

        public Builder withCancelDialogKeepWatchingButton(@NonNull final String buttonText) {
            this.mKeepWatchingButtonText = buttonText;
            return this;
        }

        public Builder withStartMuted(boolean isStartMuted) {
            this.mIsStartMuted = isStartMuted;
            mExtras.put(EXTRA_START_MUTED_KEY, isStartMuted);
            return this;
        }

        public Builder withOrdinalViewCount(int ordinalViewCount) {
            this.mOrdinalViewCount = ordinalViewCount;
            mExtras.put(EXTRA_ORDINAL_VIEW_COUNT_KEY, ordinalViewCount);
            return this;
        }

        public Builder withAutoRotate(@AdConfig.Orientation int adOrientation) {
            this.mAdOrientation = adOrientation;
            mExtras.put(EXTRA_ORIENTATION_KEY, adOrientation);
            return this;
        }

        public VungleMediationConfiguration build() {
            return new VungleMediationConfiguration(this);
        }
    }

    VungleMediationConfiguration(@NonNull final Builder builder) {
        this.mUserId = builder.mUserId;
        this.mTitle = builder.mTitle;
        this.mBody = builder.mBody;
        this.mCloseButtonText = builder.mCloseButtonText;
        this.mKeepWatchingButtonText = builder.mKeepWatchingButtonText;
        this.mIsStartMuted = builder.mIsStartMuted;
        this.mOrdinalViewCount = builder.mOrdinalViewCount;
        this.mAdOrientation = builder.mAdOrientation;
        this.mExtras = builder.mExtras;
    }
}

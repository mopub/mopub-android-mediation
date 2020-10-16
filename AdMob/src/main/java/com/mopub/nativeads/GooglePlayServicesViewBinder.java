package com.mopub.nativeads;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public class GooglePlayServicesViewBinder {
    public final static class Builder {
        private final int layoutId;
        private int mediaLayoutId;
        private int titleId;
        private int textId;
        private int iconImageId;
        private int callToActionId;
        private int privacyInformationIconImageId;
        private int sponsoredTextId;

        @NonNull
        private Map<String, Integer> extras;

        public Builder(final int layoutId) {
            this.layoutId = layoutId;
            this.extras = new HashMap<>();
        }

        @NonNull
        public final Builder mediaLayoutId(final int mediaLayoutId) {
            this.mediaLayoutId = mediaLayoutId;
            return this;
        }

        @NonNull
        public final Builder titleId(final int titleId) {
            this.titleId = titleId;
            return this;
        }

        @NonNull
        public final Builder textId(final int textId) {
            this.textId = textId;
            return this;
        }

        @NonNull
        public final Builder iconImageId(final int iconImageId) {
            this.iconImageId = iconImageId;
            return this;
        }

        @NonNull
        public final Builder callToActionId(final int callToActionId) {
            this.callToActionId = callToActionId;
            return this;
        }

        @NonNull
        public final Builder privacyInformationIconImageId(final int privacyInformationIconImageId) {
            this.privacyInformationIconImageId = privacyInformationIconImageId;
            return this;
        }

        @NonNull
        public final Builder sponsoredTextId(final int sponsoredTextId) {
            this.sponsoredTextId = sponsoredTextId;
            return this;
        }

        @NonNull
        public final Builder addExtras(final Map<String, Integer> resourceIds) {
            this.extras = new HashMap<String, Integer>(resourceIds);
            return this;
        }

        @NonNull
        public final Builder addExtra(final String key, final int resourceId) {
            this.extras.put(key, resourceId);
            return this;
        }

        @NonNull
        public final GooglePlayServicesViewBinder build() {
            return new GooglePlayServicesViewBinder(this);
        }
    }

    final int layoutId;
    final int mediaLayoutId;
    final int titleId;
    final int textId;
    final int callToActionId;
    final int iconImageId;
    final int privacyInformationIconImageId;
    final int sponsoredTextId;
    @NonNull
    final Map<String, Integer> extras;

    private GooglePlayServicesViewBinder(@NonNull final Builder builder) {
        this.layoutId = builder.layoutId;
        this.mediaLayoutId = builder.mediaLayoutId;
        this.titleId = builder.titleId;
        this.textId = builder.textId;
        this.callToActionId = builder.callToActionId;
        this.iconImageId = builder.iconImageId;
        this.privacyInformationIconImageId = builder.privacyInformationIconImageId;
        this.sponsoredTextId = builder.sponsoredTextId;
        this.extras = builder.extras;
    }
}

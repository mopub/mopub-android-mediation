package com.mopub.nativeads;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PangleAdViewBinder {

    public final static class Builder {

        private final int layoutId;

        private int titleId;

        private int decriptionTextId;

        private int callToActionId;

        private int iconImageId;

        private int mediaViewId;

        private int sourceId;

        private int logoViewId;


        @NonNull
        private Map<String, Integer> extras = Collections.emptyMap();

        public Builder(final int layoutId) {
            this.layoutId = layoutId;
            this.extras = new HashMap<String, Integer>();
        }


        @NonNull
        public final Builder logoViewId(final int logoViewId) {
            this.logoViewId = logoViewId;
            return this;
        }

        @NonNull
        public final Builder titleId(final int titleId) {
            this.titleId = titleId;
            return this;
        }


        @NonNull
        public final Builder sourceId(final int sourceId) {
            this.sourceId = sourceId;

            return this;
        }

        @NonNull
        public final Builder mediaViewIdId(final int mediaViewId) {
            this.mediaViewId = mediaViewId;

            return this;
        }


        @NonNull
        public final Builder decriptionTextId(final int textId) {
            this.decriptionTextId = textId;
            return this;
        }

        @NonNull
        public final Builder callToActionId(final int callToActionId) {
            this.callToActionId = callToActionId;
            return this;
        }


        @NonNull
        public final Builder iconImageId(final int iconImageId) {
            this.iconImageId = iconImageId;
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
        public final PangleAdViewBinder build() {
            return new PangleAdViewBinder(this);
        }
    }

    public final int layoutId;
    public final int titleId;
    public final int descriptionTextId;
    public final int callToActionId;
    public final int iconImageId;
    public final int mediaViewId;
    public final int sourceId;
    public final int logoViewId;


    @NonNull
    public final Map<String, Integer> extras;

    private PangleAdViewBinder(@NonNull final Builder builder) {
        this.layoutId = builder.layoutId;
        this.titleId = builder.titleId;
        this.descriptionTextId = builder.decriptionTextId;
        this.callToActionId = builder.callToActionId;
        this.iconImageId = builder.iconImageId;
        this.mediaViewId = builder.mediaViewId;
        this.sourceId = builder.sourceId;
        this.extras = builder.extras;
        this.logoViewId = builder.logoViewId;
    }


}

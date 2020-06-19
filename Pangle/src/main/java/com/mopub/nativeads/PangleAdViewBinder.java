package com.mopub.nativeads;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public class PangleAdViewBinder {

    public final static class Builder {

        private final int mLayoutId;
        private int mTitleId;
        private int mDecriptionTextId;
        private int mCallToActionId;
        private int mIconImageId;
        private int mMediaViewId;
        private int mSourceId;
        private int mLogoViewId;

        @NonNull
        private Map<String, Integer> mExtras;

        public Builder(final int mLayoutId) {
            this.mLayoutId = mLayoutId;
            this.mExtras = new HashMap<String, Integer>();
        }

        @NonNull
        public final Builder logoViewId(final int logoViewId) {
            this.mLogoViewId = logoViewId;
            return this;
        }

        @NonNull
        public final Builder titleId(final int titleId) {
            this.mTitleId = titleId;
            return this;
        }

        @NonNull
        public final Builder sourceId(final int sourceId) {
            this.mSourceId = sourceId;

            return this;
        }

        @NonNull
        public final Builder mediaViewIdId(final int mediaViewId) {
            this.mMediaViewId = mediaViewId;

            return this;
        }


        @NonNull
        public final Builder decriptionTextId(final int textId) {
            this.mDecriptionTextId = textId;
            return this;
        }

        @NonNull
        public final Builder callToActionId(final int callToActionId) {
            this.mCallToActionId = callToActionId;
            return this;
        }

        @NonNull
        public final Builder iconImageId(final int iconImageId) {
            this.mIconImageId = iconImageId;
            return this;
        }

        @NonNull
        public final Builder addExtras(final Map<String, Integer> resourceIds) {
            this.mExtras = new HashMap<String, Integer>(resourceIds);
            return this;
        }

        @NonNull
        public final Builder addExtra(final String key, final int resourceId) {
            this.mExtras.put(key, resourceId);
            return this;
        }

        @NonNull
        public final PangleAdViewBinder build() {
            return new PangleAdViewBinder(this);
        }
    }

    public final int mLayoutId;
    public final int mTitleId;
    public final int mDescriptionTextId;
    public final int mCallToActionId;
    public final int mIconImageId;
    public final int mMediaViewId;
    public final int mSourceId;
    public final int mLogoViewId;

    @NonNull
    public final Map<String, Integer> mExtras;

    private PangleAdViewBinder(@NonNull final Builder builder) {
        this.mLayoutId = builder.mLayoutId;
        this.mTitleId = builder.mTitleId;
        this.mDescriptionTextId = builder.mDecriptionTextId;
        this.mCallToActionId = builder.mCallToActionId;
        this.mIconImageId = builder.mIconImageId;
        this.mMediaViewId = builder.mMediaViewId;
        this.mSourceId = builder.mSourceId;
        this.mExtras = builder.mExtras;
        this.mLogoViewId = builder.mLogoViewId;
    }
}

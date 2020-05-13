package com.mopub.nativeads;


import android.support.annotation.NonNull;

import com.union_test.toutiao.R;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * created by wuzejian on 2020-05-12
 */
public class PangolinViewBinder {
    public static final int IMAGE_MODE_LARGE_IMG = 3;
    public static final int IMAGE_MODE_SMALL_IMG = 2;
    public static final int IMAGE_MODE_GROUP_IMG = 4;
    public static final int IMAGE_MODE_VERTICAL_IMG = 16;
    public static final int IMAGE_MODE_VIDEO = 5;

    public final static class Builder {
        private int layoutType;

        private final int layoutId;

        private int titleId;

        private int decriptionTextId;

        private int callToActionId;

        private int iconImageId;

        private int mainImageId;

        private int smallImageId;

        private int verticalImageId;

        private int mediaViewId;

        private int sourceId;

        private int logoViewId;

        private int dislikeId;

        private int smallImage1Id;

        private int smallImage2Id;

        private int smallImage3Id;


        @NonNull
        private Map<String, Integer> extras = Collections.emptyMap();

        public Builder(final int layoutId) {
            this.layoutId = layoutId;
            this.extras = new HashMap<String, Integer>();
        }

        @NonNull
        public final Builder layoutType(final int layoutType) {
            this.layoutType = layoutType;
            return this;
        }

        @NonNull
        public final Builder dislikeId(final int dislikeId) {
            this.dislikeId = dislikeId;
            return this;
        }

        @NonNull
        public final Builder logoViewId(final int logoViewId) {
            this.logoViewId = logoViewId;
            return this;
        }

        @NonNull
        public final Builder verticalImageId(final int verticalImageId) {
            this.verticalImageId = verticalImageId;
            return this;
        }

        @NonNull
        public final Builder smallImageId(final int smallImageId) {
            this.smallImageId = smallImageId;
            return this;
        }

        @NonNull
        public final Builder titleId(final int titleId) {
            this.titleId = titleId;
            return this;
        }


        @NonNull
        public final Builder groupImage1Id(final int smallImage1Id) {
            this.smallImage1Id = smallImage1Id;
            return this;
        }

        @NonNull
        public final Builder groupImage2Id(final int smallImage2Id) {
            this.smallImage2Id = smallImage2Id;
            return this;
        }


        @NonNull
        public final Builder groupImage3Id(final int smallImage3Id) {
            this.smallImage3Id = smallImage3Id;
            return this;
        }

        @NonNull
        public final Builder mainImageId(final int mediaLayoutId) {
            this.mainImageId = mediaLayoutId;
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
        public final PangolinViewBinder build() {
            return new PangolinViewBinder(this);
        }
    }

    public int layoutType;
    public final int layoutId;
    public final int titleId;
    public final int descriptionTextId;
    public final int callToActionId;
    public final int iconImageId;
    public final int mainImageId;//big image
    public final int smallImageId;
    public final int verticalImageId;
    public final int mediaViewId;
    public final int sourceId;
    public final int groupImage1Id;
    public final int groupImage2Id;
    public final int groupImage3Id;
    public final int dislikeId;
    public final int logoViewId;


    @NonNull
    public final Map<String, Integer> extras;

    private PangolinViewBinder(@NonNull final Builder builder) {
        this.layoutType = builder.layoutType;
        this.layoutId = builder.layoutId;
        this.titleId = builder.titleId;
        this.descriptionTextId = builder.decriptionTextId;
        this.callToActionId = builder.callToActionId;
        this.iconImageId = builder.iconImageId;
        this.mainImageId = builder.mainImageId;
        this.mediaViewId = builder.mediaViewId;
        this.sourceId = builder.sourceId;
        this.extras = builder.extras;
        this.groupImage1Id = builder.smallImage1Id;
        this.groupImage2Id = builder.smallImage2Id;
        this.groupImage3Id = builder.smallImage3Id;
        this.dislikeId = builder.dislikeId;
        this.smallImageId = builder.smallImageId;
        this.verticalImageId = builder.verticalImageId;
        this.logoViewId = builder.logoViewId;
    }



}

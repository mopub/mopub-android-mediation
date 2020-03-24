// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.testing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CallbackDataItem {

    /**
     * The name of the callback
     */
    @NonNull
    private final String mCallbackName;

    /**
     * Optional additional data to show
     */
    @Nullable
    private String mAdditionalData;

    /**
     * Whether or not this callback has been called
     */
    private boolean mCalled;

    CallbackDataItem(@NonNull final String callbackName) {
        mCallbackName = callbackName;
    }

    @NonNull
    String getCallbackName() {
        return mCallbackName;
    }

    @Nullable
    String getAdditionalData() {
        return mAdditionalData;
    }

    boolean isCalled() {
        return mCalled;
    }

    void setAdditionalData(@Nullable final String additionalData) {
        mAdditionalData = additionalData;
    }

    void setCalled() {
        mCalled = true;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final CallbackDataItem that = (CallbackDataItem) o;

        if (mCalled != that.mCalled) return false;
        if (!mCallbackName.equals(that.mCallbackName)) return false;
        return mAdditionalData != null ? mAdditionalData.equals(that.mAdditionalData) : that.mAdditionalData == null;
    }

    @Override
    public int hashCode() {
        int result = mCallbackName.hashCode();
        result = 31 * result + (mAdditionalData != null ? mAdditionalData.hashCode() : 0);
        result = 31 * result + (mCalled ? 1 : 0);
        return result;
    }
}

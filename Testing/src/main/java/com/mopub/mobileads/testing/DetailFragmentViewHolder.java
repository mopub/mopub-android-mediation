// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.testing;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * ViewHolder data object that parses and stores named child Views for sample app DetailFragments,
 * e.g. {@link InterstitialDetailFragment}.
 */
class DetailFragmentViewHolder {
    @NonNull
    final TextView mDescriptionView;
    @NonNull
    final Button mLoadButton;
    @Nullable
    final Button mShowButton;
    @NonNull
    final Button mChangeAdSizeButton;
    @NonNull
    final TextView mAdUnitIdView;
    @NonNull
    final EditText mKeywordsField;
    @NonNull
    final EditText mUserDataKeywordsField;
    @Nullable
    final EditText mCustomDataField;
    @NonNull
    final TextView mAdSizeInfoView;

    /**
     * Internal constructor. Use {@link #fromView(View)} to create instances of this class.
     *
     * @param descriptionView       Displays ad full name, e.g. "MoPub Banner Sample"
     * @param adUnitIdView          Displays adUnitId
     * @param loadButton            Loads an ad. For non-cached ad formats, this will also display the ad
     * @param showButton            Displays an ad. (optional, only defined for interstitial and rewarded ads)
     * @param keywordsField         Application keywords. This is passed in the 'q' query parameter
     * @param userDataKeywordsField User data keyword entry field. This is eventually passed in the
     *                              'user_data_q' query parameter in the ad request
     * @param customDataField       Custom data entry field. Used to include arbitrary data to rewarded
     *                              completion URLs. View visibility defaults to {@link View#GONE}.
     *                              (optional, only defined for rewarded ads)
     */
    private DetailFragmentViewHolder(
            @NonNull final TextView descriptionView,
            @NonNull final TextView adUnitIdView,
            @NonNull final Button loadButton,
            @Nullable final Button showButton,
            @Nullable final Button changeAdSizeButton,
            @NonNull final EditText keywordsField,
            @NonNull final EditText userDataKeywordsField,
            @Nullable final EditText customDataField,
            @NonNull final TextView adSizeInfoView) {
        mDescriptionView = descriptionView;
        mAdUnitIdView = adUnitIdView;
        mLoadButton = loadButton;
        mShowButton = showButton;
        mChangeAdSizeButton = changeAdSizeButton;
        mKeywordsField = keywordsField;
        mUserDataKeywordsField = userDataKeywordsField;
        mCustomDataField = customDataField;
        mAdSizeInfoView = adSizeInfoView;
    }

    static DetailFragmentViewHolder fromView(@NonNull final View view) {
        final TextView descriptionView = view.findViewById(R.id.description);
        final TextView adUnitIdView = view.findViewById(R.id.ad_unit_id);
        final Button loadButton = view.findViewById(R.id.load_button);
        final Button showButton = view.findViewById(R.id.show_button);
        final Button changeAdSizeButton = view.findViewById(R.id.ad_size_button);
        final EditText keywordsField = view.findViewById(R.id.keywords_field);
        final EditText userDataKeywordsField = view.findViewById(R.id.user_data_keywords_field);
        final EditText customDataField = view.findViewById(R.id.custom_data_field);
        final TextView adSizeInfoView = view.findViewById(R.id.ad_size_info);

        return new DetailFragmentViewHolder(descriptionView,
                adUnitIdView,
                loadButton,
                showButton,
                changeAdSizeButton,
                keywordsField,
                userDataKeywordsField,
                customDataField,
                adSizeInfoView);
    }
}

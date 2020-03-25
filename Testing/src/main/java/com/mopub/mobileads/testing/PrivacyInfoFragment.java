// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.testing;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mopub.common.MoPub;
import com.mopub.common.privacy.ConsentData;
import com.mopub.common.privacy.ConsentStatus;
import com.mopub.common.privacy.PersonalInfoManager;

import java.util.ArrayList;
import java.util.List;

import static androidx.recyclerview.widget.DividerItemDecoration.VERTICAL;

public class PrivacyInfoFragment extends Fragment {

    private static class PrivacyItem {
        @NonNull
        final String mTitle;
        @NonNull
        final String mDescription;
        @NonNull
        final String mValue;

        PrivacyItem(@Nullable String title, @Nullable String value, @Nullable String description) {
            mTitle = title == null ? "" : title;
            mValue = value == null ? "" : value;
            mDescription = description == null ? "" : description;
        }
    }

    private static final int TYPE_PRIVACY_INFO = 0;
    private static final int TYPE_DIVIDER = 1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.privacy_info_fragment, container, false);

        final List<PrivacyItem> privacySettings = readPrivacySettings();
        if (privacySettings != null && !privacySettings.isEmpty()) {
            RecyclerView recyclerView = view.findViewById(R.id.privacy_recycler_view);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setAdapter(new PrivacyAdapter(privacySettings));
            recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), VERTICAL));
        }

        final Button closeButton = view.findViewById(R.id.privacy_close_btn);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Activity activity = getActivity();
                if (activity != null) {
                    activity.onBackPressed();
                }
            }
        });
        return view;
    }

    class PrivacyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @NonNull
        final List<PrivacyItem> mPrivacyInfo;

        PrivacyAdapter(@Nullable final List<PrivacyItem> privacyInfo) {
            if (privacyInfo != null) {
                mPrivacyInfo = privacyInfo;
            } else {
                mPrivacyInfo = new ArrayList<>();
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
            @NonNull RecyclerView.ViewHolder viewHolder;
            if (viewType == TYPE_PRIVACY_INFO) {
                final View itemView = inflater.inflate(R.layout.privacy_info_item, viewGroup, false);
                viewHolder = new PrivacyAdapter.ViewHolder(itemView);
                itemView.setTag(viewHolder);
            } else {
                final View itemView = inflater.inflate(R.layout.privacy_info_divider, viewGroup, false);
                viewHolder = new PrivacyAdapter.DividerViewHolder(itemView);
                itemView.setTag(viewHolder);
            }

            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
            final PrivacyItem item = mPrivacyInfo.get(i);
            if (isContentItem(item)) {
                PrivacyAdapter.ViewHolder holder = (PrivacyAdapter.ViewHolder) viewHolder;
                holder.titleTextView.setText(item.mTitle);
                holder.descTextView.setText(item.mDescription);
                holder.valueTextView.setText(item.mValue);
            } else {
                PrivacyAdapter.DividerViewHolder holder = (PrivacyAdapter.DividerViewHolder) viewHolder;
                holder.dividerTextView.setText(item.mDescription);
            }
        }

        @Override
        public int getItemCount() {
            return mPrivacyInfo.size();
        }

        @Override
        public int getItemViewType(int position) {
            final PrivacyItem item = mPrivacyInfo.get(position);
            return isContentItem(item) ? TYPE_PRIVACY_INFO : TYPE_DIVIDER;
        }

        // viewholder
        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView titleTextView;
            final TextView descTextView;
            final TextView valueTextView;

            ViewHolder(View itemView) {
                super(itemView);
                titleTextView = itemView.findViewById(R.id.privacy_title_view);
                descTextView = itemView.findViewById(R.id.privacy_desc_view);
                valueTextView = itemView.findViewById(R.id.privacy_value_view);
            }
        }

        // divider
        class DividerViewHolder extends RecyclerView.ViewHolder {
            final TextView dividerTextView;

            DividerViewHolder(View itemView) {
                super(itemView);
                dividerTextView = itemView.findViewById(R.id.text_divider);
            }
        }
    }

    private static boolean isContentItem(@Nullable PrivacyItem item) {
        return !(item != null && TextUtils.isEmpty(item.mTitle) && TextUtils.isEmpty(item.mValue));
    }

    private static List<PrivacyItem> readPrivacySettings() {
        final PersonalInfoManager manager = MoPub.getPersonalInformationManager();
        if (manager == null) {
            return new ArrayList<>();
        }

        final ConsentData consentData = manager.getConsentData();
        final ConsentStatus status = manager.getPersonalInfoConsentStatus();
        final Boolean gdprApplies = manager.gdprApplies();

        final String gdprAppliesString = (gdprApplies == null || gdprApplies) ? "true" : "false";

        ArrayList<PrivacyItem> list = new ArrayList<>();
        list.add(new PrivacyItem("", "", "Allowable Data Collection"));
        list.add(new PrivacyItem("Is GDPR applicable?", gdprAppliesString, ""));
        list.add(new PrivacyItem("Consent Status", status.getValue(), ""));
        list.add(new PrivacyItem("Can Collect PII", manager.canCollectPersonalInformation() ? "true" : "false", ""));
        list.add(new PrivacyItem("Should Show Consent Dialog", manager.shouldShowConsentDialog() ? "true" : "false", ""));
        list.add(new PrivacyItem("Is Whitelisted", status.equals(ConsentStatus.POTENTIAL_WHITELIST) ? "true" : "false", ""));
        list.add(new PrivacyItem("", "", "Current Versions"));
        list.add(new PrivacyItem("Current Vendor List Url", "", consentData.getCurrentVendorListLink()));
        list.add(new PrivacyItem("Current Vendor List Version", consentData.getCurrentVendorListVersion(), ""));
        list.add(new PrivacyItem("Current Privacy Policy Url", "", consentData.getCurrentPrivacyPolicyLink()));
        list.add(new PrivacyItem("Current Privacy Policy Version", consentData.getCurrentPrivacyPolicyVersion(), ""));
        list.add(new PrivacyItem("Current IAB Vendor List Format", consentData.getCurrentVendorListIabFormat(), ""));
        list.add(new PrivacyItem("", "", "Consented Versions"));
        list.add(new PrivacyItem("Consented Vendor List Version", consentData.getConsentedVendorListVersion(), ""));
        list.add(new PrivacyItem("Consented Privacy Policy Version", consentData.getConsentedPrivacyPolicyVersion(), ""));
        list.add(new PrivacyItem("Consented IAB Vendor List Version", consentData.getConsentedVendorListIabFormat(), ""));

        return list;
    }
}

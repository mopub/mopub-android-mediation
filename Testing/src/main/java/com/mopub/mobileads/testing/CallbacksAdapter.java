// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.testing;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CallbacksAdapter extends RecyclerView.Adapter<CallbacksAdapter.ViewHolder> {

    private static final String TAG = CallbacksAdapter.class.getName();

    private final int mBackgroundLight;
    private final int mBackgroundDark;

    @NonNull
    private List<CallbackDataItem> mCallbacks;

    CallbacksAdapter(@NonNull final Context context) {
        mBackgroundDark = context.getResources().getColor(R.color.listDark);
        mBackgroundLight = context.getResources().getColor(R.color.listLight);
        mCallbacks = new ArrayList<>();
    }

    @NonNull
    @Override
    public CallbacksAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int i) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.callback_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final CallbacksAdapter.ViewHolder viewHolder, final int i) {
        final CallbackDataItem callback;
        try {
            callback = mCallbacks.get(i);
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "Index out of bounds exception when binding CallbacksAdapter.", e);
            return;
        }
        if (callback == null) {
            Log.e("TAG", "Failed to get callback from CallbacksAdapter.");
            return;
        }

        viewHolder.callbackNameTextView.setText(callback.getCallbackName());
        final String additionalData = callback.getAdditionalData();
        if (!TextUtils.isEmpty(additionalData)) {
            viewHolder.additionalDataTextView.setText(additionalData);
        } else {
            viewHolder.additionalDataTextView.setText("");
        }

        if (callback.isCalled()) {
            viewHolder.checkMarkImageView.setVisibility(View.VISIBLE);
            viewHolder.callbackNameTextView.setEnabled(true);
        } else {
            viewHolder.checkMarkImageView.setVisibility(View.INVISIBLE);
            viewHolder.callbackNameTextView.setEnabled(false);
        }

        final int color = (i & 1) == 0 ? mBackgroundDark : mBackgroundLight;
        viewHolder.itemView.setBackgroundColor(color);
    }

    @Override
    public int getItemCount() {
        return mCallbacks.size();
    }

    void generateCallbackList(@NonNull final Class<? extends Enum> callbacksEnumClass) {
        mCallbacks.clear();
        for (Enum callback : callbacksEnumClass.getEnumConstants()) {
            mCallbacks.add(new CallbackDataItem(callback.toString()));
        }
        notifyDataSetChanged();
    }

    void notifyCallbackCalled(@NonNull final String methodName) {
        notifyCallbackCalled(methodName, null);
    }

    void notifyCallbackCalled(@NonNull final String methodName,
                              @Nullable final String additionalData) {
        for (CallbackDataItem item : mCallbacks) {
            if (item.getCallbackName().equals(methodName)) {
                item.setCalled();
                if (!TextUtils.isEmpty(additionalData)) {
                    item.setAdditionalData(additionalData);
                }

                notifyDataSetChanged();
                break;
            }
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        @NonNull
        final TextView callbackNameTextView;
        @NonNull
        final TextView additionalDataTextView;
        @NonNull
        final ImageView checkMarkImageView;
        @NonNull
        final View itemView;

        ViewHolder(@NonNull final View itemView) {
            super(itemView);
            callbackNameTextView = itemView.findViewById(R.id.callback_name_tv);
            additionalDataTextView = itemView.findViewById(R.id.additional_data_tv);
            checkMarkImageView = itemView.findViewById(R.id.checkmark_iv);
            this.itemView = itemView;
        }
    }
}

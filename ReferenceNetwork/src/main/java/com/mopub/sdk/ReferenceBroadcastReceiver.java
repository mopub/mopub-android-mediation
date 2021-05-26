package com.mopub.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.mopub.common.Preconditions;

import static com.mopub.sdk.ReferenceConstants.BROADCAST_IDENTIFIER_KEY;

/**
 * A simple network SDK implementation that works with the reference adapters.
 * <p>
 * INTERNAL USE ONLY. DO NOT REFERENCE OTHERWISE.
 */
public abstract class ReferenceBroadcastReceiver extends BroadcastReceiver {
    private final long mBroadcastIdentifier;
    @Nullable
    private Context mContext;

    @NonNull
    public abstract IntentFilter getIntentFilter();

    public ReferenceBroadcastReceiver(final long broadcastIdentifier) {
        mBroadcastIdentifier = broadcastIdentifier;
    }

    public static void broadcastAction(@NonNull final Context context, final long broadcastIdentifier,
                                       @NonNull final String action) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(action);

        final Intent intent = new Intent(action);
        intent.putExtra(BROADCAST_IDENTIFIER_KEY, broadcastIdentifier);

        LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(intent);
    }

    public void register(@NonNull final BroadcastReceiver broadcastReceiver,
                         @NonNull final Context context) {
        Preconditions.checkNotNull(broadcastReceiver);
        Preconditions.checkNotNull(context);

        mContext = context.getApplicationContext();
        LocalBroadcastManager.getInstance(mContext).registerReceiver(broadcastReceiver,
                getIntentFilter());
    }

    public void unregister(final @Nullable BroadcastReceiver broadcastReceiver) {
        if (mContext != null && broadcastReceiver != null) {
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(broadcastReceiver);
            mContext = null;
        }
    }

    /**
     * Only consume this broadcast if the identifier on the received Intent and this broadcast
     * match up. This allows us to target broadcasts to the ad that spawned them. We include
     * this here because there is no appropriate IntentFilter condition that can recreate this
     * behavior.
     */
    public boolean shouldConsumeBroadcast(@NonNull final Intent intent) {
        Preconditions.checkNotNull(intent);

        final long receivedIdentifier = intent.getLongExtra(BROADCAST_IDENTIFIER_KEY, -1);
        return mBroadcastIdentifier == receivedIdentifier;
    }
}

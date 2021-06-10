package com.mopub.sdk;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;

import com.mopub.common.IntentActions;

/**
 * A simple network SDK implementation that works with the reference adapters.
 * <p>
 * INTERNAL USE ONLY. DO NOT REFERENCE OTHERWISE.
 */
public class ReferenceEventForwardingBroadcastReceiver extends ReferenceBroadcastReceiver {
    private final ReferenceFullScreenAd.ReferenceFullScreenAdListener mListener;
    private static IntentFilter sIntentFilter;

    public ReferenceEventForwardingBroadcastReceiver(final ReferenceFullScreenAd.ReferenceFullScreenAdListener listener,
                                                     final long broadcastIdentifier) {
        super(broadcastIdentifier);
        mListener = listener;
        getIntentFilter();
    }

    @NonNull
    public IntentFilter getIntentFilter() {
        if (sIntentFilter == null) {
            sIntentFilter = new IntentFilter();
            sIntentFilter.addAction(ReferenceIntentActions.ACTION_FULLSCREEN_FAIL);
            sIntentFilter.addAction(ReferenceIntentActions.ACTION_FULLSCREEN_SHOW);
            sIntentFilter.addAction(ReferenceIntentActions.ACTION_FULLSCREEN_DISMISS);
            sIntentFilter.addAction(ReferenceIntentActions.ACTION_FULLSCREEN_CLICK);
            sIntentFilter.addAction(ReferenceIntentActions.ACTION_REWARDED_AD_REWARD);
        }
        return sIntentFilter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mListener == null) {
            return;
        }

        if (!shouldConsumeBroadcast(intent)) {
            return;
        }

        final String action = intent.getAction();
        if (ReferenceIntentActions.ACTION_FULLSCREEN_FAIL.equals(action)) {
            mListener.onFullScreenAdLoadFailed();
        } else if (ReferenceIntentActions.ACTION_FULLSCREEN_SHOW.equals(action)) {
            mListener.onFullScreenAdShown();
        } else if (ReferenceIntentActions.ACTION_FULLSCREEN_DISMISS.equals(action)) {
            mListener.onFullScreenAdDismissed();
            unregister(this);
        } else if (ReferenceIntentActions.ACTION_FULLSCREEN_CLICK.equals(action)) {
            mListener.onFullScreenAdClicked();
        } else if (ReferenceIntentActions.ACTION_REWARDED_AD_REWARD.equals(action)) {
            mListener.onFullScreenAdRewarded();
        }
    }
}

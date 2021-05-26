// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.webkit.WebView;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import com.mopub.common.util.Utils;

import static com.mopub.sdk.ReferenceConstants.BROADCAST_IDENTIFIER_KEY;
import static com.mopub.sdk.ReferenceConstants.REWARDED_KEY;

/**
 * A simple network SDK implementation that works with the reference adapters to request a fullscreen ad.
 * The decision whether to request interstitial or rewarded ads is based on the flag set on the MoPub UI.
 * <p>
 * INTERNAL USE ONLY. DO NOT REFERENCE OTHERWISE.
 */
@SuppressLint("ViewConstructor")
public class ReferenceFullScreenAd extends RelativeLayout {
    private static boolean mIsRewarded;

    private final Context mContext;
    private final ReferenceFullScreenAdListener mListener;

    private String mFullscreenAdMarkup;

    @Nullable
    private WebView mFullscreenAdContainer;

    public interface ReferenceFullScreenAdListener {
        void onFullScreenAdLoaded();

        void onFullScreenAdLoadFailed();

        void onFullScreenAdShowFailed();

        void onFullScreenAdShown();

        void onFullScreenAdClicked();

        void onFullScreenAdDismissed();

        void onFullScreenAdRewarded();
    }

    public ReferenceFullScreenAd(Context context, ReferenceFullScreenAdListener listener) {
        super(context);

        mContext = context;
        mListener = listener;
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void loadAd(boolean isRewarded) {
        mIsRewarded = isRewarded;

        if (!isRewarded) {
            mFullscreenAdMarkup = ReferenceConstants.STATIC_MARKUP;
        } else {
            mFullscreenAdMarkup = ReferenceConstants.VIDEO_MARKUP;
        }

        mFullscreenAdContainer = new WebView(mContext);

        if (mListener != null) {
            mListener.onFullScreenAdLoaded();
        }
    }

    public void destroy() {
        if (mFullscreenAdContainer != null) {
            mFullscreenAdContainer.destroy();
            mFullscreenAdContainer = null;
        }
    }

    public void show() {
        long broadcastIdentifier = Utils.generateUniqueId();
        final ReferenceEventForwardingBroadcastReceiver mBroadcastReceiver = new
                ReferenceEventForwardingBroadcastReceiver(mListener, broadcastIdentifier);

        mBroadcastReceiver.register(mBroadcastReceiver, mContext);

        final Intent fullScreenActivity = new Intent(mContext, ReferenceFullScreenActivity.class);
        fullScreenActivity.putExtra(ReferenceConstants.FULLSCREEN_MARKUP_KEY, mFullscreenAdMarkup);
        fullScreenActivity.putExtra(BROADCAST_IDENTIFIER_KEY, broadcastIdentifier);
        fullScreenActivity.putExtra(REWARDED_KEY, mIsRewarded);

        mContext.startActivity(fullScreenActivity);
    }

    public boolean isAdLoaded() {
        return mFullscreenAdContainer != null;
    }
}

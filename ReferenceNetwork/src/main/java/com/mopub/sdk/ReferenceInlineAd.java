// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.sdk;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.webkit.WebView;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import static com.mopub.common.IntentActions.ACTION_FULLSCREEN_CLICK;
import static com.mopub.sdk.ReferenceConstants.CLICKTHROUGH_URL;

/**
 * A simple network SDK implementation that works with the reference adapters to request and show an
 * inline ad.
 * <p>
 * INTERNAL USE ONLY. DO NOT REFERENCE OTHERWISE.
 */
@SuppressLint("ViewConstructor")
public class ReferenceInlineAd extends RelativeLayout {
    private final Context mContext;
    private ReferenceInlineAdListener mListener;

    @Nullable
    private WebView mInlineAdContainer;

    public interface ReferenceInlineAdListener {
        void onAdLoaded();

        void onAdImpression();

        void onAdFailedToLoad();

        void onAdClicked();
    }

    public ReferenceInlineAd(Context context, ReferenceInlineAdListener listener) {
        super(context);

        mContext = context;
        this.mListener = listener;
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    public void loadAd() {
        final String inlineAdMarkup = ReferenceConstants.STATIC_MARKUP;

        mInlineAdContainer = new WebView(mContext);
        mInlineAdContainer.setLayoutParams(new ActionBar.LayoutParams(320, 50));

        mInlineAdContainer.getSettings().setJavaScriptEnabled(true);
        mInlineAdContainer.loadData(inlineAdMarkup, "text/html", "UTF-8");
        mInlineAdContainer.setOnTouchListener(new View.OnTouchListener() {
            long startTime = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startTime = System.currentTimeMillis();
                }

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    // So that we don't track clicks when swiping on the view
                    if (System.currentTimeMillis() - startTime < ViewConfiguration.getTapTimeout()) {
                        if (mListener != null) {
                            mListener.onAdClicked();
                        }

                        final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(CLICKTHROUGH_URL));
                        browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        mContext.startActivity(browserIntent);
                    }
                }

                return true;
            }
        });

        if (mListener != null) {
            mListener.onAdLoaded();
        }

        // Since an inline ad shows when it's loaded, an impression might be assumed to be tracked at the same time.
        // For simplicity, we simulate an impression callback with no considerations for metrics.
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onAdImpression();
                }
            }
        }, 500);
    }

    public void destroy() {
        if (mInlineAdContainer != null) {
            mInlineAdContainer.destroy();
            mInlineAdContainer = null;
        }

        mListener = null;
    }

    @Nullable
    public WebView getAdView() {
        return mInlineAdContainer;
    }
}

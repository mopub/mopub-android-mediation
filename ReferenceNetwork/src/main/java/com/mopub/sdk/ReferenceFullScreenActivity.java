// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.VideoView;

import com.mopub.mobileads.referencenetwork.R;

import static com.mopub.sdk.ReferenceConstants.BROADCAST_IDENTIFIER_KEY;
import static com.mopub.sdk.ReferenceConstants.CLICKTHROUGH_URL;
import static com.mopub.sdk.ReferenceConstants.FULLSCREEN_MARKUP_KEY;
import static com.mopub.sdk.ReferenceConstants.REWARDED_KEY;
import static com.mopub.sdk.ReferenceIntentActions.ACTION_FULLSCREEN_CLICK;
import static com.mopub.sdk.ReferenceIntentActions.ACTION_FULLSCREEN_DISMISS;
import static com.mopub.sdk.ReferenceIntentActions.ACTION_FULLSCREEN_SHOW;
import static com.mopub.sdk.ReferenceIntentActions.ACTION_REWARDED_AD_REWARD;

/**
 * A simple network SDK implementation that works with the reference adapters to show a fullscreen ad.
 * The decision whether to request interstitial or rewarded ads is based on the flag set on the MoPub UI.
 * <p>
 * INTERNAL USE ONLY. DO NOT REFERENCE OTHERWISE.
 */
public class ReferenceFullScreenActivity extends Activity {
    private long mBroadcastIdentifier;
    private WebView mWebView;
    private VideoView mVideoView;
    private boolean mIsAdRewarded;

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reference_full_screen_activity);

        final String adMarkup = getIntent().getStringExtra(FULLSCREEN_MARKUP_KEY);

        mBroadcastIdentifier = getIntent().getLongExtra(BROADCAST_IDENTIFIER_KEY, -1);
        mIsAdRewarded = getIntent().getBooleanExtra(REWARDED_KEY, false);

        if (mIsAdRewarded) {
            mVideoView = findViewById(R.id.reference_fullscreen_videoview);
            mVideoView.setVisibility(View.VISIBLE);
            mVideoView.setVideoPath(adMarkup);
            mVideoView.start();

            mVideoView.setOnTouchListener(new View.OnTouchListener() {
                long startTime = 0;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        startTime = System.currentTimeMillis();
                    }

                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        // So that we don't track clicks when swiping on the view
                        if (System.currentTimeMillis() - startTime < ViewConfiguration.getTapTimeout()) {
                            // Deliberately terminate ad playback upon clickthrough.
                            // When the user comes back from the landing page, they will also be exiting the ad experience.
                            cleanUp();
                            performClick();
                        }
                    }
                    return true;
                }
            });
        } else {
            mWebView = findViewById(R.id.reference_fullscreen_webview);
            mWebView.getSettings().setJavaScriptEnabled(true);
            mWebView.setVisibility(View.VISIBLE);
            mWebView.setWebChromeClient(new WebChromeClient());
            mWebView.loadData(adMarkup, "text/html", "UTF-8");
            mWebView.setOnTouchListener(new View.OnTouchListener() {
                long startTime = 0;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        startTime = System.currentTimeMillis();
                    }

                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        // So that we don't track clicks when swiping on the view
                        if (System.currentTimeMillis() - startTime < ViewConfiguration.getTapTimeout()) {
                            performClick();
                        }
                    }
                    return true;
                }
            });
        }

        ReferenceBroadcastReceiver.broadcastAction(getApplicationContext(), mBroadcastIdentifier, ACTION_FULLSCREEN_SHOW);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if (mIsAdRewarded) {
            ReferenceBroadcastReceiver.broadcastAction(getApplicationContext(), mBroadcastIdentifier, ACTION_REWARDED_AD_REWARD);
        }

        ReferenceBroadcastReceiver.broadcastAction(getApplicationContext(), mBroadcastIdentifier, ACTION_FULLSCREEN_DISMISS);

        cleanUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mIsAdRewarded) {
            ReferenceBroadcastReceiver.broadcastAction(getApplicationContext(), mBroadcastIdentifier, ACTION_REWARDED_AD_REWARD);
        }

        ReferenceBroadcastReceiver.broadcastAction(getApplicationContext(), mBroadcastIdentifier, ACTION_FULLSCREEN_DISMISS);

        cleanUp();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // If the current creative is a rewarded ad, it's already deliberately terminated when the user
        // clicks through. Now that the user is back, skip the ad Activity.
        if (mIsAdRewarded && mVideoView == null) {
            cleanUp();
            finish();
        }
    }

    private void performClick() {
        ReferenceBroadcastReceiver.broadcastAction(getApplicationContext(), mBroadcastIdentifier, ACTION_FULLSCREEN_CLICK);

        final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(CLICKTHROUGH_URL));
        browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(browserIntent);
    }

    private void cleanUp() {
        if (mWebView != null) {
            mWebView.destroy();
            mWebView = null;
        }

        if (mVideoView != null) {
            mVideoView.stopPlayback();
            mVideoView.clearAnimation();
            mVideoView.suspend();
            mVideoView = null;
        }
    }
}

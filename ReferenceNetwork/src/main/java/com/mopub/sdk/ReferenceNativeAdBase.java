// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import java.util.Arrays;

import static com.mopub.sdk.ReferenceConstants.CLICKTHROUGH_URL;

/**
 * A simple network SDK implementation that works with the reference adapters to request and show an
 * native ad.
 * <p>
 * INTERNAL USE ONLY. DO NOT REFERENCE OTHERWISE.
 */
public class ReferenceNativeAdBase extends Activity {
    private final Context mContext;
    private final String[] mNativeAdViewIds = {"native_main_image", "native_icon_image", "native_title",
            "native_text", "native_sponsored_text_view", "native_cta", "native_privacy_information_icon_image"};

    private ReferenceNativeAdListener mListener;

    public ReferenceNativeAdBase(Context context) {
        mContext = context;
    }

    public interface ReferenceNativeAdListener {
        void onAdLoaded();

        void onAdImpression();

        void onAdFailedToLoad();

        void onAdClicked();
    }

    public void setAdListener(ReferenceNativeAdListener listener) {
        this.mListener = listener;
    }

    public void loadAd() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onAdLoaded();

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mContext instanceof Activity) {
                                final Activity activity = (Activity) mContext;
                                final ViewGroup vg = activity.getWindow().getDecorView().
                                        findViewById(android.R.id.content);

                                // Assuming the ad is immediately shown, attempt to track impression
                                // and attach click listeners 1000ms after the native ad has loaded
                                trackImpressionAndClick(vg);
                            }
                        }
                    }, 1000);
                }
            }
        }, 500);
    }

    public void trackImpressionAndClick(ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            final View view = parent.getChildAt(i);

            if (view instanceof ViewGroup) {
                trackImpressionAndClick((ViewGroup) view);
            } else {
                if (view.getId() != View.NO_ID) {
                    String id = view.getResources().getResourceName(view.getId());
                    id = id.substring(id.indexOf('/') + 1);

                    // A view has an ID matching one of the IDs from the native ad layout
                    if (Arrays.asList(mNativeAdViewIds).contains(id)) {
                        if (id.equals("native_main_image") && view.getVisibility() == View.VISIBLE) {
                            if (mListener != null) {
                                mListener.onAdImpression();
                            }
                        }

                        view.setOnTouchListener(new View.OnTouchListener() {
                            long startTime = 0;

                            @SuppressLint("ClickableViewAccessibility")
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
                    }
                }
            }
        }
    }

    public String getMainImageUrl() {
        return ReferenceConstants.NATIVE_MAIN_IMAGE_URL;
    }

    public String getIconImageUrl() {
        return ReferenceConstants.NATIVE_ICON_IMAGE_URL;
    }

    public String getAdvertiserName() {
        return ReferenceConstants.NATIVE_ADVERTISER_NAME;
    }

    public String getAdTitle() {
        return ReferenceConstants.NATIVE_AD_TITLE;
    }

    public String getAdBodyText() {
        return ReferenceConstants.NATIVE_BODY_TEXT;
    }

    public String getAdCallToAction() {
        return ReferenceConstants.NATIVE_CTA;
    }

    public String getSponsoredTranslation() {
        return ReferenceConstants.NATIVE_SPONSORED;
    }

    public String getAdChoicesLinkUrl() {
        return ReferenceConstants.NATIVE_OPT_OUT_URL;
    }

    public void destroy() {
        mListener = null;
    }
}

package com.mopub.mobileads;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdDislike;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTImage;
import com.bytedance.sdk.openadsdk.TTNativeAd;
import com.mopub.common.logging.MoPubLog;
import com.mopub.nativeads.NativeImageHelper;
import com.union_test.toutiao.R;
import java.util.ArrayList;
import java.util.List;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.mobileads.PangolinAudienceAdAdapterInterstitial.ADAPTER_NAME;

/**
 * created by wuzejian on 2020/5/11
 */
public class PangolinAdNativeInterstitialLoader {
    private static final String TAG = "AdNativeInterLoader";
    private Context mContext;
    private CustomEventInterstitial.CustomEventInterstitialListener mInterstitialListener;
    private boolean mIsLoading;
    private Dialog mAdDialog;
    private TTNativeAd mTTNativeAd;
    private TextView mDislikeView;

    PangolinAdNativeInterstitialLoader(Context context, CustomEventInterstitial.CustomEventInterstitialListener interstitialListener) {
        this.mContext = context;
        this.mInterstitialListener = interstitialListener;
    }


    public void loadAdNativeInterstitial(AdSlot adSlot, TTAdNative ttAdNative) {
        if (ttAdNative == null || mContext == null || adSlot == null || TextUtils.isEmpty(adSlot.getCodeId())) {
            return;
        }
        ttAdNative.loadNativeAd(adSlot, mNativeAdListener);
    }


    TTAdNative.NativeAdListener mNativeAdListener = new TTAdNative.NativeAdListener() {
        @Override
        public void onError(int code, String message) {
            mIsLoading = false;
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, ErrorCode.mapErrorCode(code), message);
            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialFailed(ErrorCode.mapErrorCode(code));
            }
        }

        @Override
        public void onNativeAdLoad(List<TTNativeAd> ads) {
            mIsLoading = false;
            if (ads.get(0) == null) {
                return;
            }
            mTTNativeAd = ads.get(0);
            showAd(mTTNativeAd);
        }
    };


    void showNativeInterstitial(Activity activity) {
        if (activity != null) {
            bindDislikeAction(mTTNativeAd, activity, mDislikeView);
        }
        if (mAdDialog != null && mIsLoading) {
            mAdDialog.show();
        }
    }


    @SuppressWarnings("RedundantCast")
    private void showAd(TTNativeAd ad) {
        mAdDialog = new Dialog(mContext, R.style.native_insert_dialog);
        mAdDialog.setCancelable(false);
        mAdDialog.setContentView(R.layout.native_insert_ad_layout);
        ViewGroup mRootView = mAdDialog.findViewById(R.id.native_insert_ad_root);
        ImageView mAdImageView = (ImageView) mAdDialog.findViewById(R.id.native_insert_ad_img);
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
        int maxWidth = (dm == null) ? 0 : dm.widthPixels;
        int minWidth = maxWidth / 3;
        mAdImageView.setMaxWidth(maxWidth);
        mAdImageView.setMinimumWidth(minWidth);
        //noinspection SuspiciousNameCombination
        mAdImageView.setMinimumHeight(minWidth);
        ImageView mCloseImageView = (ImageView) mAdDialog.findViewById(R.id.native_insert_close_icon_img);
        mDislikeView = mAdDialog.findViewById(R.id.native_insert_dislike_text);

        ImageView iv = mAdDialog.findViewById(R.id.native_insert_ad_logo);
        if (ad.getAdLogo() != null) {
            iv.setImageBitmap(ad.getAdLogo());
        }

        //close for ad
        mCloseImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAdDialog.dismiss();
                if (mInterstitialListener != null) {
                    mInterstitialListener.onInterstitialDismissed();
                }
            }
        });

        //interaction for ad
        bindViewInteraction(ad, mAdImageView, mRootView, mDislikeView);
        //load image
        if (ad.getImageList() != null && !ad.getImageList().isEmpty()) {
            TTImage image = ad.getImageList().get(0);
            if (image != null && image.isValid()) {
                NativeImageHelper.loadImageView(image.getImageUrl(), mAdImageView);
            }
        }
    }

    private void bindViewInteraction(TTNativeAd ad, ImageView mAdImageView, ViewGroup mRootView, TextView mDislikeView) {
        //the views that can be clicked
        List<View> clickViewList = new ArrayList<>();
        clickViewList.add(mAdImageView);

        //The views that can trigger the creative action (like download app)
        List<View> creativeViewList = new ArrayList<>();
        creativeViewList.add(mAdImageView);

        //notice! This involves advertising billing and must be called correctly. convertView must use ViewGroup.
        ad.registerViewForInteraction(mRootView, clickViewList, creativeViewList, mDislikeView, new TTNativeAd.AdInteractionListener() {
            @Override
            public void onAdClicked(View view, TTNativeAd ad) {
                Log.d(TAG, "onAdClicked");
                if (mInterstitialListener != null) {
                    mInterstitialListener.onInterstitialClicked();
                }
            }

            @Override
            public void onAdCreativeClick(View view, TTNativeAd ad) {
                Log.d(TAG, "onAdClicked");
                if (mInterstitialListener != null) {
                    mInterstitialListener.onInterstitialClicked();
                }
            }

            @Override
            public void onAdShow(TTNativeAd ad) {
                Log.d(TAG, "onAdShow");
                if (mInterstitialListener != null) {
                    mInterstitialListener.onInterstitialShown();
                }
            }
        });
    }

    //TODO:get Activity
    private void bindDislikeAction(TTNativeAd ad, Activity activity, TextView dislikeView) {
        final TTAdDislike ttAdDislike = ad.getDislikeDialog(activity);
        if (ttAdDislike != null) {
            ttAdDislike.setDislikeInteractionCallback(new TTAdDislike.DislikeInteractionCallback() {
                @Override
                public void onSelected(int position, String value) {
                    mAdDialog.dismiss();
                }

                @Override
                public void onCancel() {

                }
            });
        }
        dislikeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ttAdDislike != null)
                    ttAdDislike.showDislikeDialog();
            }
        });
    }

    public void destroy() {
        mContext = null;
        mInterstitialListener = null;
        mNativeAdListener = null;
        mAdDialog = null;
        mTTNativeAd = null;
        if (mDislikeView != null) {
            mDislikeView.setOnClickListener(null);
            mDislikeView = null;
        }
    }


}

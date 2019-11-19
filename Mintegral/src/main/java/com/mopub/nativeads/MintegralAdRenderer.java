package com.mopub.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mintegral.adapter.customnative.nativeadapter.MintegralNative;
import com.mintegral.msdk.nativex.view.MTGMediaView;
import com.mintegral.msdk.out.Campaign;
import com.mintegral.msdk.out.OnMTGMediaViewListener;
import com.mintegral.msdk.widget.MTGAdChoice;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.nativeads.BaseNativeAd;
import com.mopub.nativeads.MoPubAdRenderer;
import com.mopub.nativeads.NativeImageHelper;
import com.mopub.nativeads.NativeRendererHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import static android.view.View.VISIBLE;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.ERROR;

/**
 * Created by songjunjun on 16/11/15.
 */

public class MintegralAdRenderer implements MoPubAdRenderer<MintegralNative.MintegralStaticNativeAd> {


    public static final String TAG="renderNativeVideo";
    private final MintegralViewBinder mViewBinder;
    // This is used instead of View.setTag, which causes a memory leak in 2.3
    // and earlier: https://code.google.com/p/android/issues/detail?id=18273
    final WeakHashMap<View, MintegralNativeViewHolder> mViewHolderMap;
    Context mContext;


    public MintegralAdRenderer(final MintegralViewBinder viewBinder){
        mViewBinder = viewBinder;
        mViewHolderMap = new WeakHashMap<View, MintegralNativeViewHolder>();
    }

    @NonNull
    @Override
    public View createAdView(@NonNull Context context, @Nullable ViewGroup parent) {

        final View adView = LayoutInflater
                .from(context)
                .inflate(mViewBinder.layoutId, parent, false);
        final View mainImageView = adView.findViewById(mViewBinder.mainImageId);
        if (mainImageView == null) {
            return adView;
        }

        final ViewGroup.LayoutParams mainImageViewLayoutParams = mainImageView.getLayoutParams();
        final MTGMediaView.LayoutParams mediaViewLayoutParams = new MTGMediaView.LayoutParams(
                mainImageViewLayoutParams.width, mainImageViewLayoutParams.height);

        if (mainImageViewLayoutParams instanceof ViewGroup.MarginLayoutParams) {
            final ViewGroup.MarginLayoutParams marginParams =
                    (ViewGroup.MarginLayoutParams) mainImageViewLayoutParams;
            mediaViewLayoutParams.setMargins(marginParams.leftMargin,
                    marginParams.topMargin,
                    marginParams.rightMargin,
                    marginParams.bottomMargin);
        }

        if (mainImageViewLayoutParams instanceof RelativeLayout.LayoutParams) {
//            final RelativeLayout.LayoutParams mainImageViewRelativeLayoutParams =
//                    (RelativeLayout.LayoutParams) mainImageViewLayoutParams;
//            final int[] rules = mainImageViewRelativeLayoutParams.getRules();
//            for (int i = 0; i < rules.length; i++) {
//                mediaViewLayoutParams.addRule(i, rules[i]);
////                mediaViewLayoutParams
//            }
            mainImageView.setVisibility(View.INVISIBLE);
        } else {
            mainImageView.setVisibility(View.GONE);
        }

        final MTGMediaView mediaView = new MTGMediaView(context);
        ViewGroup mainImageParent = (ViewGroup) mainImageView.getParent();
        int mainImageIndex = mainImageParent.indexOfChild(mainImageView);
        mainImageParent.addView(mediaView, mainImageIndex + 1, mediaViewLayoutParams);
        return adView;

    }

    /**
     * 在该方法中渲染视图
     * @param view
     * @param ad
     */
    @Override
    public void renderAdView(@NonNull View view, @NonNull MintegralNative.MintegralStaticNativeAd ad) {

        MintegralNativeViewHolder mintegralNativeViewHolder = mViewHolderMap.get(view);

        if (mintegralNativeViewHolder == null) {
            mintegralNativeViewHolder = MintegralNativeViewHolder.fromViewBinder(view, mViewBinder);
            mViewHolderMap.put(view, mintegralNativeViewHolder);
        }

        update(mintegralNativeViewHolder, ad);

        setViewVisibility(mintegralNativeViewHolder, VISIBLE);
        ad.prepare(view);
//       TextView titleTv = (TextView) view.findViewById(mViewBinder.titleId);
//        titleTv.setText(ad.getTitle());
//
//        TextView contextTv = (TextView) view.findViewById(mViewBinder.textId);
//        contextTv.setText(ad.getText());
//
//        TextView ctaTV = (TextView) view.findViewById(mViewBinder.callToActionId);
//        if(ctaTV != null){
//            ctaTV.setText(ad.getCallToAction());
//        }
//
//        MVMediaView mediaView = (MVMediaView) view.findViewById(mViewBinder.mvmediaViewId);
//        if (mediaView!=null && ad!=null && ad.campaign!=null){
//            mediaView.setNativeAd(ad.campaign);
//
//            mediaView.setOnMediaViewListener(new OnMVMediaViewListener() {
//                @Override
//                public void onEnterFullscreen() {
//                    Log.i(TAG,"onEnterFullscreen");
//                }
//
//                @Override
//                public void onExitFullscreen() {
//                    Log.i(TAG,"onExitFullscreen");
//                }
//
//                @Override
//                public void onStartRedirection(Campaign campaign, String s) {
//                    Log.i(TAG,"onStartRedirection");
//                }
//
//                @Override
//                public void onFinishRedirection(Campaign campaign, String s) {
//                    Log.i(TAG,"onFinishRedirection");
//                }
//
//                @Override
//                public void onRedirectionFailed(Campaign campaign, String s) {
//                    Log.i(TAG,"onRedirectionFailed");
//                }
//
//                @Override
//                public void onVideoAdClicked(Campaign campaign) {
//                    Log.i(TAG,"onVideoAdClicked");
//                }
//            });
//        }
//
//        ad.prepare(view);
//        if(mContext != null){
//
//            ImageView iconImg = (ImageView) view.findViewById(mViewBinder.iconImageId);
//            Picasso.with(mContext).load(ad.getIconImageUrl()).resize(200,200).into(iconImg);
//        }

    }

    @Override
    public boolean supports(@NonNull BaseNativeAd nativeAd) {
        return nativeAd instanceof MintegralNative.MintegralStaticNativeAd;
    }

    private void update(final MintegralNativeViewHolder mintegralNativeViewHolder,
                        final MintegralNative.MintegralStaticNativeAd  nativeAd) {

        final ImageView mainImageView = mintegralNativeViewHolder.getMainImageView();
        NativeRendererHelper.addTextView(mintegralNativeViewHolder.getTitleView(),
                nativeAd.getTitle());
        NativeRendererHelper.addTextView(mintegralNativeViewHolder.getTextView(), nativeAd.getText());
        NativeRendererHelper.addTextView(mintegralNativeViewHolder.getCallToActionView(),
                nativeAd.getCallToAction());
        NativeImageHelper.loadImageView(nativeAd.getMainImageUrl(), mainImageView);
        NativeImageHelper.loadImageView(nativeAd.getIconImageUrl(),
                mintegralNativeViewHolder.getIconImageView());
        NativeRendererHelper.addPrivacyInformationIcon(
                mintegralNativeViewHolder.getPrivacyInformationIconImageView(),
                nativeAd.getPrivacyInformationIconImageUrl(),
                nativeAd.getPrivacyInformationIconClickThroughUrl());
        final MTGMediaView mediaView = mintegralNativeViewHolder.getMediaView();
        if (mediaView != null && mainImageView != null) {
            mediaView.setNativeAd(nativeAd.mCampaign);
            mediaView.setVisibility(View.VISIBLE);
            mediaView.setOnMediaViewListener(new OnMTGMediaViewListener() {
                @Override
                public void onEnterFullscreen() {

                }

                @Override
                public void onExitFullscreen() {

                }

                @Override
                public void onStartRedirection(Campaign campaign, String s) {

                }

                @Override
                public void onFinishRedirection(Campaign campaign, String s) {

                }

                @Override
                public void onRedirectionFailed(Campaign campaign, String s) {

                }

                @Override
                public void onVideoAdClicked(Campaign campaign) {
                    Log.e(TAG,"MV MEDIAVIEW CLICK");
                    nativeAd.notifyAdClicked();
                }

                @Override
                public void onVideoStart() {

                }
            });
            if (mintegralNativeViewHolder.isMainImageViewInRelativeView()) {
                mainImageView.setVisibility(View.INVISIBLE);
            } else {
                mainImageView.setVisibility(View.GONE);
            }

        }
        Campaign campaign = nativeAd.mCampaign;
        MTGAdChoice adChoice = mintegralNativeViewHolder.getAdChoice();
        try {
            RelativeLayout view = (RelativeLayout) mintegralNativeViewHolder.getTitleView().getParent();
            RelativeLayout.LayoutParams Params = (RelativeLayout.LayoutParams) view.getLayoutParams();
            Params.height = campaign.getAdchoiceSizeHeight();
            Params.width = campaign.getAdchoiceSizeWidth();
            adChoice.setLayoutParams(Params);
        }catch (Throwable e){
            Log.e(TAG, "adchoice update params: "+e.getMessage());
        }
        adChoice.setCampaign(campaign);

    }

    private static void setViewVisibility(final MintegralNativeViewHolder mintegralNativeViewHolder,
                                          final int visibility) {
        if (mintegralNativeViewHolder.getMainView() != null) {
            mintegralNativeViewHolder.getMainView().setVisibility(visibility);
        }
    }
    static class MintegralNativeViewHolder {
//        private final StaticNativeViewHolder mStaticNativeViewHolder;
        private MTGMediaView mMediaView;
        private  boolean isMainImageViewInRelativeView;
        private MTGAdChoice adChoice;

        @Nullable View mainView;
        @Nullable TextView titleView;
        @Nullable TextView textView;
        @Nullable TextView callToActionView;
        @Nullable ImageView mainImageView;
        @Nullable ImageView iconImageView;
        @Nullable ImageView privacyInformationIconImageView;

        @VisibleForTesting
        static final MintegralNativeViewHolder EMPTY_VIEW_HOLDER = new MintegralNativeViewHolder();

//        // Use fromViewBinder instead of a constructor
//        private MintegralNativeViewHolder(final StaticNativeViewHolder staticNativeViewHolder,
//                                         final MTGMediaView mediaView, final boolean mainImageViewInRelativeView) {
//            mStaticNativeViewHolder = staticNativeViewHolder;
//            mMediaView = mediaView;
//            isMainImageViewInRelativeView = mainImageViewInRelativeView;
//        }

        // Use fromViewBinder instead of a constructor
        private MintegralNativeViewHolder() {}

        static MintegralNativeViewHolder fromViewBinder(final View view,
                                                       final MintegralViewBinder viewBinder) {
            final MintegralNativeViewHolder staticNativeViewHolder = new MintegralNativeViewHolder();
            staticNativeViewHolder.mainView = view;
            try {
                staticNativeViewHolder.titleView = (TextView) view.findViewById(viewBinder.titleId);
                staticNativeViewHolder.textView = (TextView) view.findViewById(viewBinder.textId);
                staticNativeViewHolder.callToActionView =
                        (TextView) view.findViewById(viewBinder.callToActionId);
                staticNativeViewHolder.mainImageView =
                        (ImageView) view.findViewById(viewBinder.mainImageId);
                staticNativeViewHolder.iconImageView =
                        (ImageView) view.findViewById(viewBinder.iconImageId);
                staticNativeViewHolder.privacyInformationIconImageView =
                        (ImageView) view.findViewById(viewBinder.privacyInformationIconImageId);

                final View mainImageView = staticNativeViewHolder.mainImageView;
                boolean mainImageViewInRelativeView = false;
                MTGMediaView mediaView = null;
                if (mainImageView != null) {
                    final ViewGroup mainImageParent = (ViewGroup) mainImageView.getParent();
                    if (mainImageParent instanceof RelativeLayout) {
                        mainImageViewInRelativeView = true;
                    }
                    final int mainImageIndex = mainImageParent.indexOfChild(mainImageView);
                    final View viewAfterImageView = mainImageParent.getChildAt(mainImageIndex + 1);
                    if (viewAfterImageView instanceof MTGMediaView) {
                        mediaView = (MTGMediaView) viewAfterImageView;
                    }
                }
                staticNativeViewHolder.mMediaView = mediaView;
                staticNativeViewHolder.isMainImageViewInRelativeView = mainImageViewInRelativeView;
                staticNativeViewHolder.adChoice = view.findViewById(viewBinder.adchoiceId);
                return staticNativeViewHolder;
            } catch (ClassCastException exception) {
                MoPubLog.log(ERROR, "Could not cast from id in ViewBinder to expected View type", exception);
                return EMPTY_VIEW_HOLDER;
            }
        }

        public View getMainView() {
            return mainView;
        }

        public TextView getTitleView() {
            return titleView;
        }

        public TextView getTextView() {
            return textView;
        }

        public TextView getCallToActionView() {
            return callToActionView;
        }

        public ImageView getMainImageView() {
            return mainImageView;
        }

        public ImageView getIconImageView() {
            return iconImageView;
        }

        public ImageView getPrivacyInformationIconImageView() {
            return privacyInformationIconImageView;
        }

        public MTGMediaView getMediaView() {
            return mMediaView;
        }

        public boolean isMainImageViewInRelativeView() {
            return isMainImageViewInRelativeView;
        }

        public MTGAdChoice getAdChoice(){
            return adChoice;
        }
    }


    public static class MintegralViewBinder{
        public final static class Builder {
            private final int layoutId;
            private int titleId;
            private int textId;
            private int callToActionId;
            private int mainImageId;
            private int iconImageId;
            private int privacyInformationIconImageId;

            private int adchoiceId;
            @NonNull private Map<String, Integer> extras = Collections.emptyMap();

            public Builder(final int layoutId) {
                this.layoutId = layoutId;
                this.extras = new HashMap<String, Integer>();
            }

            @NonNull
            public final Builder titleId(final int titleId) {
                this.titleId = titleId;
                return this;
            }

            @NonNull
            public final Builder textId(final int textId) {
                this.textId = textId;
                return this;
            }

            @NonNull
            public final Builder callToActionId(final int callToActionId) {
                this.callToActionId = callToActionId;
                return this;
            }

            @NonNull
            public final Builder mainImageId(final int mediaLayoutId) {
                this.mainImageId = mediaLayoutId;
                return this;
            }

            @NonNull
            public final Builder iconImageId(final int iconImageId) {
                this.iconImageId = iconImageId;
                return this;
            }

            @NonNull
            public final Builder privacyInformationIconImageId(final int privacyInformationIconImageId) {
                this.privacyInformationIconImageId = privacyInformationIconImageId;
                return this;
            }

            @NonNull
            public final Builder adchoiceId(final int adchoiceId) {
                this.adchoiceId = adchoiceId;
                return this;
            }

            @NonNull
            public final Builder addExtras(final Map<String, Integer> resourceIds) {
                this.extras = new HashMap<String, Integer>(resourceIds);
                return this;
            }

            @NonNull
            public final Builder addExtra(final String key, final int resourceId) {
                this.extras.put(key, resourceId);
                return this;
            }

            @NonNull
            public final MintegralViewBinder build() {
                return new MintegralViewBinder(this);
            }
        }

        final int layoutId;
        final int titleId;
        final int textId;
        final int callToActionId;
        final int mainImageId;
        final int iconImageId;
        final int privacyInformationIconImageId;
        final int adchoiceId;
        @NonNull final Map<String, Integer> extras;

        private MintegralViewBinder(@NonNull final Builder builder) {
            this.layoutId = builder.layoutId;
            this.titleId = builder.titleId;
            this.textId = builder.textId;
            this.callToActionId = builder.callToActionId;
            this.mainImageId = builder.mainImageId;
            this.iconImageId = builder.iconImageId;
            this.privacyInformationIconImageId = builder.privacyInformationIconImageId;
            this.adchoiceId = builder.adchoiceId;
            this.extras = builder.extras;
        }
    }
}

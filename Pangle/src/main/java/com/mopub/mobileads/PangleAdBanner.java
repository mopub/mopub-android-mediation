package com.mopub.mobileads;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdDislike;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTImage;
import com.bytedance.sdk.openadsdk.TTNativeAd;
import com.bytedance.sdk.openadsdk.TTNativeExpressAd;
import com.mopub.common.DataKeys;
import com.mopub.common.logging.MoPubLog;
import com.mopub.nativeads.NativeImageHelper;
import com.union_test.toutiao.R;
import com.union_test.toutiao.utils.TToast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;

/**
 * created by wuzejian on 2019-11-29
 */
public class PangleAdBanner extends CustomEventBanner {

    protected static final String ADAPTER_NAME = "PangleAdBanner";

    /**
     * Key to obtain Pangolin ad unit ID from the extras provided by MoPub.
     */
    public static final String KEY_EXTRA_AD_UNIT_ID = "adunit";


    /**
     * pangolin network banner ad unit ID.
     */
    private String mCodeId = null;

    public final static String GDPR_RESULT = "gdpr_result";
    public final static String COPPA_VALUE = "coppa_value";
    public final static String AD_BANNER_WIDTH = "ad_banner_width";
    public final static String AD_BANNER_HEIGHT = "ad_banner_height";

    private PangleAdapterConfiguration mPangleAdapterConfiguration;

    private Context mContext;

    private PangleAdBannerExpressLoader mAdExpressBannerLoader;
    private PangleAdBannerNativeLoader mAdNativeBannerLoader;

    private float bannerWidth;
    private float bannerHeight;


    public PangleAdBanner() {
        mPangleAdapterConfiguration = new PangleAdapterConfiguration();
    }


    @Override
    protected void loadBanner(Context context, CustomEventBannerListener customEventBannerListener, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        mContext = context;
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "loadBanner mContext =" + mContext);
        /** cache data from server */
        mPangleAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);

        /** init pangolin SDK */
        TTAdManager mTTAdManager = PangleSharedUtil.get();

        String adm = null;

        boolean isExpressAd = false;

        /** set GDPR */
        if (localExtras != null && !localExtras.isEmpty()) {
            if (localExtras.containsKey(GDPR_RESULT)) {
                int gdpr = (int) localExtras.get(GDPR_RESULT);
                mTTAdManager.setGdpr(gdpr);
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "banner receive gdpr=" + gdpr);
            }

            if (localExtras.containsKey(KEY_EXTRA_AD_UNIT_ID)) {
                mCodeId = (String) localExtras.get(KEY_EXTRA_AD_UNIT_ID);
            }

            isExpressAd = mTTAdManager.getAdRequetTypeByRit(mCodeId) == TTAdConstant.REQUEST_AD_TYPE_EXPRESS;


            if (isExpressAd) {
                float[] bannerAdSizeAdapterSafely = PangleSharedUtil.getBannerAdSizeAdapterSafely(localExtras, AD_BANNER_WIDTH, AD_BANNER_HEIGHT);
                bannerWidth = bannerAdSizeAdapterSafely[0];
                bannerHeight = bannerAdSizeAdapterSafely[1];
            } else {
                /** obtain extra parameters */
                if (localExtras.containsKey(AD_BANNER_WIDTH)) {
                    bannerWidth = Float.valueOf(String.valueOf(localExtras.get(AD_BANNER_WIDTH)));
                }

                if (localExtras.containsKey(AD_BANNER_HEIGHT)) {
                    bannerHeight = Float.valueOf(String.valueOf(localExtras.get(AD_BANNER_HEIGHT)));
                }

            }
            checkSize(isExpressAd);

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "bannerWidth =" + bannerWidth + "，bannerHeight=" + bannerHeight + ",mCodeId=" + mCodeId);

        }

        /** obtain adunit from server by mopub */
        if (serverExtras != null) {
            String adunit = serverExtras.get(KEY_EXTRA_AD_UNIT_ID);
            if (!TextUtils.isEmpty(adunit)) {
                this.mCodeId = adunit;
            }
            adm = serverExtras.get(DataKeys.ADM_KEY);
        }

        MoPubLog.log(CUSTOM, ADAPTER_NAME, "loadBanner method mCodeId：" + mCodeId);

        //create request parameters for AdSlot
        AdSlot.Builder adSlotBuilder = new AdSlot.Builder()
                .setCodeId(mCodeId)
                .setSupportDeepLink(true)
                .setAdCount(1)
                .setImageAcceptedSize((int) bannerWidth, (int) bannerHeight)
                .withBid(adm);

        if (isExpressAd) {
            adSlotBuilder.setExpressViewAcceptedSize(bannerWidth, bannerHeight);
        } else {
            adSlotBuilder.setNativeAdType(AdSlot.TYPE_BANNER);
        }

        /** request ad express banner */
        if (isExpressAd) {
            mAdExpressBannerLoader = new PangleAdBannerExpressLoader(mContext, customEventBannerListener);
            mAdExpressBannerLoader.loadAdExpressBanner(adSlotBuilder.build(), mTTAdManager.createAdNative(mContext));
        } else {
            mAdNativeBannerLoader = new PangleAdBannerNativeLoader(mContext, bannerWidth, bannerHeight, customEventBannerListener);
            mAdNativeBannerLoader.loadAdNativeBanner(adSlotBuilder.build(), mTTAdManager.createAdNative(mContext));
        }

    }

    private void checkSize(boolean isExpressAd) {
        if (isExpressAd) {
            if (bannerWidth <= 0) {
                bannerWidth = 320;
                bannerHeight = 0;
            }
            if (bannerHeight < 0) {
                bannerHeight = 0;
            }
        } else {
            /** default value */
            if (bannerWidth <= 0 || bannerHeight <= 0) {
                bannerWidth = 320;
                bannerHeight = 50;
            }
        }
    }


    @Override
    protected void onInvalidate() {
        if (mAdExpressBannerLoader != null) {
            mAdExpressBannerLoader.destroy();
            mAdExpressBannerLoader = null;
        }

        if (mAdNativeBannerLoader != null) {
            mAdNativeBannerLoader.destroy();
            mAdNativeBannerLoader = null;
        }
    }


    /**
     * created by wuzejian on 2020/5/11
     * pangle native ad banner
     */
    public static class PangleAdBannerNativeLoader {

        private Context mContext;

        private CustomEventBanner.CustomEventBannerListener mCustomEventBannerListener;

        private View mBannerView;


        private float mBannerWidth;
        private float mBannerHeight;

        PangleAdBannerNativeLoader(Context context, float bannerWidth, float bannerHeight, CustomEventBanner.CustomEventBannerListener customEventBannerListener) {
            this.mCustomEventBannerListener = customEventBannerListener;
            this.mContext = context;
            this.mBannerWidth = bannerWidth;
            this.mBannerHeight = bannerHeight;
        }


        void loadAdNativeBanner(AdSlot adSlot, TTAdNative ttAdNative) {
            if (mContext == null || adSlot == null || ttAdNative == null || TextUtils.isEmpty(adSlot.getCodeId()))
                return;
            ttAdNative.loadNativeAd(adSlot, mNativeAdListener);

        }

        private TTAdNative.NativeAdListener mNativeAdListener = new TTAdNative.NativeAdListener() {
            @Override
            public void onError(int code, String message) {
                if (mCustomEventBannerListener != null) {
                    mCustomEventBannerListener.onBannerFailed(PangleSharedUtil.mapErrorCode(code));
                }
                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                        MoPubErrorCode.NETWORK_NO_FILL);
            }

            @Override
            public void onNativeAdLoad(List<TTNativeAd> ads) {
                if (ads.get(0) == null) {
                    return;
                }
                View bannerView = null;
                if (mBannerWidth / mBannerHeight < 4) {
                    bannerView = LayoutInflater.from(mContext).inflate(R.layout.tt_pangle_ad_banner_layout_600_300, null, false);
                } else {
                    bannerView = LayoutInflater.from(mContext).inflate(R.layout.tt_pangle_ad_banner_layout_600_150, null, false);
                }

                if (bannerView == null) {
                    return;
                }

                mBannerView = bannerView;

                //bind ad data and interaction
                setAdData(bannerView, ads.get(0));
                if (mCustomEventBannerListener != null) {
                    //load success add view to mMoPubView
                    mCustomEventBannerListener.onBannerLoaded(bannerView);
                }

            }
        };


        private void setAdData(View nativeView, TTNativeAd nativeAd) {

            TextView titleTv = nativeView.findViewById(R.id.tt_pangle_ad_title);
            if (titleTv != null) {
                titleTv.setText(nativeAd.getTitle());
            }

            TextView descriptionTv = nativeView.findViewById(R.id.tt_pangle_ad_content);
            if (descriptionTv != null) {
                descriptionTv.setText(nativeAd.getDescription());
            }

            Button creativeButton = nativeView.findViewById(R.id.tt_pangle_ad_btn);
            ViewGroup mainImgLayout = nativeView.findViewById(R.id.tt_pangle_ad_image_layout);
            ImageView mainImg = nativeView.findViewById(R.id.tt_pangle_ad_main_img);
            ViewGroup adContentLayout = nativeView.findViewById(R.id.tt_pangle_ad_content_layout);
            ImageView iconImage = nativeView.findViewById(R.id.tt_pangle_ad_icon_adapter);

            /** set main image size */
            ViewGroup.LayoutParams mainImgLp = null;
            ViewGroup.LayoutParams mainImgLayoutLp = null;
            ViewGroup.LayoutParams ctLp = null;
            ViewGroup.LayoutParams btnLp = null;
            ViewGroup.LayoutParams iconLp = null;

            if (mainImgLayout != null) {
                mainImgLayoutLp = mainImgLayout.getLayoutParams();
            }

            if (mainImg != null) {
                mainImgLp = mainImg.getLayoutParams();
            }

            if (adContentLayout != null) {
                ctLp = adContentLayout.getLayoutParams();
            }

            if (creativeButton != null) {
                btnLp = creativeButton.getLayoutParams();
            }

            if (iconImage != null) {
                iconLp = iconImage.getLayoutParams();
            }


            /** Dynamically set the width and height of mainImgLayout and other view */
            if (nativeAd.getImageList() != null && !nativeAd.getImageList().isEmpty()) {
                TTImage image = nativeAd.getImageList().get(0);
                if (image != null && image.isValid()) {
                    if (mainImg != null) {
                        NativeImageHelper.loadImageView(image.getImageUrl(), mainImg);
                    }
                    int mainImgWidth = image.getWidth();
                    int mainImgHeight = image.getHeight();

                    int calculateImgWidth = 0;

                    if (mBannerWidth / mBannerHeight >= 4) {
                        TextView logo = nativeView.findViewById(R.id.tt_pangle_ad_logo);

                        /** Dynamically adapter logo size */
                        if (logo != null) {
                            ViewGroup.LayoutParams logoLy = logo.getLayoutParams();
                            logoLy.width = PangleSharedUtil.dp2px(mContext, 16);
                            logoLy.height = PangleSharedUtil.dp2px(mContext, 6);
                            logo.setLayoutParams(logoLy);
                            logo.setPadding(2, 0, 0, 0);
                            logo.setTextSize(4);
                            Drawable img = ContextCompat.getDrawable(mContext, R.drawable.tt_ad_logo);
                            if (img != null) {
                                img.setBounds(0, 0, PangleSharedUtil.dp2px(mContext, 6), PangleSharedUtil.dp2px(mContext, 5));
                                logo.setCompoundDrawables(img, null, null, null);
                            }
                        }

                        /** Calculate the actual width of the picture */
                        calculateImgWidth = (int) ((mBannerHeight * mainImgWidth) / mainImgHeight);
                        if (mBannerWidth > PangleSharedUtil.getScreenWidth(mContext)) {
                            mBannerWidth = PangleSharedUtil.getScreenWidth(mContext);
                        }

                        /** Dynamically set the width and height of mainImgLayout */
                        if (mainImgLayoutLp != null) {
                            mainImgLayoutLp.height = (int) mBannerHeight;
                            mainImgLayoutLp.width = calculateImgWidth;
                            mainImgLayout.setLayoutParams(mainImgLayoutLp);
                        }

                        /** Dynamically set the width and height of adContentLayout */
                        int ctWidth = (int) (mBannerWidth - calculateImgWidth);
                        if (ctLp != null) {
                            ctLp.height = (int) mBannerHeight;
                            ctLp.width = ctWidth;
                            adContentLayout.setLayoutParams(ctLp);
                        }

                        /** 600 : 90 - 640 :100 */
                        if (mBannerWidth / mBannerHeight >= 600f / 90f || mBannerWidth / mBannerHeight >= 600f / 100f) {
                            float scale = mBannerHeight / 90f;

                            if (titleTv != null) {
                                titleTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 8 * scale);
                            }

                            if (descriptionTv != null) {
                                descriptionTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 6 * scale);

                                if (descriptionTv.getLayoutParams() != null) {
                                    ViewGroup.MarginLayoutParams vmlp = (ViewGroup.MarginLayoutParams) descriptionTv.getLayoutParams();
                                    vmlp.topMargin = PangleSharedUtil.dp2px(mContext, 2);
                                    descriptionTv.setLayoutParams(vmlp);
                                }
                            }
                        } else if (mBannerWidth / mBannerHeight >= 600f / 150f) {
                            float scale = mBannerHeight / 150f;
                            if (titleTv != null) {
                                titleTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10 * scale);
                            }

                            if (descriptionTv != null) {
                                descriptionTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9 * scale);
                            }
                        }

                    } else {
                        int decrptionTvLyHeight = 0;
                        ViewGroup.LayoutParams decrptionTvLy = null;
                        if (descriptionTv != null) {
                            decrptionTvLy = descriptionTv.getLayoutParams();
                            decrptionTvLyHeight = decrptionTvLy.height;
                        }
                        int marginHeight = PangleSharedUtil.dp2px(mContext, 10);
                        /** Calculate the actual height of the main picture */
                        int calculateMainImgHeight = (int) (mBannerHeight - decrptionTvLyHeight - marginHeight);
                        calculateImgWidth = (calculateMainImgHeight * mainImgWidth) / mainImgHeight;

                        /** Calculate the width of leftBar */
                        int leftbar = (int) (mBannerWidth / 3);

                        if (mBannerWidth - calculateImgWidth > leftbar) {
                            leftbar = (int) (mBannerWidth - calculateImgWidth);
                        }

                        /** set mainImg size */
                        if (mainImgLp != null && mainImg != null) {
                            mainImgLp.height = calculateMainImgHeight;
                            mainImgLp.width = (int) (mBannerWidth - leftbar - PangleSharedUtil.dp2px(mContext, 12));
                            mainImg.setLayoutParams(mainImgLp);
                        }

                        /** set mainImgLayout size */
                        if (mainImgLayoutLp != null) {
                            mainImgLayoutLp.height = calculateMainImgHeight;
                            mainImgLayoutLp.width = (int) (mBannerWidth - leftbar);
                            mainImgLayout.setLayoutParams(mainImgLayoutLp);
                        }


                        /** set adContentLayout size */
                        if (ctLp != null) {
                            ctLp.height = (int) mBannerHeight;
                            ctLp.width = leftbar;
                            adContentLayout.setLayoutParams(ctLp);
                        }

                        if (mBannerWidth / mBannerHeight <= 600f / 500f) {// 1.2f
                            float scale = mBannerHeight / 500f;
                            if (iconLp != null) {
                                ViewGroup.MarginLayoutParams vmlp = (ViewGroup.MarginLayoutParams) iconLp;
                                vmlp.topMargin = (int) (scale * PangleSharedUtil.dp2px(mContext, 5));
                                iconImage.setLayoutParams(vmlp);
                            }

                            /** set mainImg size */
                            if (mainImgLp != null && mainImg != null) {
                                mainImgLp.height = (int) (mBannerHeight - decrptionTvLyHeight - PangleSharedUtil.dp2px(mContext, 43));
                                mainImg.setLayoutParams(mainImgLp);
                            }

                            if (mainImgLayoutLp != null) {
                                ViewGroup.MarginLayoutParams vmlp = (ViewGroup.MarginLayoutParams) mainImgLayoutLp;
                                if (mainImgLp != null) {
                                    vmlp.height = mainImgLp.height;
                                }
                                mainImgLayout.setLayoutParams(vmlp);
                            }

                            if (ctLp != null) {
                                if (mainImgLp != null) {
                                    ctLp.height = mainImgLp.height;
                                }
                                adContentLayout.setLayoutParams(ctLp);
                            }

                            if (descriptionTv != null) {
                                ViewGroup.MarginLayoutParams vmlp = (ViewGroup.MarginLayoutParams) descriptionTv.getLayoutParams();
                                vmlp.topMargin = (int) (scale * PangleSharedUtil.dp2px(mContext, 30));
                                descriptionTv.setLayoutParams(vmlp);
                            }


                            if (btnLp != null) {
                                ViewGroup.MarginLayoutParams vmlp = (ViewGroup.MarginLayoutParams) btnLp;
                                vmlp.leftMargin = (int) (scale * PangleSharedUtil.dp2px(mContext, 10));
                                vmlp.rightMargin = (int) (scale * PangleSharedUtil.dp2px(mContext, 10));
                                creativeButton.setLayoutParams(vmlp);
                            }

                        } else if (mBannerWidth / mBannerHeight <= 640f / 400f) {// 1.6f
                            float scale = mBannerHeight / 400f;

                            if (titleTv != null) {
                                titleTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10 * scale);
                            }


                            if (iconLp != null) {
                                ViewGroup.MarginLayoutParams vmlp = (ViewGroup.MarginLayoutParams) iconLp;
                                vmlp.topMargin = (int) (scale * PangleSharedUtil.dp2px(mContext, 5));
                                iconImage.setLayoutParams(vmlp);
                            }

                            if (descriptionTv != null) {
                                ViewGroup.MarginLayoutParams vmlp = (ViewGroup.MarginLayoutParams) descriptionTv.getLayoutParams();
                                vmlp.topMargin = (int) (scale * PangleSharedUtil.dp2px(mContext, 10));
                                descriptionTv.setLayoutParams(vmlp);
                            }


                            if (mainImgLp != null && mainImg != null) {
                                mainImgLp.height = (int) (mBannerHeight - decrptionTvLyHeight - PangleSharedUtil.dp2px(mContext, 25));
                                mainImg.setLayoutParams(mainImgLp);
                            }

                            if (mainImgLayoutLp != null) {
                                ViewGroup.MarginLayoutParams vmlp = (ViewGroup.MarginLayoutParams) mainImgLayoutLp;
                                if (mainImgLp != null) {
                                    vmlp.height = mainImgLp.height;
                                }
                                vmlp.topMargin = (int) (scale * PangleSharedUtil.dp2px(mContext, 5));
                                mainImgLayout.setLayoutParams(vmlp);
                            }

                            if (ctLp != null) {
                                if (mainImgLp != null) {
                                    ctLp.height = mainImgLp.height;
                                }
                                adContentLayout.setLayoutParams(ctLp);
                            }


                            if (btnLp != null) {
                                ViewGroup.MarginLayoutParams vmlp = (ViewGroup.MarginLayoutParams) btnLp;
                                vmlp.leftMargin = (int) (scale * PangleSharedUtil.dp2px(mContext, 10));
                                vmlp.rightMargin = (int) (scale * PangleSharedUtil.dp2px(mContext, 10));
                                vmlp.height = (int) (scale * PangleSharedUtil.dp2px(mContext, 20));
                                creativeButton.setLayoutParams(vmlp);
                                creativeButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, scale * 9);
                            }

                        } else if (mBannerWidth / mBannerHeight <= 690f / 388f) { //1.78
                            float scale = mBannerHeight / 388f;

                            if (iconLp != null) {
                                ViewGroup.MarginLayoutParams vmlp = (ViewGroup.MarginLayoutParams) iconLp;
                                vmlp.topMargin = (int) (scale * PangleSharedUtil.dp2px(mContext, 6));
                                iconImage.setLayoutParams(vmlp);
                            }


                            if (titleTv != null) {
                                titleTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10 * scale);
                            }


                            if (mainImgLayoutLp != null) {
                                ViewGroup.MarginLayoutParams vmlp = (ViewGroup.MarginLayoutParams) mainImgLayoutLp;
                                vmlp.rightMargin = 0;
                                vmlp.topMargin = (int) (scale * PangleSharedUtil.dp2px(mContext, 8));
                                mainImgLayout.setLayoutParams(vmlp);
                            }

                            if (btnLp != null) {
                                ViewGroup.MarginLayoutParams vmlp = (ViewGroup.MarginLayoutParams) btnLp;
                                vmlp.leftMargin = (int) (scale * PangleSharedUtil.dp2px(mContext, 8));
                                vmlp.rightMargin = (int) (scale * PangleSharedUtil.dp2px(mContext, 8));
                                vmlp.bottomMargin = (int) (scale * PangleSharedUtil.dp2px(mContext, 10));
                                creativeButton.setLayoutParams(vmlp);
                                creativeButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, scale * 8);
                            }


                            if (adContentLayout != null) {
                                adContentLayout.setPadding(0, adContentLayout.getTop(), (int) (scale * PangleSharedUtil.dp2px(mContext, 10)), adContentLayout.getBottom());
                            }
                        } else if (mBannerWidth / mBannerHeight <= 600f / 300f) { // 2.0f
                            float scale = mBannerHeight / 300f;

                            if (iconLp != null) {
                                ViewGroup.MarginLayoutParams vmlp = (ViewGroup.MarginLayoutParams) iconLp;
                                vmlp.width = (int) (scale * PangleSharedUtil.dp2px(mContext, 35));
                                vmlp.height = (int) (scale * PangleSharedUtil.dp2px(mContext, 35));
                                iconImage.setLayoutParams(vmlp);
                            }

                            if (mainImgLayoutLp != null) {
                                ViewGroup.MarginLayoutParams vmlp = (ViewGroup.MarginLayoutParams) mainImgLayoutLp;
                                vmlp.topMargin = (int) (scale * PangleSharedUtil.dp2px(mContext, 5));
                                mainImgLayout.setLayoutParams(vmlp);
                            }

                            if (titleTv != null) {
                                titleTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 8 * scale);
                            }

                            if (btnLp != null) {
                                ViewGroup.MarginLayoutParams vmlp = (ViewGroup.MarginLayoutParams) btnLp;
                                vmlp.bottomMargin = (int) (scale * PangleSharedUtil.dp2px(mContext, 12));
                                vmlp.leftMargin = (int) (scale * PangleSharedUtil.dp2px(mContext, 6));
                                vmlp.rightMargin = (int) (scale * PangleSharedUtil.dp2px(mContext, 6));
                                vmlp.height = (int) (scale * PangleSharedUtil.dp2px(mContext, 20));
                                creativeButton.setLayoutParams(vmlp);
                                creativeButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, scale * 8);
                            }

                        } else if (mBannerWidth / mBannerHeight <= 600f / 260f) { //600 * 260   2.3f
                            float scale = mBannerHeight / 260f;

                            if (titleTv != null) {
                                titleTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 8 * scale);
                            }

                            if (iconLp != null) {
                                ViewGroup.MarginLayoutParams vmlp = (ViewGroup.MarginLayoutParams) iconLp;
                                vmlp.width = (int) (scale * PangleSharedUtil.dp2px(mContext, 30));
                                vmlp.height = (int) (scale * PangleSharedUtil.dp2px(mContext, 30));
                                iconImage.setLayoutParams(vmlp);
                            }

                            if (mainImgLayoutLp != null) {
                                ViewGroup.MarginLayoutParams vmlp = (ViewGroup.MarginLayoutParams) mainImgLayoutLp;
                                vmlp.topMargin = (int) (scale * PangleSharedUtil.dp2px(mContext, 8));
                                mainImgLayout.setLayoutParams(vmlp);
                            }

                            if (btnLp != null) {
                                ViewGroup.MarginLayoutParams vmlp = (ViewGroup.MarginLayoutParams) btnLp;
                                vmlp.bottomMargin = (int) (scale * PangleSharedUtil.dp2px(mContext, 8));
                                vmlp.leftMargin = (int) (scale * PangleSharedUtil.dp2px(mContext, 6));
                                vmlp.rightMargin = (int) (scale * PangleSharedUtil.dp2px(mContext, 6));
                                vmlp.height = (int) (scale * PangleSharedUtil.dp2px(mContext, 20));
                                creativeButton.setLayoutParams(vmlp);
                                creativeButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, scale * 6);
                            }


                        } else if (mBannerWidth / mBannerHeight <= 600f / 200f) { //3
                            float scale = mBannerHeight / 200f;

                            if (iconLp != null) {
                                ViewGroup.MarginLayoutParams vmlp = (ViewGroup.MarginLayoutParams) iconLp;
                                vmlp.topMargin = PangleSharedUtil.dp2px(mContext, 10);
                                iconImage.setLayoutParams(vmlp);
                            }

                            if (btnLp != null) {
                                ViewGroup.MarginLayoutParams vmlp = (ViewGroup.MarginLayoutParams) btnLp;
                                vmlp.bottomMargin = (int) (scale * PangleSharedUtil.dp2px(mContext, 10));
                                creativeButton.setLayoutParams(vmlp);
                            }

                        }

                    }
                }
            }

            TTImage icon = nativeAd.getIcon();
            if (icon != null && icon.isValid()) {
                if (iconImage != null) {
                    NativeImageHelper.loadImageView(icon.getImageUrl(), iconImage);
                }
            }

            if (creativeButton != null) {
                creativeButton.setText(nativeAd.getButtonText());
            }

            /** the views that can be clicked */
            List<View> clickViewList = new ArrayList<>();
            clickViewList.add(nativeView);

            /** The views that can trigger the creative action (like download app) */
            List<View> creativeViewList = new ArrayList<>();
            if (creativeButton != null) {
                creativeViewList.add(creativeButton);
            }

            /** notice! This involves advertising billing and must be called correctly. convertView must use ViewGroup. */
            nativeAd.registerViewForInteraction((ViewGroup) nativeView, clickViewList, creativeViewList, mAdInteractionListener);
        }


        TTNativeAd.AdInteractionListener mAdInteractionListener = new TTNativeAd.AdInteractionListener() {
            @Override
            public void onAdClicked(View view, TTNativeAd ad) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "banner native Ad clicked");

                if (mCustomEventBannerListener != null) {
                    mCustomEventBannerListener.onBannerClicked();
                }
            }

            @Override
            public void onAdCreativeClick(View view, TTNativeAd ad) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "banner native Ad clicked");
                if (mCustomEventBannerListener != null) {
                    mCustomEventBannerListener.onBannerClicked();
                }
            }

            @Override
            public void onAdShow(TTNativeAd ad) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "banner native Ad showed");

                if (mCustomEventBannerListener != null) {
                    mCustomEventBannerListener.onBannerImpression();
                }
            }
        };

        public void destroy() {
//            mContext = null;
            mCustomEventBannerListener = null;
            mNativeAdListener = null;
        }

    }


    /**
     * created by wuzejian on 2020/5/11
     * pangle express ad banner
     */
    public static class PangleAdBannerExpressLoader {

        private TTNativeExpressAd mTTNativeExpressAd;

        private Context mContext;

        private CustomEventBanner.CustomEventBannerListener mCustomEventBannerListener;


        PangleAdBannerExpressLoader(Context context, CustomEventBanner.CustomEventBannerListener customEventBannerListener) {
            this.mCustomEventBannerListener = customEventBannerListener;
            this.mContext = context;
        }

        /**
         * load ad
         *
         * @param adSlot
         */
        public void loadAdExpressBanner(AdSlot adSlot, TTAdNative ttAdNative) {
            if (mContext == null || adSlot == null || ttAdNative == null || TextUtils.isEmpty(adSlot.getCodeId()))
                return;
            ttAdNative.loadBannerExpressAd(adSlot, mTTNativeExpressAdListener);
        }


        /**
         * banner 广告加载回调监听
         */
        private TTAdNative.NativeExpressAdListener mTTNativeExpressAdListener = new TTAdNative.NativeExpressAdListener() {
            @SuppressLint("LongLogTag")
            @Override
            public void onError(int code, String message) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "express banner ad  onBannerFailed.-code=" + code + "," + message);

                if (mCustomEventBannerListener != null) {
                    mCustomEventBannerListener.onBannerFailed(PangleSharedUtil.mapErrorCode(code));
                }
                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                        MoPubErrorCode.NETWORK_NO_FILL);
            }

            @Override
            public void onNativeExpressAdLoad(List<TTNativeExpressAd> ads) {
                if (ads == null || ads.size() == 0) {
                    return;
                }
                mTTNativeExpressAd = ads.get(0);
                mTTNativeExpressAd.setSlideIntervalTime(30 * 1000);
                mTTNativeExpressAd.setExpressInteractionListener(mExpressAdInteractionListener);
                bindDislike(mTTNativeExpressAd);
                mTTNativeExpressAd.render();
            }
        };

        private void bindDislike(TTNativeExpressAd ad) {
            /** dislike function, maybe you can use custom dialog, please refer to the access document by yourself */
            if (mContext instanceof Activity) {
                ad.setDislikeCallback((Activity) mContext, new TTAdDislike.DislikeInteractionCallback() {
                    @Override
                    public void onSelected(int position, String value) {
                        MoPubLog.log(CUSTOM, ADAPTER_NAME, "DislikeInteractionCallback=click-value=" + value);
                    }

                    @Override
                    public void onCancel() {
                        MoPubLog.log(CUSTOM, ADAPTER_NAME, "DislikeInteractionCallbac Cancel click=");
                    }
                });
            }
        }

        /**
         * banner 渲染回调监听
         */
        private TTNativeExpressAd.ExpressAdInteractionListener mExpressAdInteractionListener = new TTNativeExpressAd.ExpressAdInteractionListener() {
            @Override
            public void onAdClicked(View view, int type) {
                if (mCustomEventBannerListener != null) {
                    mCustomEventBannerListener.onBannerClicked();
                }
            }

            @Override
            public void onAdShow(View view, int type) {
                if (mCustomEventBannerListener != null) {
                    mCustomEventBannerListener.onBannerImpression();
                }
            }

            @Override
            public void onRenderFail(View view, String msg, int code) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "banner ad onRenderFail msg = " + msg + "，code=" + code);
                if (mCustomEventBannerListener != null) {
                    mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.RENDER_PROCESS_GONE_UNSPECIFIED);
                }
                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.RENDER_PROCESS_GONE_UNSPECIFIED.getIntCode(),
                        MoPubErrorCode.RENDER_PROCESS_GONE_UNSPECIFIED);
            }

            @Override
            public void onRenderSuccess(View view, float width, float height) {
                if (mCustomEventBannerListener != null) {
                    /** render success add view to mMoPubView */
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "banner ad onRenderSuccess ");
                    mCustomEventBannerListener.onBannerLoaded(view);
                }
            }
        };

        public void destroy() {
            if (mTTNativeExpressAd != null) {
                mTTNativeExpressAd.destroy();
                mTTNativeExpressAd = null;
            }

//            this.mContext = null;
            this.mCustomEventBannerListener = null;
            this.mExpressAdInteractionListener = null;
            this.mTTNativeExpressAdListener = null;
        }
    }

}

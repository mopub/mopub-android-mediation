package com.mintegral.adapter.custombanner.banneradapter;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.mintegral.adapter.common.AdapterCommonUtil;
import com.mintegral.adapter.common.AdapterTools;
import com.mintegral.msdk.MIntegralConstans;
import com.mintegral.msdk.MIntegralSDK;
import com.mintegral.msdk.out.Campaign;
import com.mintegral.msdk.out.Frame;
import com.mintegral.msdk.out.MIntegralSDKFactory;
import com.mintegral.msdk.out.MtgNativeHandler;
import com.mintegral.msdk.out.NativeListener;
import com.mopub.mobileads.CustomEventBanner;
import com.mopub.mobileads.MoPubErrorCode;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Created by allen on 19/04/19.
 */
public class MintegralBanner extends CustomEventBanner {

    MtgNativeHandler nativeHandle;
    Campaign mCampaign;
    LinearLayout mTextLl;
    RelativeLayout mContainerRl;
    ImageView mBgImg;
    TextView mTitleTv,mDesTv;
    TextView mCtaBtn;
    CustomEventBannerListener mcustomEventBannerListener;
    public static  final String TAG="MintegralBanner";
    private Handler.Callback mCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch(msg.what){
                case 0:
                    Log.e(TAG, "handleMessage");
                    Bitmap bmp=(Bitmap)msg.obj;
                    if (bmp!=null){
                        mBgImg.setImageBitmap(bmp);
                    }
                    if (mcustomEventBannerListener!=null){
                        mcustomEventBannerListener.onBannerLoaded(mContainerRl);
                    }

                    break;
            }
            return true;
        }
    };
    private Handler mHandler = new WeakRefHandler(mCallback);//防止内存泄露使用软引用


    @Override
    protected void loadBanner(final Context context, CustomEventBannerListener customEventBannerListener, Map<String, Object> map, Map<String, String> map1) {

        final String unit_id;
        Log.e(TAG, "loadBanner");
        this.mcustomEventBannerListener=customEventBannerListener;
        if (extrasAreValid(map1, context,map)) {
            unit_id = map1.get("unitId");
        } else {
            customEventBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }
        Map<String, Object> properties = MtgNativeHandler.getNativeProperties(unit_id);
        //每次进来nativeHandle对象都是null,所以这里不需要内存优化
        nativeHandle = new MtgNativeHandler(properties, context);
        nativeHandle.addTemplate(new NativeListener.Template(MIntegralConstans.TEMPLATE_MULTIPLE_IMG, 1));
        nativeHandle.setAdListener(new NativeListener.NativeAdListener() {

            @Override
            public void onAdLoaded(List<Campaign> campaigns, int template) {
                Log.e(TAG, "onAdLoaded");
                if (campaigns != null && campaigns.size() > 0) {
                    mCampaign = campaigns.get(0);
                    if (mCampaign!=null){
                        initView(context);
                    }
                }
            }

            @Override
            public void onAdLoadError(String message) {
                Log.e(TAG, message);
                if (mcustomEventBannerListener!=null){
                    mcustomEventBannerListener.onBannerFailed(MoPubErrorCode.NO_FILL);
                }
            }

            @Override
            public void onAdFramesLoaded(List<Frame> list) {

            }

            @Override
            public void onLoggingImpression(int adsourceType) {
                Log.e(TAG, "onLoggingImpression");
                if (mcustomEventBannerListener!=null) {
                        mcustomEventBannerListener.onBannerImpression();
                }
            }

            @Override
            public void onAdClick(Campaign campaign) {
                Log.e(TAG, "onAdClickinner");
                if (mcustomEventBannerListener!=null) {
                    mcustomEventBannerListener.onBannerClicked();
                }

            }
        });
        nativeHandle.load();
    }

    @Override
    protected void onInvalidate() {

    }


    @Override
    protected void trackMpxAndThirdPartyImpressions() {
        super.trackMpxAndThirdPartyImpressions();
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras, Context mContext, Map<String, Object> localExtras) {
        final String placementId = serverExtras.get("appId");
        final String appKey = serverExtras.get("appKey");

        AdapterCommonUtil.addChannel();
        if (placementId != null && placementId.length() > 0 && appKey != null && appKey.length() > 0) {
            MIntegralSDK sdk = MIntegralSDKFactory.getMIntegralSDK();
            if(!AdapterTools.canCollectPersonalInformation()){
                sdk.setUserPrivateInfoType(mContext, MIntegralConstans.AUTHORITY_ALL_INFO,MIntegralConstans.IS_SWITCH_OFF);
            }else{
                sdk.setUserPrivateInfoType(mContext,MIntegralConstans.AUTHORITY_ALL_INFO,MIntegralConstans.IS_SWITCH_ON);
            }

            Map<String, String> map = sdk.getMTGConfigurationMap(placementId,
                    appKey);
            if (mContext instanceof Activity) {
                sdk.init(map, ((Activity) mContext).getApplication());
            } else if (mContext instanceof Application) {
                sdk.init(map, mContext);
            }
            AdapterCommonUtil.parseLocalExtras(localExtras,sdk);
            return true;
        }

        return false;
    }

    /**
     * 获取线上icon
     * @param url
     * @return
     */
    private Bitmap getURLimage(String url) {
        Bitmap bmp = null;
        HttpURLConnection conn = null;
        InputStream is = null;
        try {
            URL myurl = new URL(url);
            // 获得连接
            conn= (HttpURLConnection) myurl.openConnection();
            conn.setConnectTimeout(10000);//设置超时
            conn.setDoInput(true);
            conn.setUseCaches(false);//不缓存
            conn.connect();
            is = conn.getInputStream();//获得图片的数据流
            bmp = BitmapFactory.decodeStream(is);
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            conn.disconnect();
        }
        return bmp;
    }

    /**
     * init banner view
     * @param context
     */
    private void initView(Context context){
        //最外层的linearlayout
        mContainerRl = new RelativeLayout(context);
        RelativeLayout.LayoutParams layoutParams=new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.MATCH_PARENT );
        mContainerRl.setLayoutParams(layoutParams);

        //左边icon
        mBgImg = new ImageView(context);
        RelativeLayout.LayoutParams ivParams = new RelativeLayout.LayoutParams(dip2px(context,50), RelativeLayout.LayoutParams.MATCH_PARENT);
        ivParams.setMargins(dip2px(context,10),0,dip2px(context,10),0);
        ivParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        ivParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        mBgImg.setId(1);
        mBgImg.setLayoutParams(ivParams);
        mBgImg.setScaleType(ImageView.ScaleType.FIT_XY);
        mBgImg.setClipToOutline(true);
        GradientDrawable agradientDrawable = new GradientDrawable();
        agradientDrawable.setCornerRadius(dip2px(context,10));
        mBgImg.setBackground(agradientDrawable);
        mContainerRl.addView(mBgImg,0);


        //cta button
        mCtaBtn =new TextView(context);
        RelativeLayout.LayoutParams ctaBtnParams = new RelativeLayout.LayoutParams(dip2px(context,80), RelativeLayout.LayoutParams.MATCH_PARENT);
        ctaBtnParams.setMargins(0,dip2px(context,8),10,dip2px(context,8));
        ctaBtnParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        ctaBtnParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        mCtaBtn.setId(2);
        mCtaBtn.setLayoutParams(ctaBtnParams);
        // 创建渐变的shape drawable
        int colors[] = { Color.parseColor("#80C426") , Color.parseColor("#19C84F")};//分别为开始颜色，中间夜色，结束颜色
        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
        gradientDrawable.setCornerRadius(dip2px(context,17));
        mCtaBtn.setBackgroundDrawable(gradientDrawable);
        mCtaBtn.setText(mCampaign.getAdCall());
        mCtaBtn.setTextSize(16);
        mCtaBtn.setMaxLines(1);

//        mCtaBtn.setAutoSizeTextTypeUniformWithConfiguration(dip2px(context,2),dip2px(context,80),0);
        mCtaBtn.setGravity(Gravity.CENTER);
        mCtaBtn.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        mCtaBtn.setTextColor(Color.parseColor("#FFFFFFFF"));
        mContainerRl.addView(mCtaBtn,1);


        //中间text的linearlayout
        mTextLl = new LinearLayout(context);
        RelativeLayout.LayoutParams textLlParams = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        textLlParams.setMargins(0,dip2px(context,5),dip2px(context,10),dip2px(context,5));
        textLlParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        textLlParams.addRule(RelativeLayout.RIGHT_OF,mBgImg.getId());
        textLlParams.addRule(RelativeLayout.LEFT_OF,mCtaBtn.getId());
        mTextLl.setLayoutParams(textLlParams);
        mTextLl.setOrientation(LinearLayout.VERTICAL);
        mContainerRl.addView(mTextLl,2);


        //title
        mTitleTv = new TextView(context);
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        tvParams.weight=1;
        mTitleTv.setLayoutParams(tvParams);
        mTitleTv.setText(mCampaign.getAppName());
        mTitleTv.setTextSize(16);
        mTitleTv.setTextColor(Color.parseColor("#FF000000"));
        mTitleTv.setSingleLine(true);
        mTitleTv.setEllipsize(TextUtils.TruncateAt.END);
        mTitleTv.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));


        //desc
        mDesTv = new TextView(context);
        LinearLayout.LayoutParams desParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        desParams.weight=1;
        mDesTv.setLayoutParams(desParams);
        mDesTv.setText(mCampaign.getAppDesc());
        mDesTv.setTextSize(14);
        mDesTv.setTextColor(Color.parseColor("#FF484848"));
        mDesTv.setSingleLine(true);
        mDesTv.setEllipsize(TextUtils.TruncateAt.END);
        //添加布局
        mTextLl.addView(mTitleTv,0);
        mTextLl.addView(mDesTv,1);





        //注册监听
        nativeHandle.registerView(mContainerRl,mCampaign);

        new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                if (mCampaign.getIconUrl()!=null){
                    Bitmap bmp = getURLimage(mCampaign.getIconUrl());
                    Message msg =  Message.obtain();
                    msg.what = 0;
                    msg.obj = bmp;
                    mHandler.sendMessage(msg);
                }
            }
        }).start();
    }

    public class WeakRefHandler extends Handler {
        private WeakReference<Callback> mWeakReference;

        public WeakRefHandler(Callback callback) {
            mWeakReference = new WeakReference<Callback>(callback);
        }

        public WeakRefHandler(Callback callback, Looper looper) {
            super(looper);
            mWeakReference = new WeakReference<Callback>(callback);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mWeakReference != null && mWeakReference.get() != null) {
                Callback callback = mWeakReference.get();
                callback.handleMessage(msg);
            }
        }
    }
    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        Log.e(TAG, dipValue+"daxiao"+(int) (dipValue * scale + 0.5f));
        return (int) (dipValue * scale + 0.5f);
    }

}

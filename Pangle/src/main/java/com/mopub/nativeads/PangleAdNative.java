package com.mopub.nativeads;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.FilterWord;
import com.bytedance.sdk.openadsdk.TTAdDislike;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTDislikeDialogAbstract;
import com.bytedance.sdk.openadsdk.TTFeedAd;
import com.bytedance.sdk.openadsdk.TTImage;
import com.bytedance.sdk.openadsdk.TTNativeAd;
import com.mopub.common.DataKeys;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.PangleAdapterConfiguration;
import com.mopub.mobileads.PangleSharedUtil;

import java.util.List;
import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;

/**
 * created by wuzejian on 2020/5/12
 */
public class PangleAdNative extends CustomEventNative {
    private static final String ADAPTER_NAME = "PangleAdNative";

    /**
     * gdpr
     */
    public final static String GDPR_RESULT = "gdpr_result";

    /**
     * request ad count
     */
    public final static String REQUEST_AD_COUNT = "request_ad_count";


    /**
     * ad size
     */
    public final static String FEED_AD_WIDTH = "feed_ad_width";
    public final static String FEED_AD_HEIGHT = "feed_ad_height";

    /**
     * pangolin network native ad unit ID.
     */
    private String placementId = "";

    private Context mContext;
    private CustomEventNativeListener mCustomEventNativeListener;
    private int requestAdCount = 1;

    private PangleAdapterConfiguration mPangleAdapterConfiguration;

    public PangleAdNative() {
        mPangleAdapterConfiguration = new PangleAdapterConfiguration();
    }


    @Override
    protected void loadNativeAd(Context context, CustomEventNativeListener customEventNativeListener, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "loadNativeAd...... has been create ....");
        this.mContext = context;
        this.mCustomEventNativeListener = customEventNativeListener;

        TTAdManager ttAdManager = null;
        String adm = null;
        int feedWidth = 640;
        int feedHeight = 320;


        if (serverExtras != null) {
            /** obtain adunit from server by mopub */
            String adunit = serverExtras.get(PangleAdapterConfiguration.KEY_EXTRA_AD_UNIT_ID);
            if (!TextUtils.isEmpty(adunit)) {
                this.placementId = adunit;
            }
            adm = serverExtras.get(DataKeys.ADM_KEY);
            /** init pangolin SDK */
            String appId = serverExtras.get(PangleAdapterConfiguration.PANGLE_APP_ID_KEY);
            String appName = serverExtras.get(PangleAdapterConfiguration.PANGLE_APP_NAME_KEY);
            PangleAdapterConfiguration.pangleSdkInit(context, appId, appName);
            ttAdManager = PangleAdapterConfiguration.getPangleSdkManager();

            mPangleAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
        }


        if (localExtras != null) {
            if (placementId == null && localExtras.containsKey(PangleAdapterConfiguration.KEY_EXTRA_AD_UNIT_ID)) {
                placementId = (String) localExtras.get(PangleAdapterConfiguration.KEY_EXTRA_AD_UNIT_ID);
            }
            /**set gdpr */
            if (localExtras.containsKey(GDPR_RESULT)) {
                int gdpr = (int) localExtras.get(GDPR_RESULT);
                if (ttAdManager != null && (gdpr == 0 || gdpr == 1))
                    ttAdManager.setGdpr(gdpr);
            }

            if (localExtras.containsKey(REQUEST_AD_COUNT)) {
                requestAdCount = (int) localExtras.get(REQUEST_AD_COUNT);
            }

            if (localExtras.containsKey(FEED_AD_WIDTH)) {
                feedWidth = (int) localExtras.get(FEED_AD_WIDTH);
            }
            if (localExtras.containsKey(FEED_AD_HEIGHT)) {
                feedHeight = (int) localExtras.get(FEED_AD_HEIGHT);
            }

        }

        MoPubLog.log(CUSTOM, ADAPTER_NAME, "serverExtras...... feedWidth.." + feedWidth + ",feedHeight=" + feedHeight);
        if (ttAdManager != null) {
            TTAdNative adNative = ttAdManager.createAdNative(mContext);
            AdSlot adSlot = new AdSlot.Builder()
                    .setCodeId(placementId)
                    .setSupportDeepLink(true)
                    .setImageAcceptedSize(feedWidth, feedHeight)
                    .setAdCount(requestAdCount) /** ad count from 1 to 3 */
                    .withBid(adm)
                    .build();


            /** request ad */
            adNative.loadFeedAd(adSlot, new TTAdNative.FeedAdListener() {
                @Override
                public void onError(int code, String message) {
                    if (mCustomEventNativeListener != null) {
                        mCustomEventNativeListener.onNativeAdFailed(mapErrorCode(code));
                    }
                    MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, "Loading FeedAd encountered an error: " + mapErrorCode(code).toString() + ",error message:" + message);
                }

                @Override
                public void onFeedAdLoad(List<TTFeedAd> ads) {
                    if (ads != null && ads.size() > 0) {
                        if (mCustomEventNativeListener != null) {
                            for (TTFeedAd ad : ads) {
                                mCustomEventNativeListener.onNativeAdLoaded(new PangolinNativeAd(ad));
                            }
                        }
                    } else {
                        if (mCustomEventNativeListener != null)
                            mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_NO_FILL);
                    }
                }
            });
        }
    }


    protected static class PangolinNativeAd extends BaseNativeAd implements TTNativeAd.AdInteractionListener {

        private TTFeedAd mTTFeedAd;

        PangolinNativeAd(TTFeedAd ad) {
            this.mTTFeedAd = ad;
        }


        @Override
        public void prepare(View view) {

        }

        @Override
        public void clear(View view) {
        }

        @Override
        public void destroy() {

        }

        @Override
        public void onAdClicked(View view, TTNativeAd ad) {
            notifyAdClicked();
        }

        @Override
        public void onAdCreativeClick(View view, TTNativeAd ad) {
            notifyAdClicked();
        }

        @Override
        public void onAdShow(TTNativeAd ad) {
            notifyAdImpressed();
        }


        public final String getAdvertiserName() {
            if (mTTFeedAd != null) {
                return mTTFeedAd.getSource();
            }
            return null;
        }

        public final String getTitle() {
            if (mTTFeedAd != null) {
                return mTTFeedAd.getTitle();
            }
            return null;
        }

        public final String getDecriptionText() {
            if (mTTFeedAd != null) {
                return mTTFeedAd.getDescription();
            }
            return null;
        }

        public final String getCallToAction() {
            if (mTTFeedAd != null) {
                return mTTFeedAd.getButtonText();
            }
            return null;
        }

        public TTImage getVideoCoverImage() {
            if (mTTFeedAd != null) {
                return mTTFeedAd.getVideoCoverImage();
            }
            return null;
        }

        public TTImage getIcon() {
            if (mTTFeedAd != null) {
                return mTTFeedAd.getIcon();
            }
            return null;
        }

        public Bitmap getAdLogo() {
            if (mTTFeedAd != null) {
                return mTTFeedAd.getAdLogo();
            }
            return null;
        }

        public List<TTImage> getImageList() {
            if (mTTFeedAd != null) {
                return mTTFeedAd.getImageList();
            }
            return null;
        }

        public int getAppScore() {
            if (mTTFeedAd != null) {
                return mTTFeedAd.getAppScore();
            }
            return -1;
        }

        public int getAppCommentNum() {
            if (mTTFeedAd != null) {
                return mTTFeedAd.getAppCommentNum();
            }
            return -1;
        }

        public int getAppSize() {
            if (mTTFeedAd != null) {
                return mTTFeedAd.getAppSize();
            }
            return -1;
        }

        public int getInteractionType() {
            if (mTTFeedAd != null) {
                return mTTFeedAd.getInteractionType();
            }
            return -1;
        }

        public int getImageMode() {
            if (mTTFeedAd != null) {
                return mTTFeedAd.getImageMode();
            }
            return -1;
        }

        public List<FilterWord> getFilterWords() {
            if (mTTFeedAd != null) {
                return mTTFeedAd.getFilterWords();
            }
            return null;
        }

        public View getAdView() {
            if (mTTFeedAd != null) {
                return mTTFeedAd.getAdView();
            }
            return null;
        }


        public TTAdDislike getDislikeDialog(Activity activity) {
            if (mTTFeedAd != null) {
                return mTTFeedAd.getDislikeDialog(activity);
            }
            return null;
        }

        public TTAdDislike getDislikeDialog(TTDislikeDialogAbstract dialog) {
            if (mTTFeedAd != null) {
                return mTTFeedAd.getDislikeDialog(dialog);
            }
            return null;
        }

        public void registerViewForInteraction(@NonNull ViewGroup container, @NonNull View clickView, TTNativeAd.AdInteractionListener listener) {
            if (mTTFeedAd != null) {
                mTTFeedAd.registerViewForInteraction(container, clickView, listener);
            }
        }


        public void registerViewForInteraction(@NonNull ViewGroup container, @NonNull List<View> clickViews, @Nullable List<View> creativeViews, TTNativeAd.AdInteractionListener listener) {
            if (mTTFeedAd != null) {
                mTTFeedAd.registerViewForInteraction(container, clickViews, creativeViews, listener);
            }
        }


        public void registerViewForInteraction(@NonNull ViewGroup container, @NonNull List<View> clickViews, @Nullable List<View> creativeViews, @Nullable View dislikeView, TTNativeAd.AdInteractionListener listener) {
            if (mTTFeedAd != null) {
                mTTFeedAd.registerViewForInteraction(container, clickViews, creativeViews, dislikeView, listener);
            }
        }


    }

    private static NativeErrorCode mapErrorCode(int error) {
        switch (error) {
            case PangleSharedUtil.CONTENT_TYPE:
            case PangleSharedUtil.REQUEST_PB_ERROR:
                return NativeErrorCode.CONNECTION_ERROR;
            case PangleSharedUtil.NO_AD:
                return NativeErrorCode.NETWORK_NO_FILL;
            case PangleSharedUtil.ADSLOT_EMPTY:
            case PangleSharedUtil.ADSLOT_ID_ERROR:
                return NativeErrorCode.NETWORK_INVALID_REQUEST;
            default:
                return NativeErrorCode.UNSPECIFIED;
        }
    }

}

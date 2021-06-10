package com.mopub.mobileads;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mbridge.msdk.MBridgeConstans;
import com.mbridge.msdk.MBridgeSDK;
import com.mbridge.msdk.foundation.same.net.Aa;
import com.mbridge.msdk.mbbid.out.BidManager;
import com.mbridge.msdk.out.MBConfiguration;
import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.MoPub;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.mintegral.BuildConfig;

import java.lang.reflect.Method;
import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class MintegralAdapterConfiguration extends BaseAdapterConfiguration {
    public static final String APP_ID_KEY = "appId";
    public static final String APP_KEY = "appKey";
    public static final String PLACEMENT_ID_KEY = "placementId";
    public static final String UNIT_ID_KEY = "unitId";

    private static final String ADAPTER_VERSION = BuildConfig.VERSION_NAME;
    private static final String MOPUB_NETWORK_NAME = BuildConfig.NETWORK_NAME;
    private static final String SDK_VERSION = MBConfiguration.SDK_VERSION;

    private static int mAge;
    private static int mGender;
    private static int mPay;

    private static boolean mIsMute;

    private static String mCustomData;
    private static String mRewardId;
    private static String mUserId;

    private static Double mLatitude;
    private static Double mLongitude;

    @NonNull
    @Override
    public String getAdapterVersion() {
        return ADAPTER_VERSION;
    }

    @Nullable
    @Override
    public String getBiddingToken(@NonNull Context context) {
        Preconditions.checkNotNull(context);

        return BidManager.getBuyerUid(context);
    }

    @NonNull
    @Override
    public String getMoPubNetworkName() {
        return MOPUB_NETWORK_NAME;
    }

    @NonNull
    @Override
    public String getNetworkSdkVersion() {
        return SDK_VERSION;
    }

    @Override
    public void initializeNetwork(@NonNull Context context, @Nullable Map<String, String> configuration,
                                  @NonNull final OnNetworkInitializationFinishedListener listener) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);

        try {
            if (configuration != null && !configuration.isEmpty()) {
                final String appId = configuration.get(APP_ID_KEY);
                final String appKey = configuration.get(APP_KEY);

                if (!TextUtils.isEmpty(appId) && !TextUtils.isEmpty(appKey)) {
                    configureMintegralSdk(appId, appKey, context, new MintegralSdkManager.MBSDKInitializeListener() {
                        @Override
                        public void onInitializeSuccess(String appKey, String appID) {
                            listener.onNetworkInitializationFinished(MintegralAdapterConfiguration.class,
                                    MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
                        }

                        @Override
                        public void onInitializeFailure(String message) {
                            listener.onNetworkInitializationFinished(MintegralAdapterConfiguration.class,
                                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                        }
                    });
                } else {
                    listener.onNetworkInitializationFinished(MintegralAdapterConfiguration.class,
                            MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
            }
        } catch (Exception e) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "Failed to initialize the Mintegral MBridge " +
                    "SDK due to an exception", e);
        }
    }

    public static void configureMintegralSdk(final String appId, final String appKey, final Context context,
                                             final MintegralSdkManager.MBSDKInitializeListener listener) {
        final MBridgeSDK sdk = MintegralSdkManager.getInstance().getMBridgeSDK();

        if (sdk != null) {
            final boolean canCollectPersonalInfo = MoPub.canCollectPersonalInformation();
            final int switchState = canCollectPersonalInfo ? MBridgeConstans.IS_SWITCH_ON :
                    MBridgeConstans.IS_SWITCH_OFF;

            sdk.setUserPrivateInfoType(context, MBridgeConstans.AUTHORITY_ALL_INFO, switchState);

            final boolean debugLogEnabled = MoPubLog.getLogLevel() == MoPubLog.LogLevel.DEBUG;

            if (context instanceof Activity) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        MintegralSdkManager.getInstance().initialize(context.getApplicationContext(),
                                appKey, appId, debugLogEnabled, null, listener);
                    }
                });
            } else if (context instanceof Application) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        MintegralSdkManager.getInstance().initialize(context, appKey, appId,
                                debugLogEnabled, null, listener);
                    }
                });
            }
        } else {
            MoPubLog.log(CUSTOM, "Failed to initialize the Mintegral SDK because the " +
                    "SDK instance is null.");
        }
    }

    public static void setTargeting(MBridgeSDK sdk) {
    }

    public static void setAge(int age) {
        mAge = age;
    }

    public static int getAge() {
        return mAge;
    }

    public static void setCustomData(String customData) {
        mCustomData = customData;
    }

    public static String getCustomData() {
        return mCustomData;
    }

    public static void setGender(int gender) {
        mGender = gender;
    }

    public static int getGender() {
        return mGender;
    }

    public static void setLatitude(double latitude) {
        mLatitude = latitude;
    }

    public static Double getLatitude() {
        return mLatitude;
    }

    public static void setLongitude(double longitude) {
        mLongitude = longitude;
    }

    public static Double getLongitude() {
        return mLongitude;
    }

    public static void setPay(int pay) {
        mPay = pay;
    }

    public static int getPay() {
        return mPay;
    }

    public static void setRewardId(String rewardId) {
        mRewardId = rewardId;
    }

    public static String getRewardId() {
        return TextUtils.isEmpty(mRewardId) ? "1" : mRewardId;
    }

    public static void setUserId(String userId) {
        mUserId = userId;
    }

    public static String getUserId() {
        return TextUtils.isEmpty(mUserId) ? "" : mUserId;
    }

    public static void setMute(boolean muteStatus) {
        mIsMute = muteStatus;
    }

    public static boolean isMute() {
        return mIsMute;
    }

    static void addChannel() {
        try {
            final Aa a = new Aa();
            final Class c = a.getClass();

            final Method method = c.getDeclaredMethod("b", String.class);
            method.setAccessible(true);
            method.invoke(a, "Y+H6DFttYrPQYcIA+F2F+F5/Hv==");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}

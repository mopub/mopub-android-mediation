package com.mopub.mobileads;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mintegral.msdk.MIntegralSDK;
import com.mintegral.msdk.MIntegralUser;
import com.mintegral.msdk.base.common.net.Aa;
import com.mintegral.msdk.mtgbid.out.BidManager;
import com.mintegral.msdk.out.MIntegralSDKFactory;
import com.mintegral.msdk.out.MTGConfiguration;
import com.mopub.common.BaseAdapterConfiguration;
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
    public static final String UNIT_ID_KEY = "unitId";

    private static final String ADAPTER_VERSION = BuildConfig.VERSION_NAME;
    private static final String SDK_VERSION = MTGConfiguration.SDK_VERSION;
    private static final String MOPUB_NETWORK_NAME = BuildConfig.NETWORK_NAME;

    private static boolean isSDKInitialized = false;

    private static int mAge;
    private static String mCustomData;
    private static int mGender;
    private static Double mLatitude;
    private static Double mLongitude;
    private static boolean mIsMute;
    private static int mPay;
    private static String mRewardId;
    private static String mUserId;

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
                                  @NonNull OnNetworkInitializationFinishedListener listener) {

        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);

        try {
            if (configuration != null && !configuration.isEmpty()) {
                final String appId = configuration.get(APP_ID_KEY);
                final String appKey = configuration.get(APP_KEY);

                if (!TextUtils.isEmpty(appId) && !TextUtils.isEmpty(appKey)) {
                    configureMintegral(appId, appKey, context);

                    listener.onNetworkInitializationFinished(this.getClass(),
                            MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
                } else {
                    listener.onNetworkInitializationFinished(this.getClass(),
                            MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
            }
        } catch (Exception e) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "Failed to initialize the Mintegral SDK due " +
                    "to an exception", e);
        }
    }

    public static void configureMintegral(String appId, String appKey, Context context) {

        if (isSDKInitialized) return;

        final MIntegralSDK sdk = MIntegralSDKFactory.getMIntegralSDK();

        if (sdk != null) {
            final Map<String, String> mtgConfigurationMap = sdk.getMTGConfigurationMap(appId, appKey);

            if (context instanceof Activity) {
                sdk.init(mtgConfigurationMap, ((Activity) context).getApplication());
            } else if (context instanceof Application) {
                sdk.init(mtgConfigurationMap, context);
            }

            isSDKInitialized = true;

        } else {
            MoPubLog.log(CUSTOM, "Failed to initialize the Mintegral SDK because the SDK " +
                    "instance is null.");
        }
    }

    public static void setTargeting(MIntegralSDK sdk) {
        try {
            final MIntegralUser user = new MIntegralUser();

            final int age = getAge();
            if (age > 0) {
                user.setAge(age);
            }

            final String customData = getCustomData();
            if (!TextUtils.isEmpty(customData)) {
                user.setCustom(customData);
            }

            final int gender = getGender();
            if (gender == 1 || gender == 2) {
                user.setGender(gender);
            }

            final Double latitude = getLatitude();
            if (latitude != null) {
                user.setLat(latitude);
            }

            final Double longitude = getLongitude();
            if (longitude != null) {
                user.setLng(longitude);
            }

            final int pay = getPay();
            if (pay == 0 || pay == 1) {
                user.setPay(pay);
            }

            sdk.reportUser(user);
        } catch (Throwable t) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "Failed to set ad targeting for Mintegral.",
                    t);
        }
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

package com.mopub.mobileads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mintegral.msdk.MIntegralConstans;
import com.mintegral.msdk.MIntegralSDK;
import com.mintegral.msdk.mtgbid.out.BidManager;
import com.mintegral.msdk.out.MIntegralSDKFactory;
import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.MoPub;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.mintegral.BuildConfig;

import java.util.Map;

public class MintegralAdapterConfiguration extends BaseAdapterConfiguration {
    @NonNull
    @Override
    public String getAdapterVersion() {
        return BuildConfig.ADAPTER_VERSION;
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
        return BuildConfig.NETWORK_NAME;
    }

    @NonNull
    @Override
    public String getNetworkSdkVersion() {
        return BuildConfig.SDK_VERSION;
    }

    @Override
    public void initializeNetwork(@NonNull Context context, @Nullable Map<String, String> configuration, @NonNull OnNetworkInitializationFinishedListener listener) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);
        try {
            String appId = configuration.get("appId");
            String appKey = configuration.get("appKey");
            if (!TextUtils.isEmpty(appId) && !TextUtils.isEmpty(appKey)) {
                MIntegralSDK sdk = MIntegralSDKFactory.getMIntegralSDK();
                Map<String, String> mtgConfigurationMap = sdk.getMTGConfigurationMap(appId, appKey);
                boolean canCollectPersonalInfo = MoPub.canCollectPersonalInformation();
                if (canCollectPersonalInfo) {
                    sdk.setUserPrivateInfoType(context, MIntegralConstans.AUTHORITY_ALL_INFO, MIntegralConstans.IS_SWITCH_ON);
                } else {
                    sdk.setUserPrivateInfoType(context, MIntegralConstans.AUTHORITY_ALL_INFO, MIntegralConstans.IS_SWITCH_OFF);
                }
                sdk.init(mtgConfigurationMap, context);
                listener.onNetworkInitializationFinished(MintegralAdapterConfiguration.class, MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
            } else {
                listener.onNetworkInitializationFinished(MintegralAdapterConfiguration.class, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
        } catch (Exception e) {
            MoPubLog.log(MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE, new Object[]{"Initializing Mintegral has encountered an exception.", e});
        }

    }
}

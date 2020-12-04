package com.mopub.mobileads

import android.content.Context
import com.example.inmobi.BuildConfig
import com.mopub.utils.Utils
import com.inmobi.sdk.InMobiSdk
import com.mopub.common.BaseAdapterConfiguration
import com.mopub.common.OnNetworkInitializationFinishedListener

class InMobiSDKAdapterConfiguration : BaseAdapterConfiguration() {
    override fun getAdapterVersion(): String {
        return BuildConfig.VERSION_NAME
    }

    override fun getBiddingToken(context: Context): String? {
        return InMobiSdk.getToken(Utils.extras, null)
    }

    override fun getMoPubNetworkName(): String {
        return "inmobi"
    }

    override fun getNetworkSdkVersion(): String {
        return InMobiSdk.getVersion()
    }

    override fun initializeNetwork(context: Context, configuration: Map<String, String>?, onNetworkInitializationFinishedListener: OnNetworkInitializationFinishedListener) {
        onNetworkInitializationFinishedListener.onNetworkInitializationFinished(InMobiSDKAdapterConfiguration::class.java,
                MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS)
    }
}
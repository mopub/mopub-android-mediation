package com.mopub.mobileads

import android.content.Context
import com.inmobi.sdk.InMobiSdk
import com.mopub.common.BaseAdapterConfiguration
import com.mopub.common.OnNetworkInitializationFinishedListener
import com.mopub.mobileads.inmobi.BuildConfig
import com.mopub.utils.Utils

class InMobiSDKAdapterConfiguration : BaseAdapterConfiguration() {
    companion object {
        var ACCOUNT_ID = "accountId"
        var isSdkInitialised = false
    }

    override fun getAdapterVersion(): String {
        return BuildConfig.VERSION_NAME
    }

    override fun getBiddingToken(context: Context): String? {
        return InMobiSdk.getToken(Utils.extras, null)
    }

    override fun getMoPubNetworkName(): String {
        return BuildConfig.NETWORK_NAME
    }

    override fun getNetworkSdkVersion(): String {
        return InMobiSdk.getVersion()
    }

    override fun initializeNetwork(context: Context, configuration: Map<String, String>?, onNetworkInitializationFinishedListener: OnNetworkInitializationFinishedListener) {
        if (isSdkInitialised) {
            return
        }
        if (null != configuration) {
            val accountId = if (configuration.containsKey(ACCOUNT_ID)) configuration[ACCOUNT_ID] else null
            if (!accountId.isNullOrEmpty()) {
                InMobiSdk.init(context, accountId, InMobiGDPR.gdprConsentDictionary) { error ->
                    if (null == error) {
                        isSdkInitialised = true
                        onNetworkInitializationFinishedListener.onNetworkInitializationFinished(InMobiSDKAdapterConfiguration::class.java, MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS)
                    } else {
                        onNetworkInitializationFinishedListener.onNetworkInitializationFinished(InMobiSDKAdapterConfiguration::class.java, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR)
                    }
                }
                return
            }
        }
        onNetworkInitializationFinishedListener.onNetworkInitializationFinished(InMobiSDKAdapterConfiguration::class.java, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR)
    }
}
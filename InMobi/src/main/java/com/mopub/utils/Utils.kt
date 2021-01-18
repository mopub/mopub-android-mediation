package com.mopub.utils

import com.inmobi.ads.InMobiAdRequestStatus
import com.mopub.common.MoPub
import com.mopub.common.logging.MoPubLog
import com.mopub.mobileads.MoPubErrorCode
import java.lang.reflect.Field

object Utils {
    fun getMoPubErrorCode(statusCode: InMobiAdRequestStatus.StatusCode?): MoPubErrorCode {
        return when (statusCode) {
            InMobiAdRequestStatus.StatusCode.INTERNAL_ERROR -> MoPubErrorCode.INTERNAL_ERROR
            InMobiAdRequestStatus.StatusCode.REQUEST_INVALID -> MoPubErrorCode.NETWORK_INVALID_STATE
            InMobiAdRequestStatus.StatusCode.NETWORK_UNREACHABLE -> MoPubErrorCode.NO_CONNECTION
            InMobiAdRequestStatus.StatusCode.NO_FILL -> MoPubErrorCode.NO_FILL
            InMobiAdRequestStatus.StatusCode.REQUEST_TIMED_OUT -> MoPubErrorCode.NETWORK_TIMEOUT
            InMobiAdRequestStatus.StatusCode.SERVER_ERROR -> MoPubErrorCode.NETWORK_INVALID_STATE
            else -> MoPubErrorCode.UNSPECIFIED
        }
    }

    val extras: Map<String, String>

    init {
        val map: MutableMap<String, String> = HashMap()
        map["tp"] = "c_mopub"
        try {
            val mopubSdkClassRef = Class.forName(MoPub::class.java.name)
            val mopubSdkVersionRef: Field = mopubSdkClassRef.getDeclaredField("SDK_VERSION")
            val moPubSDKVersion = mopubSdkVersionRef[null].toString()
            map["tp-ver"] = moPubSDKVersion
        } catch (e: Exception) {
            MoPubLog.log(MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE, "InMobiUtils",
                    "Something went wrong while getting the MoPub SDK version", e)
        }
        extras = map
    }
}
package com.mopub.mobileads

import android.content.Context
import com.inmobi.ads.InMobiAdRequestStatus
import com.inmobi.sdk.InMobiSdk
import com.mopub.common.BaseAdapterConfiguration
import com.mopub.common.MoPub
import com.mopub.common.OnNetworkInitializationFinishedListener
import com.mopub.common.logging.MoPubLog
import com.mopub.common.logging.MoPubLog.AdapterLogEvent
import com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM
import com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE
import com.mopub.mobileads.inmobi.BuildConfig
import java.lang.reflect.Field

class InMobiAdapterConfiguration : BaseAdapterConfiguration() {

    private val adapterVersionName = BuildConfig.VERSION_NAME
    private val networkName = BuildConfig.NETWORK_NAME

    override fun getAdapterVersion(): String {
        return adapterVersionName
    }

    override fun getBiddingToken(context: Context): String? {
        return InMobiSdk.getToken(inMobiTPExtras, null)
    }

    override fun getMoPubNetworkName(): String {
        return networkName
    }

    override fun getNetworkSdkVersion(): String {
        return InMobiSdk.getVersion()
    }

    override fun initializeNetwork(context: Context, configuration: Map<String, String>?, onNetworkInitializationFinishedListener: OnNetworkInitializationFinishedListener) {
        when (MoPubLog.getLogLevel()) {
            MoPubLog.LogLevel.DEBUG, MoPubLog.LogLevel.INFO -> {
                InMobiSdk.setLogLevel(InMobiSdk.LogLevel.DEBUG)
            }
            MoPubLog.LogLevel.NONE -> {
                InMobiSdk.setLogLevel(InMobiSdk.LogLevel.NONE)
            }
        }

        if (configuration.isNullOrEmpty()) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "InMobi initialization failure. Network configuration map is empty. Cannot parse Account ID value for initialization"
                    + initializationErrorInfo)
            onNetworkInitializationFinishedListener.onNetworkInitializationFinished(InMobiAdapterConfiguration::class.java, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR)
            return
        }

        initializeInMobi(configuration, context, null)
        onNetworkInitializationFinishedListener.onNetworkInitializationFinished(InMobiAdapterConfiguration::class.java, MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS)
    }

    interface InMobiInitCompletionListener {
        fun onSuccess()
        fun onFailure(error: Throwable)
    }

    class InMobiPlacementIdException(message: String): Exception(message)
    class InMobiAccountIdException(message: String): Exception(message)

    companion object {
        private const val ACCOUNT_ID_KEY = "accountid"
        private const val PLACEMENT_ID_KEY = "placementid"

        val ADAPTER_NAME: String = InMobiAdapterConfiguration::class.java.simpleName
        private const val initializationErrorInfo = "InMobi will attempt to initialize on the first ad request using server extras values from MoPub UI. " +
                "If you're using InMobi for Advanced Bidding, and initializing InMobi outside and before MoPub, you may disregard this error."
        val inMobiTPExtras: Map<String, String>
        private const val accountIdErrorMessage = "Please make sure you provide correct Account ID information on MoPub UI or network configuration on initialization."
        private const val placementIdErrorMessage = "Please make sure you provide correct Placement ID information on MoPub UI."

        fun initializeInMobi(configuration: Map<String, String>, context: Context, inMobiInitCompletionListener: InMobiInitCompletionListener?) {
            try {
                val accountId = getAccountId(configuration)
                InMobiSdk.init(context, accountId, null) { error ->
                    if (error == null) {
                        MoPubLog.log(CUSTOM, ADAPTER_NAME, "InMobi initialization success.")
                        inMobiInitCompletionListener?.onSuccess()
                    } else {
                        MoPubLog.log(CUSTOM, ADAPTER_NAME, "InMobi initialization failure. Reason: ${error.message}")
                        inMobiInitCompletionListener?.onFailure(error)
                    }
                }
            } catch (accountIdException: InMobiAccountIdException) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, accountIdException.localizedMessage, accountIdException)
                inMobiInitCompletionListener?.onFailure(accountIdException)
            } catch (e: Exception) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, e.localizedMessage, e)
                inMobiInitCompletionListener?.onFailure(e)
            }
        }

        fun getMoPubErrorCode(statusCode: InMobiAdRequestStatus.StatusCode?): MoPubErrorCode {
            return when (statusCode) {
                InMobiAdRequestStatus.StatusCode.INTERNAL_ERROR -> MoPubErrorCode.INTERNAL_ERROR
                InMobiAdRequestStatus.StatusCode.NETWORK_UNREACHABLE -> MoPubErrorCode.NO_CONNECTION
                InMobiAdRequestStatus.StatusCode.NO_FILL -> MoPubErrorCode.NO_FILL
                InMobiAdRequestStatus.StatusCode.REQUEST_TIMED_OUT -> MoPubErrorCode.NETWORK_TIMEOUT
                InMobiAdRequestStatus.StatusCode.REQUEST_INVALID, InMobiAdRequestStatus.StatusCode.SERVER_ERROR -> MoPubErrorCode.NETWORK_INVALID_STATE
                else -> MoPubErrorCode.UNSPECIFIED
            }
        }

        private fun getAccountId(dict: Map<String, String>): String {
            val accountIdString: String? = dict[ACCOUNT_ID_KEY]
            return if (accountIdString.isNullOrEmpty()) {
                throw InMobiAccountIdException("InMobi Account ID parameter is null or empty. " +
                        accountIdErrorMessage)
            } else {
                accountIdString
            }
        }

        fun getPlacementId(dict: Map<String, String>): Long {
            val placementIdString: String? = dict[PLACEMENT_ID_KEY]
            if (placementIdString.isNullOrEmpty()) {
                throw InMobiPlacementIdException("InMobi Placement ID parameter is null or empty. " +
                        placementIdErrorMessage)
            }

            try {
                val placementIdLong = placementIdString.toLong()
                if (placementIdLong <= 0) {
                    throw InMobiPlacementIdException("InMobi Placement ID parameter is incorrect, it evaluates to less than or equal to 0. " +
                            placementIdErrorMessage)
                }
                return placementIdLong

            } catch (e: NumberFormatException) {
                throw InMobiPlacementIdException("InMobi Placement ID parameter is incorrect, cannot cast it to Long, it has to be a proper Long value per InMobi's placement requirements. " +
                        placementIdErrorMessage)
            }
        }

        /**
         * Call for a load or interaction related failure an error.
         *
         * @param loadListener - Populate only if the failure is load related.
         * @param interactionListener - Populate only if the failure is interaction related.
         */
        fun onInMobiAdFailWithError(error: Throwable,
                                    moPubErrorCode: MoPubErrorCode,
                                    errorMessage: String?,
                                    adapterName: String,
                                    loadListener: AdLifecycleListener.LoadListener?,
                                    interactionListener: AdLifecycleListener.InteractionListener?) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, error)
            onInMobiAdFail(errorMessage, moPubErrorCode, adapterName, loadListener, interactionListener)
        }

        /**
         * Call for a load or interaction related failure with an event.
         *
         * @param loadListener - Populate only if the failure is load related.
         * @param interactionListener - Populate only if the failure is interaction related.
         */
        fun onInMobiAdFailWithEvent(logEvent: AdapterLogEvent,
                                    placementId: String,
                                    moPubErrorCode: MoPubErrorCode,
                                    errorMessage: String?,
                                    adapterName: String,
                                    loadListener: AdLifecycleListener.LoadListener?,
                                    interactionListener: AdLifecycleListener.InteractionListener?) {
            MoPubLog.log(placementId, logEvent, adapterName, moPubErrorCode.intCode, moPubErrorCode)
            onInMobiAdFail(errorMessage, moPubErrorCode, adapterName, loadListener, interactionListener)
        }

        /**
         * Call for a load or interaction related failure with an event.
         *
         * @param loadListener - Populate only if the failure is load related.
         * @param interactionListener - Populate only if the failure is interaction related.
         */
        private fun onInMobiAdFail(errorMessage: String?,
                                   moPubErrorCode: MoPubErrorCode,
                                   adapterName: String,
                                   loadListener: AdLifecycleListener.LoadListener?,
                                   interactionListener: AdLifecycleListener.InteractionListener?) {
            errorMessage?.let {
                MoPubLog.log(CUSTOM, adapterName, it)
            }

            loadListener?.onAdLoadFailed(moPubErrorCode)
            interactionListener?.onAdFailed(moPubErrorCode)
        }

        init {
            val map: MutableMap<String, String> = HashMap()
            map["tp"] = "c_mopub"
            try {
                val moPubSdkClassRef = Class.forName(MoPub::class.java.name)
                val moPubSdkVersionRef: Field = moPubSdkClassRef.getDeclaredField("SDK_VERSION")
                moPubSdkVersionRef[null]?.let {
                    val moPubSDKVersion = it.toString()
                    map["tp-ver"] = moPubSDKVersion
                }
            } catch (e: Exception) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "InMobiUtils",
                        "Something went wrong while getting the MoPub SDK version", e)
            }
            inMobiTPExtras = map
        }
    }
}
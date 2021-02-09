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
import com.mopub.common.privacy.ConsentStatus
import com.mopub.mobileads.inmobi.BuildConfig
import org.json.JSONException
import org.json.JSONObject
import java.lang.Error
import java.lang.NullPointerException
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
        if (configuration.isNullOrEmpty()) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "InMobi initialization failure. Network configuration map is empty. Cannot parse Account ID value for initialization"
                    + initializationErrorInfo)
            onNetworkInitializationFinishedListener.onNetworkInitializationFinished(InMobiAdapterConfiguration::class.java, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR)
            return
        }

        initialiseInMobi(configuration, context, object : InitCompletionListener {
            override fun onSuccess() {
                onNetworkInitializationFinishedListener.onNetworkInitializationFinished(InMobiAdapterConfiguration::class.java, MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS)
            }

            override fun onFailure(error: Error?, exception: Exception?) {
                exception?.let {
                    MoPubLog.log(AdapterLogEvent.CUSTOM_WITH_THROWABLE, "InMobi initialization failed with an exception. $initializationErrorInfo", it)
                } ?: run {
                    error?.let {
                        MoPubLog.log(CUSTOM, ADAPTER_NAME, "InMobi initialization failure. Reason: ${it.message}")
                    }
                }

                onNetworkInitializationFinishedListener.onNetworkInitializationFinished(InMobiAdapterConfiguration::class.java, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR)
            }
        })
    }

    interface InitCompletionListener {
        fun onSuccess()
        fun onFailure(error: Error?, exception: Exception?)
    }

    companion object {
        const val ACCOUNT_ID_KEY = "accountId"
        const val PLACEMENT_ID_KEY = "placementId"

        val ADAPTER_NAME: String = InMobiAdapterConfiguration::class.java.simpleName
        val initializationErrorInfo = "InMobi will attempt to initialize on the first ad request using server extras values from MoPub UI. " +
                "If you're using InMobi for Advanced Bidding, and initializing InMobi outside and before MoPub, you may disregard this error."
        val inMobiTPExtras: Map<String, String>
        private val accountIdErrorMessage = "Please make sure you provide correct Account ID information on MoPub UI."
        private val placementIdErrorMessage = "Please make sure you provide correct Placement ID information on MoPub UI."

        fun initialiseInMobi(configuration: Map<String, String>, context: Context, initCompletionListener: InitCompletionListener) {
            try {
                val accountId = getAccountId(configuration)
                InMobiSdk.init(context, accountId, null) {
                    if (it == null) {
                        initCompletionListener.onSuccess()
                    } else {
                        initCompletionListener.onFailure(it, null)
                    }
                }
            } catch (e: Exception) {
                initCompletionListener.onFailure(null, e)
            }
        }

        // TODO: Revision needed
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

        fun getAccountId(dict: Map<String, String>): String {
            val accountIdString: String? = dict[ACCOUNT_ID_KEY]
            if (accountIdString.isNullOrEmpty()) {
                throw NullPointerException("InMobi Account ID parameter is null or empty. " +
                        accountIdErrorMessage)
            } else {
                return accountIdString
            }
        }

        fun getPlacementId(dict: Map<String, String>): Long {
            val placementIdString: String? = dict[PLACEMENT_ID_KEY]
            if (placementIdString.isNullOrEmpty()) {
                throw NullPointerException("InMobi Placement ID parameter is null or empty. " +
                        placementIdErrorMessage)
            }

            try {
                val placementIdLong = placementIdString.toLong()
                if (placementIdLong <= 0) {
                    throw Exception("InMobi Placement ID parameter is incorrect, it should be greater than 0. " +
                            placementIdErrorMessage)
                }
                return placementIdLong

            } catch (e: NumberFormatException) {
                throw Exception("InMobi Placement ID parameter is incorrect, cannot cast it to Long, it has to be a long value. " +
                        placementIdErrorMessage)
            }
        }

        /**
         * Call for a load or interaction related failure an error.
         *
         * @param loadListener - Populate only if the failure is load related.
         * @param interactionListener - Populate only if the failure is interaction related.
         */
        fun onInMobiAdFailWithError(error: Exception,
                                    moPubErrorCode: MoPubErrorCode,
                                    errorMessage: String?,
                                    adapterName: String,
                                    loadListener: AdLifecycleListener.LoadListener?,
                                    interactionListener: AdLifecycleListener.InteractionListener?) {
            MoPubLog.log(AdapterLogEvent.CUSTOM_WITH_THROWABLE, error)
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
                val mopubSdkClassRef = Class.forName(MoPub::class.java.name)
                val mopubSdkVersionRef: Field = mopubSdkClassRef.getDeclaredField("SDK_VERSION")
                val moPubSDKVersion = mopubSdkVersionRef[null].toString()
                map["tp-ver"] = moPubSDKVersion
            } catch (e: Exception) {
                MoPubLog.log(MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE, "InMobiUtils",
                        "Something went wrong while getting the MoPub SDK version", e)
            }
            inMobiTPExtras = map
        }
    }
}
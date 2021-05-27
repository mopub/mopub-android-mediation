package com.mopub.nativeads

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import com.inmobi.ads.AdMetaInfo
import com.inmobi.ads.InMobiAdRequestStatus
import com.inmobi.ads.InMobiNative
import com.inmobi.ads.exceptions.SdkNotInitializedException
import com.inmobi.ads.listeners.NativeAdEventListener
import com.inmobi.ads.listeners.VideoEventListener
import com.inmobi.sdk.InMobiSdk
import com.mopub.common.DataKeys
import com.mopub.common.logging.MoPubLog
import com.mopub.common.logging.MoPubLog.AdapterLogEvent
import com.mopub.mobileads.InMobiAdapterConfiguration
import com.mopub.mobileads.InMobiBanner
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class InMobiNative : CustomEventNative() {

    private var inMobiStaticNativeAd: InMobiNativeAd? = null
    private var mPlacementId: Long = 0
    private var mContext: WeakReference<Context>? = null
    private var mNativeListener: CustomEventNativeListener? = null

    override fun loadNativeAd(
        context: Context,
        customEventNativeListener: CustomEventNativeListener,
        localExtras: Map<String, Any>,
        serverExtras: Map<String, String>
    ) {
        mContext = WeakReference(context)
        mNativeListener = customEventNativeListener

        try {
            mPlacementId = InMobiAdapterConfiguration.getPlacementId(serverExtras)
        } catch (placementIdException: InMobiAdapterConfiguration.InMobiPlacementIdException) {
            val errorMsg =
                "InMobi Native request failed. Placement Id is not available or incorrect. Please make sure you set valid Placement Id on MoPub UI."
            onError(customEventNativeListener, errorMsg)
            return
        }

        InMobiAdapterConfiguration.initializeInMobi(
            serverExtras,
            context,
            object : InMobiAdapterConfiguration.InMobiInitCompletionListener {
                override fun onSuccess() {
                    loadNative(serverExtras, customEventNativeListener)
                }

                override fun onFailure(error: Throwable) {
                    val msg = "InMobi interstitial request failed due to InMobi initialization failed with an exception."
                    onError(customEventNativeListener, msg)
                }
            })
    }

    private fun onError(customEventNativeListener: CustomEventNativeListener, errorMsg: String) {
        customEventNativeListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR)
        MoPubLog.log(
            getAdNetworkId(),
            AdapterLogEvent.LOAD_FAILED,
            TAG,
            NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR,
            errorMsg
        )
    }

    private fun getAdNetworkId(): String {
        return mPlacementId.toString() ?: ""
    }

    private fun loadNative(serverExtras: Map<String, String>, customEventNativeListener: CustomEventNativeListener) {
//        InMobiSdk.updateGDPRConsent(InMobiGDPR.getGDPRConsentDictionary())
        InMobiSdk.setLogLevel(InMobiSdk.LogLevel.DEBUG)
        inMobiStaticNativeAd = try {
            val context = mContext?.get()
            if (context == null) {
                MoPubLog.log(AdapterLogEvent.CUSTOM, TAG, "Context passed to the Adapter is null or might have garbage collected")
                customEventNativeListener.onNativeAdFailed(NativeErrorCode.UNSPECIFIED)
                return
            }
            InMobiNativeAd(context, customEventNativeListener, mPlacementId)
        } catch (e: SdkNotInitializedException) {
            MoPubLog.log(AdapterLogEvent.CUSTOM, TAG, "Error while creating InMobiNative Object. " + e.message)
            handleNativeInitializationFailure()
            return
        }
        inMobiStaticNativeAd?.setExtras(InMobiAdapterConfiguration.inMobiTPExtras)

        val adMarkup = serverExtras[DataKeys.ADM_KEY]
        if (adMarkup != null) {
            MoPubLog.log(
                AdapterLogEvent.CUSTOM, InMobiBanner.ADAPTER_NAME,
                "Ad markup for InMobi banner ad request is present. Will make Advanced Bidding ad request " +
                        "using markup: " + adMarkup
            )
            inMobiStaticNativeAd?.loadAd(adMarkup?.toByteArray())
        } else {
            MoPubLog.log(
                AdapterLogEvent.CUSTOM, InMobiBanner.ADAPTER_NAME,
                "Ad markup for InMobi banner ad request is not present. Will make traditional ad request "
            )
            inMobiStaticNativeAd?.loadAd()
        }

        inMobiStaticNativeAd?.let { STATIC_MAP.putIfAbsent(inMobiStaticNativeAd.hashCode(), it) }
    }

    private fun handleNativeInitializationFailure() {
//        InMobiAdapterConfiguration.setInitializationStatus(false)
        mNativeListener?.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR)
    }

    class InMobiNativeAd internal constructor(
        private val mContext: Context,
        private val mCustomEventNativeListener: CustomEventNativeListener,
        placementId: Long
    ) : BaseNativeAd() {

        private val mNativeClickHandler: NativeClickHandler = NativeClickHandler(mContext)
        private val mInMobiNative: InMobiNative
        private var mIsImpressionRecorded = false
        private var mIsClickRecorded = false

        private val nativeAdEventListener: NativeAdEventListener = object : NativeAdEventListener() {
            override fun onAdLoadSucceeded(ad: InMobiNative, info: AdMetaInfo) {
                MoPubLog.log(AdapterLogEvent.CUSTOM, TAG, "InMobi Native Ad loaded successfully")
                val imageUrls: MutableList<String> = ArrayList()
                val iconImageUrl = adIconUrl
                imageUrls.add(iconImageUrl)

                NativeImageHelper.preCacheImages(mContext, imageUrls, object : NativeImageHelper.ImageListener {
                    override fun onImagesCached() {
                        mCustomEventNativeListener.onNativeAdLoaded(this@InMobiNativeAd)
                    }

                    override fun onImagesFailedToCache(errorCode: NativeErrorCode) {
                        mCustomEventNativeListener.onNativeAdFailed(errorCode)
                    }
                })
                STATIC_MAP.remove(this.hashCode())
            }

            override fun onAdLoadFailed(
                inMobiNative: InMobiNative,
                inMobiAdRequestStatus: InMobiAdRequestStatus
            ) {
                super.onAdLoadFailed(inMobiNative, inMobiAdRequestStatus)
                var errorMessage = "Failed to load Native Strand:"
                when (inMobiAdRequestStatus.statusCode) {
                    InMobiAdRequestStatus.StatusCode.INTERNAL_ERROR -> {
                        errorMessage += "INTERNAL_ERROR"
                        mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_INVALID_STATE)
                    }
                    InMobiAdRequestStatus.StatusCode.REQUEST_INVALID -> {
                        errorMessage += "INVALID_REQUEST"
                        mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_INVALID_REQUEST)
                    }
                    InMobiAdRequestStatus.StatusCode.NETWORK_UNREACHABLE -> {
                        errorMessage += "NETWORK_UNREACHABLE"
                        mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.CONNECTION_ERROR)
                    }
                    InMobiAdRequestStatus.StatusCode.NO_FILL -> {
                        errorMessage += "NO_FILL"
                        mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_NO_FILL)
                    }
                    InMobiAdRequestStatus.StatusCode.REQUEST_PENDING -> {
                        errorMessage += "REQUEST_PENDING"
                        mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.UNSPECIFIED)
                    }
                    InMobiAdRequestStatus.StatusCode.REQUEST_TIMED_OUT -> {
                        errorMessage += "REQUEST_TIMED_OUT"
                        mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_TIMEOUT)
                    }
                    InMobiAdRequestStatus.StatusCode.SERVER_ERROR -> {
                        errorMessage += "SERVER_ERROR"
                        mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.SERVER_ERROR_RESPONSE_CODE)
                    }
                    InMobiAdRequestStatus.StatusCode.AD_ACTIVE -> {
                        errorMessage += "AD_ACTIVE"
                        mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.UNSPECIFIED)
                    }
                    InMobiAdRequestStatus.StatusCode.EARLY_REFRESH_REQUEST -> {
                        errorMessage += "EARLY_REFRESH_REQUEST"
                        mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.UNSPECIFIED)
                    }
                    else -> {
                        errorMessage = "UNKNOWN_ERROR" + inMobiAdRequestStatus.statusCode
                        mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.UNSPECIFIED)
                    }
                }
                MoPubLog.log(MoPubLog.SdkLogEvent.ERROR, TAG, errorMessage)
                STATIC_MAP.remove(this.hashCode())
                destroy()
            }

            override fun onAdFullScreenDismissed(inMobiNative: InMobiNative) {
                super.onAdFullScreenDismissed(inMobiNative)
                MoPubLog.log(AdapterLogEvent.CUSTOM, TAG, "InMobi Native ad onAdFullScreenDismissed")
            }

            override fun onAdFullScreenWillDisplay(inMobiNative: InMobiNative) {
                super.onAdFullScreenWillDisplay(inMobiNative)
                MoPubLog.log(AdapterLogEvent.CUSTOM, TAG, "InMobi Native ad onAdFullScreenWillDisplay")
            }

            override fun onAdFullScreenDisplayed(inMobiNative: InMobiNative) {
                super.onAdFullScreenDisplayed(inMobiNative)
                MoPubLog.log(AdapterLogEvent.CUSTOM, TAG, "InMobi Native ad onAdFullScreenDisplayed")
            }

            override fun onUserWillLeaveApplication(inMobiNative: InMobiNative) {
                super.onUserWillLeaveApplication(inMobiNative)
                MoPubLog.log(AdapterLogEvent.CUSTOM, TAG, "InMobi Native ad onUserWillLeaveApplication")
            }

            override fun onAdImpressed(inMobiNative: InMobiNative) {
                super.onAdImpressed(inMobiNative)
                MoPubLog.log(AdapterLogEvent.CUSTOM, TAG, "InMobi Native ad is displayed")
                if (!mIsImpressionRecorded) {
                    mIsImpressionRecorded = true
                    notifyAdImpressed()
                }
            }

            override fun onAdClicked(inMobiNative: InMobiNative) {
                super.onAdClicked(inMobiNative)
                MoPubLog.log(AdapterLogEvent.CUSTOM, TAG, "InMobi Native ad is clicked")
                if (!mIsClickRecorded) {
                    notifyAdClicked()
                    mIsClickRecorded = true
                }
            }

            override fun onAdStatusChanged(inMobiNative: InMobiNative) {
                super.onAdStatusChanged(inMobiNative)
                MoPubLog.log(AdapterLogEvent.CUSTOM, TAG, "InMobi Native ad onAdStatusChanged")
            }
        }

        fun setExtras(map: Map<String, String>?) {
            mInMobiNative.setExtras(map)
        }

        fun loadAd(response: ByteArray? = null) {
            mInMobiNative.setVideoEventListener(object : VideoEventListener() {
                override fun onVideoCompleted(inMobiNative: InMobiNative) {
                    super.onVideoCompleted(inMobiNative)
                    MoPubLog.log(AdapterLogEvent.CUSTOM, TAG, "InMobi Native Video completed")
                }

                override fun onVideoSkipped(inMobiNative: InMobiNative) {
                    super.onVideoSkipped(inMobiNative)
                    MoPubLog.log(AdapterLogEvent.CUSTOM, TAG, "InMobi Native Video skipped")
                }

                override fun onAudioStateChanged(inMobiNative: InMobiNative, b: Boolean) {
                    super.onAudioStateChanged(inMobiNative, b)
                    MoPubLog.log(AdapterLogEvent.CUSTOM, TAG, "InMobi Native Video onAudioStateChanged")
                }
            })

            response?.let { mInMobiNative.load(it) } ?: mInMobiNative.load()
        }

        /**
         * Returns the String corresponding to the ad's title.
         */
        val adTitle: String
            get() = mInMobiNative.adTitle

        /**
         * Returns the String corresponding to the ad's description text. May be null.
         */
        val adDescription: String
            get() = mInMobiNative.adDescription

        /**
         * Returns the String url corresponding to the ad's icon image. May be null.
         */
        val adIconUrl: String
            get() = mInMobiNative.adIconUrl

        /**
         * Returns the Call To Action String (i.e. "Install" or "Learn More") associated with this ad.
         */
        val adCtaText: String
            get() = mInMobiNative.adCtaText

        val adRating: Float
            get() = mInMobiNative.adRating

        fun getPrimaryAdView(parent: ViewGroup): View {
            val width =
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, parent.width.toFloat(), mContext.resources.displayMetrics).toInt()
            return mInMobiNative.getPrimaryViewOfWidth(parent.context, null, parent, width)
        }

        fun onCtaClick() {
            mInMobiNative.reportAdClickAndOpenLandingPage()
        }

        override fun clear(view: View) {
            mNativeClickHandler.clearOnClickListener(view)
        }

        override fun destroy() {
            Handler(Looper.getMainLooper()).post { mInMobiNative.destroy() }
        }

        override fun prepare(view: View) {}

        companion object {
            private const val TAG = "InMobiNativeAd"
        }

        init {
            mInMobiNative = InMobiNative(mContext, placementId, nativeAdEventListener)
        }
    }

    companion object {
        val TAG: String = com.mopub.nativeads.InMobiNative::class.java.simpleName
        private val STATIC_MAP = ConcurrentHashMap<Int, InMobiNativeAd>(10, 0.9f, 10)
    }
}
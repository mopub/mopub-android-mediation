package com.mopub.mobileads

import android.app.Activity
import android.content.Context
import com.inmobi.ads.AdMetaInfo
import com.inmobi.ads.InMobiAdRequestStatus
import com.inmobi.ads.InMobiInterstitial
import com.inmobi.ads.exceptions.SdkNotInitializedException
import com.inmobi.ads.listeners.InterstitialAdEventListener
import com.mopub.common.DataKeys
import com.mopub.common.LifecycleListener
import com.mopub.common.logging.MoPubLog
import com.mopub.common.logging.MoPubLog.AdapterLogEvent
import com.mopub.mobileads.InMobiAdapterConfiguration.Companion.onInMobiAdFailWithError
import com.mopub.mobileads.InMobiAdapterConfiguration.Companion.onInMobiAdFailWithEvent
import com.mopub.mobileads.InMobiAdapterConfiguration.InMobiPlacementIdException

class InMobiInterstitial : BaseAd() {

    companion object {
        val ADAPTER_NAME: String = InMobiInterstitial::class.java.simpleName
    }

    private var mPlacementId: Long? = null
    private var mInMobiInterstitial: InMobiInterstitial? = null

    override fun onInvalidate() {
        MoPubLog.log(AdapterLogEvent.CUSTOM, ADAPTER_NAME, "InMobi interstitial destroyed")
        if (mInMobiInterstitial != null) {
            mInMobiInterstitial = null
        }
    }

    override fun getLifecycleListener(): LifecycleListener? {
        return null
    }

    override fun getAdNetworkId(): String {
        return mPlacementId?.toString() ?: ""
    }

    override fun checkAndInitializeSdk(launcherActivity: Activity, adData: AdData): Boolean {
        return false
    }

    override fun load(context: Context, adData: AdData) {
        setAutomaticImpressionAndClickTracking(false)
        val extras: Map<String, String> = adData.extras

        try {
            mPlacementId = InMobiAdapterConfiguration.getPlacementId(extras)
        } catch (placementIdException: InMobiPlacementIdException) {
            onInMobiAdFailWithError(placementIdException, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR,
                    "InMobi interstitial request failed. Placement Id is not available or incorrect. " +
                            "Please make sure you set valid Placement Id on MoPub UI.",
                    ADAPTER_NAME, mLoadListener, null)
            return
        }

        InMobiAdapterConfiguration.initializeInMobi(extras, context, object : InMobiAdapterConfiguration.InMobiInitCompletionListener {
            override fun onSuccess() {
                loadInterstitial(context, extras)
            }

            override fun onFailure(error: Throwable) {
                onInMobiAdFailWithError(error, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR,
                        "InMobi interstitial request failed due to InMobi initialization failed with an exception.",
                        ADAPTER_NAME, mLoadListener, null)
            }
        })
    }

    private fun loadInterstitial(context: Context, extras: Map<String, String>) {

        try {
            mInMobiInterstitial = InMobiInterstitial(context, mPlacementId!!, object : InterstitialAdEventListener() {

                override fun onAdLoadSucceeded(ad: InMobiInterstitial, info: AdMetaInfo) {
                    MoPubLog.log(adNetworkId, AdapterLogEvent.LOAD_SUCCESS, ADAPTER_NAME)
                    mLoadListener?.onAdLoaded()
                }

                override fun onAdLoadFailed(inMobiInterstitial: InMobiInterstitial,
                                            inMobiAdRequestStatus: InMobiAdRequestStatus) {
                    onInMobiAdFailWithEvent(AdapterLogEvent.LOAD_FAILED, adNetworkId,
                            InMobiAdapterConfiguration.getMoPubErrorCode(inMobiAdRequestStatus.statusCode),
                            "InMobi interstitial request failed " +
                                    "with message: ${inMobiAdRequestStatus.message} " +
                                    "and status code: ${inMobiAdRequestStatus.statusCode}.",
                            ADAPTER_NAME, mLoadListener, null)
                }

                override fun onAdWillDisplay(inMobiInterstitial: InMobiInterstitial) {
                    MoPubLog.log(adNetworkId, AdapterLogEvent.WILL_APPEAR, ADAPTER_NAME)
                }

                override fun onAdDisplayed(ad: InMobiInterstitial, info: AdMetaInfo) {
                    MoPubLog.log(adNetworkId, AdapterLogEvent.CUSTOM, ADAPTER_NAME,
                            "InMobi interstitial ad displayed")
                    MoPubLog.log(adNetworkId, AdapterLogEvent.SHOW_SUCCESS, ADAPTER_NAME)
                    mInteractionListener?.onAdShown()
                    mInteractionListener?.onAdImpression()
                }

                override fun onAdClicked(inMobiInterstitial: InMobiInterstitial,
                                         params: Map<Any?, Any?>?) {
                    MoPubLog.log(adNetworkId, AdapterLogEvent.CLICKED, ADAPTER_NAME)
                    mInteractionListener?.onAdClicked()
                }

                override fun onAdDisplayFailed(inMobiInterstitial: InMobiInterstitial) {
                    onInMobiAdFailWithEvent(AdapterLogEvent.SHOW_FAILED, adNetworkId,
                            MoPubErrorCode.FULLSCREEN_SHOW_ERROR,
                            "InMobi interstitial show failed",
                            ADAPTER_NAME, null, mInteractionListener)
                }

                override fun onAdDismissed(inMobiInterstitial: InMobiInterstitial) {
                    MoPubLog.log(adNetworkId, AdapterLogEvent.CUSTOM, ADAPTER_NAME,
                            "InMobi interstitial ad dismissed")
                    MoPubLog.log(adNetworkId, AdapterLogEvent.DID_DISAPPEAR, ADAPTER_NAME)
                    mInteractionListener?.onAdDismissed()
                }
            })

            MoPubLog.log(adNetworkId, AdapterLogEvent.LOAD_ATTEMPTED, ADAPTER_NAME)
            val adMarkup = extras[DataKeys.ADM_KEY]
            if (adMarkup != null) {
                MoPubLog.log(AdapterLogEvent.CUSTOM, ADAPTER_NAME,
                        "Ad markup for InMobi interstitial ad request is present. Will make Advanced Bidding ad request " +
                                "using markup: " + adMarkup)
                mInMobiInterstitial!!.load(adMarkup.toByteArray())

            } else {
                MoPubLog.log(AdapterLogEvent.CUSTOM, ADAPTER_NAME,
                        "Ad markup for InMobi interstitial ad request is not present. Will make traditional ad request ")
                mInMobiInterstitial!!.setExtras(InMobiAdapterConfiguration.inMobiTPExtras)
                mInMobiInterstitial!!.load()
            }

        } catch (inMobiSdkNotInitializedException: SdkNotInitializedException) {
            onInMobiAdFailWithEvent(AdapterLogEvent.LOAD_FAILED, adNetworkId, MoPubErrorCode.NETWORK_INVALID_STATE,
                    "Attempting to create InMobi interstitial object before InMobi SDK is initialized caused failure" +
                            "Please make sure InMobi is properly initialized. InMobi will attempt to initialize on next ad request.",
                    ADAPTER_NAME, mLoadListener, null)
        } catch (e: Exception) {
            onInMobiAdFailWithError(e, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR,
                    "InMobi interstitial failed due to a configuration issue",
                    ADAPTER_NAME, mLoadListener, null)
        }
    }

    override fun show() {
        MoPubLog.log(adNetworkId, AdapterLogEvent.SHOW_ATTEMPTED, ADAPTER_NAME)

        if (mInMobiInterstitial?.isReady == true) {
            mInMobiInterstitial?.show()
        } else {
            onInMobiAdFailWithEvent(AdapterLogEvent.SHOW_FAILED, adNetworkId,
                    MoPubErrorCode.FULLSCREEN_SHOW_ERROR,
                    "InMobi interstitial show failed, because InMobi interstitial is not ready yet. " +
                            "Please ensure interstitial is loaded first.",
                    ADAPTER_NAME, null, mInteractionListener)
        }
    }
}
package com.mopub.mobileads

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.FrameLayout
import com.inmobi.ads.AdMetaInfo
import com.inmobi.ads.InMobiAdRequestStatus
import com.inmobi.ads.InMobiBanner
import com.inmobi.ads.exceptions.SdkNotInitializedException
import com.inmobi.ads.listeners.BannerAdEventListener
import com.mopub.common.DataKeys
import com.mopub.common.LifecycleListener
import com.mopub.common.logging.MoPubLog
import com.mopub.common.logging.MoPubLog.AdapterLogEvent
import com.mopub.mobileads.InMobiAdapterConfiguration.Companion.onInMobiAdFailWithError
import com.mopub.mobileads.InMobiAdapterConfiguration.Companion.onInMobiAdFailWithEvent
import com.mopub.mobileads.InMobiAdapterConfiguration.InMobiPlacementIdException
import kotlin.math.roundToInt

class InMobiBanner : BaseAd() {

    companion object {
        val ADAPTER_NAME: String = InMobiBanner::class.java.simpleName
    }

    private var mPlacementId: Long? = null
    private var mInMobiBanner: InMobiBanner? = null
    private var mInMobiBannerContainer: FrameLayout? = null

    override fun onInvalidate() {
        MoPubLog.log(AdapterLogEvent.CUSTOM, ADAPTER_NAME, "InMobi banner destroyed")
        mInMobiBanner?.destroy()
        mInMobiBanner = null
        mInMobiBannerContainer = null
    }

    override fun getAdView(): View? {
        return mInMobiBannerContainer
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
        setAutomaticImpressionAndClickTracking(true)
        val extras: Map<String, String> = adData.extras

        try {
            mPlacementId = InMobiAdapterConfiguration.getPlacementId(extras)
        } catch (placementIdException: InMobiPlacementIdException) {
                onInMobiAdFailWithError(placementIdException, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR,
                        "InMobi banner request failed. Placement Id is not available or incorrect. " +
                                "Please make sure you set valid Placement Id on MoPub UI.",
                        ADAPTER_NAME, mLoadListener, null)
            return
        }

        InMobiAdapterConfiguration.initializeInMobi(extras, context, object : InMobiAdapterConfiguration.InMobiInitCompletionListener {
            override fun onSuccess() {
                loadBanner(context, adData, extras)
            }

            override fun onFailure(error: Throwable) {
                onInMobiAdFailWithError(error, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR,
                        "InMobi banner request failed due to InMobi initialization failed with an exception.",
                        ADAPTER_NAME, mLoadListener, null)
            }
        })
    }

    private fun loadBanner(context: Context, adData: AdData, extras: Map<String, String>) {
        val (adWidth, adHeight) = getAdSize(adData) ?: kotlin.run {
            MoPubLog.log(AdapterLogEvent.CUSTOM, ADAPTER_NAME, "Failing InMobi banner ad request. Ad size data incorrect.")
            return
        }

        try {
            mInMobiBanner = InMobiBanner(context, mPlacementId!!)
            mInMobiBanner!!.run {
                setListener(object : BannerAdEventListener() {

                    override fun onAdLoadSucceeded(inMobiBanner: InMobiBanner, info: AdMetaInfo) {
                        MoPubLog.log(adNetworkId, AdapterLogEvent.LOAD_SUCCESS, ADAPTER_NAME)
                        mLoadListener?.onAdLoaded()
                    }

                    override fun onAdLoadFailed(inMobiBanner: InMobiBanner,
                                                inMobiAdRequestStatus: InMobiAdRequestStatus) {
                        onInMobiAdFailWithEvent(AdapterLogEvent.LOAD_FAILED, adNetworkId,
                                InMobiAdapterConfiguration.getMoPubErrorCode(inMobiAdRequestStatus.statusCode),
                                "InMobi banner request failed " +
                                        "with message: ${inMobiAdRequestStatus.message} " +
                                        "and status code: ${inMobiAdRequestStatus.statusCode}.",
                                ADAPTER_NAME, mLoadListener, null)
                    }

                    override fun onAdClicked(inMobiBanner: InMobiBanner, map: Map<Any, Any>) {
                        MoPubLog.log(adNetworkId, AdapterLogEvent.CLICKED, ADAPTER_NAME)
                        mInteractionListener?.onAdClicked()
                    }

                    // Fired when anything fullscreen is opened in the app (expanded, in-app browser etc.)
                    // after the user clicks on the banner ad
                    override fun onAdDisplayed(inMobiBanner: InMobiBanner) {
                        MoPubLog.log(adNetworkId, AdapterLogEvent.CUSTOM, ADAPTER_NAME,
                                "InMobi banner ad will open / expand ad")
                        mInteractionListener?.onAdExpanded()
                    }

                    override fun onAdDismissed(inMobiBanner: InMobiBanner) {
                        MoPubLog.log(adNetworkId, AdapterLogEvent.CUSTOM, ADAPTER_NAME,
                                "InMobi banner ad will close / dismiss ad")
                        mInteractionListener?.onAdCollapsed()
                    }

                    override fun onUserLeftApplication(inMobiBanner: InMobiBanner) {
                        MoPubLog.log(adNetworkId, AdapterLogEvent.WILL_LEAVE_APPLICATION, ADAPTER_NAME)
                    }
                })

                mInMobiBannerContainer = FrameLayout(context)
                mInMobiBannerContainer?.addView(this)

                setEnableAutoRefresh(false)
                setAnimationType(InMobiBanner.AnimationType.ANIMATION_OFF)

                val dm = resources.displayMetrics

                layoutParams = FrameLayout.LayoutParams(
                        (adWidth * dm.density).roundToInt(),
                        (adHeight * dm.density).roundToInt())

                MoPubLog.log(adNetworkId, AdapterLogEvent.LOAD_ATTEMPTED, ADAPTER_NAME)
                val adMarkup = extras[DataKeys.ADM_KEY]
                if (adMarkup != null) {
                    MoPubLog.log(AdapterLogEvent.CUSTOM, ADAPTER_NAME,
                            "Ad markup for InMobi banner ad request is present. Will make Advanced Bidding ad request " +
                                    "using markup: " + adMarkup)
                    load(adMarkup.toByteArray())
                } else {
                    setExtras(InMobiAdapterConfiguration.inMobiTPExtras)
                    MoPubLog.log(AdapterLogEvent.CUSTOM, ADAPTER_NAME,
                            "Ad markup for InMobi banner ad request is not present. Will make traditional ad request ")
                    load()
                }
            }
            
        } catch (inMobiSdkNotInitializedException: SdkNotInitializedException) {
            onInMobiAdFailWithEvent(AdapterLogEvent.LOAD_FAILED, adNetworkId, MoPubErrorCode.NETWORK_INVALID_STATE,
                    "InMobi banner request failed. InMobi SDK is not initialized yet." +
                            "InMobi SDK will attempt to initialize on next ad request.",
                    ADAPTER_NAME, mLoadListener, null)
        } catch (e: Exception) {
            onInMobiAdFailWithError(e, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR,
                    "InMobi banner failed due to a configuration issue",
                    ADAPTER_NAME, mLoadListener, null)
        }
    }

    private fun getAdSize(adData: AdData): Pair<Int, Int>? {
        if (adData.adWidth == null || adData.adHeight == null) {
            onInMobiAdFailWithEvent(AdapterLogEvent.LOAD_FAILED, adNetworkId, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR,
                    "InMobi banner request failed. Banner Width and Height not provided. Please provide a width and height.",
                    ADAPTER_NAME, mLoadListener, null)
            return null
        }

        if (adData.adWidth == 0 || adData.adHeight == 0) {
            onInMobiAdFailWithEvent(AdapterLogEvent.LOAD_FAILED, adNetworkId, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR,
                    "InMobi banner request failed. Width or Height is 0. Please provide a valid width and height.",
                    ADAPTER_NAME, mLoadListener, null)
            return null
        }

        return Pair(adData.adWidth!!, adData.adHeight!!)
    }
}
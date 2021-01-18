package com.mopub.mobileads

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import com.mopub.utils.Utils
import com.inmobi.ads.AdMetaInfo
import com.inmobi.ads.InMobiAdRequestStatus
import com.inmobi.ads.InMobiBanner
import com.inmobi.ads.exceptions.SdkNotInitializedException
import com.inmobi.ads.listeners.BannerAdEventListener
import com.mopub.common.DataKeys
import com.mopub.common.LifecycleListener
import com.mopub.common.logging.MoPubLog
import kotlin.math.roundToInt

class InMobiBannerAd : BaseAd() {

    private var mPlacementId: Long = -1
    private var mAdWidth: Int = -1
    private var mAdHeight: Int = -1
    private var mInMobiBanner: InMobiBanner? = null
    private var mInMobiBannerContainer: FrameLayout? = null

    override fun onInvalidate() {
        MoPubLog.log(MoPubLog.AdapterLogEvent.CUSTOM, TAG, "InMobi Banner destroyed")
        mInMobiBanner?.destroy()
        mInMobiBanner = null
    }

    override fun getLifecycleListener(): LifecycleListener? {
        return null
    }

    override fun getAdNetworkId(): String {
        return mPlacementId.toString()
    }

    override fun checkAndInitializeSdk(launcherActivity: Activity, adData: AdData): Boolean {
        return false
    }

    override fun load(context: Context, adData: AdData) {
        val extras: Map<String, String> = adData.extras
        val placementIdStr: String? = extras[PLACEMENT_ID_KEY]
        if (placementIdStr.isNullOrEmpty()) {
            submitAdLoadFailed("InMobi Placement id not available to load an ad.",
                    MoPubErrorCode.NETWORK_INVALID_STATE)
            return
        }

        try {
            mPlacementId = placementIdStr.toLong()
        } catch (e: NumberFormatException) {
            submitAdLoadFailed("InMobi Placement id not valid to load an ad. ${e.message}",
                    MoPubErrorCode.NETWORK_INVALID_STATE)
            return
        }

        adData.adWidth?.let {
            mAdWidth = it
        }
        adData.adHeight?.let {
            mAdHeight = it
        }

        if (mAdWidth <= 0 || mAdHeight <= 0) {
            submitAdLoadFailed("Invalid ad size.", MoPubErrorCode.INTERNAL_ERROR)
            return
        }

        try {
            val adMarkup = extras[DataKeys.ADM_KEY]
            if (adMarkup == null) {
                submitAdLoadFailed("Ad Markup is missing in the response.",
                        MoPubErrorCode.INTERNAL_ERROR)
                return
            }

            mInMobiBanner = InMobiBanner(context, mPlacementId)
            mInMobiBanner!!.run {
                setListener(object : BannerAdEventListener() {
                    override fun onAdLoadSucceeded(inMobiBanner: InMobiBanner, info: AdMetaInfo) {
                        MoPubLog.log(MoPubLog.AdapterLogEvent.CUSTOM, TAG,
                                "InMobi Banner loaded successfully.")
                        MoPubLog.log(MoPubLog.AdapterLogEvent.LOAD_SUCCESS, TAG)
                        mLoadListener?.onAdLoaded()
                        mInteractionListener?.onAdImpression()
                    }

                    override fun onAdLoadFailed(inMobiBanner: InMobiBanner,
                                                inMobiAdRequestStatus: InMobiAdRequestStatus) {
                        submitAdLoadFailed("InMobi Banner ad failed to load. " +
                                "\nError code: ${inMobiAdRequestStatus.statusCode}" +
                                " \nError message: ${inMobiAdRequestStatus.message}",
                                Utils.getMoPubErrorCode(inMobiAdRequestStatus.statusCode))
                    }

                    override fun onAdClicked(inMobiBanner: InMobiBanner, map: Map<Any, Any>) {
                        MoPubLog.log(MoPubLog.AdapterLogEvent.CUSTOM, TAG,
                                "InMobi Banner Ad Clicked")
                        MoPubLog.log(MoPubLog.AdapterLogEvent.CLICKED, TAG)
                        mInteractionListener?.onAdClicked()
                    }

                    override fun onAdDisplayed(inMobiBanner: InMobiBanner) {
                        MoPubLog.log(MoPubLog.AdapterLogEvent.CUSTOM, TAG,
                                "InMobi Banner Ad displayed")
                        mInteractionListener?.onAdExpanded()
                    }

                    override fun onAdDismissed(inMobiBanner: InMobiBanner) {
                        MoPubLog.log(MoPubLog.AdapterLogEvent.CUSTOM, TAG,
                                "InMobi Banner Ad Dismissed")
                        mInteractionListener?.onAdCollapsed()
                    }

                    override fun onUserLeftApplication(inMobiBanner: InMobiBanner) {
                        MoPubLog.log(MoPubLog.AdapterLogEvent.CUSTOM, TAG, "User left application")
                        MoPubLog.log(MoPubLog.AdapterLogEvent.WILL_LEAVE_APPLICATION, TAG)
                    }
                })

                mInMobiBannerContainer = FrameLayout(context)
                mInMobiBannerContainer?.addView(this)

                setEnableAutoRefresh(false)
                setAnimationType(InMobiBanner.AnimationType.ANIMATION_OFF)

                val dm = resources.displayMetrics;

                setExtras(Utils.extras)

                layoutParams = FrameLayout.LayoutParams((mAdWidth * dm.density).roundToInt(),
                        (mAdHeight * dm.density).roundToInt())

                load(adMarkup.toByteArray())
            }
        } catch (sdkNotInitExe: SdkNotInitializedException) {
            submitAdLoadFailed("Please initialise InMobi SDK.", MoPubErrorCode.INTERNAL_ERROR)
        } catch (exception: Exception) {
            submitAdLoadFailed("Something went wrong ${exception.message}",
                    MoPubErrorCode.INTERNAL_ERROR)
        }
    }

    private fun submitAdLoadFailed(msg: String, errorCode: MoPubErrorCode) {
        MoPubLog.log(MoPubLog.AdapterLogEvent.CUSTOM, TAG, msg)
        MoPubLog.log(MoPubLog.AdapterLogEvent.LOAD_FAILED, TAG, errorCode.intCode, errorCode)
        mLoadListener?.onAdLoadFailed(errorCode)
    }

    override fun getAdView(): View? {
        return mInMobiBannerContainer
    }

    companion object {
        private const val PLACEMENT_ID_KEY = "placementId"
        private val TAG = InMobiBannerAd::class.java.simpleName
    }
}
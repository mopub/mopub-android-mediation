package com.mopub.mobileads

import android.app.Activity
import android.content.Context
import com.mopub.utils.Utils
import com.inmobi.ads.AdMetaInfo
import com.inmobi.ads.InMobiAdRequestStatus
import com.inmobi.ads.InMobiInterstitial
import com.inmobi.ads.exceptions.SdkNotInitializedException
import com.inmobi.ads.listeners.InterstitialAdEventListener
import com.mopub.common.DataKeys
import com.mopub.common.LifecycleListener
import com.mopub.common.MoPubReward
import com.mopub.common.logging.MoPubLog

open class InMobiInterstitialAd : BaseAd() {

    private var mPlacementId: Long = -1
    private var mInMobiInterstitial: InMobiInterstitial? = null

    override fun onInvalidate() {
        mInMobiInterstitial = null
    }

    override fun getLifecycleListener(): LifecycleListener? {
        return null
    }

    override fun getAdNetworkId(): String {
        return mPlacementId.toString();
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

        try {
            val adMarkup = extras[DataKeys.ADM_KEY]
            if (adMarkup == null) {
                submitAdLoadFailed("Ad Markup is missing in the response.",
                        MoPubErrorCode.INTERNAL_ERROR)
                return
            }

            mInMobiInterstitial = InMobiInterstitial(context, mPlacementId,
                    object : InterstitialAdEventListener() {
                        override fun onAdLoadSucceeded(ad: InMobiInterstitial, info: AdMetaInfo) {
                            MoPubLog.log(MoPubLog.AdapterLogEvent.CUSTOM, TAG,
                                    "InMobi interstitial ad loaded successfully.")
                            MoPubLog.log(MoPubLog.AdapterLogEvent.LOAD_SUCCESS, TAG)
                            mLoadListener?.onAdLoaded()
                        }

                        override fun onAdLoadFailed(inMobiInterstitial: InMobiInterstitial,
                                                    inMobiAdRequestStatus: InMobiAdRequestStatus) {
                            submitAdLoadFailed("InMobi interstitial ad failed to load." +
                                    "\nError code: ${inMobiAdRequestStatus.statusCode}" +
                                    "\nError message: ${inMobiAdRequestStatus.message}",
                                    Utils.getMoPubErrorCode(inMobiAdRequestStatus.statusCode))
                        }

                        override fun onAdClicked(inMobiInterstitial: InMobiInterstitial,
                                                 params: Map<Any?, Any?>?) {
                            MoPubLog.log(MoPubLog.AdapterLogEvent.CUSTOM, TAG,
                                    "InMobi interstitial interaction happening.")
                            MoPubLog.log(MoPubLog.AdapterLogEvent.CLICKED, TAG)
                            mInteractionListener?.onAdClicked()
                        }

                        override fun onAdWillDisplay(inMobiInterstitial: InMobiInterstitial) {
                            MoPubLog.log(MoPubLog.AdapterLogEvent.CUSTOM, TAG,
                                    "Interstitial ad will display.")
                            MoPubLog.log(MoPubLog.AdapterLogEvent.WILL_APPEAR, TAG)
                        }

                        override fun onAdDisplayed(ad: InMobiInterstitial, info: AdMetaInfo) {
                            MoPubLog.log(MoPubLog.AdapterLogEvent.CUSTOM, TAG,
                                    "InMobi interstitial show on screen.")
                            MoPubLog.log(MoPubLog.AdapterLogEvent.DID_APPEAR, TAG)
                            mInteractionListener?.onAdShown()
                            mInteractionListener?.onAdImpression()
                        }

                        override fun onAdDisplayFailed(inMobiInterstitial: InMobiInterstitial) {
                            MoPubLog.log(MoPubLog.AdapterLogEvent.CUSTOM, TAG,
                                    "Interstitial ad failed to display.")
                            MoPubLog.log(MoPubLog.AdapterLogEvent.SHOW_FAILED, TAG)
                        }

                        override fun onAdDismissed(inMobiInterstitial: InMobiInterstitial) {
                            MoPubLog.log(MoPubLog.AdapterLogEvent.CUSTOM, TAG,
                                    "Interstitial ad dismissed.")
                            MoPubLog.log(MoPubLog.AdapterLogEvent.DID_DISAPPEAR, TAG)
                            mInteractionListener?.onAdDismissed()
                        }

                        override fun onRewardsUnlocked(inMobiInterstitial: InMobiInterstitial,
                                                       rewards: Map<Any, Any>?) {
                            MoPubLog.log(MoPubLog.AdapterLogEvent.CUSTOM, TAG,
                                    "InMobi interstitial onRewardsUnlocked.")

                            rewards?.let {
                                val iterator: Iterator<Any> = it.keys.iterator()
                                var key: String = ""
                                var value: Int? = null
                                try {
                                    if (iterator.hasNext()) {
                                        key = iterator.next().toString()
                                        value = it[key].toString().toInt()
                                        MoPubLog.log(MoPubLog.AdapterLogEvent.CUSTOM,
                                                "Rewards: ", "$key:$value")
                                    }
                                } catch (e: Exception) {
                                    MoPubLog.log(MoPubLog.AdapterLogEvent.CUSTOM, TAG,
                                            "Error while parsing InMobi rewards ${e.message}")
                                }
                                if (value != null) {
                                    MoPubLog.log(MoPubLog.AdapterLogEvent.SHOULD_REWARD, TAG,
                                            key, value)
                                    mInteractionListener?.onAdComplete(
                                            MoPubReward.success(key, value))
                                } else {
                                    mInteractionListener?.onAdComplete(MoPubReward.failure())
                                }
                            }
                        }
                    })

            mInMobiInterstitial?.run {
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

    companion object {
        const val PLACEMENT_ID_KEY = "placementId"
        val TAG = InMobiInterstitialAd::class.java.simpleName
    }
}
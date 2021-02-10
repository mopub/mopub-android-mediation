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
import com.mopub.common.MoPubReward
import com.mopub.common.logging.MoPubLog
import com.mopub.common.logging.MoPubLog.AdapterLogEvent
import com.mopub.mobileads.InMobiAdapterConfiguration.Companion.onInMobiAdFailWithError
import com.mopub.mobileads.InMobiAdapterConfiguration.Companion.onInMobiAdFailWithEvent

class InMobiRewardedVideo : BaseAd() {

    companion object {
        val ADAPTER_NAME: String = InMobiRewardedVideo::class.java.simpleName
    }

    private var mPlacementId: Long? = null
    private var mInMobiRewardedVideo: InMobiInterstitial? = null

    override fun onInvalidate() {
        MoPubLog.log(AdapterLogEvent.CUSTOM, ADAPTER_NAME, "InMobi rewarded video destroyed")
        if (mInMobiRewardedVideo != null) {
            mInMobiRewardedVideo = null
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

        InMobiAdapterConfiguration.initialiseInMobi(extras, context, object : InMobiAdapterConfiguration.InitCompletionListener {
            override fun onSuccess() {
                loadRewarded(context, adData, extras)
            }

            override fun onFailure(error: Error?, exception: Exception?) {
                exception?.let {
                    onInMobiAdFailWithError(it, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR,
                            "InMobi banner request failed due to InMobi initialization failed with an exception.",
                            InMobiBanner.ADAPTER_NAME, mLoadListener, null)
                } ?: run {
                    error?.let {
                        onInMobiAdFailWithEvent(AdapterLogEvent.LOAD_FAILED, adNetworkId, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR,
                                "InMobi banner request failed due to InMobi initialization failed with a reason: ${error.message}",
                                com.mopub.mobileads.InMobiBanner.ADAPTER_NAME, mLoadListener, null)
                    }
                }
            }
        })
    }

    private fun loadRewarded(context: Context, adData: AdData, extras: Map<String, String>) {
        try {
            mPlacementId = InMobiAdapterConfiguration.getPlacementId(extras)
            mInMobiRewardedVideo = InMobiInterstitial(context, mPlacementId!!, object : InterstitialAdEventListener() {

                override fun onAdLoadSucceeded(ad: InMobiInterstitial, info: AdMetaInfo) {
                    MoPubLog.log(adNetworkId, AdapterLogEvent.LOAD_SUCCESS, ADAPTER_NAME)
                    mLoadListener?.onAdLoaded()
                }

                override fun onAdLoadFailed(inMobiInterstitial: InMobiInterstitial,
                                            inMobiAdRequestStatus: InMobiAdRequestStatus) {
                    onInMobiAdFailWithEvent(AdapterLogEvent.LOAD_FAILED, adNetworkId,
                            InMobiAdapterConfiguration.getMoPubErrorCode(inMobiAdRequestStatus.statusCode),
                            "InMobi rewarded video request failed " +
                                    "with message: ${inMobiAdRequestStatus.message} " +
                                    "and status code: ${inMobiAdRequestStatus.statusCode}.",
                            ADAPTER_NAME, mLoadListener, null)
                }

                override fun onAdWillDisplay(inMobiInterstitial: InMobiInterstitial) {
                    MoPubLog.log(adNetworkId, AdapterLogEvent.WILL_APPEAR, ADAPTER_NAME)
                }

                override fun onAdDisplayed(ad: InMobiInterstitial, info: AdMetaInfo) {
                    MoPubLog.log(adNetworkId, AdapterLogEvent.CUSTOM, ADAPTER_NAME,
                            "InMobi rewarded video ad displayed")
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
                            "InMobi rewarded video show failed",
                            ADAPTER_NAME, null, mInteractionListener)
                }

                override fun onAdDismissed(inMobiInterstitial: InMobiInterstitial) {
                    MoPubLog.log(adNetworkId, AdapterLogEvent.CUSTOM, ADAPTER_NAME,
                            "InMobi rewarded video ad dismissed")
                    MoPubLog.log(adNetworkId, AdapterLogEvent.DID_DISAPPEAR, ADAPTER_NAME)
                    mInteractionListener?.onAdDismissed()
                }

                override fun onRewardsUnlocked(inMobiInterstitial: InMobiInterstitial,
                                               rewards: Map<Any, Any>?) {
                    MoPubLog.log(adNetworkId, AdapterLogEvent.CUSTOM, ADAPTER_NAME,
                            "InMobi rewarded video ad reward should be received.")

                    // Rewards dict should only contain one single key-value of (String, Int) type, a pair for reward name-amount
                    rewards?.let { rewardsDict ->
                        try {
                            val rewardName = rewardsDict.keys.iterator().next().toString()
                            val rewardAmount = rewardsDict[rewardName].toString().toInt()

                            MoPubLog.log(adNetworkId, AdapterLogEvent.SHOULD_REWARD, ADAPTER_NAME,
                                    MoPubReward.DEFAULT_REWARD_AMOUNT, MoPubReward.NO_REWARD_LABEL)
                            MoPubLog.log(adNetworkId, AdapterLogEvent.CUSTOM, ADAPTER_NAME,
                                    "InMobi reward name: $rewardName, amount: $rewardAmount")

                            mInteractionListener?.onAdComplete(MoPubReward.success(rewardName, rewardAmount))
                            return

                        } catch (e: Exception) {
                            MoPubLog.log(adNetworkId, AdapterLogEvent.CUSTOM, ADAPTER_NAME,
                                    "Error while parsing InMobi rewards ${e.message}")
                        }
                    }

                    MoPubLog.log(adNetworkId, AdapterLogEvent.CUSTOM, ADAPTER_NAME,
                            "Failed to reward user with reward as rewards dictionary received is empty. " +
                                    "There are no proper rewards set on your InMobi ad with placementId: " + adNetworkId +
                                    "Please ensure your InMobi ad's rewards settings are correct.")
                    mInteractionListener?.onAdComplete(MoPubReward.failure())
                }
            })

            MoPubLog.log(adNetworkId, AdapterLogEvent.LOAD_ATTEMPTED, ADAPTER_NAME)
            val adMarkup = extras[DataKeys.ADM_KEY]
            if (adMarkup != null) {
                MoPubLog.log(AdapterLogEvent.CUSTOM, ADAPTER_NAME,
                        "Ad markup for InMobi rewarded video ad request is present. Will make Advanced Bidding ad request " +
                                "using markup: " + adMarkup)
                mInMobiRewardedVideo!!.load(adMarkup.toByteArray())
            } else {
                MoPubLog.log(AdapterLogEvent.CUSTOM, ADAPTER_NAME,
                        "Ad markup for InMobi rewarded video ad request is not present. Will make traditional ad request ")
                mInMobiRewardedVideo!!.setExtras(InMobiAdapterConfiguration.inMobiTPExtras)
                mInMobiRewardedVideo!!.load()
            }

        } catch (inMobiSdkNotInitializedException: SdkNotInitializedException) {
            onInMobiAdFailWithEvent(AdapterLogEvent.LOAD_FAILED, adNetworkId, MoPubErrorCode.NETWORK_INVALID_STATE,
                    "Attempting to create InMobi rewarded video object before InMobi SDK is initialized caused failure" +
                            "Please make sure InMobi is properly initialized. InMobi will attempt to initialize on next ad request.",
                    ADAPTER_NAME, mLoadListener, null)
        } catch (npe: NullPointerException) {
            onInMobiAdFailWithEvent(AdapterLogEvent.LOAD_FAILED, adNetworkId, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR,
                    "InMobi rewarded video request failed. Placement Id is null. " +
                            "Please make sure you set valid Placement Id on MoPub UI.",
                    ADAPTER_NAME, mLoadListener, null)
        } catch (e: Exception) {
            onInMobiAdFailWithEvent(AdapterLogEvent.LOAD_FAILED, adNetworkId, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR,
                    "InMobi rewarded video failed due to configuration issue",
                    ADAPTER_NAME, mLoadListener, null)
            return
        }
    }

    override fun show() {
        if (mInMobiRewardedVideo?.isReady == true) {
            mInMobiRewardedVideo?.show()
        } else {
            onInMobiAdFailWithEvent(AdapterLogEvent.SHOW_FAILED, adNetworkId,
                    MoPubErrorCode.FULLSCREEN_SHOW_ERROR,
                    "InMobi rewarded video show failed",
                    ADAPTER_NAME, null, mInteractionListener)
            MoPubLog.log(AdapterLogEvent.CUSTOM, ADAPTER_NAME, "InMobi Rewarded video is not ready yet. " +
                    "It is still loading. Please make sure ad is loaded.")
        }
    }
}
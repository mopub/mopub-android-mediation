// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.testing

import android.content.Context
import com.mopub.mobileads.testing.MoPubSampleAdUnit.AdType.*

internal enum class SampleAppAdUnits(private val adUnitId: Int,
                                     private val adType: MoPubSampleAdUnit.AdType,
                                     private val description: String,
                                     private val keywords: String = "") {
    SAMPLE_BANNER(R.string.ad_unit_id_banner, BANNER, "MoPub Banner Sample"),
    SAMPLE_MEDIUM_RECTANGLE(R.string.ad_unit_id_medium_rectangle, MEDIUM_RECTANGLE, "MoPub Medium Rectangle Sample"),
    SAMPLE_INTERSTITIAL(R.string.ad_unit_id_interstitial, INTERSTITIAL, "MoPub Interstitial Sample"),
    SAMPLE_REWARDED_VIDEO(R.string.ad_unit_id_rewarded_video, REWARDED_VIDEO, "MoPub Rewarded Video Sample"),
    SAMPLE_REWARDED_RICH_MEDIA(R.string.ad_unit_id_rewarded_rich_media, REWARDED_VIDEO, "MoPub Rewarded Rich Media Sample"),
    SAMPLE_NATIVE_LIST_VIEW(R.string.ad_unit_id_native, LIST_VIEW, "MoPub Ad Placer Sample"),
    SAMPLE_NATIVE_RECYCLER_VIEW(R.string.ad_unit_id_native, RECYCLER_VIEW, "MoPub Recycler View Sample"),
    SAMPLE_NATIVE_VIEW_PAGER(R.string.ad_unit_id_native, CUSTOM_NATIVE, "MoPub View Pager Sample"),
    SAMPLE_NATIVE_MANUAL(R.string.ad_unit_id_native, MANUAL_NATIVE, "MoPub Manual Native Sample");

    companion object Defaults {
        fun getAdUnits(context: Context): List<MoPubSampleAdUnit> {
            return values().map {
                MoPubSampleAdUnit.Builder(context.getString(it.adUnitId), it.adType)
                        .description(it.description)
                        .keywords(it.keywords)
                        .build()
            }
        }
    }

}

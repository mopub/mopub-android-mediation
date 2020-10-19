package com.mopub.mobileads.testing;

import android.content.Context;

import androidx.annotation.NonNull;

import com.mopub.common.Preconditions;

import java.util.ArrayList;
import java.util.List;

import static com.mopub.mobileads.testing.MoPubSampleAdUnit.AdType.BANNER;
import static com.mopub.mobileads.testing.MoPubSampleAdUnit.AdType.INTERSTITIAL;
import static com.mopub.mobileads.testing.MoPubSampleAdUnit.AdType.MANUAL_NATIVE;
import static com.mopub.mobileads.testing.MoPubSampleAdUnit.AdType.MEDIUM_RECTANGLE;
import static com.mopub.mobileads.testing.MoPubSampleAdUnit.AdType.RECYCLER_VIEW;
import static com.mopub.mobileads.testing.MoPubSampleAdUnit.AdType.REWARDED_VIDEO;

enum SampleAppDefaultAdUnits {
    SAMPLE_BANNER(R.string.ad_unit_id_banner, BANNER, "MoPub Banner Sample"),
    SAMPLE_MEDIUM_RECTANGLE(R.string.ad_unit_id_medium_rectangle, MEDIUM_RECTANGLE, "MoPub MediumRectangle Sample"),
    SAMPLE_INTERSTITIAL(R.string.ad_unit_id_interstitial, INTERSTITIAL,
            "MoPub Interstitial Sample"),
    SAMPLE_REWARDED_VIDEO(R.string.ad_unit_id_rewarded_video, REWARDED_VIDEO,
            "MoPub Rewarded Video Sample"),
    SAMPLE_REWARDED_RICH_MEDIA(R.string.ad_unit_id_rewarded_rich_media, REWARDED_VIDEO,
            "MoPub Rewarded Rich Media Sample"),
    SAMPLE_NATIVE_RECYCLER_VIEW(R.string.ad_unit_id_native, RECYCLER_VIEW,
            "MoPub Recycler View Sample"),
    SAMPLE_NATIVE_MANUAL(R.string.ad_unit_id_native, MANUAL_NATIVE, "MoPub Manual Native Sample");

    private int mAdUnitIdStringKey;
    @NonNull private MoPubSampleAdUnit.AdType mAdType;
    @NonNull private String mDescription;

    SampleAppDefaultAdUnits(final int adUnitIdStringKey,
            @NonNull final MoPubSampleAdUnit.AdType adType, @NonNull final String description) {
        mAdUnitIdStringKey = adUnitIdStringKey;
        mAdType = adType;
        mDescription = description;
    }

    static List<MoPubSampleAdUnit> getDefaultAdUnits(@NonNull final Context context) {
        Preconditions.checkNotNull(context);

        final List<MoPubSampleAdUnit> adUnitList = new ArrayList<>();
        for (SampleAppDefaultAdUnits adUnit : SampleAppDefaultAdUnits.values()) {
            adUnitList.add(
                    new com.mopub.mobileads.testing.MoPubSampleAdUnit
                            .Builder(context.getString(adUnit.mAdUnitIdStringKey), adUnit.mAdType)
                            .description(adUnit.mDescription)
                            .build());
        }
        return adUnitList;
    }
}

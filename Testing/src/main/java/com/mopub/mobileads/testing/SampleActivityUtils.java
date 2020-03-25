// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.testing;

import androidx.annotation.NonNull;

import com.mopub.common.Preconditions;
import com.mopub.common.SdkConfiguration;
import com.mopub.mobileads.FlurryAdapterConfiguration;
import com.mopub.mobileads.TapjoyAdapterConfiguration;
import com.mopub.mobileads.TapjoyInterstitial;

import java.util.HashMap;
import java.util.Map;

class SampleActivityUtils {
    static void addDefaultNetworkConfiguration(@NonNull final SdkConfiguration.Builder builder) {
        Preconditions.checkNotNull(builder);

        final Map<String, String> flurryConfig = new HashMap<>();
        flurryConfig.put("apiKey", "VX85BD4YBFNW629NN2SP");
        builder.withMediatedNetworkConfiguration(FlurryAdapterConfiguration.class.getName(),
                flurryConfig);

        final Map<String, String> tapjoyConfig = new HashMap<>();
        tapjoyConfig.put(TapjoyInterstitial.SDK_KEY,
                "cSOY1BYrRsSyJljkFWPdsgECRpZaaWDkWwXH1N1hIUbz1-c0o-DKATsLtckr");
        builder.withMediatedNetworkConfiguration(TapjoyAdapterConfiguration.class.getName(),
                tapjoyConfig);
    }
}

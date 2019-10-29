package com.mopub.mobileads;

import androidx.annotation.NonNull;

public interface VungleRouterListener {

    @Deprecated
    void onAdEnd(@NonNull String var1, boolean var2, boolean var3);

    void onAdEnd(String id);

    void onAdClick(String id);

    void onAdRewarded(String id);

    void onAdLeftApplication(String id);

    void onAdStart(@NonNull String var1);

    void onUnableToPlayAd(@NonNull String var1, String var2);

    void onAdAvailabilityUpdate(@NonNull String var1, boolean var2);
}

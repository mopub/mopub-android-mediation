package com.mopub.mobileads;

import androidx.annotation.NonNull;

public interface VungleRouterListener {

    void onAdEnd(String placementId);

    void onAdClick(String placementId);

    void onAdRewarded(String placementId);

    void onAdLeftApplication(String placementId);

    void onAdStart(@NonNull String placementId);

    void onAdViewed(@NonNull String placementId);

    void onUnableToPlayAd(@NonNull String placementId, String reason);

    void onAdAvailabilityUpdate(@NonNull String placementId, boolean isAdAvailable);

}

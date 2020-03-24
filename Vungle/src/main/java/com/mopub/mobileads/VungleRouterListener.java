package com.mopub.mobileads;

import androidx.annotation.NonNull;

public interface VungleRouterListener {

    void onAdEnd(@NonNull String placementId, boolean wasSuccessfulView, boolean wasCallToActionClicked);

    void onAdStart(@NonNull String placementId);

    void onUnableToPlayAd(@NonNull String placementId, String reason);

    void onAdAvailabilityUpdate(@NonNull String placementId, boolean isAdAvailable);

}

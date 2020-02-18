package com.mopub.mobileads;

import androidx.annotation.NonNull;

public interface VungleRouterListener {

    void onAdEnd(@NonNull String placementRefId, boolean wasSuccessfulView, boolean wasCallToActionClicked);

    void onAdStart(@NonNull String placementRefId);

    void onUnableToPlayAd(@NonNull String placementRefId, String reason);

    void onAdAvailabilityUpdate(@NonNull String placementRefId, boolean isAdAvailable);

}

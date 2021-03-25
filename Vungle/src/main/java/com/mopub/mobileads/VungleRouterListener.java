package com.mopub.mobileads;

import androidx.annotation.NonNull;

import com.vungle.warren.error.VungleException;

public interface VungleRouterListener {

    void onAdEnd(String placementId);

    void onAdClick(String placementId);

    void onAdRewarded(String placementId);

    void onAdLeftApplication(String placementId);

    void onAdStart(@NonNull String placementId);

    void onAdViewed(@NonNull String placementId);

    void onAdPlayError(@NonNull String placementId, VungleException exception);

    void onAdLoadError(@NonNull String placementId, VungleException exception);

    void onAdLoaded(@NonNull String placementId);

}

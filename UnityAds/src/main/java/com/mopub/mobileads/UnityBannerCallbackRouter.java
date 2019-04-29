package com.mopub.mobileads;

import android.view.View;

import com.unity3d.services.banners.IUnityBannerListener;

import java.util.HashMap;
import java.util.Map;

public class UnityBannerCallbackRouter implements IUnityBannerListener {

    private final Map<String, IUnityBannerListener> listeners = new HashMap<>();
    private String currentPlacementId;

    @Override
    public void onUnityBannerLoaded(String placementId, View view) {
        IUnityBannerListener listener = listeners.get(placementId);
        if (listener != null) {
            listener.onUnityBannerLoaded(placementId, view);
        }
    }

    @Override
    public void onUnityBannerUnloaded(String placementId) {
        IUnityBannerListener listener = listeners.get(placementId);
        if (listener != null) {
            listener.onUnityBannerUnloaded(placementId);
        }
    }

    @Override
    public void onUnityBannerShow(String placementId) {
        IUnityBannerListener listener = listeners.get(placementId);
        if (listener != null) {
            listener.onUnityBannerShow(placementId);
        }
    }

    @Override
    public void onUnityBannerClick(String placementId) {
        IUnityBannerListener listener = listeners.get(placementId);
        if (listener != null) {
            listener.onUnityBannerClick(placementId);
        }
    }

    @Override
    public void onUnityBannerHide(String placementId) {
        IUnityBannerListener listener = listeners.get(placementId);
        if (listener != null) {
            listener.onUnityBannerHide(placementId);
        }
    }

    @Override
    public void onUnityBannerError(String message) {
        IUnityBannerListener listener = listeners.get(currentPlacementId);
        if (listener != null) {
            listener.onUnityBannerError(message);
        }
    }

    public void addListener(String placementId, IUnityBannerListener listener) {
        listeners.put(placementId, listener);
    }

    public void removeListener(String placementId) {
        listeners.remove(placementId);
    }

    public void setCurrentPlacementId(String placementId) {
        currentPlacementId = placementId;
    }
}

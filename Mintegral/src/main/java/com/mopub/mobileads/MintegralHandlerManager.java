package com.mopub.mobileads;

import com.mintegral.msdk.interstitialvideo.out.MTGBidInterstitialVideoHandler;
import com.mintegral.msdk.interstitialvideo.out.MTGInterstitialVideoHandler;
import com.mintegral.msdk.out.MTGBidRewardVideoHandler;
import com.mintegral.msdk.out.MTGRewardVideoHandler;

import java.util.HashMap;
import java.util.Map;

final class MintegralHandlerManager {
    private Map<String, MTGRewardVideoHandler> rewardedMap = new HashMap<>();
    private Map<String, MTGBidRewardVideoHandler> bidRewardedMap = new HashMap<>();
    private Map<String, MTGInterstitialVideoHandler> interstitialMap = new HashMap<>();
    private Map<String, MTGBidInterstitialVideoHandler> bidInterstitialMap = new HashMap<>();

    private MintegralHandlerManager() {
    }

    static MintegralHandlerManager getInstance() {
        return ClassHolder.MINTEGRAL_HANDLER_MANAGER;
    }

    MTGRewardVideoHandler getMTGRewardVideoHandler(String unitID) {
        if (rewardedMap != null && rewardedMap.containsKey(unitID)) {
            return rewardedMap.get(unitID);
        }
        return null;
    }

    void addMTGRewardVideoHandler(String unitID, MTGRewardVideoHandler handler) {
        if (rewardedMap != null) {
            rewardedMap.put(unitID, handler);
        }
    }

    MTGBidRewardVideoHandler getMTGBidRewardVideoHandler(String unitID) {
        if (bidRewardedMap != null && bidRewardedMap.containsKey(unitID)) {
            return bidRewardedMap.get(unitID);
        }
        return null;
    }

    void addMTGBidRewardVideoHandler(String unitID, MTGBidRewardVideoHandler handler) {
        if (bidRewardedMap != null) {
            bidRewardedMap.put(unitID, handler);
        }
    }

    MTGInterstitialVideoHandler getMTGInterstitialVideoHandler(String unitID) {
        if (interstitialMap != null && interstitialMap.containsKey(unitID)) {
            return interstitialMap.get(unitID);
        }
        return null;
    }

    void addMTGInterstitialVideoHandler(String unitID, MTGInterstitialVideoHandler handler) {
        if (interstitialMap != null) {
            interstitialMap.put(unitID, handler);
        }
    }

    MTGBidInterstitialVideoHandler getMTGBidInterstitialVideoHandler(String unitID) {
        if (bidInterstitialMap != null && bidInterstitialMap.containsKey(unitID)) {
            return bidInterstitialMap.get(unitID);
        }
        return null;
    }

    void addMTGBidInterstitialVideoHandler(String unitID, MTGBidInterstitialVideoHandler handler) {
        if (bidInterstitialMap != null) {
            bidInterstitialMap.put(unitID, handler);
        }
    }

    private static final class ClassHolder {
        private static final MintegralHandlerManager MINTEGRAL_HANDLER_MANAGER = new MintegralHandlerManager();
    }
}

package com.mopub.mobileads;

import com.mintegral.msdk.interstitialvideo.out.MTGBidInterstitialVideoHandler;
import com.mintegral.msdk.interstitialvideo.out.MTGInterstitialVideoHandler;
import com.mintegral.msdk.out.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author fiissh.zhao
 * @version 1.0.0
 * @email yongchun.zhao@mintegral.com
 * @create_time 2020-四月-26 星期日
 * @description TODO
 */
final class MintegralHandlerManager {
    private Map<String, MTGRewardVideoHandler> mtgRewardVideoHandlerHashMap = new HashMap<String, MTGRewardVideoHandler>();
    private Map<String, MTGBidRewardVideoHandler> mtgBidRewardVideoHandlerHashMap = new HashMap<String, MTGBidRewardVideoHandler>();
    private Map<String, MTGInterstitialVideoHandler> mtgInterstitialVideoHandlerHashMap = new HashMap<String, MTGInterstitialVideoHandler>();
    private Map<String, MTGBidInterstitialVideoHandler> mtgBidInterstitialVideoHandlerHashMap = new HashMap<String, MTGBidInterstitialVideoHandler>();

    private MintegralHandlerManager() {
    }

    public static MintegralHandlerManager getInstance() {
        return ClassHolder.MINTEGRAL_HANDLER_MANAGER;
    }

    public MTGRewardVideoHandler getMTGRewardVideoHandler(String unitID) {
        if (mtgRewardVideoHandlerHashMap != null && mtgRewardVideoHandlerHashMap.containsKey(unitID)) {
            return mtgRewardVideoHandlerHashMap.get(unitID);
        }
        return null;
    }

    public void addMTGRewardVideoHandler(String unitID, MTGRewardVideoHandler mtgRewardVideoHandler) {
        if (mtgRewardVideoHandlerHashMap != null) {
            mtgRewardVideoHandlerHashMap.put(unitID, mtgRewardVideoHandler);
        }
    }

    public MTGBidRewardVideoHandler getMTGBidRewardVideoHandler(String unitID) {
        if (mtgBidRewardVideoHandlerHashMap != null && mtgBidRewardVideoHandlerHashMap.containsKey(unitID)) {
            return mtgBidRewardVideoHandlerHashMap.get(unitID);
        }
        return null;
    }

    public void addMTGBidRewardVideoHandler(String unitID, MTGBidRewardVideoHandler mtgBidRewardVideoHandler) {
        if (mtgBidRewardVideoHandlerHashMap != null) {
            mtgBidRewardVideoHandlerHashMap.put(unitID, mtgBidRewardVideoHandler);
        }
    }


    public MTGInterstitialVideoHandler getMTGInterstitialVideoHandler(String unitID) {
        if (mtgInterstitialVideoHandlerHashMap != null && mtgInterstitialVideoHandlerHashMap.containsKey(unitID)) {
            return mtgInterstitialVideoHandlerHashMap.get(unitID);
        }
        return null;
    }

    public void addMTGInterstitialVideoHandler(String unitID, MTGInterstitialVideoHandler mtgInterstitialVideoHandler) {
        if (mtgInterstitialVideoHandlerHashMap != null) {
            mtgInterstitialVideoHandlerHashMap.put(unitID, mtgInterstitialVideoHandler);
        }
    }

    public MTGBidInterstitialVideoHandler getMTGBidInterstitialVideoHandler(String unitID) {
        if (mtgBidInterstitialVideoHandlerHashMap != null && mtgBidInterstitialVideoHandlerHashMap.containsKey(unitID)) {
            return mtgBidInterstitialVideoHandlerHashMap.get(unitID);
        }
        return null;
    }

    public void addMTGBidInterstitialVideoHandler(String unitID, MTGBidInterstitialVideoHandler mtgBidInterstitialVideoHandler) {
        if (mtgBidInterstitialVideoHandlerHashMap != null) {
            mtgBidInterstitialVideoHandlerHashMap.put(unitID, mtgBidInterstitialVideoHandler);
        }
    }

    private static final class ClassHolder {
        private static final MintegralHandlerManager MINTEGRAL_HANDLER_MANAGER = new MintegralHandlerManager();
    }
}

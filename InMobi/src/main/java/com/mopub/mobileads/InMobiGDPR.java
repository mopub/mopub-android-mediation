package com.mopub.mobileads;

import androidx.annotation.Nullable;

import org.json.JSONObject;

public class InMobiGDPR {
    private static JSONObject consent = null;

    /**
     * Call InMobiGDPR.setGDPRConsentDictionary() to provide GDPR consent to InMobi.
     */
    public static void setGDPRConsentDictionary(@Nullable JSONObject consentDictionary) {
        consent = consentDictionary;
    }

    /**
     * Call InMobiGDPR.getGDPRConsentDictionary() to get the last updated consent.
     */
    @Nullable
    public static JSONObject getGDPRConsentDictionary() {
        return consent;
    }
}

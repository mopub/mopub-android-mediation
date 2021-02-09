package com.mopub.mobileads

import com.inmobi.sdk.InMobiSdk
import org.json.JSONObject

class InMobiGDPR {
    companion object {
        @JvmStatic
        fun setGDPRConsentDictionary(consentDictionary: JSONObject?) {
            InMobiSdk.updateGDPRConsent(consentDictionary)
        }
    }
}
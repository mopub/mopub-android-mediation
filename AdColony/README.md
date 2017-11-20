## Overview
  * This folder contains mediation adapters used to mediate AdColony.
  * To download and integrate the AdColony SDK, please check [this tutorial](https://github.com/AdColony/AdColony-Android-SDK-3/wiki/Project-Setup).
  * For inquiries and support, please email support@adcolony.com.
  
## Adapter integration
  * Ensure your ad unit ID is configured to mediate AdColony.
  * Download the desired adapter file(s) for the ad format(s) you are going to mediate.
  * Place the adapter in your app's src/.../com/mopub/mobileads (for non-native ad formats) or src/.../com/mopub/nativeads (for native ad).

## Changelog
  * 3.2.1.0
    * This version of the adapters has been certified with AdColony 3.2.1.
    * When changing AdColony zone IDs on the MoPub portal, the SDK will now reconfigure.
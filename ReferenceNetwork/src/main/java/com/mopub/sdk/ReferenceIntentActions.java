// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.sdk;

/**
 * A simple network SDK implementation that works with the reference adapters.
 * <p>
 * INTERNAL USE ONLY. DO NOT REFERENCE OTHERWISE.
 */
public class ReferenceIntentActions {

    /**
     * IntentActions are used by a reference broadcast receiver to relay information about the
     * current state of a base ad activity.
     */
    public static final String ACTION_FULLSCREEN_FAIL = "com.mopub.sdk.action.fullscreen.fail";
    public static final String ACTION_FULLSCREEN_SHOW = "com.mopub.sdk.action.fullscreen.show";
    public static final String ACTION_FULLSCREEN_DISMISS = "com.mopub.sdk.action.fullscreen.dismiss";
    public static final String ACTION_FULLSCREEN_CLICK = "com.mopub.sdk.action.fullscreen.click";
    public static final String ACTION_REWARDED_AD_REWARD = "com.mopub.sdk.action.rewarded.reward";

    private ReferenceIntentActions() {
    }
}

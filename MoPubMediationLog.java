package com.mopub;

import com.mopub.common.logging.MoPubLog;

public class MoPubMediationLog {

    private static String mAdFormat;
    private static String mAdNetwork;
    private static String[] mSdkAndAdFormats;

    public enum AdEvent {
        DEFAULT, UNAVAILABLE, LOADED, IMPRESSED, CLICKED, DISMISSED,
        COMPLETED, DESTROYED, ERROR, EXPIRED, SHOWN
    }

    public enum AdFormatDelimiters {
        Banner, Interstitial, RewardedVideo, Native, Base, CustomEvent, AdRenderer, Utils, Router, Listener, AgentWrapper, Shared
    }

    public enum NetworkNameForConversion {
        GooglePlayServices, Millennial, Unity
    }

    public enum AdFormatForConversion {
        RewardedVideo, Native
    }

    /**
     * This constructor is useful for when the class name contains both the ad network and the ad format.
     * We will just parse for the information and avoid hard-coding any params.
     *
     * @param className The mediation adapter class name obtained at the time of instantiation
     */
    public MoPubMediationLog(String className) {
        parseSdkAndAdFormat(className);
    }

    /**
     * This constructor is useful for when the class name does not contain the ad format.
     * This is usually the case for utility/router classes that log for all ad formats in the same scope.
     * It consumes the ad format manually passed to it at the time of instantiation.
     *
     * @param network The ad network name manually passed in by the adapter
     * @param format  The ad format manually passed in by the adapter
     */
    public MoPubMediationLog(String network, String format) {
        mAdNetwork = network;
        mAdFormat = format;
    }

    /**
     * TODO: Evaluate if we need an empty constructor.
     * This constructor might be necessary when a router class does not log based on ad formats.
     */
    public MoPubMediationLog() {
    }

    public static void logParamsInvalid(String info, String source) {
        MoPubLog.i(info + " from " + source + " for " + getNetworkName() + " is null or invalid. Please check again.");
    }

    public static void logAdRequestSent() {
        MoPubLog.i("Requesting an ad of type " + getAdFormat() + " from " + getNetworkName() + ".");
    }

    public static void logOnInvalidate() {
        MoPubLog.i("Cleaning up resources.");
    }

    public static void logAdEvent(AdEvent ev) {

        boolean isVideo = isVideo();
        String eventMessage = null;

        switch (ev) {
            case UNAVAILABLE:
                eventMessage = "is not loaded/cached yet. Try again.";
                break;
            case LOADED:
                eventMessage = (!isVideo) ? ("has successfully loaded.") : ("has successfully cached.");
                break;
            case SHOWN:
                eventMessage = (!isVideo) ? ("is now showing.") : ("is now playing.");
                break;
            case IMPRESSED:
                eventMessage = "has logged an impression.";
                break;
            case CLICKED:
                eventMessage = "has been clicked.";
                break;
            case DISMISSED:
                eventMessage = "has been dismissed.";
                break;
            case COMPLETED:
                eventMessage = "has completed playing. Rewarding the user.";
                break;
            case DESTROYED:
                eventMessage = "has been destroyed.";
                break;
            case ERROR:
                eventMessage = "has run into an error.";
                break;
            case EXPIRED:
                eventMessage = "has expired due to " + getNetworkName() + "'s expiration policy.";
                break;
        }
        MoPubLog.i(getNetworkName() + "'s " + getAdFormat() + " " + eventMessage);
    }

    public static void logAdSizeInvalid() {
        MoPubLog.i("Ad size is invalid.");
    }

    public static void logAdListenerNull() {
        MoPubLog.i(getNetworkName() + " " + getAdFormat() + " listener null. Please check again.");
    }

    private static String getNetworkName() {
        return mAdNetwork;
    }

    private static String getAdFormat() {
        return mAdFormat;
    }

    private static boolean isVideo() {
        return getAdFormat().equals(String.valueOf(AdFormatDelimiters.RewardedVideo));
    }

    private void parseSdkAndAdFormat(String className) {
        boolean alreadySetAdFormat = false;
        for (AdFormatDelimiters format : AdFormatDelimiters.values()) {
            if (className.contains(format.toString())) {
                if (!alreadySetAdFormat) {
                    setAdFormat(format.toString());
                    alreadySetAdFormat = true;
                }
                mSdkAndAdFormats = className.split(String.valueOf(format));
            }
        }
        setNetworkName(mSdkAndAdFormats[0]);
    }

    private void setNetworkName(String network) {
        mAdNetwork = network;

        if (enumContains(NetworkNameForConversion.class, network)) {
            convertToOfficialName(NetworkNameForConversion.valueOf(network));
        }
    }

    private void setAdFormat(String format) {
        mAdFormat = format.toLowerCase();

        if (enumContains(AdFormatForConversion.class, format)) {
            convertToReadableFormat(AdFormatForConversion.valueOf(format));
        }
    }

    private void convertToOfficialName(NetworkNameForConversion nameForConversion) {
        switch (nameForConversion) {
            case GooglePlayServices:
                mAdNetwork = "AdMob";
                break;
            case Millennial:
                mAdNetwork = "One by AOL";
                break;
            case Unity:
                mAdNetwork = "Unity Ads";
                break;
        }
    }

    private void convertToReadableFormat(AdFormatForConversion adFormatForConversion) {
        switch (adFormatForConversion) {
            case RewardedVideo:
                mAdFormat = "rewarded video";
                break;
            case Native:
                mAdFormat = "native ad";
        }
    }

    private <E extends Enum<E>> boolean enumContains(Class<E> e, String s) {
        try {
            Enum.valueOf(e, s);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
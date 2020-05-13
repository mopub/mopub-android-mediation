package com.mopub.mobileads;

/**
 * created by wuzejian on 2020-02-25
 */
public class ErrorCode {

    public static final int CONTENT_TYPE = 40000;//# http conent_type
    public static final int REQUEST_PB_ERROR = 40001;//# http request pb
    public static final int NO_AD = 20001;//# no ad
    public static final int ADSLOT_EMPTY = 40004;// ad code id can't been null
    public static final int ADSLOT_ID_ERROR = 40006;// code id error

    public static MoPubErrorCode mapErrorCode(int error) {
        switch (error) {
            case ErrorCode.CONTENT_TYPE:
            case ErrorCode.REQUEST_PB_ERROR:
                return MoPubErrorCode.NO_CONNECTION;
            case ErrorCode.NO_AD:
                return MoPubErrorCode.NETWORK_NO_FILL;
            case ErrorCode.ADSLOT_EMPTY:
            case ErrorCode.ADSLOT_ID_ERROR:
                return MoPubErrorCode.MISSING_AD_UNIT_ID;
            default:
                return MoPubErrorCode.UNSPECIFIED;
        }
    }
}

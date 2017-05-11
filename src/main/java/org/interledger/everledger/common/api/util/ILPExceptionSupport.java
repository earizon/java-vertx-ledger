package org.interledger.everledger.common.api.util;



import org.interledger.everledger.Config;
import org.interledger.everledger.HTTPInterledgerException;
import org.interledger.ilp.InterledgerAddress;
import org.interledger.ilp.InterledgerAddressBuilder;
import org.interledger.ilp.InterledgerError;
import org.interledger.ilp.InterledgerError.ErrorCode;

// TODO:(?) Move "somewhere else". This util has dependencies in config. Ummmm,...
public class ILPExceptionSupport {
    private static InterledgerAddress triggeredBy = InterledgerAddressBuilder
            .builder().value(Config.ilpPrefix).build();

    /**
     * Well known ILP Errors as defined in the RFCs
     * @param errCode
     * @param data
     */
    public static HTTPInterledgerException createILPException(int httpErrCode, ErrorCode errCode , String data){
        return new HTTPInterledgerException(httpErrCode, new InterledgerError(errCode, triggeredBy, data));
    }

    /**
     * Shortcut for Non-ILP-related/Non-Expected Errors that will be returned
     * as ILP Error TOO Internal Errors
     * 
     * @param data
     */
    public static HTTPInterledgerException createILPInternalException(String data) {
        return new HTTPInterledgerException(500, new InterledgerError(ErrorCode.T00_INTERNAL_ERROR /* Looks Internal error is not Temporal */, triggeredBy, data));
    }

    public static HTTPInterledgerException createILPUnauthorizedException() {
        // TODO:(0) Use new ErrorCode.??_FORBIDDEN
        return createILPException(401, ErrorCode.T00_INTERNAL_ERROR /* TODO:(RFC) Create another code for this?*/, "Forbidden"); 
    }

    public static HTTPInterledgerException createILPForbiddenException() {
        // TODO:(0) Use new ErrorCode.??_FORBIDDEN
        return createILPException(403, ErrorCode.T00_INTERNAL_ERROR /* TODO:(RFC) Create another code for this?*/, "Forbidden"); 
    }

    public static HTTPInterledgerException createILPNotFoundException() {
        return createILPException(404, ErrorCode.T00_INTERNAL_ERROR /* TODO:(RFC) Create another code for this?*/, "Forbidden"); 
    }

}

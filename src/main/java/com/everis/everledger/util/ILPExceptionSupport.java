package com.everis.everledger.util;

import org.interledger.InterledgerAddress;
import org.interledger.ilp.InterledgerError;
import org.interledger.ilp.InterledgerError.ErrorCode;

import com.everis.everledger.Config;
import com.everis.everledger.HTTPInterledgerException;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

// TODO:(?) Move "somewhere else". This util has dependencies in config. Ummmm,...
public class ILPExceptionSupport {
    private static InterledgerAddress selfAddress  = InterledgerAddress.builder().value(Config.ilpPrefix).build();


    /**
     * Well known ILP Errors as defined in the RFCs
     * @param errCode
     * @param data
     */
    public static HTTPInterledgerException createILPException(int httpErrCode, ErrorCode errCode , String data){
        return new HTTPInterledgerException(httpErrCode, 
                new InterledgerError(errCode, /* triggeredBy */ selfAddress, ZonedDateTime.now(), new ArrayList<InterledgerAddress>(), /*self Address*/ selfAddress, data));
    }

    // Next follow some wrappers arount createILPException, more human-readable.
    // ----------- Internal --------------
    public static HTTPInterledgerException createILPInternalException(String data) {
        return createILPException(500, ErrorCode.T00_INTERNAL_ERROR, data);
    }

    // ------------ Unauthorized -------------
    public static HTTPInterledgerException createILPUnauthorizedException(String data) {
        // TODO:(RFC) Use new ErrorCode.??_FORBIDDEN / Unauthorized
        return createILPException(401, ErrorCode.T00_INTERNAL_ERROR , data); 
    }
    public static HTTPInterledgerException createILPUnauthorizedException() {
        return createILPUnauthorizedException("Unauthorized"); 
    }

    // ----------- Forbidden --------------
    public static HTTPInterledgerException createILPForbiddenException(String data) {
        // TODO:(ILP) Use new ErrorCode.??_FORBIDDEN
        return createILPException(403, ErrorCode.T00_INTERNAL_ERROR , "data"); 
    }
    public static HTTPInterledgerException createILPForbiddenException() {
        return createILPForbiddenException("Forbidden"); 
    }

    // ------------ NotFound -------------
    public static HTTPInterledgerException createILPNotFoundException(String data) {
        // TODO:(ILP) Use new ErrorCode.??_NOT_FOUND
        return createILPException(404, ErrorCode.T00_INTERNAL_ERROR , data); 
    }
    public static HTTPInterledgerException createILPNotFoundException() {
        return createILPNotFoundException("Not Found"); 
    }

    // ------------- BadRequest ------------
    public static HTTPInterledgerException createILPBadRequestException(String data) {
        return createILPException(400, ErrorCode.F00_BAD_REQUEST , data); 
    }
    public static HTTPInterledgerException createILPBadRequestException() {
        return createILPBadRequestException("Forbidden"); 
    }

    // ------------- Unprocessable Entity ------------
    public static HTTPInterledgerException createILPUnprocessableEntityException(String data) {
        return createILPException(422, ErrorCode.F00_BAD_REQUEST , data); 
    }
    public static HTTPInterledgerException createILPUnprocessableEntityException() {
        return createILPUnprocessableEntityException("Unprocessable"); 
    }
}

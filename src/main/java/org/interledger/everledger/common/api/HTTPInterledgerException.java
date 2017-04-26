package org.interledger.everledger.common.api;

import org.interledger.ilp.InterledgerError;
import org.interledger.ilp.exceptions.InterledgerException;

/*
 * Wrapper around InterledgerException to allow adding HTTP expected Error Codes
 * 
 * Note that in general the InterledgerException could be transported through
 * any transport channel (HTTP, WebSockets, gRPC, ...,...).
 */
// TODO:(?) Port to java-ilp-core?
public class HTTPInterledgerException extends InterledgerException {

    private static final long serialVersionUID = 1L;
    private final int httpErrCode;


    public HTTPInterledgerException(int httpErrCode, final InterledgerError interledgerError) {
        super(interledgerError);
        this.httpErrCode = httpErrCode;
    }

    public int getHTTPErrorCode() {
        return httpErrCode;
    }
}

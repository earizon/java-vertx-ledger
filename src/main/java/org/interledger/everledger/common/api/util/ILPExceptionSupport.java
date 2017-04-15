package org.interledger.everledger.common.api.util;

import static org.interledger.everledger.common.config.Key.ILP;
import static org.interledger.everledger.common.config.Key.LEDGER;
import static org.interledger.everledger.common.config.Key.PREFIX;

import org.interledger.everledger.common.config.Config;
import org.interledger.everledger.ledger.LedgerFactory;
import org.interledger.everledger.ledger.impl.simple.SimpleLedger;
import org.interledger.ilp.InterledgerAddress;
import org.interledger.ilp.InterledgerAddressBuilder;
import org.interledger.ilp.InterledgerError;
import org.interledger.ilp.InterledgerError.ErrorCode;
import org.interledger.ilp.exceptions.InterledgerException;

// TODO:(?) Move "somewhere else". This util has dependencies in config. Ummmm,...
public class ILPExceptionSupport {
    private static Config config = ((SimpleLedger)LedgerFactory.getDefaultLedger()).getConfig();
    private static InterledgerAddress triggeredBy = InterledgerAddressBuilder
            .builder().value(config.getString(LEDGER, ILP, PREFIX)).build();
    
    public static void launchILPException(ErrorCode errCode , String data){
        throw new InterledgerException(new InterledgerError(errCode, triggeredBy, data));
         
    }

    public static void launchILPException(String data) {
        throw new InterledgerException(new InterledgerError(ErrorCode.T00_INTERNAL_ERROR, triggeredBy, data));
    }

    public static void launchILPForbiddenException() {
        launchILPException("Forbidden");
    }

}

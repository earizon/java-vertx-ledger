package org.interledger.ilp.common.api.util;

import static org.interledger.ilp.common.config.Key.ILP;
import static org.interledger.ilp.common.config.Key.LEDGER;
import static org.interledger.ilp.common.config.Key.PREFIX;

import org.interledger.ilp.InterledgerAddress;
import org.interledger.ilp.InterledgerAddressBuilder;
import org.interledger.ilp.common.config.Config;
import org.interledger.ilp.exceptions.InterledgerException;
import org.interledger.ilp.exceptions.InterledgerException.ErrorCode;
import org.interledger.ilp.ledger.LedgerFactory;
import org.interledger.ilp.ledger.impl.simple.SimpleLedger;

// TODO:(?) Move "somewhere else". This util has dependencies in config. Ummmm,...
public class ILPExceptionSupport {
    private static Config config = ((SimpleLedger)LedgerFactory.getDefaultLedger()).getConfig();
    private static InterledgerAddress triggeredBy = InterledgerAddressBuilder
            .builder().value(config.getString(LEDGER, ILP, PREFIX)).build();
    
    public static void launchILPException(ErrorCode errCode , String data){
        throw new InterledgerException(errCode, triggeredBy, data);
    }

    public static void launchILPException(String data) {
        throw new InterledgerException(InterledgerException.ErrorCode.T00_INTERNAL_ERROR , triggeredBy, data);
    }

    public static void launchILPForbiddenException() {
        launchILPException("Forbidden");
    }

}

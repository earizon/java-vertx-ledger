package org.interledger.everledger.ledger.transfer;

import java.util.regex.Pattern;

import org.interledger.everledger.util.ILPExceptionSupport;

public class ILPSpecTransferID {
    // TODO:(0) Recheck. It was created to indicate ILP transferIDs in old rfcs.
    // Replace with java.util.UUID ?

    final private static String SREGEX
            = // Must be similar to 3a2a1d9e-8640-4d2d-b06c-84f2cd613204
            "[0-9a-fA-F]+{8}-[0-9a-fA-F]+{4}-[0-9a-fA-F]+{4}-[0-9a-fA-F]+{4}-[0-9a-fA-F]+{12}";
    final private static Pattern regex = Pattern.compile(SREGEX);
    public final String transferID;

    public ILPSpecTransferID(String transferID) {
        if (transferID == null) {
            throw new RuntimeException("transferID can't be null");
        }
        int idx=transferID.indexOf("/transfers/");
        if (idx > 0) {
            transferID = transferID.substring(idx+"/transfers/".length());
        }
        java.util.regex.Matcher m = regex.matcher(transferID);
        if (!m.matches()) {
            throw ILPExceptionSupport.createILPBadRequestException();
        }
        this.transferID = transferID;
    }
    
    @Override
    public String toString() {
        return transferID;
    }
    
    @Override
    public int hashCode() {
        return transferID.hashCode();
    }
    
    @Override
    public boolean equals(Object other) {
        if (other == null) throw new RuntimeException("comparing to null @ ILPSpecTransferID");
        if (other == this) return true;
        if (!(other instanceof ILPSpecTransferID))return false;
        return transferID.equals(((ILPSpecTransferID)other).transferID);
    }
}

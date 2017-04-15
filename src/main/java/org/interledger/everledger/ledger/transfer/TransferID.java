package org.interledger.ilp.ledger.transfer;

import java.util.regex.Pattern;

public class TransferID {
    // TODO:(0) Recheck. It was created to indicate ILP transferIDs in old rfcs. Now is just an internal Ledger class with free implementation

    final private static String SREGEX
            = // Must be similar to 3a2a1d9e-8640-4d2d-b06c-84f2cd613204
            "[0-9a-fA-F]+{8}-[0-9a-fA-F]+{4}-[0-9a-fA-F]+{4}-[0-9a-fA-F]+{4}-[0-9a-fA-F]+{12}";
    final private static Pattern regex = Pattern.compile(SREGEX);
    public final String transferID;

    public TransferID(String transferID) {
        if (transferID == null) {
            throw new RuntimeException("transferID can't be null");
        }
        int idx=transferID.indexOf("/transfers/");
        if (idx > 0) {
            transferID = transferID.substring(idx+"/transfers/".length());
        }
        java.util.regex.Matcher m = regex.matcher(transferID);
        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "transferID '" + transferID + "' doesn't match " + SREGEX);
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
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof TransferID))return false;
        return transferID.equals(((TransferID)other).transferID);
    }
}

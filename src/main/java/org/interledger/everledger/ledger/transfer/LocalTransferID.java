package org.interledger.everledger.ledger.transfer;

/*
 *  TODO:(0) This must be defined by an Interface. Differente ledgers/
 * blockchains will map public "ILP transferIDs" to local transferIDs
 * differently.
 */
public class LocalTransferID {

    public final String transferID;

    public static LocalTransferID ILPSpec2LocalTransferID
        (ILPSpecTransferID ilpTransferID) {
        return new LocalTransferID(ilpTransferID.transferID);
    }

    public LocalTransferID(String transferID) {
        this.transferID = java.util.Objects.requireNonNull(transferID);
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
        if (other == null) throw new RuntimeException("comparing to null @ LocalTransferID");
        if (other == this) return true;
        if (!(other instanceof LocalTransferID))return false;
        return transferID.equals(((LocalTransferID)other).transferID);
    }
}

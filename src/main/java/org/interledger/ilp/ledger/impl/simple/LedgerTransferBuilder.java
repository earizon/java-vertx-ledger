package org.interledger.ilp.ledger.impl.simple;

//import java.util.Date;
//import javax.money.MonetaryAmount;
//import org.apache.commons.lang.StringUtils;
//import org.interledger.cryptoconditions.Condition;
//import org.interledger.ilp.core.InterledgerPacketHeader;
//import org.interledger.ilp.core.LedgerTransfer;
//import org.interledger.ilp.core.exceptions.InsufficientAmountException;
//import org.interledger.ilp.ledger.account.LedgerAccount;

/**
 * LedgerTransfer builder
 *
 * @author mrmx
 */
// FIXME: Replaced with SimpleLedgerTransfer, Remove
public class LedgerTransferBuilder {
//    private LedgerAddressParser ledgerAddressParser;
//    private String destinationAddress;
//    private String from, to;
//    private MonetaryAmount amount;
//    private String data;
//    private String note;
//    private Condition condition;
//    private Date expiry;
//
//    private LedgerTransferBuilder() {
//    }
//
//    public static LedgerTransferBuilder instance() {
//        return new LedgerTransferBuilder().withLedgerAddressParser(new SimpleLedgerAddressParser());
//    }
//
//    public LedgerTransferBuilder withLedgerAddressParser(LedgerAddressParser ledgerAddressParser) {
//        this.ledgerAddressParser = ledgerAddressParser;
//        return this;
//    }
//
//    public LedgerTransferBuilder destination(String destinationAddress) {
//        this.destinationAddress = destinationAddress;
//        return this;
//    }
//
//    public LedgerTransferBuilder from(LedgerAccount from) {
//        this.from = from.getName();
//        return this;
//    }
//
//    public LedgerTransferBuilder from(String from) {
//        this.from = from;
//        return this;
//    }
//
//    public LedgerTransferBuilder to(LedgerAccount to) {
//        this.to = to.getName();
//        return this;
//    }
//
//    public LedgerTransferBuilder to(String to) {
//        this.to = to;
//        return this;
//    }
//
//    public LedgerTransferBuilder amount(MonetaryAmount amount) {
//        this.amount = amount;
//        return this;
//    }
//
//    public LedgerTransferBuilder data(String data) {
//        this.data = data;
//        return this;
//    }
//
//    public LedgerTransferBuilder note(String note) {
//        this.note = note;
//        return this;
//    }
//
//    public LedgerTransferBuilder with(Condition condition) {
//        this.condition = condition;
//        return this;
//    }
//
//    public LedgerTransferBuilder with(Date expiry) {
//        this.expiry = expiry;
//        return this;
//    }
//
//    public LedgerTransfer build() {
//        if (amount == null) {
//            throw new IllegalArgumentException("null amount");
//        }
//        if (!amount.isPositive()) {
//            throw new InsufficientAmountException(amount.toString());
//        }        
//        LedgerTransferImpl ledgerTransfer = new LedgerTransferImpl();
//        ledgerTransfer.destinationAddress = destinationAddress;
//        ledgerTransfer.from = from;
//        if(StringUtils.isEmpty(to)) {
//            ledgerAddressParser.parse(destinationAddress);
//            to = ledgerAddressParser.getAccountName();
//        }
//        ledgerTransfer.to = to;
//        ledgerTransfer.amount = amount.getNumber().toString();
//        ledgerTransfer.data = data;
//        ledgerTransfer.note = note;
//        ledgerTransfer.condition = condition;
//        ledgerTransfer.expiry = expiry;
//        return ledgerTransfer;
//    }
//
//    public static final class LedgerTransferImpl implements LedgerTransfer {
//
//        private InterledgerPacketHeader packetHeader;
//        private String destinationAddress;
//        private String from, to;
//        private String amount;
//        private String data;
//        private String note;
//        private Condition condition;
//        private Date expiry;
//
//        public InterledgerPacketHeader getHeader() {
//            if (packetHeader == null) {
//                packetHeader = new InterledgerPacketHeader(destinationAddress, amount, condition, expiry);
//            }
//            return packetHeader;
//        }
//
//        public AccountURI getFromAccount() {
//            return from;
//        }
//
//        public String getToAccount() {
//            return to;
//        }
//
//        public String getAmount() {
//            return amount;
//        }
//
//        public String getData() {
//            return data;
//        }
//
//        public String getNoteToSelf() {
//            return note;
//        }
//
//        @Override
//        public String toString() {
//            return "LedgerTransfer["
//                    + "destination:" + destinationAddress
//                    + " from:" + from
//                    + " to:" + to
//                    + " amount:" + amount
//                    + "]";
//        }
//    }
}

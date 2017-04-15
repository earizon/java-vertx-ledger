package org.interledger.everledger.ledger.transfer;

import javax.money.MonetaryAmount;

import org.interledger.everledger.ledger.account.LedgerAccount;

public class Debit extends LedgerPartialEntry {
    public Debit(LedgerAccount account, MonetaryAmount amount){
        super(account, amount);
    }

    public boolean getAuthorized() {
        /*
         * FIXME:
         *  REF: five-bells-ledger/src/controllers/transfers.js
         *  
         *  @api {put} /transfers/:id Propose or Prepare Transfer
         *
         * @apiDescription Creates or updates a Transfer object. The transfer is
         *    "proposed" if it contains debits that do not have `"authorized": true`.
         *    To set the `authorized` field, call this method
         *    [authenticated](#authentication) as owner of the account to be debited,
         *    or as an admin. The transfer is "prepared" when all debits have been
         *    authorized. When a transfer becomes prepared, it executes immediately if
         *    there is no condition. If an `execution_condition` is specified, the
         *    funds are held until a
         *    [matching fulfillment is submitted](#api-Transfer_Methods-PutTransferFulfillment)
         *    or the `expires_at` time is reached.
         */
        
        return true;
    }
}

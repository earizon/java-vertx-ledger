package com.everis.everledger.impl

import com.everis.everledger.AuthInfo
import com.everis.everledger.util.Config
import com.everis.everledger.ifaces.account.IfaceAccount
import io.vertx.core.json.JsonObject
import org.interledger.InterledgerAddress
import java.net.URI
import java.net.URISyntaxException
import java.security.PublicKey
import javax.money.MonetaryAmount
import org.javamoney.moneta.Money

data class SimpleAccount (// TODO:(0) Convert to "inmutable" object.
        val uniqId: String,
        val balance: MonetaryAmount,
        val minimumAllowedBalance: MonetaryAmount,
        val disabled: Boolean = false,
        private val authInfo : AuthInfo
        ) : IfaceAccount {

    // START IMPLEMENTATION IfaceLocalAccount {
    override fun getLocalID() : String = uniqId

    override fun getLocalBalance(): MonetaryAmount  = balance
    override fun getBalanceAsString(): String {
        val _balance = balance.number
        return if (_balance.amountFractionDenominator == 0L)
                   _balance.longValueExact().toString()
               else
                   _balance.doubleValueExact().toString()
    }

 // override fun credit(amount: MonetaryAmount): SimpleAccount {
 //     assert(amount.isPositive)
 //     return copy(balance = balance.add(amount))
 // }

 // override fun debit(amount: MonetaryAmount): SimpleAccount {
 //     assert(amount.isPositive)
 //     return  copy(balance = balance.subtract(amount))
 // }
    // } END   IMPLEMENTATION IfaceLocalAccount

    // START IMPLEMENTATION IfaceILPSpecAccount {
    val ilpLedger = InterledgerAddress.builder().value(Config.ilpPrefix).build()
    override fun getILPLedger(): InterledgerAddress = ilpLedger

    override fun getId(): String  = localID

    override fun getName(): String = authInfo.login

    override fun getAddress(): InterledgerAddress =
            InterledgerAddress.builder().value(Config.ilpPrefix + uniqId).build()

    override fun getILPBalance(): MonetaryAmount =  localBalance

    override fun isDisabled(): Boolean  = disabled

    override fun getPublicKey(): PublicKey {
        throw RuntimeException("Not implemented")
    }

    override fun getCertificateFingerprint(): ByteArray {
        return byteArrayOf() // TODO:(0) FIXME
    }

    override fun getILPMinimumAllowedBalance(): MonetaryAmount = minimumAllowedBalance
    // } END   IMPLEMENTATION IfaceILPSpecAccount

    override fun getAuthInfo() : AuthInfo  = authInfo
}
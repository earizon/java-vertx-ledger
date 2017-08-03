package com.everis.everledger.impl

import com.everis.everledger.Config
import com.everis.everledger.ifaces.account.IfaceAccount
import org.interledger.InterledgerAddress
import java.net.URI
import java.net.URISyntaxException
import java.security.PublicKey
import javax.money.MonetaryAmount
import org.javamoney.moneta.Money

data class SimpleAccount (// TODO:(0) Convert to "inmutable" object.
        val uniqId: String,
        val balance: MonetaryAmount = Money.of(0, Config.ledgerCurrencyCode),
        val minimumAllowedBalance: MonetaryAmount = Money.of(0, Config.ledgerCurrencyCode),
        val disabled: Boolean = false
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

    override fun credit(amount: MonetaryAmount): SimpleAccount {
        assert(amount.isPositive)
        return copy(balance = balance.add(amount))
    }

    override fun debit(amount: MonetaryAmount): SimpleAccount {
        assert(amount.isPositive)
        return  copy(balance = balance.subtract(amount))
    }
    // } END   IMPLEMENTATION IfaceLocalAccount

    // START IMPLEMENTATION IfaceILPSpecAccount {
    override fun getILPLedger(): InterledgerAddress =
            InterledgerAddress.builder().value(Config.ilpPrefix).build()

    override fun getId(): String {
        val baseURI = Config.publicURL.toString()
        val sURI = baseURI + "accounts/" + getLocalID()
        try {
            val result = URI(sURI)
            return result.toString()
        } catch (e: URISyntaxException) {
            throw RuntimeException("Can't create URI from string '$sURI'")
        }
    }

    override fun getName(): String {
        return getLocalID()
    }

    override fun getAddress(): InterledgerAddress =
            InterledgerAddress.builder().value(Config.ilpPrefix + "." + uniqId).build()

    override fun getILPBalance(): MonetaryAmount =  localBalance

    override fun isDisabled(): Boolean  = disabled

    override fun getPublicKey(): PublicKey {
        throw RuntimeException("Not implemented")
    }

    override fun getCertificateFingerprint(): ByteArray {
        return byteArrayOf() // TODO:(0) FIXME
    }

    override fun getILPMinimumAllowedBalance(): MonetaryAmount {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    // } END   IMPLEMENTATION IfaceILPSpecAccount

}
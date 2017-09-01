package com.everis.everledger.impl.manager
import com.everis.everledger.AccessRoll
import com.everis.everledger.AuthInfo
import com.everis.everledger.util.Config
import com.everis.everledger.ifaces.account.IfaceAccount
import com.everis.everledger.ifaces.account.IfaceAccountManager
import com.everis.everledger.ifaces.account.IfaceLocalAccount
import com.everis.everledger.impl.SimpleAccount
import com.everis.everledger.util.AuthManager
import com.everis.everledger.util.ILPExceptionSupport
import org.javamoney.moneta.Money
import org.slf4j.LoggerFactory

import java.util.TreeMap

import java.util.function.Consumer
private val ILP_HOLD_ACCOUNT = "@@HOLD@@"

private val log             = LoggerFactory.getLogger(SimpleAccountManager::class.java)
/**
 * Simple in-memory `LedgerAccountManager`.
 */
public object SimpleAccountManager
    : IfaceAccountManager {
    private var accountMap: MutableMap<String, IfaceAccount> = resetAccounts();

    private fun resetAccounts() : MutableMap<String, IfaceAccount> {
        accountMap = TreeMap<String, IfaceAccount>()
        return accountMap
    }

    override fun getHOLDAccountILP(): IfaceAccount {
        var existingAccount : IfaceAccount? = accountMap.get(ILP_HOLD_ACCOUNT)
        existingAccount?.let { return existingAccount }
        // Create and store
        val ilpHold = SimpleAccount(ILP_HOLD_ACCOUNT,
            Money.of(0, Config.ledgerCurrencyCode), // TODO:(Kotlin) once kotlinified remove defaults
            Money.of(0, Config.ledgerCurrencyCode),
            false,
            AuthInfo(Config.test_ethereum_address_escrow,
                    /*login*/""+Math.random()+""+Math.random(),
                    /*passw*/""+Math.random()+""+Math.random(), AccessRoll.USER ) )
        return store(ilpHold, false)
    }

    // } end IfaceILPSpecAccountManager implementation

    // start IfaceILPSpecAccountManager implementation {
    init {
        resetAccounts()
    }

    override fun store(account: IfaceAccount, update: Boolean) :IfaceAccount {
        if (update == false && hasAccount(account.id)) {
            throw ILPExceptionSupport.createILPForbiddenException("account already exists")
        }
        accountMap.put(account.id, account)
        return account;
    }

    override fun hasAccount(name: String): Boolean {
        return accountMap.containsKey(name)
    }

    override fun getAccountById(id: String): IfaceAccount {
        accountMap.get(id)?.let { return accountMap.get(id)!! }
        log.warn("'$id' account not found")
        throw ILPExceptionSupport.createILPNotFoundException()
    }

    override fun getAccounts(page: Int, pageSize: Int): MutableCollection<IfaceAccount> {
        val accounts : MutableList<IfaceAccount> = mutableListOf()
        accountMap.values
                .stream()
                // .filter((LedgerAccount a) -> !a.getName().equals(ILP_HOLD_ACCOUNT))
                .forEach(Consumer<IfaceAccount> { accounts.add(it) })
        return accounts
    }

    override fun getTotalAccounts(): Int {
        return accountMap.size
    }

    fun developerTestingReset() {
        if (!Config.unitTestsActive) {
            throw RuntimeException("developer.unitTestsActive must be true @ application.conf " + "to be able to reset tests")
        }
        //
        SimpleAccountManager.resetAccounts() // STEP 1: Reset accounts to initial state

        // STEP 2: Create an account for each pre-configured user in AuthManager:
        val devUsers : Map<AuthInfo, IntArray/*[balance,minAllowedBalance]*/> = AuthManager.configureDevelopmentEnvironment()/*blance*/
        for ( (auth, bal_l) in devUsers) {
             val account = SimpleAccount(auth.id,
                 Money.of(bal_l[0], Config.ledgerCurrencyCode),
                 Money.of(bal_l[1], Config.ledgerCurrencyCode),
                 false,
                 auth)
            SimpleAccountManager.store(account, false)

        }
    }

}
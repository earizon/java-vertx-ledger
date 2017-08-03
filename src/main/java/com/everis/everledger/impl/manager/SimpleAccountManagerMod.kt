package com.everis.everledger.impl.manager
import com.everis.everledger.Config
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
            false)
        return store(ilpHold, false)
    }

    // } end IfaceILPSpecAccountManager implementation

    // start IfaceILPSpecAccountManager implementation {
    init {
        resetAccounts()
    }

    override fun store(account: IfaceAccount, update: Boolean) :IfaceAccount {
        if (update == false && hasAccount(account.name)) {
            throw ILPExceptionSupport.createILPForbiddenException("account already exists")
        }
        accountMap.put(account.name, account)
        return account;
    }

    override fun hasAccount(name: String): Boolean {
        return accountMap.containsKey(name)
    }

    override fun getAccountByName(name: String): IfaceAccount {
        accountMap.get(name)?.let { return accountMap.get(name)!! }
        log.warn("'$name' account not found")
        throw ILPExceptionSupport.createILPNotFoundException()
    }

    override fun getAccounts(page: Int, pageSize: Int): MutableCollection<IfaceLocalAccount> {
        val accounts : MutableList<IfaceLocalAccount> = mutableListOf()
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
        if (!com.everis.everledger.Config.unitTestsActive) {
            throw RuntimeException("developer.unitTestsActive must be true @ application.conf " + "to be able to reset tests")
        }
        //
        SimpleAccountManager.resetAccounts() // STEP 1: Reset accounts to initial state

        // STEP 2: Create an account for each pre-configured user in AuthManager:
        val devUsers = AuthManager.configureDevelopmentEnvironment()/*blance*/
        val users = devUsers.keys
        for (ai in users) {
            val account = SimpleAccount(ai.getId(),
                    Money.of(0, Config.ledgerCurrencyCode), // TODO:(Kotlin) once kotlinified remove defaults
                    Money.of(0, Config.ledgerCurrencyCode),
                    false)
            SimpleAccountManager.store(account, false)
        }
    }

}
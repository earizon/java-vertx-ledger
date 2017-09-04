package com.everis.everledger.impl.manager
import com.everis.everledger.AccessRoll
import com.everis.everledger.AuthInfo
import com.everis.everledger.util.Config
import com.everis.everledger.ifaces.account.IfaceAccount
import com.everis.everledger.ifaces.account.IfaceAccountManager
import com.everis.everledger.impl.SimpleAccount
import com.everis.everledger.util.AuthManager
import com.everis.everledger.util.ILPExceptionSupport
import org.javamoney.moneta.Money
import org.slf4j.LoggerFactory

import java.util.TreeMap

import java.util.function.Consumer
private val ILP_HOLD_ACCOUNT =
        if   (Config.unitTestsActive) Config.test_ethereum_address_escrow
        else                          Config.ethereum_address_escrow

private val log             = LoggerFactory.getLogger(SimpleAccountManager::class.java)
/**
 * Simple in-memory `LedgerAccountManager`.
 */
public object SimpleAccountManager
    : IfaceAccountManager {
    private var Id2AccountMap   : MutableMap<String, IfaceAccount> = TreeMap<String, IfaceAccount>()
    // TODO:(?)
    //    "patch" solution to allow reuse of five-bells-ledger tests
    //    using login as account identifiers
    private var login2AccountMap: MutableMap<String, IfaceAccount> = TreeMap<String, IfaceAccount>()

    private fun resetAccounts() {
        Id2AccountMap    = TreeMap<String, IfaceAccount>()
        login2AccountMap = TreeMap<String, IfaceAccount>()
    }

    override fun getHOLDAccountILP(): IfaceAccount {
        var existingAccount : IfaceAccount? = Id2AccountMap.get(ILP_HOLD_ACCOUNT)
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

    init {
        resetAccounts()
    }

    // start IfaceLocalAccountManager implementation {
    override fun store(account: IfaceAccount, update: Boolean) :IfaceAccount {
        if (update == false && hasAccount(account.id)) {
            throw ILPExceptionSupport.createILPForbiddenException("account already exists")
        }
        Id2AccountMap   .put(account.id            , account)
        login2AccountMap.put(account.authInfo.login, account)
        return account;
    }

    override fun hasAccount(idOrLogin: String): Boolean {
        return Id2AccountMap   .containsKey(idOrLogin)
            || login2AccountMap.containsKey(idOrLogin)
    }

    override fun getAccountById(idOrLogin: String): IfaceAccount {
        Id2AccountMap   .get(idOrLogin)?.let { return Id2AccountMap   .get(idOrLogin)!! }
        login2AccountMap.get(idOrLogin)?.let { return login2AccountMap.get(idOrLogin)!! }
        log.warn("'$idOrLogin' account not found")
        throw ILPExceptionSupport.createILPNotFoundException()
    }

   override fun getAccounts(page: Int, pageSize: Int): MutableCollection<IfaceAccount> {
        val accounts : MutableList<IfaceAccount> = mutableListOf()
        Id2AccountMap.values
                .stream()
                // .filter((LedgerAccount a) -> !a.getName().equals(ILP_HOLD_ACCOUNT))
                .forEach(Consumer<IfaceAccount> { accounts.add(it) })
        return accounts
    }

    override fun getTotalAccounts(): Int {
        return Id2AccountMap.size
    }


    override fun authInfoMatchAccount(account: IfaceAccount, ai: AuthInfo): Boolean =
            ai.id  == account.id
                    || ai.id == account.authInfo.login

    // } end IfaceLocalAccountManager implementation

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
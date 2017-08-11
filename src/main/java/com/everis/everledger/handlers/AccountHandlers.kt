package com.everis.everledger.handlers

import com.everis.everledger.util.Config
import com.everis.everledger.ifaces.account.IfaceAccount
import com.everis.everledger.impl.SimpleAccount
import com.everis.everledger.impl.manager.SimpleAccountManager
import com.everis.everledger.util.AuthManager
import com.everis.everledger.util.ConversionUtil
import com.everis.everledger.util.ILPExceptionSupport
import com.everis.everledger.util.JsonObjectBuilder
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import org.apache.commons.lang3.StringUtils
import org.javamoney.moneta.Money
import org.slf4j.LoggerFactory

private val AM = SimpleAccountManager
class AccountsHandler
private constructor() : RestEndpointHandler(arrayOf(HttpMethod.GET, HttpMethod.PUT, HttpMethod.POST), arrayOf("accounts/:" + PARAM_NAME)) {

    override fun handleGet(context: RoutingContext) {
        val ai = AuthManager.authenticate(context, true)
        val accountName = getAccountName(context)
        val isAuthenticated = ai.roll == "admin" || ai.id  == accountName
        val account = AM.getAccountByName(accountName)
        val result = accountToJsonObject(account, isAuthenticated)
        response(context, HttpResponseStatus.OK, result)
    }

    override fun handlePut(context: RoutingContext) {
        log.debug("Handing put account")
        AuthManager.authenticate(context)
        val accountName = getAccountName(context)
        val exists = AM.hasAccount(accountName)
        val data = RestEndpointHandler.getBodyAsJson(context)
        var data_id: String? = data.getString("id")
        if (data_id == null) data_id = data.getString("name") // Second chance
        if (data_id == null) {
            throw ILPExceptionSupport.createILPBadRequestException("id no provided")
        }
        var password = data.getString("password")
        if (password == null && exists == true) {
            password = AuthManager.getUsers()[accountName]!!.pass
        }
        if (password == null) {
            throw ILPExceptionSupport.createILPBadRequestException("password no provided for id:" + data_id)
        }
        var li = data_id.lastIndexOf('/')
        if (li < 0) li = -1
        data_id = data_id.substring(li + 1)
        if (accountName != data_id) {
            throw ILPExceptionSupport.createILPBadRequestException(
                    "id in body '$data_id'doesn't match account name '$accountName' in URL")
        }
        if (exists && !accountName.equals(accountName, ignoreCase = true)) {
            throw ILPExceptionSupport.createILPBadRequestException()
        }

        log.debug("Put data: {} to account {}", data, accountName)
        var sMinAllowVal: String? = data.getString(PARAM_MIN_ALLOWED_BALANCE)
        if (sMinAllowVal == null) sMinAllowVal = "0" // TODO:(1) Arbitrary value. Use Config....
        sMinAllowVal = sMinAllowVal.toLowerCase().replace("infinity", "1000000000000000") // TODO:(0) Arbitrary value. Use Config...
        val minAllowedBalance = ConversionUtil.toNumber(sMinAllowVal)

        val balance = if (data.containsKey(PARAM_BALANCE))
            ConversionUtil.toNumber(data.getValue(PARAM_BALANCE))
        else
            ConversionUtil.toNumber("0")

        val account = SimpleAccount(
                accountName,
                Money.of(balance, Config.ledgerCurrencyCode),
                Money.of(minAllowedBalance, Config.ledgerCurrencyCode),
                false
        )

        AM.store(account, true /*update if already exists*/)
        AuthManager.setUser(accountName, password, "user"/*roll*/ /* TODO:(1) allow user|admin|...*/)

        // if(data.containsKey(PARAM_DISABLED)) {
        //     ((SimpleLedgerAccount)account).setDisabled(data.getBoolean(PARAM_DISABLED, false));
        // }
        response(context, if (exists) HttpResponseStatus.OK else HttpResponseStatus.CREATED,
                JsonObjectBuilder.create().from(account))
    }

    override fun handlePost(context: RoutingContext) {
        handlePut(context)
    }

    private fun getAccountName(context: RoutingContext): String {
        val accountName = context.request().getParam(PARAM_NAME)
        if (StringUtils.isBlank(accountName)) {
            throw ILPExceptionSupport.createILPBadRequestException(PARAM_NAME + "not provided")
        }
        return accountName
    }

    private fun accountToJsonObject(account: IfaceAccount, isAuthenticated: Boolean): JsonObject {
        var ledger = Config.publicURL.toString()
        if (ledger.endsWith("/")) {
            ledger = ledger.substring(0, ledger.length - 1)
        }

        val build = JsonObjectBuilder.create()
                .put("id", account.id)
                .put("name", account.localID)
                .put("ledger", ledger)
        if (isAuthenticated) {
            build
                    .put("balance", account.balanceAsString)
                    // .put("connector", "??????" /* TODO:(?) Recheck */)
                    .put("is_disabled", account.isDisabled)
                    .put("minimum_allowed_balance", account.ilpMinimumAllowedBalance.number.toString())
        }

        return build.get()
    }

    companion object {

        private val log = LoggerFactory.getLogger(AccountsHandler::class.java)

        private val PARAM_NAME = "name"
        private val PARAM_BALANCE = "balance"
        private val PARAM_MIN_ALLOWED_BALANCE = "minimum_allowed_balance"

        fun create(): AccountsHandler {
            return AccountsHandler()
        }
    }
}


class AccountsListHandler private constructor() : RestEndpointHandler(arrayOf(HttpMethod.GET), arrayOf("accounts")) {

    override fun handleGet(context: RoutingContext) {
        val ai = AuthManager.authenticate(context)
        if (!ai.isAdmin) {
            throw ILPExceptionSupport.createILPForbiddenException()
        }
        val request = RestEndpointHandler.getBodyAsJson(context)
        val page = request.getInteger("page", 1)!!
        val pageSize = request.getInteger("pageSize", 10)!!
        context.response()
                .putHeader("content-type", "application/json; charset=utf-8") //TODO create decorator
                .end(Json.encode(AM.getAccounts(page, pageSize)))
    }

    companion object {
        private val log = LoggerFactory.getLogger(AccountsListHandler::class.java)
        fun create(): AccountsListHandler = AccountsListHandler()
    }

}
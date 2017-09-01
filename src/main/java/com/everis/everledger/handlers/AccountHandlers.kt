package com.everis.everledger.handlers

import com.everis.everledger.AccessRoll
import com.everis.everledger.AuthInfo
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
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import org.javamoney.moneta.Money
import org.slf4j.LoggerFactory


private val PARAM_ID = "id"
private val PARAM_NAME = "name"
private val PARAM_BALANCE = "balance"
private val PARAM_MIN_ALLOWED_BALANCE = "minimum_allowed_balance"
private val PARAM_PASSWORD = "password"

private fun accountToJsonObject(account: IfaceAccount, isAuthenticated: Boolean): JsonObject {
    // TODO:(0) Move to JSON Support?

    val build = JsonObjectBuilder.create()
            .put("id", Config.publicURL.toString()+"/accounts/"+account.id)
            .put("name", account.authInfo.login)
    //      .put("ledger", Config.publicURL.toString())
    if (isAuthenticated) {
        build
                .put("balance", account.balanceAsString)
                // .put("connector", "??????" /* TODO:(?) Recheck */)
                .put("is_disabled", account.isDisabled)
                .put("minimum_allowed_balance", account.ilpMinimumAllowedBalance.number.toString())
    }

    return build.get()
}

class AccountsHandler
private constructor() : RestEndpointHandler(
        arrayOf(HttpMethod.GET, HttpMethod.PUT, HttpMethod.POST), arrayOf("accounts/:" + PARAM_ID)) {

    override fun handleGet(context: RoutingContext) {
        val ai = AuthManager.authenticate(context, true)

    // private fun getAccountName(context: RoutingContext): String {
    //     val accountName =
    //         throw ILPExceptionSupport.createILPBadRequestException(PARAM_NAME + "not provided")
    //     }
    //     return accountName
    // }
        val data_id = ConversionUtil.parseNonEmptyString(context.request().getParam(PARAM_ID))
        val isAuthenticated = ai.roll == AccessRoll.ADMIN || ai.id  == data_id
        val account = SimpleAccountManager.getAccountById(data_id)
        val result = accountToJsonObject(account, isAuthenticated)
        response(context, HttpResponseStatus.OK, result)
    }

    override fun handlePut(context: RoutingContext) {
        log.debug("Handing put account")
        AuthManager.authenticate(context)
        val id = ConversionUtil.parseNonEmptyString(context.request().getParam(PARAM_ID))
        val exists = SimpleAccountManager.hasAccount(id)
        val data = RestEndpointHandler.getBodyAsJson(context)
        val data_id01 = data.getString(PARAM_ID)
        val data_id01_offset = data_id01.lastIndexOf('/')
        val data_id = if (data_id01_offset >= 0) data_id01.substring(data_id01_offset+1) else data_id01

        if (id != data_id) {
            throw ILPExceptionSupport.createILPBadRequestException(
                    "id in body '$data_id' doesn't match account id '$id' in URL")
        }

        // TODO:(0) Check data_id is valid (for example valid Ethereum address)
        val data_name: String = data.getString(PARAM_NAME)
                ?: throw ILPExceptionSupport.createILPBadRequestException("id not provided")

        var data_password = data.getString(PARAM_PASSWORD) ?:
                if (exists)  AuthManager.getUsers()[data_name]!!.pass
                else throw ILPExceptionSupport.createILPBadRequestException("password not provided for id:" + data_id)


        if (exists && !data_name.equals(data_name, ignoreCase = true)) {
            throw ILPExceptionSupport.createILPBadRequestException()
        }

        log.info("Put data: {} to account {}", data, data_name)
        var sMinAllowVal: String? = data.getString(PARAM_MIN_ALLOWED_BALANCE)
        if (sMinAllowVal == null) sMinAllowVal = "0" // TODO:(1) Arbitrary value. Use Config....
        sMinAllowVal = sMinAllowVal.toLowerCase().replace("infinity", "1000000000000000") // TODO:(0) Arbitrary value. Use Config...
        val minAllowedBalance = ConversionUtil.toNumber(sMinAllowVal)

        val balance = if (data.containsKey(PARAM_BALANCE))
            ConversionUtil.toNumber(data.getValue(PARAM_BALANCE))
        else
            ConversionUtil.toNumber("0")

        val ai = AuthInfo(data_id, data_name, data_password, AccessRoll.USER )

        val account = SimpleAccount(
                data_id,
                Money.of(balance, Config.ledgerCurrencyCode),
                Money.of(minAllowedBalance, Config.ledgerCurrencyCode),
                false, ai )

        SimpleAccountManager.store(account, true /*update if already exists*/)
        AuthManager.setUser(ai)

        // if(data.containsKey(PARAM_DISABLED)) {
        //     ((SimpleLedgerAccount)account).setDisabled(data.getBoolean(PARAM_DISABLED, false));
        // }
        response(context,
                if (exists) HttpResponseStatus.OK else HttpResponseStatus.CREATED,
                accountToJsonObject(account, true)  )
    }

    override fun handlePost(context: RoutingContext) {
        handlePut(context)
    }

    companion object {

        private val log = LoggerFactory.getLogger(AccountsHandler::class.java)

       fun create(): AccountsHandler {
            return AccountsHandler()
        }
    }
}


class AccountsListHandler private constructor() :
        RestEndpointHandler(arrayOf(HttpMethod.GET), arrayOf("accounts")) {

    override fun handleGet(context: RoutingContext) {
        val ai = AuthManager.authenticate(context)
        if (!ai.isAdmin) {
            throw ILPExceptionSupport.createILPForbiddenException()
        }
        val request = RestEndpointHandler.getBodyAsJson(context)
        val page = request.getInteger("page", 1)!!
        val pageSize = request.getInteger("pageSize", 10)!!
        val response : JsonArray  = JsonArray()
        for (account in SimpleAccountManager.getAccounts(page, pageSize)) {
            response.add(accountToJsonObject(account, true))
        }
        context.response()
                .putHeader("content-type", "application/json; charset=utf-8") //TODO create decorator
                .end(response.encode())
    }

    companion object {
        private val log = LoggerFactory.getLogger(AccountsListHandler::class.java)
        fun create(): AccountsListHandler = AccountsListHandler()
    }

}
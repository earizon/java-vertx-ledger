package com.everis.everledger.util

import com.everis.everledger.HTTPInterledgerException
import org.apache.commons.lang3.math.NumberUtils
import org.interledger.Condition
import org.interledger.Fulfillment
import org.interledger.InterledgerAddress
import org.interledger.ilp.InterledgerError
import org.interledger.ledger.model.LedgerInfo
import org.interledger.ledger.money.format.LedgerSpecificDecimalMonetaryAmountFormat
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.lang.Long
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.security.PublicKey
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.regex.Pattern
import javax.money.CurrencyUnit
import javax.money.Monetary
import javax.money.format.MonetaryAmountFormat

object TimeUtils {
    val future = ZonedDateTime.ofInstant( Date(Long.MAX_VALUE).toInstant(), ZoneId.of("Europe/Paris"))
    val testingDate = ZonedDateTime.parse("2015-06-16T00:00:00.000Z")
    var ilpFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
}

object ConversionUtil {

    fun toNumber(value: Any): Number = if (value is Number) value else NumberUtils.createNumber(value.toString())

    fun fulfillmentToBase64(FF: Fulfillment): String {
        var response = Base64.getEncoder().encodeToString(FF.preimage)
        response = response.substring(0, response.indexOf('='))
        return response
    }

    /**
     * Parses a URI formatted crypto-condition
     *
     * @param uri
     * *  The crypto-condition formatted as a uri.
     * *
     * @return
     * *  The crypto condition
     */
    fun parseURI(uri: URI): Condition {
        //based strongly on the five bells implementation at
        //https://github.com/interledgerjs/five-bells-condition (7b6a97990cd3a51ee41b276c290e4ae65feb7882)
        if ("ni" != uri.scheme) {
            throw RuntimeException("Serialized condition must start with 'ni:'")
        }

        val CONDITION_REGEX_STRICT = "^ni://([A-Za-z0-9_-]?)/sha-256;([a-zA-Z0-9_-]{0,86})\\?(.+)$"

        //the regex covers the entire uri format including the 'ni:' scheme
        val m = Pattern.compile(CONDITION_REGEX_STRICT).matcher(uri.toString())

        if (!m.matches()) {
            throw RuntimeException("Invalid condition format")
        }

        val fingerprint = Base64.getUrlDecoder().decode(m.group(2))
        return Condition.builder().hash(fingerprint).build()
    }
}

object ILPExceptionSupport {
    private val selfAddress = InterledgerAddress.builder().value(Config.ilpPrefix).build()
    /**
     * Well known ILP Errors as defined in the RFCs
     * @param errCode
     *
     * @param data
     */
    fun createILPException(httpErrCode: Int, errCode: InterledgerError.ErrorCode, data: String): HTTPInterledgerException =
        HTTPInterledgerException(httpErrCode, InterledgerError(errCode, /* triggeredBy */ selfAddress,
                ZonedDateTime.now(), ArrayList<InterledgerAddress>(), /*self Address*/ selfAddress, data))

    // Next follow some wrappers arount createILPException, more human-readable.
    // ----------- Internal --------------
    fun createILPInternalException(data: String): HTTPInterledgerException =
         createILPException(500, InterledgerError.ErrorCode.T00_INTERNAL_ERROR, data)

    // ------------ Unauthorized ------------- // TODO:(RFC) Use new ErrorCode.??_UNAUTHORIZED
    @JvmOverloads fun createILPUnauthorizedException(data: String = "Unauthorized"): HTTPInterledgerException =
        createILPException(401, InterledgerError.ErrorCode.T00_INTERNAL_ERROR, data)

    // ----------- Forbidden --------------// TODO:(RFC) Use new ErrorCode.??_FORBIDDEN
    @JvmOverloads fun createILPForbiddenException(data: String = "Forbidden"): HTTPInterledgerException =
        createILPException(403, InterledgerError.ErrorCode.T00_INTERNAL_ERROR, "data")

    // ------------ NotFound ------------- // TODO:(ILP) Use new ErrorCode.??_NOT_FOUND
    @JvmOverloads fun createILPNotFoundException(data: String = "Not Found"): HTTPInterledgerException =
        createILPException(404, InterledgerError.ErrorCode.T00_INTERNAL_ERROR, data)

    // ------------- BadRequest ------------
    @JvmOverloads fun createILPBadRequestException(data: String = "Forbidden"): HTTPInterledgerException =
        createILPException(400, InterledgerError.ErrorCode.F00_BAD_REQUEST, data)

    // ------------- Unprocessable Entity ------------
    @JvmOverloads fun createILPUnprocessableEntityException(data: String = "Unprocessable"): HTTPInterledgerException =
        createILPException(422, InterledgerError.ErrorCode.F00_BAD_REQUEST, data)
}

private data class SimpleLedgerInfo(
        private val ilpAddress: InterledgerAddress,
        private val precision: Int,
        private val scale: Int,
        private val currencyUnit: CurrencyUnit,
        private val monetaryAmountFormat: MonetaryAmountFormat,
        private val conditionSignPublicKey: PublicKey,
        private val notificationSignPublicKey: PublicKey) : LedgerInfo {
    override fun getAddressPrefix()= ilpAddress
    override fun getPrecision() = precision
    override fun getScale() = scale
    override fun getCurrencyUnit() = currencyUnit
    override fun getMonetaryAmountFormat() = monetaryAmountFormat
    override fun getConditionSignPublicKey() = conditionSignPublicKey
    override fun getNotificationSignPublicKey() = notificationSignPublicKey
    override fun getId() = "" // TODO:(?)
}

object Config {
    val CONFIG_FILE = "application.conf"
    val log = LoggerFactory.getLogger(Config::class.java)
    val publicURL: URL
    val prop = Properties()

    init {
        try {
            val inputStream = FileInputStream(CONFIG_FILE)
            prop.load(inputStream)
            val keysEnum = prop.keys()
            while (keysEnum.hasMoreElements()) {
                val key = keysEnum.nextElement() as String
                prop.setProperty(key, prop.getProperty(key).trim { it <= ' ' })
            }
        } catch (e: Exception) {
            throw RuntimeException("Can not set-up ILP config for file " + CONFIG_FILE +
                    "due to " + e.toString() + "\n")
        }

    }

    val vertxBodyLimit = getInteger("vertx.request.bodyLimit").toLong()

    val unitTestsActive = getBoolean("developer.unitTestsActive")

    val debug = getBoolean("server.debug")
    val ilpPrefix = getString("ledger.ilp.prefix")
    val ledgerCurrencyCode = getString("ledger.currency.code")
    val ledgerCurrencySymbol = getString("ledger.currency.symbol")
    val ledgerPathPrefix = getString("ledger.path.prefix")

    val serverHost = getString("server.host")
    val serverPort = getInteger("server.port")
    val serverPublicHost = getString("server.public.host")
    val serverPublicPort = getString("server.public.port")
    val serverUseHTTPS = getBoolean("server.public.use_https")

    val ledgerPrecision = getInteger("ledger.precision")
    val ledgerScale = getInteger("ledger.scale")
    val ledgerVersion = getString("ledger.version")

    val tls_key = getString("server.tls_key")
    val tls_crt = getString("server.tls_cert")


    // Note: DSAPrivPubKeySupport.main support is used to configure pub/priv.key
    //    private static final String sConditionSignPrivateKey = getString("ledger.ed25519.conditionSignPrivateKey");
    private val sConditionSignPublicKey = getString("ledger.ed25519.conditionSignPublicKey")
    //    private static final String sNoticificationSignPrivateKey = getString("ledger.ed25519.notificationSignPrivateKey");
    private val sNoticificationSignPublicKey = getString("ledger.ed25519.notificationSignPublicKey")

    var ilpLedgerInfo: LedgerInfo = SimpleLedgerInfo(
            InterledgerAddress.builder().value(ilpPrefix).build(),
            ledgerPrecision,
            ledgerScale,
            Monetary.getCurrency(ledgerCurrencyCode),
            LedgerSpecificDecimalMonetaryAmountFormat(
                    Monetary.getCurrency(ledgerCurrencyCode), ledgerPrecision, ledgerScale) as MonetaryAmountFormat,
            DSAPrivPubKeySupport.loadPublicKey(sConditionSignPublicKey),
            DSAPrivPubKeySupport.loadPublicKey(sNoticificationSignPublicKey)
    )


    init {
        try {
            val inputStream = FileInputStream(CONFIG_FILE)
            prop.load(inputStream)
            val keysEnum = prop.keys()
            while (keysEnum.hasMoreElements()) {
                val key = keysEnum.nextElement() as String
                prop.setProperty(key, prop.getProperty(key).trim { it <= ' ' })
            }

            //
            val pubSsl = getBoolean("server.public.use_https")
            val pubHost = getString("server.public.host")
            val pubPort = getInteger("server.public.port")
            var prefixUri = getString("ledger.ilp.prefix")
            if (!prefixUri.startsWith("/")) {
                prefixUri = "/" + prefixUri
            } // sanitize
            try {
                publicURL = URL("http" + if (pubSsl) "s" else "", pubHost, pubPort, ledgerPathPrefix)
            } catch (e: MalformedURLException) {
                throw RuntimeException("Could NOT create URL with {"
                        + "pubHost='" + pubHost + "', "
                        + "pubPort='" + pubPort + "', "
                        + "prefixUri='" + prefixUri + "'}."
                        + " recheck server config")
            }

            log.info("serverPublicURL: {}", publicURL)
        } catch (e: Exception) {
            throw RuntimeException("Can not read application.conf due to " + e.toString())
        }

    }

    val indexHandlerMap: MutableMap<String, Any> = HashMap()

    init {
        indexHandlerMap.put("ilp_prefix", Config.ilpPrefix)
        indexHandlerMap.put("currency_code", Config.ledgerCurrencyCode)
        indexHandlerMap.put("currency_symbol", Config.ledgerCurrencySymbol)
        indexHandlerMap.put("precision", Config.ledgerPrecision)
        indexHandlerMap.put("scale", Config.ledgerScale)
        indexHandlerMap.put("version", Config.ledgerVersion)

        val services = HashMap<String, String>()

        // REF:
        //   - five-bells-ledger/src/controllers/metadata.js
        //   - plugin.js (REQUIRED_LEDGER_URLS) @ five-bells-plugin
        //   The conector five-bells-plugin of the js-ilp-connector expect a
        //   map urls { health:..., transfer: ...,}
        val base = Config.publicURL.toString()
        // Required by wallet
        services.put("health", base + "health")
        services.put("accounts", base + "accounts")
        services.put("transfer_state", base + "transfers/:id/state")
        services.put("account", base + "accounts/:name")
        services.put("websocket", base.replace("http://", "ws://")
                .replace("https://", "ws://") + "websocket")
        // Required by wallet & ilp (ilp-plugin-bells) connector
        services.put("transfer", base + "transfers/:id")
        services.put("transfer_fulfillment", base + "transfers/:id/fulfillment")
        services.put("message", base + "messages")
        services.put("auth_token", base + "auth_token")
        services.put("transfer_rejection", base + "not_available") // required by ilp-kit not RFCs
        indexHandlerMap.put("urls", services)
        indexHandlerMap.put("condition_sign_public_key",
                DSAPrivPubKeySupport.savePublicKey(Config.ilpLedgerInfo.conditionSignPublicKey))


        /* TODO:(0) Fill connectors with real connected ones */
        val connectors = ArrayList<Map<String, String>>()
        /* Formato connector ???
           ilp: "us.usd.red."
           "account":"https://red.ilpdemo.org/ledger/accounts/connector"
           "currency":"USD",
           ...
         */
        indexHandlerMap.put("connectors", connectors)
    }


    private fun getString(key: String): String {
        val result = prop.getProperty(key) ?: throw RuntimeException(key + " was not found in " + CONFIG_FILE)
        return result
    }

    private fun getBoolean(key: String): Boolean {
        val auxi = getString(key).toLowerCase()
        if (auxi == "false" || auxi == "0") return false
        if (auxi == "true" || auxi == "1") return true
        throw RuntimeException(key + "defined in " + CONFIG_FILE +
                " can not be parsed as boolean. Use true|false or 0|1")
    }

    private fun getInteger(key: String): Int {
        val auxi = getString(key).toLowerCase()
        try {
            val result = Integer.parseInt(auxi)
            return result
        } catch (e: Exception) {
            throw RuntimeException(key + "defined in " + CONFIG_FILE +
                    " can not be parsed as integer")
        }

    }

    /**
     * Execute this Config.main as java application to check that config is OK!
     */
    @JvmStatic fun main(args: Array<String>) {
        println(Config.debug)
    }

}

package com.everis.everledger.util

import io.vertx.core.AsyncResult
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.NumberUtils
import org.interledger.Condition
import org.interledger.Fulfillment
import org.interledger.InterledgerAddress
import org.interledger.InterledgerProtocolException
import org.interledger.ilp.InterledgerError
import org.interledger.ledger.model.LedgerInfo
import org.interledger.ledger.money.format.LedgerSpecificDecimalMonetaryAmountFormat
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.lang.Long
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.regex.Pattern
import javax.money.CurrencyUnit
import javax.money.Monetary
import javax.money.format.MonetaryAmountFormat

class JsonObjectBuilder : Supplier<JsonObject> {

    private var beanMap: MutableMap<String, Any> = HashMap<String, Any>()

    override fun get(): JsonObject {
        return JsonObject(beanMap)
    }

    // TODO:(1) recheck SuppressWarnings
    fun from(value: Any): JsonObjectBuilder {
        // TODO:(0) beanMap = Json.mapper.convertValue(value, Map<*, *>::class.java)
        return this
    }

    fun with(vararg pairs: Any): JsonObjectBuilder {
        if (pairs.size % 2 != 0) {
            throw IllegalArgumentException("Argument pairs must be even! " + pairs.size)
        }
        var i = 0
        while (i < pairs.size) {
            put(pairs[i], pairs[i + 1])
            i += 2
        }
        return this
    }

    fun put(key: Any, value: Any): JsonObjectBuilder {
        beanMap.put(key.toString(), value)
        return this
    }


    companion object {
        fun create(): JsonObjectBuilder = JsonObjectBuilder()
    }


}

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

    fun parseNonEmptyString(input: String) : String {
        val result = input.trim()
        if ( StringUtils.isEmpty(result) )
           throw RuntimeException("Trimmed string is empty")
        return result
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
    val ilpPrefix01 = getString("ledger.ilp.prefix")
    val ilpPrefix = if (ilpPrefix01.endsWith(".")) ilpPrefix01 else ilpPrefix01+"."
    val ledgerCurrencyCode = getString("ledger.currency.code")
    val ledgerCurrencySymbol = getString("ledger.currency.symbol")
    val ldpauxi01 = getString("ledger.path.prefix")
    val ledgerPathPrefix = if(ldpauxi01.endsWith("/")) ldpauxi01.substring(0, ldpauxi01.length - 1) else ldpauxi01

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

    val ethereum_address_escrow            = getString("ethereum.address.escrow")
    val test_ethereum_address_escrow       = getString("test.ethereum.address.escrow")
    val test_ethereum_address_admin        = getString("test.ethereum.address.admin")
    val test_ethereum_address_ilpconnector = getString("test.ethereum.address.ilpconnector")
    val test_ethereum_address_alice        = getString("test.ethereum.address.alice")
    val test_ethereum_address_bob          = getString("test.ethereum.address.bob")
    val test_ethereum_address_eve          = getString("test.ethereum.address.eve")

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
        // services.put("transfer_rejection", base + "not_available") // required by ilp-kit not RFCs
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
        return result.trim()
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
    @JvmStatic fun main(args: Array<String>) { // TODO:(0) Move to Unit tests
        println(Config.debug)
    }

}


object DSAPrivPubKeySupport {
    fun loadPrivateKey(key64: String): PrivateKey {
        val clear = Base64.getDecoder().decode(key64)
        val keySpec = PKCS8EncodedKeySpec(clear)
        try {
            val fact = KeyFactory.getInstance("DSA")
            val priv = fact.generatePrivate(keySpec)
            Arrays.fill(clear, 0.toByte())
            return priv
        } catch (e: Exception) {
            throw RuntimeException(e.toString())
        }

    }

    fun loadPublicKey(stored: String): PublicKey {
        val data = Base64.getDecoder().decode(stored)
        val spec = X509EncodedKeySpec(data)
        try {
            val fact = KeyFactory.getInstance("DSA")
            return fact.generatePublic(spec)
        } catch (e: Exception) {
            throw RuntimeException(e.toString())
        }

    }

    fun savePrivateKey(priv: PrivateKey): String {
        try {
            val fact = KeyFactory.getInstance("DSA")
            val spec = fact.getKeySpec(priv,
                    PKCS8EncodedKeySpec::class.java)
            val packed = spec.encoded
            val key64 = String(Base64.getEncoder().encode(packed))
            Arrays.fill(packed, 0.toByte())
            return key64
        } catch (e: Exception) {
            throw RuntimeException(e.toString())
        }

    }

    fun savePublicKey(publ: PublicKey): String {
        // TODO:(0) FIXME:
        // It's supposed to return something similar to
        //    2A5PxZUtFUuoL64r8oxzrsV73Y5ma76NZLUV8P2DG1M=
        // but it's actually returning something like:
        //    MIIBtzCCASwGByqGSM44BAEwggEfAoGBAP1/U4EddRIpUt9KnC7s5Of2EbdSPO9EAMMeP4C2USZpRV1AIlH7WT2NWPq/xfW6MPbLm1Vs14E7gB00b/JmYLdrmVClpJ+f6AR7ECLCT7up1/63xhv4O1fnxqimFQ8E+4P208UewwI1VBNaFpEy9nXzrith1yrv8iIDGZ3RSAHHAhUAl2BQjxUjC8yykrmCouuEC/BYHPUCgYEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLRJFnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImog9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoDgYQAAoGAARbabwyUW4v/xtnQjbRd4iEPvHnOCQpZx5d1RbaNe1XkmYj4JNdD1kmqjBhIDD8nKSdBk2oPWpujzjPs+T//7xWxixZ6BFrhAQ8qNWXF4tZKkmjtHqxo3JWhBe5OvGwNmBR9VJ4K7Xyk/YbZX2dK6o/Gl87yh/zWiUXfGAkua7A=
        try {
            val fact = KeyFactory.getInstance("DSA")
            val spec = fact.getKeySpec(publ,
                    X509EncodedKeySpec::class.java)
            return String(Base64.getEncoder().encode(spec.encoded))
        } catch (e: Exception) {
            throw RuntimeException(e.toString())
        }

    }

    @Throws(Exception::class)
    @JvmStatic fun main(args: Array<String>) { // TODO:(0) Move to UnitTests
        val gen = KeyPairGenerator.getInstance("DSA")
        val pair = gen.generateKeyPair()
        val pubKey = savePublicKey(pair.public)
        val privKey = savePrivateKey(pair.private)
        println("privKey:" + privKey)
        println("pubKey :" + pubKey)
        // PublicKey pubSaved = loadPublicKey(pubKey);
        // System.out.println(pair.getPublic()+"\n"+pubSaved);
        // PrivateKey privSaved = loadPrivateKey(privKey);
        // System.out.println(pair.getPrivate()+"\n"+privSaved);
    }
}


object VertxRunner {

    private val log = LoggerFactory.getLogger(VertxRunner::class.java)

    fun run(clazz: Class<*>) {
        val verticleID = clazz.name
        var baseDir = ""
        val options = VertxOptions().setClustered(false)
        // Smart cwd detection

        // Based on the current directory (.) and the desired directory (baseDir), we try to compute the vertx.cwd
        // directory:
        try {
            // We need to use the canonical file. Without the file name is .
            val current = File(".").canonicalFile
            if (baseDir.startsWith(current.name) && baseDir != current.name) {
                baseDir = baseDir.substring(current.name.length + 1)
            }
        } catch (e: IOException) {
            // Ignore it.
        }

        System.setProperty("vertx.cwd", baseDir)
        val deployLatch = CountDownLatch(1)
        val deployHandler = { result : AsyncResult<String> ->
            if (result.succeeded()) {
                log.info("Deployed verticle {}", result.result())
                deployLatch.countDown()
            } else {
                log.error("Deploying verticle", result.cause())
            }
        }
        val runner = { vertx : io.vertx.core.Vertx ->
            try {
                vertx.deployVerticle(verticleID, deployHandler)
                // Alt: vertx.deployVerticle(verticleID, deploymentOptions, deployHandler);
            } catch (e: Throwable) {
                log.error("Deploying verticle " + verticleID, e)
                throw e
            }
        }
        //DefaultChannelId.newInstance();//Warm up java ipv6 localhost dns
        if (options.isClustered) {
            Vertx.clusteredVertx(options) { res ->
                if (res.succeeded()) {
                    val vertx = res.result()
                    // runner.accept(vertx)
                    runner.invoke(vertx)
                } else {
                    log.error("Deploying clustered verticle " + verticleID, res.cause())
                }
            }
        } else {
            val vertx = Vertx.vertx(options)
            // runner.accept(vertx)
            runner.invoke(vertx)
            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() {
                    log.info("Shutting down")
                    vertx.close()
                }
            })
        }

        while (true) {
            try {
                if (!deployLatch.await(40, TimeUnit.SECONDS)) {
                    log.error("Timed out waiting to start")
                    System.exit(3)
                }
                break
            } catch (e: InterruptedException) {
                //ignore
            }

        }
        log.info("Launched")
    }
}

/*
 * Wrapper class around InterledgerError to store the http error code
 */
// TODO:(0) Change to
//     data class HTTPInterledgerException(val httpErrorCode: Int, ILPException: InterledgerProtocolException)
class HTTPInterledgerException(val httpErrorCode: Int, interledgerError: InterledgerError) :
        InterledgerProtocolException(interledgerError)


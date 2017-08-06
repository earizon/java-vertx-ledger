package com.everis.everledger.util

import com.everis.everledger.Config
import com.everis.everledger.HTTPInterledgerException
import org.apache.commons.lang3.math.NumberUtils
import org.interledger.Condition
import org.interledger.Fulfillment
import org.interledger.InterledgerAddress
import org.interledger.ilp.InterledgerError
import java.lang.Long
import java.net.URI
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.regex.Pattern

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



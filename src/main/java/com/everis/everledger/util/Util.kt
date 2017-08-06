package com.everis.everledger.util

import org.apache.commons.lang3.math.NumberUtils
import org.interledger.Condition
import org.interledger.Fulfillment
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

    fun toNumber(value: Any): Number {
        if (value is Number) { return value }
        return NumberUtils.createNumber(value.toString())
    }

    fun fulfillmentToBase64(FF: Fulfillment): String {
        var response = Base64.getEncoder().encodeToString(FF.preimage)
        response = response.substring(0, response.indexOf('='))
        return response
    }

    /**
     * Parses a URI formatted crypto-condition

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
package org.interledger.everledger.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
/**
 * Various number conversion utility methods
 * 
 */
public class TimeUtils {

    public static final ZonedDateTime future = 
        ZonedDateTime.ofInstant(
            (new Date(Long.MAX_VALUE)).toInstant(),
            ZoneId.of("Europe/Paris") );
    public static final ZonedDateTime testingDate = 
        ZonedDateTime.parse("2015-06-16T00:00:00.000Z");
    public static DateTimeFormatter ilpFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
}

package org.interledger.everledger.common.config;

import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Properties;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.format.MonetaryAmountFormat;

import org.interledger.ilp.InterledgerAddress;
import org.interledger.ilp.InterledgerAddressBuilder;
import org.interledger.ilp.ledger.model.LedgerInfo;
import org.interledger.ilp.ledger.money.format.LedgerSpecificDecimalMonetaryAmountFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * KISS CONFIG SUPPORT
 */
@SuppressWarnings("rawtypes")
public class Config {
    public static final String CONFIG_FILE="application.conf";
    public static final Logger log = LoggerFactory.getLogger(Config.class);
    public static final URL publicURL;
    public static final Properties prop = new Properties();
    static {
                try {
        FileInputStream inputStream = new FileInputStream(CONFIG_FILE);
        prop.load(inputStream);
        Enumeration keysEnum = prop.keys();
        while (keysEnum.hasMoreElements()){
            String key = (String)keysEnum.nextElement();
            prop.setProperty(key, prop.getProperty(key).trim());
            }
                }catch(Exception e){
        throw new RuntimeException("Can not set-up ILP config for file "+CONFIG_FILE + 
                "due to " + e.toString() +"\n");
                }
    }
    
    public static final boolean debug = getBoolean("server.debug");
    public static final String ilpPrefix=getString("ledger.ilp.prefix");
    public static final String ledgerCurrencyCode=getString("ledger.currency.code");
    public static final String ledgerCurrencySymbol=getString("ledger.currency.symbol");

    public static final String serverHost=getString("server.host");
    public static final int    serverPort=getInteger("server.port");
    public static final String serverPublicHost=getString("server.public.host");
    public static final String serverPublicPort=getString("server.public.port");
    public static final boolean serverUseHTTPS=getBoolean("server.public.use_https");
    
    public static final int ledgerPrecision = getInteger("ledger.precision");
    public static final int ledgerScale     = getInteger("ledger.scale");
    
    public static final String tls_key=getString("server.tls_key");
    public static final String tls_crt=getString("server.tls_cert");


//    private static final String sConditionSignPrivateKey = getString("ledger.ed25519.conditionSignPrivateKey");
    private static final String sConditionSignPublicKey  = getString("ledger.ed25519.conditionSignPublicKey" );
//    private static final String sNoticificationSignPrivateKey = getString("ledger.ed25519.notificationSignPrivateKey");
    private static final String sNoticificationSignPublicKey  = getString("ledger.ed25519.notificationSignPublicKey" );

    private static PublicKey bas642PubKey(String stored) {
        // TODO:(?) move to java-ilp-core (Priv/Pub Key support)
        // REF: http://stackoverflow.com/questions/19743851/base64-java-encode-and-decode-a-string
        byte[] data = Base64.getDecoder().decode(stored);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
                try {
        KeyFactory fact = KeyFactory.getInstance("DSA");
        return fact.generatePublic(spec);
                }catch(Exception e){
        throw new RuntimeException("Couldn't convert Base64 String '"+stored+"' to PublicKey "+e.toString());
                }
    }


    public static LedgerInfo ilpLedgerInfo = new Config.SimpleLedgerInfo(
            InterledgerAddressBuilder.builder().value(ilpPrefix).build(),
            ledgerPrecision,
            ledgerScale,
            Monetary.getCurrency(ledgerCurrencyCode),
            (MonetaryAmountFormat)new LedgerSpecificDecimalMonetaryAmountFormat(
                 Monetary.getCurrency(ledgerCurrencyCode), ledgerPrecision,ledgerScale),
            bas642PubKey(sConditionSignPublicKey),
            bas642PubKey(sNoticificationSignPublicKey)
             );
    

    static {
                        try {
        FileInputStream inputStream = new FileInputStream(CONFIG_FILE);
        prop.load(inputStream);
        Enumeration keysEnum = prop.keys();
        while (keysEnum.hasMoreElements()){
            String key = (String)keysEnum.nextElement();
            prop.setProperty(key, prop.getProperty(key).trim());
        }

        // 
        boolean pubSsl = getBoolean("server.public.use_https");
        String pubHost = getString ("server.public.host");
        int pubPort    = getInteger("server.public.port");
        String prefixUri = getString("ledger.ilp.prefix");
        if (!prefixUri.startsWith("/")) { prefixUri = "/" + prefixUri; } // sanitize
        URL serverPublicURL;
        try {
            serverPublicURL = new URL("http" + (pubSsl ? "s" : ""), pubHost, pubPort, prefixUri);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could NOT create URL with {"
                    + "pubHost='"+pubHost+"', "
                    + "pubPort='"+pubPort+"', "
                    + "prefixUri='"+prefixUri+"'}."
                    + " recheck server config");
        }
        log.debug("serverPublicURL: {}", serverPublicURL);
        publicURL = serverPublicURL;
                        } catch(Exception e){
        throw new RuntimeException("Can not read application.conf due to "+ e.toString());
                        }
    }

    private static String getString(final String key){
        final String result = prop.getProperty(key);
        if (result == null) {
            throw new RuntimeException(key + " was not found in "+CONFIG_FILE);
        }
        return result;
    }

    private static boolean getBoolean(final String key) {
        final String auxi = getString(key).toLowerCase();
        if (auxi.equals("false") || auxi.equals("0")) return false;
        if (auxi.equals("true" ) || auxi.equals("1")) return true;
        throw new RuntimeException(key + "defined in "+CONFIG_FILE +
                " can not be parsed as boolean. Use true|false or 0|1");
    }

    private static int getInteger(final String key) {
        final String auxi = getString(key).toLowerCase();
        try {
            int result = Integer.parseInt(auxi);
            return result;
        }catch(Exception e){
            throw new RuntimeException(key + "defined in "+CONFIG_FILE +
                    " can not be parsed as integer");
        }
    }
    
    
    static class SimpleLedgerInfo implements LedgerInfo {
        private final InterledgerAddress ilpAddress;
        private final int precision;
        private final int scale;
        private final CurrencyUnit currencyUnit;
        private final MonetaryAmountFormat monetaryAmountFormat;
        private final PublicKey conditionSignPublicKey;
        private final PublicKey notificationSignPublicKey;
        
        public SimpleLedgerInfo(
                InterledgerAddress ilpAddress,
                int precision,
                int scale, 
                CurrencyUnit currencyUnit,
                MonetaryAmountFormat monetaryAmountFormat, 
                PublicKey conditionSignPublicKey,
                PublicKey notificationSignPublicKey) {
            this.ilpAddress                = ilpAddress               ;
            this.precision                 = precision                ;
            this.scale                     = scale                    ;
            this.currencyUnit              = currencyUnit             ;
            this.monetaryAmountFormat      = monetaryAmountFormat     ;
            this.conditionSignPublicKey    = conditionSignPublicKey   ;
            this.notificationSignPublicKey = notificationSignPublicKey;
        }

        @Override public InterledgerAddress getAddressPrefix(){ return ilpAddress; }

        @Override public int getPrecision(){ return precision; }

        @Override public int getScale(){ return scale; }

        @Override public CurrencyUnit getCurrencyUnit(){ return currencyUnit; }

        @Override public MonetaryAmountFormat getMonetaryAmountFormat(){ return monetaryAmountFormat; }

        @Override public PublicKey getConditionSignPublicKey(){ return conditionSignPublicKey; }

        @Override public PublicKey getNotificationSignPublicKey(){ return notificationSignPublicKey; }
    }
}

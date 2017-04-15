package org.interledger.everledger.common.config;

import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * KISS CONFIG SUPPORT
 */
@SuppressWarnings("rawtypes")
public class Config {
    private static final Logger log = LoggerFactory.getLogger(Config.class);
    private static final URL publicURL;
    private static final Properties prop = new Properties();
    private static final String CONFIG_FILE="application.conf";
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
        boolean pubSsl = getBoolean("server.pulic.use_https");
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

    public static String getString(final String key){
        final String result = prop.getProperty(key);
        if (result == null) {
            throw new RuntimeException(key + " was not found in "+CONFIG_FILE);
        }
        return result;
    }
    
    public static boolean getBoolean(final String key) {
        final String auxi = getString(key).toLowerCase();
        if (auxi.equals("false") || auxi.equals("0")) return false;
        if (auxi.equals("true" ) || auxi.equals("1")) return true;
        throw new RuntimeException(key + "defined in "+CONFIG_FILE +
                " can not be parsed as boolean. Use true|false or 0|1");
    }
    
    public static int getInteger(final String key) {
        final String auxi = getString(key).toLowerCase();
        try {
            int result = Integer.parseInt(auxi);
            return result;
        }catch(Exception e){
            throw new RuntimeException(key + "defined in "+CONFIG_FILE +
                    " can not be parsed as integer");
        }
    }

    public URL getPublicURI() {
            return publicURL;
    }
}

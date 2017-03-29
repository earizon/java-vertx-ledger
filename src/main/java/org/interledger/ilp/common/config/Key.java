package org.interledger.ilp.common.config;

/**
 * Allowable ILP configuration keys.
 *
 * @author mrmx
 */
public enum Key {
    DEBUG,PREFIX,
    ACCOUNT,CURRENCY,NAME,CODE,SYMBOL,
    PAIRS,
    NOTIFICATION,
    ILP,
    CONNECTOR,
    PUBLIC,
    LEDGER, LEDGERS,
    SERVER,URI,REQUEST,LIMIT,
    USE_HTTPS, HOST, PORT,
    CERT, KEY, KEYS, CA,
    TLS_KEY, TLS_CERT, TLS_CRL, TLS_CA,
    VERIFY,
    SLIPPAGE, FX_SPREAD,
    ROUTE, BROADCAST,
    CLEANUP, ENABLED,
    TIME, INTERVAL, EXPIRY, TIMEOUT, HOLD,
    MIN, MAX,
    MESSAGE, WINDOW,
    ED25519, PRIVATE_KEY, PUBLIC_KEY

}

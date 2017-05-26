package com.everis.everledger.handlers;

import java.util.HashMap;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

import com.everis.everledger.AuthInfo;
import com.everis.everledger.Config;
import com.everis.everledger.handlers.RestEndpointHandler;
import com.everis.everledger.util.AuthManager;

import io.jsonwebtoken.impl.crypto.MacProvider;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.net.URISyntaxException;
import java.security.Key;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Handler for the index route.
 */
public class AuthTokenHandler extends RestEndpointHandler {

    private static SignatureAlgorithm SigAlgth = SignatureAlgorithm.HS256;
    private static final Logger log = LoggerFactory.getLogger(AuthTokenHandler.class);
    private static Key key = MacProvider.generateKey(SigAlgth);
    public static JwtParser parser =  Jwts.parser().setSigningKey(AuthTokenHandler.key);

    private AuthTokenHandler() {
        super(new HttpMethod[]{HttpMethod.GET},  new String[] {"auth_token"});
    }

    public static RestEndpointHandler create() {
        return new AuthTokenHandler();
    }

    @Override
    public void handleGet(RoutingContext context) {
        AuthInfo ai = AuthManager.authenticate(context, false);
        /*
         * https://github.com/interledgerjs/five-bells-ledger/blob/master/src/models/authTokens.js
         * jwt.sign({}, config.authTokenSecret, {
         *   algorithm: 'HS256',
         *   subject: uri.make('account', requestingUser.name.toLowerCase()),
         *   issuer: config.server.base_uri,
         *   expiresIn: '7 days'
         * }
         */
        // REF: https://github.com/jwtk/jjwt/
        String urlAccount = Config.publicURL+"/accounts/"+ai.name.toLowerCase();
        String subject;
        try {
            subject = (new java.net.URI(urlAccount)).toString();
        } catch (URISyntaxException e) {
            String sError = "'"+urlAccount+"' can not be parsed as URI";
            log.error(sError);
            throw new RuntimeException(sError);
        }
        String compactJwsToken = Jwts.builder()
                .setSubject(subject)
                .setIssuer(Config.serverPublicHost /* config.server.base_uri */)
                .signWith(SigAlgth, key).compact();
        HashMap<String, Object> result = new HashMap<>(); result.put("token", compactJwsToken);
        context.response()
           .putHeader("content-type", "application/json; charset=utf-8")
           .end(Json.encodePrettily(result));
    }

}

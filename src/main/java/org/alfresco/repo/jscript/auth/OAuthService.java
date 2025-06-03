package org.alfresco.repo.jscript.auth;

import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.*;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service class responsible for handling OAuth 2.0 Client Credentials authentication.
 * It retrieves and caches access tokens for reuse, avoiding repeated token requests
 * until expiration.
 */
public class OAuthService {

    private static final Logger logger = LoggerFactory.getLogger(OAuthService.class);

    private String oauthUrl;
    private String clientId;
    private String clientSecret;

    /**
     * Token cache, keyed by token name ("default" in this case).
     * Uses a thread-safe map to allow concurrent access in multi-threaded environments.
     */
    private final Map<String, Token> cache = new ConcurrentHashMap<>();

    /**
     * Returns a valid OAuth access token.
     * If a non-expired token exists in the cache, it is reused.
     * Otherwise, a new token is requested from the OAuth server.
     *
     * @return a valid access token as a String.
     * @throws RuntimeException if token retrieval fails.
     */
    public String getToken() throws Exception {
        Token t = cache.get("default");

        // Reuse cached token if still valid
        if (t != null && !t.isExpired()) return t.getAccessToken();

        // Otherwise, request a new token
        try (CloseableHttpClient c = HttpClients.createDefault()) {
            HttpPost p = new HttpPost(oauthUrl);
            p.addHeader("Content-Type", "application/x-www-form-urlencoded");

            // Construct the request body for client credentials grant
            String body = "grant_type=client_credentials"
                    + "&client_id="     + clientId
                    + "&client_secret=" + clientSecret;
            p.setEntity(new StringEntity(body, StandardCharsets.UTF_8));

            try (CloseableHttpResponse r = c.execute(p)) {
                int status = r.getStatusLine().getStatusCode();
                String responseBody = r.getEntity() != null
                        ? EntityUtils.toString(r.getEntity(), StandardCharsets.UTF_8)
                        : "";

                // Success: parse and cache token
                if (status >= 200 && status < 300) {
                    JSONObject o = new JSONObject(responseBody);
                    Token tok = new Token(o.getString("access_token"), o.getLong("expires_in"));
                    cache.put("default", tok);
                    return tok.getAccessToken();
                } else {
                    // Log warning and fail fast if response is not 2xx
                    logger.warn("OAuth token request failed (HTTP {}): {}", status, responseBody);
                    throw new RuntimeException("OAuth token request failed: " + status);
                }
            }
        } catch (Exception ex) {
            // Catch network or parsing errors
            logger.error("OAuth token request failed: {}", ex.getMessage(), ex);
            throw new RuntimeException("Unable to obtain OAuth token", ex);
        }
    }

    /**
     * Sets the OAuth 2.0 token endpoint.
     */
    public void setOauthUrl(String u) {
        this.oauthUrl = u;
    }

    /**
     * Sets the OAuth 2.0 client ID.
     */
    public void setClientId(String id) {
        this.clientId = id;
    }

    /**
     * Sets the OAuth 2.0 client secret.
     */
    public void setClientSecret(String s) {
        this.clientSecret = s;
    }

}

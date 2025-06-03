package org.alfresco.repo.jscript.auth;

/**
 * Represents an OAuth 2.0 access token with an expiration timestamp.
 * This token is typically acquired using the client credentials flow
 * and is cached for reuse until it expires.
 */
public class Token {

    /**
     * The raw OAuth 2.0 access token string.
     */
    private final String accessToken;

    /**
     * The expiration time of the token in epoch milliseconds.
     * Calculated at instantiation time using the `expires_in` value minus a 60-second buffer.
     */
    private final long expiresAtMillis;

    /**
     * Constructs a new Token instance.
     *
     * @param token              the access token string returned from the OAuth server.
     * @param expiresInSeconds   the validity duration (in seconds) as returned by the OAuth server.
     *                           A 60-second buffer is subtracted to account for potential clock skew or request latency.
     */
    Token(String token, long expiresInSeconds) {
        this.accessToken = token;
        this.expiresAtMillis = System.currentTimeMillis() + (expiresInSeconds - 60) * 1000;
    }

    /**
     * Returns the access token string.
     *
     * @return the current access token.
     */
    String getAccessToken() {
        return accessToken;
    }

    /**
     * Checks whether the token is expired (including the 60-second safety buffer).
     *
     * @return true if the current time is greater than or equal to the expiration timestamp.
     */
    boolean isExpired() {
        return System.currentTimeMillis() >= expiresAtMillis;
    }
}

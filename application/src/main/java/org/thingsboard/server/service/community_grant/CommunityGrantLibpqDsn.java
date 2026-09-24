// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

/**
 * Rewrites this deployment's JDBC url as the libpq connection string the instance checker's driver
 * understands.
 */
final class CommunityGrantLibpqDsn {

    private static final String JDBC_PREFIX = "jdbc:";
    private static final String JDBC_POSTGRESQL_PREFIX = "jdbc:postgresql:";
    private static final String POSTGRES_SCHEME = "postgres://";

    private CommunityGrantLibpqDsn() {
    }

    /**
     * The JDBC query string is dropped: its parameters are the JDBC driver's, not libpq's.
     *
     * @throws IllegalStateException if the url is not a PostgreSQL JDBC url naming a single host and a
     *                               database, or the username is blank
     */
    static String from(String jdbcUrl, String username, String password) {
        if (jdbcUrl == null || !jdbcUrl.startsWith(JDBC_POSTGRESQL_PREFIX)) {
            throw new IllegalStateException("The datasource url is not a PostgreSQL JDBC url");
        }
        URI jdbc;
        try {
            jdbc = new URI(jdbcUrl.substring(JDBC_PREFIX.length()));
        } catch (URISyntaxException e) {
            // The cause is dropped: its message quotes the url, which may carry the database password.
            throw new IllegalStateException("The datasource url is not a valid PostgreSQL JDBC url");
        }
        if (jdbc.getHost() == null) {
            throw new IllegalStateException("The datasource url does not name a single host");
        }
        String path = jdbc.getPath();
        if (path == null || path.length() <= 1) {
            throw new IllegalStateException("The datasource url does not name a database");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalStateException("The datasource has no configured username");
        }
        String userInfo = percentEncodeUserInfoComponent(username) + ":"
                + percentEncodeUserInfoComponent(password == null ? "" : password);
        try {
            // Spliced in rather than passed to the URI constructor, which would escape every '%' above again.
            String withoutUserInfo = new URI("postgres", null, jdbc.getHost(), jdbc.getPort(), path, null, null)
                    .toASCIIString();
            return POSTGRES_SCHEME + userInfo + "@" + withoutUserInfo.substring(POSTGRES_SCHEME.length());
        } catch (URISyntaxException e) {
            // The cause is dropped for the same reason as above.
            throw new IllegalStateException("Failed to build the instance check connection string");
        }
    }

    /** Escapes everything outside the RFC 3986 unreserved set, ':' and '%' included. */
    private static String percentEncodeUserInfoComponent(String value) {
        StringBuilder encoded = new StringBuilder();
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xFF;
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '.' || c == '_' || c == '~') {
                encoded.append((char) c);
            } else {
                encoded.append('%').append(String.format("%02X", c));
            }
        }
        return encoded.toString();
    }

}

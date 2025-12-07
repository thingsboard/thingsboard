/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.utils;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.Charset;


/**
 * Miscellaneous utility methods for various operations including HTTP request handling,
 * hash function selection, and URL construction.
 * <p>
 * This utility class provides helper methods for extracting information from HTTP requests,
 * particularly handling proxy headers (x-forwarded-proto, x-forwarded-port) commonly used
 * in load-balanced and reverse-proxy environments.
 *
 * @author Andrew Shvayka
 */
public class MiscUtils {

    public static final Charset UTF8 = Charset.forName("UTF-8");

    private static final int HTTP_DEFAULT_PORT = 80;
    private static final int HTTPS_DEFAULT_PORT = 443;
    private static final String HTTP_SCHEME = "http";
    private static final String HTTPS_SCHEME = "https";

    /**
     * Generates an error message for a missing configuration property.
     *
     * @param propertyName the name of the missing property
     * @return a formatted error message string
     */
    public static String missingProperty(String propertyName) {
        return "The " + propertyName + " property need to be set!";
    }

    /**
     * Returns a Guava HashFunction instance for the specified hash algorithm name.
     * <p>
     * Supported algorithms: murmur3_32, murmur3_128, crc32, md5
     *
     * @param name the name of the hash function
     * @return the corresponding HashFunction instance
     * @throws IllegalArgumentException if the hash function name is not recognized
     */
    @SuppressWarnings("deprecation")
    public static HashFunction forName(String name) {
        switch (name) {
            case "murmur3_32":
                return Hashing.murmur3_32();
            case "murmur3_128":
                return Hashing.murmur3_128();
            case "crc32":
                return Hashing.crc32();
            case "md5":
                return Hashing.md5();
            default:
                throw new IllegalArgumentException("Can't find hash function with name " + name);
        }
    }

    /**
     * Constructs a complete base URL from an HTTP request.
     * <p>
     * The URL is constructed in the format: scheme://domainName:port
     * <p>
     * This method respects proxy headers (x-forwarded-proto, x-forwarded-port) when present.
     *
     * @param request the HTTP servlet request
     * @return the constructed base URL string
     */
    public static String constructBaseUrl(HttpServletRequest request) {
        return String.format("%s://%s:%d",
                getScheme(request),
                getDomainName(request),
                getPort(request));
    }

    /**
     * Extracts the scheme (protocol) from an HTTP request.
     * <p>
     * If the x-forwarded-proto header is present (typically set by reverse proxies),
     * it takes precedence over the request's native scheme.
     *
     * @param request the HTTP servlet request
     * @return the scheme (http or https)
     */
    public static String getScheme(HttpServletRequest request){
        String scheme = request.getScheme();
        String forwardedProto = request.getHeader("x-forwarded-proto");
        if (forwardedProto != null) {
            scheme = forwardedProto;
        }
        return scheme;
    }

    /**
     * Extracts the domain name from an HTTP request.
     *
     * @param request the HTTP servlet request
     * @return the server name (domain)
     */
    public static String getDomainName(HttpServletRequest request){
        return request.getServerName();
    }

    /**
     * Constructs a domain name with port suffix if the port is non-standard.
     * <p>
     * Standard ports (80 for HTTP, 443 for HTTPS) are omitted from the result.
     * Non-standard ports are appended in the format: domainName:port
     *
     * @param request the HTTP servlet request
     * @return the domain name with port if non-standard, or just the domain name
     */
    public static String getDomainNameAndPort(HttpServletRequest request){
        String domainName = getDomainName(request);
        String scheme = getScheme(request);
        int port = MiscUtils.getPort(request);
        if (needsPort(scheme, port)) {
            domainName += ":" + port;
        }
        return domainName;
    }

    /**
     * Determines if a port number should be included in a URL for the given scheme.
     * <p>
     * Returns false if the port matches the default for the scheme (80 for HTTP, 443 for HTTPS).
     *
     * @param scheme the URL scheme (http or https)
     * @param port the port number
     * @return true if the port should be included in the URL, false otherwise
     */
    private static boolean needsPort(String scheme, int port) {
        return port != getDefaultPortForScheme(scheme);
    }

    /**
     * Extracts the port number from an HTTP request.
     * <p>
     * This method handles proxy scenarios by checking headers in the following order:
     * <ol>
     *   <li>x-forwarded-port header (if present and valid)</li>
     *   <li>x-forwarded-proto header (defaults to 80 for http, 443 for https)</li>
     *   <li>request's native server port</li>
     * </ol>
     *
     * @param request the HTTP servlet request
     * @return the port number
     */
    public static int getPort(HttpServletRequest request){
        String forwardedProto = request.getHeader("x-forwarded-proto");

        int serverPort = request.getServerPort();
        if (request.getHeader("x-forwarded-port") != null) {
            try {
                serverPort = request.getIntHeader("x-forwarded-port");
            } catch (NumberFormatException e) {
                // Keep original serverPort if parsing fails
            }
        } else if (forwardedProto != null) {
            int defaultPort = getDefaultPortForScheme(forwardedProto);
            if (defaultPort != -1) {
                serverPort = defaultPort;
            }
        }
        return serverPort;
    }

    /**
     * Returns the default port for the given scheme.
     *
     * @param scheme the HTTP scheme (http or https)
     * @return the default port (80 for http, 443 for https), or -1 if unknown
     */
    private static int getDefaultPortForScheme(String scheme) {
        if (scheme == null) {
            return -1;
        }
        String lowerScheme = scheme.toLowerCase();
        if (HTTP_SCHEME.equals(lowerScheme)) {
            return HTTP_DEFAULT_PORT;
        } else if (HTTPS_SCHEME.equals(lowerScheme)) {
            return HTTPS_DEFAULT_PORT;
        }
        return -1;
    }
}

/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.monitoring.metrics;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.monitoring.config.transport.TransportType;

import java.net.URI;
import java.util.Map;
import java.util.function.Function;

// Derives the "check"/"endpoint" labels used to tag probe metrics.
@Slf4j
public final class ProbeLabelResolver {

    public static final String LOGIN_PATH = "/api/auth/login";

    private static final Map<String, Integer> DEFAULT_PORTS = Map.of(
            "mqtt", 1883, "mqtts", 8883,
            "coap", 5683, "coaps", 5684,
            "http", 80, "https", 443,
            "lwm2m", 5685
    );

    private ProbeLabelResolver() {
    }

    public record ProbeLabels(String check, String endpoint) {
    }

    // returns null (rather than logging) when the endpoint can't be resolved (missing scheme?) - this
    // is a stateless utility, so warning-dedup across repeated calls for the same target is the
    // caller's (ProbeMetricsRecorder's) responsibility, not this class's
    public static ProbeLabels resolveTransportLabels(TransportType type, String baseUrl) {
        URI uri = URI.create(baseUrl);
        String checkType = resolveCheckType(type, uri);
        String endpoint = resolveEndpoint(uri, checkType);
        if (endpoint == null) {
            return null;
        }
        return new ProbeLabels(checkType, endpoint);
    }

    public static String resolveLoginEndpoint(String restBaseUrl) {
        return tryResolve("monitoring.rest.base_url", restBaseUrl, "login", endpoint -> endpoint + LOGIN_PATH);
    }

    public static String resolveWsEndpoint(String wsBaseUrl) {
        return tryResolve("monitoring.ws.base_url", wsBaseUrl, "ws", Function.identity());
    }

    private static String tryResolve(String configKey, String baseUrl, String probeName,
                                      Function<String, String> postProcess) {
        try {
            URI uri = URI.create(baseUrl);
            boolean secureScheme = "https".equalsIgnoreCase(uri.getScheme()) || "wss".equalsIgnoreCase(uri.getScheme());
            String hostPort = resolveHostPort(uri, secureScheme ? 443 : 80);
            if (hostPort == null) {
                // schemeless/opaque URL - just as much a misconfiguration as an unparseable one
                log.warn("Failed to resolve endpoint from {} [{}] - \"{}\" probe telemetry will not be recorded",
                        configKey, baseUrl, probeName);
                return null;
            }
            return postProcess.apply(hostPort);
        } catch (Exception e) {
            // an invalid base URL must not fail app startup - caller treats null as "skip this probe"
            log.warn("Failed to resolve endpoint from {} [{}] - \"{}\" probe telemetry will not be recorded",
                    configKey, baseUrl, probeName, e);
            return null;
        }
    }

    private static String resolveCheckType(TransportType type, URI uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        return switch (type) {
            case MQTT -> "ssl".equals(scheme) ? "mqtts" : "mqtt";
            case COAP -> "coaps".equals(scheme) ? "coaps" : "coap";
            case HTTP -> "https".equals(scheme) ? "https" : "http";
            case LWM2M -> "lwm2m";
        };
    }

    private static String resolveEndpoint(URI uri, String checkType) {
        return resolveHostPort(uri, DEFAULT_PORTS.get(checkType));
    }

    // URI.getHost()/getPort() return null/-1 for authorities Java doesn't consider valid hostnames
    // (e.g. underscores in docker-compose service names, a common target naming convention) - fall
    // back to parsing the authority component directly instead of silently losing the host.
    public static String resolveHostPort(URI uri, int defaultPort) {
        String host = uri.getHost();
        int port = uri.getPort();
        if (host == null) {
            String authority = uri.getAuthority();
            if (authority == null) {
                // schemeless URLs (e.g. "acme.example.com:1883") parse as opaque, with neither a host
                // nor an authority at all - nothing to fall back to, so let the caller skip the probe
                return null;
            }
            String hostPort = authority.contains("@") ? authority.substring(authority.lastIndexOf('@') + 1) : authority;
            int colonIdx = hostPort.lastIndexOf(':');
            if (colonIdx != -1) {
                host = hostPort.substring(0, colonIdx);
                try {
                    port = Integer.parseInt(hostPort.substring(colonIdx + 1));
                } catch (NumberFormatException e) {
                    port = -1;
                }
            } else {
                host = hostPort;
            }
        }
        return host + ":" + (port != -1 ? port : defaultPort);
    }

}

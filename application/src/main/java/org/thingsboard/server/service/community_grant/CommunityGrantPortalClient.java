// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.thingsboard.server.common.data.community_grant.CommunityGrantMode;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.UUID;

/**
 * The only outbound path to the license portal. Calls that carry grant state are authenticated by the claim
 * token, never by the cluster id, which is not a secret.
 */
@Component
@TbCoreComponent
@Slf4j
public class CommunityGrantPortalClient {

    private static final String BASE_PATH = "/api/noauth/communityGrant";
    private static final String CLUSTER_STATUS_PATH = BASE_PATH + "/clusterStatus?clusterId={clusterId}";
    private static final String LICENSE_CLAIM_PATH = "/api/noauth/freeLicense/claim";

    /** A header rather than a query parameter, which would be written to access and proxy logs. */
    private static final String CLAIM_TOKEN_HEADER = "X-TB-Claim-Token";

    private static final UUID REACHABILITY_PROBE_CLUSTER_ID = new UUID(0L, 0L);

    private static final int REACHABILITY_PROBE_MAX_BODY_BYTES = 4096;

    private static final int ERROR_BODY_MAX_BYTES = 4096;

    @Value("${community-grant.enabled:true}")
    private boolean enabled;

    @Value("${community-grant.base-url:https://license.thingsboard.io}")
    private String baseUrl;

    @Value("${community-grant.connect-timeout-sec:5}")
    private int connectTimeoutSec;

    @Value("${community-grant.read-timeout-sec:10}")
    private int readTimeoutSec;

    @Value("${community-grant.max-checker-size-bytes:33554432}")
    private long maxCheckerSizeBytes;

    private RestTemplate restTemplate;

    @PostConstruct
    public void initRestClient() {
        // Validation failure stops the node, so a disabled feature does not validate its URL.
        if (enabled) {
            baseUrl = normalizeBaseUrl(baseUrl);
        }
        restTemplate = new RestTemplateBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSec))
                .readTimeout(Duration.ofSeconds(readTimeoutSec))
                .errorHandler(new BoundedBodyErrorHandler())
                .build();
    }

    /** Spring's default handler buffers an error body in full. */
    private static class BoundedBodyErrorHandler extends DefaultResponseErrorHandler {

        @Override
        protected byte[] getResponseBody(ClientHttpResponse response) {
            try (InputStream in = response.getBody()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int read;
                while (out.size() < ERROR_BODY_MAX_BYTES && (read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                return out.toByteArray();
            } catch (IOException e) {
                return new byte[0];
            }
        }
    }

    /** A trailing slash would 404 every route, and the reachability probe counts a 404 as reachable. */
    private static String normalizeBaseUrl(String configured) {
        String normalized = configured == null ? "" : configured.trim().replaceAll("/+$", "");
        URI uri;
        try {
            uri = new URI(normalized);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("community-grant.base-url is not a valid URL: " + configured, e);
        }
        if (uri.getHost() == null || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {
            throw new IllegalStateException("community-grant.base-url must be an absolute http(s) URL: " + configured);
        }
        return normalized;
    }

    public String getPortalUrl() {
        return baseUrl;
    }

    /**
     * The platform rides along on every link, not only an offline one: the portal needs it to build a checker
     * bundle if the enrollment later moves to the by-hand route.
     *
     * @throws IllegalArgumentException if {@code community-grant.base-url} cannot be parsed
     */
    public String buildSignUpUrl(UUID clusterId, String token, CommunityGrantMode mode) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/communityGrant")
                .queryParam("clusterId", clusterId)
                .queryParam("claimToken", token);
        if (mode == CommunityGrantMode.OFFLINE) {
            builder.queryParam("offline", true);
        }
        appendPlatform(builder);
        return builder.build().toUriString();
    }

    /** Omitted on an architecture with no checker build rather than failing the enrollment. */
    private static void appendPlatform(UriComponentsBuilder builder) {
        CommunityGrantPlatform platform;
        try {
            platform = CommunityGrantPlatform.current();
        } catch (RuntimeException e) {
            log.debug("Leaving the platform out of the community grant sign-up link", e);
            return;
        }
        builder.queryParam("os", platform.os()).queryParam("arch", platform.arch());
    }

    /** Only a transport failure counts as unreachable; any HTTP status means reachable. */
    public boolean isPortalReachable() {
        String url = baseUrl + CLUSTER_STATUS_PATH;
        try {
            restTemplate.execute(url, HttpMethod.GET, null, response -> {
                try (InputStream in = response.getBody()) {
                    byte[] buffer = new byte[1024];
                    int total = 0;
                    int read;
                    while (total < REACHABILITY_PROBE_MAX_BODY_BYTES && (read = in.read(buffer)) != -1) {
                        total += read;
                    }
                }
                return null;
            }, REACHABILITY_PROBE_CLUSTER_ID);
            return true;
        } catch (HttpStatusCodeException e) {
            return true;
        } catch (RestClientException e) {
            log.info("The license portal is not reachable from this server: {}", e.getMessage());
            return false;
        }
    }

    public boolean isAlreadyRegistered(UUID clusterId) {
        String url = baseUrl + CLUSTER_STATUS_PATH;
        CommunityGrantClusterStatus response = restTemplate.getForObject(url, CommunityGrantClusterStatus.class, clusterId);
        return response != null && response.alreadyRegistered();
    }

    /** The portal always answers a bare 200, so a registration is never confirmed to the caller. */
    public void requestAccess(UUID clusterId) {
        String url = baseUrl + BASE_PATH + "/requestAccess?clusterId={clusterId}";
        restTemplate.postForObject(url, null, Void.class, clusterId);
    }

    /**
     * The cluster id goes with every poll: the token resolves to an account whose grant may belong to a
     * different installation.
     *
     * @return the portal's parsed answer, or {@code null} if the portal answered with an empty body
     */
    public CommunityGrantPortalStatus getStatus(String token, UUID clusterId) {
        String url = baseUrl + BASE_PATH + "/status?clusterId={clusterId}";
        HttpHeaders headers = new HttpHeaders();
        headers.set(CLAIM_TOKEN_HEADER, token);
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                CommunityGrantPortalStatus.class, clusterId).getBody();
    }

    /**
     * @return the portal's status after it accepted the report, or {@code null} if the portal answered with
     * an empty body.
     */
    public String uploadReport(String token, String report) {
        String url = baseUrl + BASE_PATH + "/report";
        CommunityGrantReportResponse response = restTemplate.postForObject(url,
                new CommunityGrantReportRequest(token, report), CommunityGrantReportResponse.class);
        return response == null ? null : response.status();
    }

    /**
     * This route reads the claim token from the body, not from the header the grant routes use.
     *
     * @return the granted license key, or {@code null} while the portal has none to hand over yet
     * @throws CommunityGrantLicenseClaimRefusedException if the portal refuses the claim
     */
    public String claimLicense(String token) {
        String url = baseUrl + LICENSE_CLAIM_PATH;
        CommunityGrantLicenseClaimResponse response;
        try {
            response = restTemplate.postForObject(url, new CommunityGrantLicenseClaimRequest(token),
                    CommunityGrantLicenseClaimResponse.class);
        } catch (HttpStatusCodeException e) {
            // A rate-limited claim is the one client error that is worth trying again.
            if (e.getStatusCode().is4xxClientError()
                    && e.getStatusCode().value() != HttpStatus.TOO_MANY_REQUESTS.value()) {
                throw new CommunityGrantLicenseClaimRefusedException(e);
            }
            throw e;
        }
        return response != null && response.activated() ? response.secret() : null;
    }

    /**
     * @throws CommunityGrantCheckerTooLargeException if the checker exceeds {@code maxCheckerSizeBytes}
     * @throws ResourceAccessException if reading the stream fails
     */
    public byte[] downloadChecker(String token, String os, String arch) {
        return download(BASE_PATH + "/checker?os={os}&arch={arch}", "instance checker", token, os, arch);
    }

    /**
     * @throws HttpStatusCodeException if the portal has no signature staged for this platform
     */
    public byte[] downloadCheckerSignature(String token, String os, String arch) {
        return download(BASE_PATH + "/checkerSignature?os={os}&arch={arch}",
                "instance checker's release signature", token, os, arch);
    }

    private byte[] download(String path, String what, String token, String os, String arch) {
        String url = baseUrl + path;
        final long limit = maxCheckerSizeBytes;
        return restTemplate.execute(url, HttpMethod.GET, request -> setClaimToken(request, token), response -> {
            long contentLength = response.getHeaders().getContentLength();
            if (contentLength > limit) {
                throw new CommunityGrantCheckerTooLargeException("The " + what + " size " + contentLength
                        + " bytes exceeds the configured limit of " + limit + " bytes");
            }
            int initialCapacity = contentLength > 0 && contentLength <= Integer.MAX_VALUE
                    ? (int) contentLength : 8192;
            ByteArrayOutputStream out = new ByteArrayOutputStream(initialCapacity);
            boolean complete = false;
            try (InputStream in = response.getBody()) {
                byte[] buffer = new byte[8192];
                long total = 0;
                int read;
                while ((read = in.read(buffer)) != -1) {
                    total += read;
                    if (total > limit) {
                        throw new CommunityGrantCheckerTooLargeException("The " + what
                                + " stream exceeded the configured limit of " + limit + " bytes");
                    }
                    out.write(buffer, 0, read);
                }
                complete = true;
            } catch (IOException e) {
                if (!complete) {
                    throw new ResourceAccessException("Failed to read the " + what + " (GET " + url + ")", e);
                }
                // Only close() failed after a complete read; the download is kept.
                log.debug("Failed to close the {} response stream after a complete read", what, e);
            }
            return out.toByteArray();
        }, os, arch);
    }

    private static void setClaimToken(ClientHttpRequest request, String token) {
        request.getHeaders().set(CLAIM_TOKEN_HEADER, token);
    }

}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trendz;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.trendz.TrendzConfiguration;
import org.thingsboard.server.common.data.trendz.TrendzHealthcheckResult;
import org.thingsboard.server.common.data.trendz.TrendzPaginationData;
import org.thingsboard.server.common.data.trendz.TrendzSettings;
import org.thingsboard.server.common.data.trendz.TrendzSummary;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationResult;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationResultType;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationStatus;
import org.thingsboard.server.common.data.trendz.TrendzUsage;
import org.thingsboard.server.common.data.trendz.TrendzViewConfig;
import org.thingsboard.server.common.data.trendz.TrendzViewConfigLite;
import org.thingsboard.server.dao.pat.ApiKeyService;
import org.thingsboard.server.dao.trendz.TrendzSettingsService;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.thingsboard.server.dao.trendz.TrendzSyncService.TRENDZ_API_KEY_DESCRIPTION;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrendzClient {

    public static final String TRENDZ_API_KEY_HEADER = "X-Trendz-Api-Key";
    public static final String TRENDZ_TENANT_ID_HEADER = "X-Trendz-Tenant-Id";
    public static final String TRENDZ_CUSTOMER_ID_HEADER = "X-Trendz-Customer-Id";
    public static final String TRENDZ_USER_ID_HEADER = "X-Trendz-User-Id";

    public static final String TRENDZ_INFO_URI = "/apiTrendz/publicApi/info";
    public static final String TRENDZ_SYNC_INIT_URI = "/apiTrendz/publicApi/sync/init";
    public static final String TRENDZ_HEALTHCHECK_URI = "/apiTrendz/publicApi/sync/check";

    public static final String TRENDZ_VIEW_CONFIGS_GET_ALL_URI = "/apiTrendz/view/config/all";
    public static final String TRENDZ_VIEW_CONFIGS_GET_BY_ID_URI = "/apiTrendz/view/config/%s";

    public static final String TRENDZ_SUMMARY_URI = "/apiTrendz/summary";
    public static final String TRENDZ_USAGE_URI = "/apiTrendz/summary/usage";

    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            HttpHeaders.TRANSFER_ENCODING.toLowerCase(),
            HttpHeaders.CONNECTION.toLowerCase(),
            HttpHeaders.CONTENT_LENGTH.toLowerCase(),
            HttpHeaders.CONTENT_ENCODING.toLowerCase(),
            "Keep-Alive".toLowerCase(),
            "Proxy-Authenticate".toLowerCase(),
            "Proxy-Authorization".toLowerCase(),
            "TE".toLowerCase(),
            "Trailer".toLowerCase(),
            "Upgrade".toLowerCase()
    );

    @Value("${trendz.request_timeout_ms:15000}")
    private int requestTimeoutMs;
    @Value("${trendz.usage_request_timeout_ms:3000}")
    private int usageRequestTimeoutMs;
    @Value("${trendz.enabled:true}")
    private boolean trendzEnabled;

    private final TrendzSettingsService trendzSettingsService;
    private final ApiKeyService apiKeyService;

    private RestTemplate restTemplate;
    private RestTemplate usageRestTemplate;

    @PostConstruct
    private void init() {
        restTemplate = new RestTemplateBuilder()
                .connectTimeout(Duration.ofMillis(requestTimeoutMs))
                .readTimeout(Duration.ofMillis(requestTimeoutMs))
                .build();
        usageRestTemplate = new RestTemplateBuilder()
                .connectTimeout(Duration.ofMillis(usageRequestTimeoutMs))
                .readTimeout(Duration.ofMillis(usageRequestTimeoutMs))
                .build();
    }

    public JsonNode checkTrendzReachability(String trendzUrl) {
        return sendTrendzRequest(trendzUrl, TRENDZ_INFO_URI, HttpMethod.GET, null, JsonNode.class, "Checking Trendz reachability");
    }

    public TrendzHealthcheckResult processTrendzInitRequest(String trendzUrl, String internalTbUrl, String externalTbUrl, String currentApiKey, String prevApiKey) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("internalTbUrl", internalTbUrl);
        requestBody.put("externalTbUrl", externalTbUrl);
        requestBody.put("currentTbAccessToken", currentApiKey);
        requestBody.put("prevTbAccessToken", prevApiKey);

        return sendTrendzRequest(trendzUrl, TRENDZ_SYNC_INIT_URI, HttpMethod.POST, requestBody, TrendzHealthcheckResult.class, "Initiating Trendz sync");
    }

    public TrendzHealthcheckResult sendHealthcheckRequest(String trendzUrl, String apiKey) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("apiKey", apiKey);

        TrendzHealthcheckResult result = sendTrendzRequest(trendzUrl, TRENDZ_HEALTHCHECK_URI, HttpMethod.POST,
                requestBody, TrendzHealthcheckResult.class, "Performing Trendz healthcheck");

        if (result == null) {
            return new TrendzHealthcheckResult(
                    null,
                    TrendzSynchronizationResultType.TRENDZ_URL_UNREACHABLE,
                    TrendzSynchronizationStatus.AVAILABLE,
                    TrendzSynchronizationResultType.TRENDZ_URL_UNREACHABLE.getMessage()
            );
        }

        return result;
    }

    public TrendzViewConfig getTrendzViewById(UUID viewId, User user) throws ThingsboardException {
        return sendTrendzRequest(HttpMethod.GET, TRENDZ_VIEW_CONFIGS_GET_BY_ID_URI.formatted(viewId), Collections.emptyMap(), null,
                new ParameterizedTypeReference<>() {
                }, user, "Get Trendz view config by id");
    }

    public TrendzPaginationData<TrendzViewConfigLite> getAllTrendzViews(PageLink pageLink, User user) throws ThingsboardException {
        Map<String, Object> params = new HashMap<>();
        params.put("prefix", pageLink.getTextSearch());
        params.put("page", pageLink.getPage());
        params.put("pageSize", pageLink.getPageSize());
        params.put("sort", toTrendzSortParameter(pageLink.getSortOrder()));
        return sendTrendzRequest(HttpMethod.GET, TRENDZ_VIEW_CONFIGS_GET_ALL_URI, params, null,
                new ParameterizedTypeReference<>() {
                }, user, "Get all Trendz view configs");
    }

    public TrendzSummary getTrendzSummary(User user) throws ThingsboardException {
        return sendTrendzRequest(HttpMethod.GET, TRENDZ_SUMMARY_URI, Collections.emptyMap(), null,
                new ParameterizedTypeReference<>() {
                }, user, "Get Trendz summary");
    }

    public TrendzUsage getTrendzUsage(User user) throws ThingsboardException {
        return sendTrendzRequest(HttpMethod.GET, TRENDZ_USAGE_URI, Collections.emptyMap(), null,
                new ParameterizedTypeReference<>() {
                }, user, "Get Trendz usage", usageRestTemplate);
    }

    public ResponseEntity<byte[]> sendTrendzProxyRequest(String uriPath, Map<String, String[]> params, HttpMethod method, byte[] body, HttpHeaders headers) throws ThingsboardException {

        String trendzUrl = getBaseTrendzUrl();

        try {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder
                    .fromUriString(trendzUrl)
                    .path(uriPath);
            params.forEach(uriBuilder::queryParam);
            URI uri = uriBuilder.build(false)
                    .toUri();

            headers.set(HttpHeaders.HOST, uri.getHost());

            log.debug("Trendz proxy request at: {}", uri);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    uri, method, new HttpEntity<>(body, headers), byte[].class
            );

            log.debug("Trendz proxy request completed successfully");
            return ResponseEntity.status(response.getStatusCode())
                    .headers(clearResponseHeaders(response.getHeaders()))
                    .body(response.getBody());
        } catch (RestClientResponseException e) {
            log.debug("Trendz proxy request received non-successful response: {}", e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode())
                    .headers(clearResponseHeaders(e.getResponseHeaders()))
                    .body(e.getResponseBodyAsByteArray());
        } catch (Exception e) {
            log.debug("Trendz proxy request failed at {} [{}]: {}", trendzUrl, uriPath, e.getMessage(), e);
            throw new ThingsboardException("Unexpected error during Trendz proxy request", e, ThingsboardErrorCode.GENERAL);
        }
    }

    private HttpHeaders clearResponseHeaders(HttpHeaders original) {
        if (original == null) {
            return null;
        }
        HttpHeaders cleared = new HttpHeaders();
        original.forEach((key, values) -> {
            if (!HOP_BY_HOP_HEADERS.contains(key.toLowerCase())) {
                cleared.put(key, values);
            }
        });
        return cleared;
    }

    private <T> T sendTrendzRequest(String trendzUrl, String uriPath, HttpMethod method,
                                    Map<String, Object> requestBody, Class<T> responseType, String operationName) {
        try {
            String url = normalizeUrl(trendzUrl) + uriPath;
            log.debug("{} at: {}", operationName, url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<T> response = restTemplate.exchange(url, method, request, responseType);
            return fetchContent(response, operationName);
        } catch (Exception e) {
            log.debug("{} failed at {} [{}]: {}", operationName, trendzUrl, uriPath, e.getMessage(), e);
            return null;
        }
    }

    private <T> T sendTrendzRequest(HttpMethod method, String uriPath, Map<String, Object> params, Object requestBody,
                                    ParameterizedTypeReference<T> typeReference, User user, String operationName
    ) throws ThingsboardException {
        return sendTrendzRequest(method, uriPath, params, requestBody, typeReference, user, operationName, restTemplate);
    }

    private <T> T sendTrendzRequest(HttpMethod method, String uriPath, Map<String, Object> params, Object requestBody,
                                    ParameterizedTypeReference<T> typeReference, User user, String operationName, RestTemplate restTemplate
    ) throws ThingsboardException {
        String trendzUrl = getBaseTrendzUrl();

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(uriPath);
        params.forEach(uriBuilder::queryParam);
        String urlWithParams = uriBuilder.toUriString();

        HttpHeaders headers = getTrendzAuthHeaders(user);
        HttpEntity<?> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<T> response = restTemplate.exchange(trendzUrl + urlWithParams, method, entity, typeReference);
            return fetchContent(response, operationName);
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("{} not found at {} [{}]: {}", operationName, trendzUrl, uriPath, e.getMessage(), e);
            throw new ThingsboardException("%s. Item wasn't found".formatted(operationName), ThingsboardErrorCode.ITEM_NOT_FOUND);
        } catch (HttpClientErrorException.BadRequest e) {
            log.debug("{} bad request at {} [{}]: {}", operationName, trendzUrl, uriPath, e.getMessage(), e);
            throw new ThingsboardException("%s. Bad request.".formatted(operationName), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        } catch (Exception e) {
            log.warn("{} failed at {} [{}]: {}", operationName, trendzUrl, uriPath, e.getMessage(), e);
            throw new ThingsboardException("%s. Unexpected error during Trendz request.".formatted(operationName), ThingsboardErrorCode.GENERAL);
        }
    }

    private <T> T fetchContent(ResponseEntity<T> response, String operationName) {
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            log.debug("{} completed successfully", operationName);
            return response.getBody();
        }
        log.debug("{} received non-successful response: {}", operationName, response.getStatusCode());
        return null;
    }

    private HttpHeaders getTrendzAuthHeaders(User user) throws ThingsboardException {
        String trendzApiKey = Optional.ofNullable(apiKeyService.findInternalApiKeyByDescription(TenantId.SYS_TENANT_ID, TRENDZ_API_KEY_DESCRIPTION))
                .map(ApiKey::getValue)
                .orElseThrow(() -> new ThingsboardException(
                        "Trendz API key is not configured. Please configure the Trendz API key in system settings.", ThingsboardErrorCode.GENERAL
                ));

        HttpHeaders headers = new HttpHeaders();
        headers.add(TRENDZ_API_KEY_HEADER, trendzApiKey);
        if (!user.isSystemAdmin()) {
            headers.add(TRENDZ_TENANT_ID_HEADER, user.getTenantId().getId().toString());
            headers.add(TRENDZ_CUSTOMER_ID_HEADER, user.getCustomerId().getId().toString());
            headers.add(TRENDZ_USER_ID_HEADER, user.getUuidId().toString());
        }
        return headers;
    }

    private String getBaseTrendzUrl() throws ThingsboardException {
        if (!trendzEnabled) {
            throw new ThingsboardException("Trendz is disabled.", ThingsboardErrorCode.GENERAL);
        }
        Optional<TrendzSettings> trendzSettings = Optional.ofNullable(trendzSettingsService.findTrendzSettings());
        trendzSettings.map(TrendzSettings::synchronizationResult)
                .map(TrendzSynchronizationResult::status)
                .orElseThrow(() -> new ThingsboardException(
                        "Trendz is not synced. Please sync before using it.", ThingsboardErrorCode.GENERAL
                ));
        return trendzSettings.map(TrendzSettings::configuration)
                .map(TrendzConfiguration::trendzUrl)
                .map(this::normalizeUrl)
                .orElseThrow(() -> new ThingsboardException(
                        "Trendz URL is not configured. Please configure the Trendz URL in system settings.", ThingsboardErrorCode.GENERAL
                ));
    }

    private String toTrendzSortParameter(SortOrder sortOrder) {
        if (sortOrder == null || sortOrder.getProperty() == null || sortOrder.getDirection() == null) {
            return null;
        }
        return "%s:%s".formatted(
                sortOrder.getProperty(),
                sortOrder.getDirection()
        );
    }

    private String normalizeUrl(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

}

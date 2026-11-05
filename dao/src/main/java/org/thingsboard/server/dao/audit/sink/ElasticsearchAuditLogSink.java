// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.audit.sink;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.id.TenantId;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(prefix = "audit-log.sink", value = "type", havingValue = "elasticsearch")
@Slf4j
public class ElasticsearchAuditLogSink implements AuditLogSink {

    private static final String TENANT_PLACEHOLDER = "@{TENANT}";
    private static final String DATE_PLACEHOLDER = "@{DATE}";
    static final String LEGACY_DOCUMENT_TYPE = "audit_log";
    static final String TYPELESS_DOCUMENT_TYPE = "_doc";
    static final long DISCOVERY_RETRY_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1);

    @Value("${audit-log.sink.index_pattern}")
    private String indexPattern;
    @Value("${audit-log.sink.scheme_name}")
    private String schemeName;
    @Value("${audit-log.sink.host}")
    private String host;
    @Value("${audit-log.sink.port}")
    private int port;
    @Value("${audit-log.sink.user_name}")
    private String userName;
    @Value("${audit-log.sink.password}")
    private String password;
    @Value("${audit-log.sink.date_format}")
    private String dateFormat;
    @Value("${audit-log.sink.index_api:auto}")
    private String indexApi;

    private RestClient restClient;
    private ExecutorService executor;

    private boolean autoDetectDocumentType;
    // null until resolved; reset on server error so cluster upgrades are picked up
    private volatile String documentType;
    private long lastDiscoveryTs;

    @PostConstruct
    public void init() {
        switch (StringUtils.isBlank(indexApi) ? "auto" : indexApi.trim().toLowerCase(Locale.ROOT)) {
            case "auto" -> autoDetectDocumentType = true;
            case "typed" -> documentType = LEGACY_DOCUMENT_TYPE;
            case "typeless" -> documentType = TYPELESS_DOCUMENT_TYPE;
            default -> throw new IllegalArgumentException("Unsupported audit-log.sink.index_api value: " + indexApi +
                    ". Possible options: auto, typed, typeless");
        }
        try {
            log.trace("Adding elastic rest endpoint... host [{}], port [{}], scheme name [{}]",
                    host, port, schemeName);
            RestClientBuilder builder = RestClient.builder(
                    new HttpHost(host, port, schemeName));

            if (StringUtils.isNotEmpty(userName) &&
                    StringUtils.isNotEmpty(password)) {
                log.trace("...using username [{}] and password ***", userName);
                final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(userName, password));
                builder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
            }

            this.restClient = builder.build();
            this.executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("elasticsearch-audit-log"));
        } catch (Exception e) {
            log.error("Sink init failed!", e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @PreDestroy
    private void destroy() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    public void logAction(AuditLog auditLogEntry) {
        executor.execute(() -> {
            try {
                doLogAction(auditLogEntry);
            } catch (Exception e) {
                log.error("Failed to log action", e);
            }
        });
    }

    private void doLogAction(AuditLog auditLogEntry) {
        String jsonContent = createElasticJsonRecord(auditLogEntry);

        HttpEntity entity = new NStringEntity(
                jsonContent,
                ContentType.APPLICATION_JSON);

        Request request = new Request(HttpMethod.POST.name(), String.format("/%s/%s", getIndexName(auditLogEntry.getTenantId()), getDocumentType()));
        request.setEntity(entity);

        restClient.performRequestAsync(request, responseListener);
    }

    private String createElasticJsonRecord(AuditLog auditLog) {
        ObjectNode auditLogNode = JacksonUtil.newObjectNode();
        auditLogNode.put("postDate", LocalDateTime.now().toString());
        auditLogNode.put("id", auditLog.getId().getId().toString());
        auditLogNode.put("entityName", auditLog.getEntityName());
        auditLogNode.put("tenantId", auditLog.getTenantId().getId().toString());
        if (auditLog.getCustomerId() != null) {
            auditLogNode.put("customerId", auditLog.getCustomerId().getId().toString());
        }
        auditLogNode.put("entityId", auditLog.getEntityId().getId().toString());
        auditLogNode.put("entityType", auditLog.getEntityId().getEntityType().name());
        auditLogNode.put("userId", auditLog.getUserId().getId().toString());
        auditLogNode.put("userName", auditLog.getUserName());
        auditLogNode.put("actionType", auditLog.getActionType().name());
        if (auditLog.getActionData() != null) {
            auditLogNode.put("actionData", auditLog.getActionData().toString());
        }
        auditLogNode.put("actionStatus", auditLog.getActionStatus().name());
        auditLogNode.put("actionFailureDetails", auditLog.getActionFailureDetails());
        return auditLogNode.toString();
    }

    private ResponseListener responseListener = new ResponseListener() {
        @Override
        public void onSuccess(Response response) {
            log.trace("Elasticsearch sink log action method succeeded. Response result [{}]!", response);
        }

        @Override
        public void onFailure(Exception exception) {
            log.warn("Elasticsearch sink log action method failed!", exception);
            if (autoDetectDocumentType && exception instanceof ResponseException) {
                documentType = null;
            }
        }
    };

    private String getDocumentType() {
        String type = documentType;
        if (type == null) {
            long now = System.currentTimeMillis();
            if (now - lastDiscoveryTs >= DISCOVERY_RETRY_INTERVAL_MS) {
                lastDiscoveryTs = now;
                type = discoverDocumentType();
                documentType = type;
            }
        }
        return type != null ? type : TYPELESS_DOCUMENT_TYPE;
    }

    /*
     * Custom mapping types ("/{index}/audit_log") were removed in Elasticsearch 8 and OpenSearch 2.
     * The legacy endpoint is kept where it is still supported, so existing indices stay unchanged.
     */
    private String discoverDocumentType() {
        JsonNode info;
        try {
            Response response = restClient.performRequest(new Request(HttpMethod.GET.name(), "/"));
            info = JacksonUtil.toJsonNode(response.getEntity().getContent());
        } catch (Exception e) {
            log.warn("Failed to detect audit log sink server version, using typeless index API. Will retry in {} ms",
                    DISCOVERY_RETRY_INTERVAL_MS, e);
            return null;
        }
        // OpenSearch in compatibility mode reports version 7.10.2 without "distribution", but keeps its tagline
        boolean openSearch = "opensearch".equalsIgnoreCase(info.path("version").path("distribution").asText()) ||
                info.path("tagline").asText().toLowerCase(Locale.ROOT).contains("opensearch");
        String number = info.path("version").path("number").asText();
        int major = parseMajorVersion(number);
        boolean legacy = openSearch ? major == 1 : major > 0 && major < 8;
        String type = legacy ? LEGACY_DOCUMENT_TYPE : TYPELESS_DOCUMENT_TYPE;
        log.info("Detected audit log sink server: {} {}, using document type [{}]", openSearch ? "OpenSearch" : "Elasticsearch", number, type);
        return type;
    }

    private static int parseMajorVersion(String version) {
        try {
            return Integer.parseInt(version.substring(0, version.indexOf('.')));
        } catch (Exception e) {
            return -1;
        }
    }

    private String getIndexName(TenantId tenantId) {
        String indexName = indexPattern;
        if (indexName.contains(TENANT_PLACEHOLDER) && tenantId != null) {
            indexName = indexName.replace(TENANT_PLACEHOLDER, tenantId.getId().toString());
        }
        if (indexName.contains(DATE_PLACEHOLDER)) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
            indexName = indexName.replace(DATE_PLACEHOLDER, now.format(formatter));
        }
        return indexName.toLowerCase();
    }
}

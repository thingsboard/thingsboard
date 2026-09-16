// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.rest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.credentials.AnonymousCredentials;
import org.thingsboard.rule.engine.credentials.ClientCredentials;
import org.thingsboard.server.common.data.util.KeyValueEntry;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Data
public class TbRestApiCallNodeConfiguration implements NodeConfiguration<TbRestApiCallNodeConfiguration> {

    private String restEndpointUrlPattern;
    private String requestMethod;
    // null for legacy configs - triggers different URL encoding logic in TbHttpClient to preserve backward compatibility
    // all new/modified configs since introduction of this property are forced to have non-null value by UI (and use new encoding logic)
    private List<@NotNull KeyValueEntry<String, String>> queryParams;
    private Map<String, String> headers;
    private int readTimeoutMs;
    private int maxParallelRequestsCount;
    private boolean parseToPlainText;
    private boolean enableProxy;
    private boolean useSystemProxyProperties;
    private String proxyHost;
    private int proxyPort;
    private String proxyUser;
    private String proxyPassword;
    private ClientCredentials credentials;
    private boolean ignoreRequestBody;
    private String requestBodyTemplate;
    private int maxInMemoryBufferSizeInKb;

    @JsonIgnore
    @AssertTrue(message = "query parameter names and values must be non-null")
    public boolean isValid() {
        if (queryParams == null) {
            return true; // null indicates legacy configuration; keep using old URL encoding logic
        }
        for (KeyValueEntry<String, String> queryParam : queryParams) {
            if (queryParam == null || queryParam.key() == null || queryParam.value() == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public TbRestApiCallNodeConfiguration defaultConfiguration() {
        var configuration = new TbRestApiCallNodeConfiguration();
        configuration.setRestEndpointUrlPattern("http://localhost/api");
        configuration.setRequestMethod("POST");
        configuration.setHeaders(Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
        configuration.setQueryParams(Collections.emptyList());
        configuration.setReadTimeoutMs(0);
        configuration.setMaxParallelRequestsCount(0);
        configuration.setParseToPlainText(false);
        configuration.setEnableProxy(false);
        configuration.setCredentials(new AnonymousCredentials());
        configuration.setIgnoreRequestBody(false);
        configuration.setMaxInMemoryBufferSizeInKb(256);
        return configuration;
    }

    public ClientCredentials getCredentials() {
        return Objects.requireNonNullElseGet(credentials, AnonymousCredentials::new);
    }

}

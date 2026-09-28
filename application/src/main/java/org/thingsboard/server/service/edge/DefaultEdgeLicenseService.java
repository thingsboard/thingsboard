// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.edge;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.common.data.StringUtils.isNotEmpty;

@Service
@TbCoreComponent
@Slf4j
public class DefaultEdgeLicenseService implements EdgeLicenseService {

    private static final int CONNECT_TIMEOUT = 30000; // Default connect timeout in ms (30 seconds)
    private static final int READ_TIMEOUT = 30000; // Default read timeout in ms (30 seconds)

    private RestTemplate restTemplate;

    private static final String EDGE_LICENSE_SERVER_ENDPOINT = "https://license.thingsboard.io";

    @Value("${edges.enabled:false}")
    private boolean edgesEnabled;

    @PostConstruct
    public void init() {
        if (edgesEnabled) {
            this.restTemplate = initRestTemplate();
        }
    }

    @Override
    public ResponseEntity<JsonNode> checkInstance(JsonNode request) {
        log.trace("checkInstance [{}]", request);
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<JsonNode> response = this.restTemplate.postForEntity(
                EDGE_LICENSE_SERVER_ENDPOINT + "/api/license/checkInstance",
                new HttpEntity<>(request, headers), JsonNode.class);
        log.trace("checkInstance response: {}", response);
        // removing headers from response of the license server, because it might be a conflict with the proxy from we are accepting incoming connections
        return new ResponseEntity<JsonNode>(response.getBody(), response.getStatusCode());
    }

    @Override
    public ResponseEntity<JsonNode> activateInstance(String edgeLicenseSecret, String releaseDate) {
        log.trace("activateInstance [{}]", releaseDate);
        Map<String, String> params = new HashMap<>();
        params.put("licenseSecret", edgeLicenseSecret);
        params.put("releaseDate", releaseDate);
        ResponseEntity<JsonNode> response = this.restTemplate.postForEntity(
                EDGE_LICENSE_SERVER_ENDPOINT + "/api/license/activateInstance?licenseSecret={licenseSecret}&releaseDate={releaseDate}",
                null, JsonNode.class, params);
        log.trace("activateInstance response: {}", response);
        // removing headers from response of the license server, because it might be a conflict with the proxy from we are accepting incoming connections
        return new ResponseEntity<JsonNode>(response.getBody(), response.getStatusCode());
    }

    private RestTemplate initRestTemplate() {
        boolean jdkHttpClientEnabled = isNotEmpty(System.getProperty("tb.proxy.jdk")) && System.getProperty("tb.proxy.jdk").equalsIgnoreCase("true");
        boolean systemProxyEnabled = isNotEmpty(System.getProperty("tb.proxy.system")) && System.getProperty("tb.proxy.system").equalsIgnoreCase("true");
        boolean proxyEnabled = isNotEmpty(System.getProperty("tb.proxy.host")) && isNotEmpty(System.getProperty("tb.proxy.port"));
        CloseableHttpClient httpClient;
        if (jdkHttpClientEnabled) {
            log.warn("Going to use plain JDK Http Client!");
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            if (proxyEnabled) {
                log.warn("Going to use Proxy Server: [{}:{}]", System.getProperty("tb.proxy.host"), System.getProperty("tb.proxy.port"));
                factory.setProxy(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(System.getProperty("tb.proxy.host"), Integer.parseInt(System.getProperty("tb.proxy.port")))));
            }
            factory.setConnectTimeout(CONNECT_TIMEOUT);
            factory.setReadTimeout(READ_TIMEOUT);
            return new RestTemplate(factory);
        } else if (systemProxyEnabled) {
            log.warn("Going to use System Proxy Server!");
            httpClient = HttpClientBuilder.create().useSystemProperties().setConnectionManager(createConnectionManager(true)).build();
            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
            factory.setHttpClient(httpClient);
            factory.setConnectTimeout(CONNECT_TIMEOUT);
            return new RestTemplate(factory);
        } else if (proxyEnabled) {
            log.warn("Going to use Proxy Server: [{}:{}]", System.getProperty("tb.proxy.host"), System.getProperty("tb.proxy.port"));
            httpClient = HttpClients.custom()
                    .setConnectionManager(createConnectionManager(false))
                    .setProxy(new HttpHost("https", System.getProperty("tb.proxy.host"), Integer.parseInt(System.getProperty("tb.proxy.port")))).build();
            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
            factory.setHttpClient(httpClient);
            factory.setConnectTimeout(CONNECT_TIMEOUT);
            return new RestTemplate(factory);
        } else {
            httpClient = HttpClients.custom().setConnectionManager(createConnectionManager(false)).build();
            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
            factory.setHttpClient(httpClient);
            factory.setConnectTimeout(CONNECT_TIMEOUT);
            return new RestTemplate(factory);
        }
    }

    private HttpClientConnectionManager createConnectionManager(boolean systemProperties) {
        var socketFactory = systemProperties ? SSLConnectionSocketFactory.getSystemSocketFactory() : SSLConnectionSocketFactory.getSocketFactory();
        var socketConfig = SocketConfig.custom().setSoTimeout(Timeout.of(READ_TIMEOUT, TimeUnit.MILLISECONDS)).build();
        return PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultSocketConfig(socketConfig)
                .setSSLSocketFactory(socketFactory).build();
    }

}



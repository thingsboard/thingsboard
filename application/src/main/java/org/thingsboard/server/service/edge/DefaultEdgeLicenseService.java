/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.edge;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.queue.util.TbCoreComponent;

import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Service
@TbCoreComponent
@Slf4j
public class DefaultEdgeLicenseService implements EdgeLicenseService {

    private RestTemplate restTemplate;

    private static final String EDGE_LICENSE_SERVER_ENDPOINT = "https://license.thingsboard.io";

    @Value("${edges.enabled:false}")
    private boolean edgesEnabled;

    @PostConstruct
    public void init() {
        if (edgesEnabled) {
            initRestTemplate();
        }
    }

    @Override
    public ResponseEntity<JsonNode> checkInstance(JsonNode request) {
        return this.restTemplate.postForEntity(EDGE_LICENSE_SERVER_ENDPOINT + "/api/license/checkInstance", request, JsonNode.class);
    }

    @Override
    public ResponseEntity<JsonNode> activateInstance(String edgeLicenseSecret, String releaseDate) {
        Map<String, String> params = new HashMap<>();
        params.put("licenseSecret", edgeLicenseSecret);
        params.put("releaseDate", releaseDate);
        return this.restTemplate.postForEntity(EDGE_LICENSE_SERVER_ENDPOINT + "/api/license/activateInstance?licenseSecret={licenseSecret}&releaseDate={releaseDate}", null, JsonNode.class, params);
    }

    private void initRestTemplate() {
        boolean jdkHttpClientEnabled = isNotEmpty(System.getProperty("tb.proxy.jdk")) && System.getProperty("tb.proxy.jdk").equalsIgnoreCase("true");
        boolean systemProxyEnabled = isNotEmpty(System.getProperty("tb.proxy.system")) && System.getProperty("tb.proxy.system").equalsIgnoreCase("true");
        boolean proxyEnabled = isNotEmpty(System.getProperty("tb.proxy.host")) && isNotEmpty(System.getProperty("tb.proxy.port"));
        if (jdkHttpClientEnabled) {
            log.warn("Going to use plain JDK Http Client!");
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            if (proxyEnabled) {
                log.warn("Going to use Proxy Server: [{}:{}]", System.getProperty("tb.proxy.host"), System.getProperty("tb.proxy.port"));
                factory.setProxy(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(System.getProperty("tb.proxy.host"), Integer.parseInt(System.getProperty("tb.proxy.port")))));
            }

            this.restTemplate = new RestTemplate(new SimpleClientHttpRequestFactory());
        } else {
            CloseableHttpClient httpClient;
            HttpComponentsClientHttpRequestFactory requestFactory;
            if (systemProxyEnabled) {
                log.warn("Going to use System Proxy Server!");
                httpClient = HttpClients.createSystem();
                requestFactory = new HttpComponentsClientHttpRequestFactory();
                requestFactory.setHttpClient(httpClient);
                this.restTemplate = new RestTemplate(requestFactory);
            } else if (proxyEnabled) {
                log.warn("Going to use Proxy Server: [{}:{}]", System.getProperty("tb.proxy.host"), System.getProperty("tb.proxy.port"));
                httpClient = HttpClients.custom().setSSLHostnameVerifier(new DefaultHostnameVerifier()).setProxy(new HttpHost(System.getProperty("tb.proxy.host"), Integer.parseInt(System.getProperty("tb.proxy.port")), "https")).build();
                requestFactory = new HttpComponentsClientHttpRequestFactory();
                requestFactory.setHttpClient(httpClient);
                this.restTemplate = new RestTemplate(requestFactory);
            } else {
                httpClient = HttpClients.custom().setSSLHostnameVerifier(new DefaultHostnameVerifier()).build();
                requestFactory = new HttpComponentsClientHttpRequestFactory();
                requestFactory.setHttpClient(httpClient);
                this.restTemplate = new RestTemplate(requestFactory);
            }
        }
    }
}



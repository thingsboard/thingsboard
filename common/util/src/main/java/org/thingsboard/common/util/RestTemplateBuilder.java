/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;

@Slf4j
public final class RestTemplateBuilder {

    private String proxyHost;
    private Integer proxyPort;
    private String proxyUser;
    private String proxyPassword;
    private String proxyHostScheme;
    private boolean jdkHttpClientEnabled;
    private boolean systemProxyEnabled;

    private RestTemplateBuilder() {
    }

    public static RestTemplate buildBySystemProperties() {
        boolean jdkHttpClientEnabled = StringUtils.isNotEmpty(System.getProperty("tb.proxy.jdk")) && System.getProperty("tb.proxy.jdk").equalsIgnoreCase("true");
        boolean systemProxyEnabled = StringUtils.isNotEmpty(System.getProperty("tb.proxy.system")) && System.getProperty("tb.proxy.system").equalsIgnoreCase("true");
        return RestTemplateBuilder
                .builder()
                .proxyHost(System.getProperty("tb.proxy.host"))
                .proxyPort(StringUtils.isNotEmpty(System.getProperty("tb.proxy.port")) ? Integer.parseInt(System.getProperty("tb.proxy.port")) : null)
                .proxyUser(System.getProperty("tb.proxy.user"))
                .proxyPassword(System.getProperty("tb.proxy.password"))
                .proxyHostScheme(System.getProperty("tb.proxy.scheme"))
                .jdkHttpClientEnabled(jdkHttpClientEnabled)
                .systemProxyEnabled(systemProxyEnabled)
                .build();
    }

    public static RestTemplateBuilder builder() {
        return new RestTemplateBuilder();
    }

    public RestTemplateBuilder proxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
        return this;
    }

    public RestTemplateBuilder proxyPort(Integer proxyPort) {
        this.proxyPort = proxyPort;
        return this;
    }

    public RestTemplateBuilder proxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
        return this;
    }

    public RestTemplateBuilder proxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
        return this;
    }

    public RestTemplateBuilder proxyHostScheme(String proxyHostScheme) {
        this.proxyHostScheme = proxyHostScheme;
        return this;
    }

    public RestTemplateBuilder jdkHttpClientEnabled(boolean jdkHttpClientEnabled) {
        this.jdkHttpClientEnabled = jdkHttpClientEnabled;
        return this;
    }

    public RestTemplateBuilder systemProxyEnabled(boolean systemProxyEnabled) {
        this.systemProxyEnabled = systemProxyEnabled;
        return this;
    }

    public RestTemplate build() {
        boolean proxyEnabled = StringUtils.isNotEmpty(proxyHost) && proxyPort != null;
        boolean useAuth = StringUtils.isNotEmpty(proxyUser) && StringUtils.isNotEmpty(proxyPassword);

        if (jdkHttpClientEnabled) {
            log.warn("Going to use plain JDK Http Client!");
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            if (proxyEnabled) {
                log.warn("Going to use Proxy Server: [{}:{}]", proxyHost, proxyPort);
                factory.setProxy(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(proxyHost, proxyPort)));
            }

            if (useAuth) {
                Authenticator.setDefault(new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
                    }
                });
            }

            return new RestTemplate(factory);
        } else {
            CloseableHttpClient httpClient;
            HttpComponentsClientHttpRequestFactory requestFactory;
            if (systemProxyEnabled) {
                log.warn("Going to use System Proxy Server!");
                httpClient = HttpClients.createSystem();
                requestFactory = new HttpComponentsClientHttpRequestFactory();
                requestFactory.setHttpClient(httpClient);

                if (useAuth) {
                    Authenticator.setDefault(new Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
                        }
                    });
                }

                return new RestTemplate(requestFactory);
            } else if (proxyEnabled) {
                log.warn("Going to use Proxy Server: [{}:{}]", proxyHost, proxyPort);
                HttpClientBuilder httpClientBuilder = HttpClients
                        .custom()
                        .setSSLHostnameVerifier(new DefaultHostnameVerifier())
                        .setProxy(new HttpHost(proxyHost, proxyPort, proxyHostScheme));
                if (useAuth) {
                    CredentialsProvider credsProvider = new BasicCredentialsProvider();
                    credsProvider.setCredentials(
                            new AuthScope(proxyHost, proxyPort),
                            new UsernamePasswordCredentials(proxyUser, proxyPassword)
                    );
                    httpClientBuilder.setDefaultCredentialsProvider(credsProvider);
                }

                httpClient = httpClientBuilder.build();
                requestFactory = new HttpComponentsClientHttpRequestFactory();
                requestFactory.setHttpClient(httpClient);
                return new RestTemplate(requestFactory);
            } else {
                httpClient = HttpClients.custom().setSSLHostnameVerifier(new DefaultHostnameVerifier()).build();
                requestFactory = new HttpComponentsClientHttpRequestFactory();
                requestFactory.setHttpClient(httpClient);
                return new RestTemplate(requestFactory);
            }
        }
    }
}

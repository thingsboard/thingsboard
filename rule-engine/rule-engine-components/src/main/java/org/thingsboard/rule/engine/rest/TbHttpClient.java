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
package org.thingsboard.rule.engine.rest;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.SsrfProtectionValidator;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.credentials.BasicCredentials;
import org.thingsboard.rule.engine.credentials.ClientCredentials;
import org.thingsboard.rule.engine.credentials.CredentialsType;
import org.thingsboard.rule.engine.credentials.DigestCredentials;
import org.thingsboard.rule.engine.util.DigestAuthUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.ProxyProvider;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Data
@Slf4j
public class TbHttpClient {

    private static final String STATUS = "status";
    private static final String STATUS_CODE = "statusCode";
    private static final String STATUS_REASON = "statusReason";
    private static final String ERROR = "error";
    private static final String ERROR_BODY = "error_body";
    private static final String ERROR_SYSTEM_PROPERTIES = "Didn't set any system proxy properties. Should be added next system proxy properties: \"http.proxyHost\" and \"http.proxyPort\" or  \"https.proxyHost\" and \"https.proxyPort\" or \"socksProxyHost\" and \"socksProxyPort\"";

    private static final String HTTP_PROXY_HOST = "http.proxyHost";
    private static final String HTTP_PROXY_PORT = "http.proxyPort";
    private static final String HTTPS_PROXY_HOST = "https.proxyHost";
    private static final String HTTPS_PROXY_PORT = "https.proxyPort";

    private static final String SOCKS_PROXY_HOST = "socksProxyHost";
    private static final String SOCKS_PROXY_PORT = "socksProxyPort";
    private static final String SOCKS_VERSION = "socksProxyVersion";
    private static final String SOCKS_VERSION_5 = "5";
    private static final String SOCKS_VERSION_4 = "4";
    public static final String PROXY_USER = "tb.proxy.user";
    public static final String PROXY_PASSWORD = "tb.proxy.password";

    public static final String MAX_IN_MEMORY_BUFFER_SIZE_IN_KB = "tb.http.maxInMemoryBufferSizeInKb";

    private final TbRestApiCallNodeConfiguration config;

    private EventLoopGroup eventLoopGroup;
    private WebClient webClient;
    private Semaphore semaphore;

    TbHttpClient(TbRestApiCallNodeConfiguration config, EventLoopGroup eventLoopGroupShared) throws TbNodeException {
        try {
            this.config = config;
            if (config.getMaxParallelRequestsCount() > 0) {
                semaphore = new Semaphore(config.getMaxParallelRequestsCount());
            }

            ConnectionProvider connectionProvider = ConnectionProvider
                    .builder("rule-engine-http-client")
                    .maxConnections(getPoolMaxConnections())
                    .build();

            HttpClient httpClient = HttpClient.create(connectionProvider)
                    .followRedirect(false)
                    .runOn(getSharedOrCreateEventLoopGroup(eventLoopGroupShared))
                    .doOnConnected(c ->
                            c.addHandlerLast(new ReadTimeoutHandler(config.getReadTimeoutMs(), TimeUnit.MILLISECONDS)));

            if (config.isEnableProxy()) {
                if (config.isUseSystemProxyProperties()) {
                    checkSystemProxyProperties();
                    httpClient = httpClient.proxy(this::createSystemProxyProvider);
                } else {
                    checkProxyHost(config.getProxyHost());
                    checkProxyPort(config.getProxyPort());
                    String proxyUser = config.getProxyUser();
                    String proxyPassword = config.getProxyPassword();

                    httpClient = httpClient.proxy(options -> {
                        var o = options.type(ProxyProvider.Proxy.HTTP)
                                .host(config.getProxyHost())
                                .port(config.getProxyPort());

                        if (useAuth(proxyUser, proxyPassword)) {
                            o.username(proxyUser).password(u -> proxyPassword);
                        }
                    });
                    SslContext sslContext = config.getCredentials().initSslContext();
                    httpClient = httpClient.secure(t -> t.sslContext(sslContext));
                }
            } else if (config.isUseSimpleClientHttpFactory()) {
                if (CredentialsType.CERT_PEM == config.getCredentials().getType()) {
                    throw new TbNodeException("Simple HTTP Factory does not support CERT PEM credentials!");
                }
            } else {
                SslContext sslContext = config.getCredentials().initSslContext();
                httpClient = httpClient.secure(t -> t.sslContext(sslContext));
            }

            if (SsrfProtectionValidator.isEnabled()) {
                httpClient = httpClient.resolver(SsrfSafeAddressResolverGroup.INSTANCE);
            }

            validateMaxInMemoryBufferSize(config);

            this.webClient = WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(
                            (config.getMaxInMemoryBufferSizeInKb() > 0 ? config.getMaxInMemoryBufferSizeInKb() : 256) * 1024))
                    .build();
        } catch (SSLException e) {
            throw new TbNodeException(e);
        }
    }

    private int getPoolMaxConnections() {
        String poolMaxConnectionsEnv = System.getenv("TB_RE_HTTP_CLIENT_POOL_MAX_CONNECTIONS");

        int poolMaxConnections;
        if (poolMaxConnectionsEnv != null) {
            poolMaxConnections = Integer.parseInt(poolMaxConnectionsEnv);
        } else {
            poolMaxConnections = ConnectionProvider.DEFAULT_POOL_MAX_CONNECTIONS;
        }
        return poolMaxConnections;
    }

    private void validateMaxInMemoryBufferSize(TbRestApiCallNodeConfiguration config) throws TbNodeException {
        int systemMaxInMemoryBufferSizeInKb = 25000;
        try {
            Properties properties = System.getProperties();
            if (properties.containsKey(MAX_IN_MEMORY_BUFFER_SIZE_IN_KB)) {
                systemMaxInMemoryBufferSizeInKb = Integer.parseInt(properties.getProperty(MAX_IN_MEMORY_BUFFER_SIZE_IN_KB));
            }
        } catch (Exception ignored) {}
        if (config.getMaxInMemoryBufferSizeInKb() > systemMaxInMemoryBufferSizeInKb) {
            throw new TbNodeException("The configured maximum in-memory buffer size (in KB) exceeds the system limit for this parameter.\n" +
                    "The system limit is " + systemMaxInMemoryBufferSizeInKb + " KB.\n" +
                    "Please use the system variable '" + MAX_IN_MEMORY_BUFFER_SIZE_IN_KB + "' to override the system limit.");
        }
    }

    EventLoopGroup getSharedOrCreateEventLoopGroup(EventLoopGroup eventLoopGroupShared) {
        if (eventLoopGroupShared != null) {
            return eventLoopGroupShared;
        }
        return this.eventLoopGroup = new NioEventLoopGroup();
    }

    private void checkSystemProxyProperties() throws TbNodeException {
        boolean useHttpProxy = !StringUtils.isEmpty(System.getProperty("http.proxyHost")) && !StringUtils.isEmpty(System.getProperty("http.proxyPort"));
        boolean useHttpsProxy = !StringUtils.isEmpty(System.getProperty("https.proxyHost")) && !StringUtils.isEmpty(System.getProperty("https.proxyPort"));
        boolean useSocksProxy = !StringUtils.isEmpty(System.getProperty("socksProxyHost")) && !StringUtils.isEmpty(System.getProperty("socksProxyPort"));
        if (!(useHttpProxy || useHttpsProxy || useSocksProxy)) {
            log.warn(ERROR_SYSTEM_PROPERTIES);
            throw new TbNodeException(ERROR_SYSTEM_PROPERTIES);
        }
    }

    private boolean useAuth(String proxyUser, String proxyPassword) {
        return !StringUtils.isEmpty(proxyUser) && !StringUtils.isEmpty(proxyPassword);
    }

    void destroy() {
        if (this.eventLoopGroup != null) {
            this.eventLoopGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }
    }

    public void processMessage(TbContext ctx, TbMsg msg,
                               Consumer<TbMsg> onSuccess,
                               BiConsumer<TbMsg, Throwable> onFailure) {
        try {
            if (semaphore != null && !semaphore.tryAcquire(config.getReadTimeoutMs(), TimeUnit.MILLISECONDS)) {
                onFailure.accept(msg, new RuntimeException("Timeout during waiting for reply!"));
                return;
            }

            String endpointUrl = TbNodeUtils.processPattern(config.getRestEndpointUrlPattern(), msg);
            HttpMethod method = HttpMethod.valueOf(config.getRequestMethod());
            URI uri = buildEncodedUri(endpointUrl);

            boolean hasBody =
                    (HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method) ||
                            HttpMethod.PATCH.equals(method) || HttpMethod.DELETE.equals(method))
                            && !config.isIgnoreRequestBody();

            Object requestBodyObj = null;
            byte[] requestBodyBytes = null;
            if (hasBody) {
                Object raw = getData(msg, config.isParseToPlainText());
                if (raw instanceof String s) {
                    requestBodyObj = s;
                    requestBodyBytes = s.getBytes(StandardCharsets.UTF_8);
                } else if (raw instanceof byte[] b) {
                    requestBodyObj = b;
                    requestBodyBytes = b;
                } else {
                    String json = JacksonUtil.toString(raw);
                    requestBodyObj = json;
                    requestBodyBytes = json.getBytes(StandardCharsets.UTF_8);
                }
            }

            issueRequest(ctx, msg, method, uri, requestBodyObj, requestBodyBytes,
                    null, null, onSuccess, onFailure, false);

        } catch (InterruptedException e) {
            log.warn("Timeout during waiting for reply!", e);
        }
    }

    private void issueRequest(TbContext ctx, TbMsg msg,
                              HttpMethod method, URI uri,
                              Object requestBodyObj, byte[] requestBodyBytes,
                              String digestAuthHeader,
                              String cookieHeader,
                              Consumer<TbMsg> onSuccess,
                              BiConsumer<TbMsg, Throwable> onFailure,
                              boolean attemptedDigest) {

        RequestBodySpec spec = webClient
                .method(method)
                .uri(uri)
                .headers(headers -> {
                    prepareHeaders(headers, msg);
                    if (digestAuthHeader != null) {
                        headers.add(HttpHeaders.AUTHORIZATION, digestAuthHeader);
                    }
                    if (cookieHeader != null && !cookieHeader.isEmpty()) {
                        headers.add(HttpHeaders.COOKIE, cookieHeader);
                    }
                });

        if (requestBodyObj != null) {
            spec.body(BodyInserters.fromValue(requestBodyObj));
        }

        spec.retrieve()
                .toEntity(String.class)
                .subscribe(responseEntity -> {
                    try {
                        if (responseEntity.getStatusCode().is2xxSuccessful()) {
                            onSuccess.accept(processResponse(ctx, msg, responseEntity));
                        } else {
                            onFailure.accept(processFailureResponse(msg, responseEntity), null);
                        }
                    } finally {
                        if (semaphore != null) semaphore.release();
                    }
                }, throwable -> {
                    try {
                        if (CredentialsType.DIGEST == config.getCredentials().getType()
                                && !attemptedDigest
                                && throwable instanceof WebClientResponseException wcre
                                && wcre.getStatusCode().value() == 401) {

                            String wwwAuth = wcre.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE);
                            String setCookie = wcre.getHeaders().getFirst(HttpHeaders.SET_COOKIE);

                            String authz = buildDigestAuthHeaderIfNeeded(method, uri, wwwAuth, requestBodyBytes);
                            String cookie = extractCookieHeader(setCookie);

                            issueRequest(ctx, msg, method, uri, requestBodyObj, requestBodyBytes,
                                    authz, cookie, onSuccess, onFailure, true);
                            return;
                        }

                        onFailure.accept(processException(msg, throwable), processThrowable(throwable));
                    } finally {
                        if (semaphore != null) semaphore.release();
                    }
                });
    }

    private String buildDigestAuthHeaderIfNeeded(HttpMethod method, URI uri,
                                                 String wwwAuthenticate, byte[] requestBodyBytes) {
        if (wwwAuthenticate == null
                || !wwwAuthenticate.toLowerCase(java.util.Locale.ROOT).contains("digest")) {
            return null;
        }

        String username = null, password = null;
        ClientCredentials creds = config.getCredentials();
        if (creds instanceof DigestCredentials dc) {
            username = dc.getUsername();
            password = dc.getPassword();
        }

        String reqTarget = DigestAuthUtil.requestUriForDigest(uri);

        return DigestAuthUtil.buildDigestAuthHeader(
                username,
                password,
                method.name(),
                reqTarget,
                wwwAuthenticate,
                requestBodyBytes
        );
    }

    private static String extractCookieHeader(String setCookie) {
        if (setCookie == null || setCookie.isEmpty()) return null;
        int i = setCookie.indexOf(';');
        String c = (i > 0 ? setCookie.substring(0, i) : setCookie).trim();
        return c.isEmpty() ? null : c;
    }

    private Throwable processThrowable(Throwable origin) {
        if (origin instanceof WebClientResponseException restClientResponseException
                && restClientResponseException.getStatusCode().is2xxSuccessful()) {
            // return cause instead of original exception in case 2xx status code
            // this will provide meaningful error message to the user
            return new RuntimeException(restClientResponseException.getCause());
        }
        return origin;
    }

    public URI buildEncodedUri(String endpointUrl) {
        if (endpointUrl == null) {
            throw new RuntimeException("Url string cannot be null!");
        }
        if (endpointUrl.isEmpty()) {
            throw new RuntimeException("Url string cannot be empty!");
        }

        URI uri = UriComponentsBuilder.fromUriString(endpointUrl).build().encode().toUri();
        if (uri.getScheme() == null || uri.getScheme().isEmpty()) {
            throw new RuntimeException("Transport scheme(protocol) must be provided!");
        }

        boolean authorityNotValid = uri.getAuthority() == null || uri.getAuthority().isEmpty();
        boolean hostNotValid = uri.getHost() == null || uri.getHost().isEmpty();
        if (authorityNotValid || hostNotValid) {
            throw new RuntimeException("Url string is invalid!");
        }

        SsrfProtectionValidator.validateUri(uri);

        return uri;
    }

    private Object getData(TbMsg tbMsg, boolean parseToPlainText) {
        String data = tbMsg.getData();
        return parseToPlainText ? JacksonUtil.toPlainText(data) : JacksonUtil.toJsonNode(data);
    }

    private TbMsg processResponse(TbContext ctx, TbMsg origMsg, ResponseEntity<String> response) {
        TbMsgMetaData metaData = origMsg.getMetaData();
        HttpStatus httpStatus = (HttpStatus) response.getStatusCode();
        metaData.putValue(STATUS, httpStatus.name());
        metaData.putValue(STATUS_CODE, response.getStatusCode().value() + "");
        metaData.putValue(STATUS_REASON, httpStatus.getReasonPhrase());
        headersToMetaData(response.getHeaders(), metaData::putValue);
        String body = response.getBody() == null ? TbMsg.EMPTY_JSON_OBJECT : response.getBody();
        return ctx.transformMsg(origMsg, metaData, body);
    }

    void headersToMetaData(Map<String, List<String>> headers, BiConsumer<String, String> consumer) {
        if (headers == null) {
            return;
        }
        headers.forEach((key, values) -> {
            if (values != null && !values.isEmpty()) {
                if (values.size() == 1) {
                    consumer.accept(key, values.get(0));
                } else {
                    consumer.accept(key, JacksonUtil.toString(values));
                }
            }
        });
    }

    private TbMsg processFailureResponse(TbMsg origMsg, ResponseEntity<String> response) {
        HttpStatus httpStatus = (HttpStatus) response.getStatusCode();
        TbMsgMetaData metaData = origMsg.getMetaData();
        metaData.putValue(STATUS, httpStatus.name());
        metaData.putValue(STATUS_CODE, httpStatus.value() + "");
        metaData.putValue(STATUS_REASON, httpStatus.getReasonPhrase());
        metaData.putValue(ERROR_BODY, response.getBody());
        headersToMetaData(response.getHeaders(), metaData::putValue);
        return origMsg.transform()
                .metaData(metaData)
                .build();
    }

    private TbMsg processException(TbMsg origMsg, Throwable e) {
        TbMsgMetaData metaData = origMsg.getMetaData();
        metaData.putValue(ERROR, e.getClass() + ": " + e.getMessage());
        if (e instanceof WebClientResponseException restClientResponseException) {
            metaData.putValue(STATUS, restClientResponseException.getStatusText());
            metaData.putValue(STATUS_CODE, restClientResponseException.getStatusCode().value() + "");
            metaData.putValue(ERROR_BODY, restClientResponseException.getResponseBodyAsString());
        }
        return origMsg.transform()
                .metaData(metaData)
                .build();
    }

    private void prepareHeaders(HttpHeaders headers, TbMsg msg) {
        config.getHeaders().forEach((k, v) -> headers.add(TbNodeUtils.processPattern(k, msg), TbNodeUtils.processPattern(v, msg)));
        ClientCredentials credentials = config.getCredentials();
        if (CredentialsType.BASIC == credentials.getType()) {
            BasicCredentials basicCredentials = (BasicCredentials) credentials;
            String authString = basicCredentials.getUsername() + ":" + basicCredentials.getPassword();
            String encodedAuthString = new String(Base64.getEncoder().encode(authString.getBytes(StandardCharsets.UTF_8)));
            headers.add("Authorization", "Basic " + encodedAuthString);
        }
    }

    private static void checkProxyHost(String proxyHost) {
        if (StringUtils.isEmpty(proxyHost)) {
            throw new IllegalArgumentException("Proxy host can't be empty");
        }
    }

    private static void checkProxyPort(int proxyPort) {
        if (proxyPort < 0 || proxyPort > 65535) {
            throw new IllegalArgumentException("Proxy port out of range:" + proxyPort);
        }
    }

    private void createSystemProxyProvider(ProxyProvider.TypeSpec option) {
        Properties properties = System.getProperties();
        if (properties.containsKey(HTTP_PROXY_HOST) || properties.containsKey(HTTPS_PROXY_HOST)) {
            createHttpProxyFrom(option, properties);
        } else if (properties.containsKey(SOCKS_PROXY_HOST)) {
            createSocksProxyFrom(option, properties);
        }
    }

    private void createHttpProxyFrom(ProxyProvider.TypeSpec option, Properties properties) {
        String hostProperty;
        String portProperty;
        if (properties.containsKey(HTTPS_PROXY_HOST)) {
            hostProperty = HTTPS_PROXY_HOST;
            portProperty = HTTPS_PROXY_PORT;
        } else {
            hostProperty = HTTP_PROXY_HOST;
            portProperty = HTTP_PROXY_PORT;
        }

        String hostname = properties.getProperty(hostProperty);
        int port = Integer.parseInt(properties.getProperty(portProperty));

        checkProxyHost(hostname);
        checkProxyPort(port);

        var proxy = option
                .type(ProxyProvider.Proxy.HTTP)
                .host(hostname)
                .port(port);

        var proxyUser = properties.getProperty(PROXY_USER);
        var proxyPassword = properties.getProperty(PROXY_PASSWORD);

        if (useAuth(proxyUser, proxyPassword)) {
            proxy.username(proxyUser).password(u -> proxyPassword);
        }
    }

    private void createSocksProxyFrom(ProxyProvider.TypeSpec option, Properties properties) {
        String hostname = properties.getProperty(SOCKS_PROXY_HOST);
        String version = properties.getProperty(SOCKS_VERSION, SOCKS_VERSION_5);
        if (!SOCKS_VERSION_5.equals(version) && !SOCKS_VERSION_4.equals(version)) {
            throw new IllegalArgumentException(String.format("Wrong socks version %s! Supported only socks versions 4 and 5.", version));
        }

        ProxyProvider.Proxy type = SOCKS_VERSION_5.equals(version) ? ProxyProvider.Proxy.SOCKS5 : ProxyProvider.Proxy.SOCKS4;
        int port = Integer.parseInt(properties.getProperty(SOCKS_PROXY_PORT));

        checkProxyHost(hostname);
        checkProxyPort(port);

        ProxyProvider.Builder proxy = option
                .type(type)
                .host(hostname)
                .port(port);

        var proxyUser = properties.getProperty(PROXY_USER);
        var proxyPassword = properties.getProperty(PROXY_PASSWORD);

        if (useAuth(proxyUser, proxyPassword)) {
            proxy.username(proxyUser).password(u -> proxyPassword);
        }
    }

}

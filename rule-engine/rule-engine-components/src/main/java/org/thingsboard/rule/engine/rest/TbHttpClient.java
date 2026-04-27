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
import org.thingsboard.rule.engine.api.TbHttpClientSettings;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.credentials.BasicCredentials;
import org.thingsboard.rule.engine.credentials.ClientCredentials;
import org.thingsboard.rule.engine.credentials.CredentialsType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import reactor.core.scheduler.Schedulers;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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

    private static final long ANOMALY_REPORT_INTERVAL_MS = 60_000;

    private final TbRestApiCallNodeConfiguration config;
    private final String tenantId;
    private final String nodeId;
    private final TbHttpClientSettings settings;

    private EventLoopGroup eventLoopGroup;
    private WebClient webClient;
    private Semaphore semaphore;
    private BlockingQueue<PendingTask> pendingQueue;

    private final AtomicLong dispatchedCount   = new AtomicLong();
    private final AtomicLong successCount      = new AtomicLong();
    private final AtomicLong failureCount      = new AtomicLong();
    private final AtomicLong droppedFullCount  = new AtomicLong();
    private final AtomicLong droppedStaleCount = new AtomicLong();
    private volatile long lastAnomalyReportAt  = 0;

    private record PendingTask(
            TbContext ctx,
            TbMsg msg,
            Consumer<TbMsg> onSuccess,
            BiConsumer<TbMsg, Throwable> onFailure,
            long enqueuedNanos) {}

    TbHttpClient(TbRestApiCallNodeConfiguration config, EventLoopGroup eventLoopGroupShared) throws TbNodeException {
        this(config, eventLoopGroupShared, "n/a", "n/a", TbHttpClientSettings.DEFAULT);
    }

    TbHttpClient(TbRestApiCallNodeConfiguration config, EventLoopGroup eventLoopGroupShared,
                 String tenantId, String nodeId, TbHttpClientSettings settings) throws TbNodeException {
        try {
            this.config = config;
            this.tenantId = tenantId;
            this.nodeId = nodeId;
            this.settings = settings;
            int effectiveParallel = effectiveMax(config.getMaxParallelRequestsCount(), settings.getMaxParallelRequests());
            if (effectiveParallel > 0) {
                semaphore = new Semaphore(effectiveParallel);
                int effectivePending = effectiveMax(0, settings.getMaxPendingRequests());
                pendingQueue = effectivePending > 0 ? new LinkedBlockingQueue<>(effectivePending) : new LinkedBlockingQueue<>();
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

    /**
     * Returns the effective limit: {@code min(userMax, systemMax)} when both are positive,
     * {@code systemMax} when only the system ceiling is set, or {@code userMax} otherwise.
     * A value of {@code 0} means unlimited.
     */
    private static int effectiveMax(int userMax, int systemMax) {
        if (systemMax <= 0) return userMax;
        if (userMax <= 0)   return systemMax;
        return Math.min(userMax, systemMax);
    }

    private int getPoolMaxConnections() {
        int configured = settings.getPoolMaxConnections();
        return configured > 0 ? configured : ConnectionProvider.DEFAULT_POOL_MAX_CONNECTIONS;
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
        long full = droppedFullCount.get();
        long stale = droppedStaleCount.get();
        int availablePermits = semaphore != null ? semaphore.availablePermits() : -1;
        if (full > 0 || stale > 0) {
            log.warn("[{}][{}] REST API call node destroyed with anomalies: " +
                            "droppedQueueFull={}, droppedStale={}, dispatched={}, success={}, failure={}, semaphorePermits={}.",
                    tenantId, nodeId, full, stale,
                    dispatchedCount.get(), successCount.get(), failureCount.get(), availablePermits);
        } else {
            log.debug("[{}][{}] REST API call node destroyed. dispatched={}, success={}, failure={}, semaphorePermits={}.",
                    tenantId, nodeId, dispatchedCount.get(), successCount.get(), failureCount.get(), availablePermits);
        }
    }

    public void processMessage(TbContext ctx, TbMsg msg,
                               Consumer<TbMsg> onSuccess,
                               BiConsumer<TbMsg, Throwable> onFailure) {
        if (semaphore == null) {
            doHttpCall(new PendingTask(ctx, msg, onSuccess, onFailure, 0L));
            return;
        }
        if (!pendingQueue.offer(new PendingTask(ctx, msg, onSuccess, onFailure, System.nanoTime()))) {
            droppedFullCount.incrementAndGet();
            log.debug("[{}][{}] REST API call queue full, dropping msg {}.", tenantId, nodeId, msg.getId());
            maybeReportAnomalies();
            onFailure.accept(msg, new RuntimeException("Max pending requests limit exceeded!"));
            return;
        }
        tryProcess();
    }

    /**
     * Tries to acquire one concurrency slot and fire the next queued task.
     * Stale messages (whose message pack has expired) are silently dropped.
     * Safe to call from any thread under high concurrency.
     */
    private void tryProcess() {
        while (true) {
            if (!semaphore.tryAcquire()) {
                return; // all slots are in use; a callback will call tryProcess() when one frees up
            }
            PendingTask next = pendingQueue.poll();
            if (next == null) {
                semaphore.release();
                return; // queue is empty; slot released
            }
            if (!next.msg().isValid()) {
                semaphore.release();
                droppedStaleCount.incrementAndGet();
                log.debug("[{}][{}] Dropping stale msg {} from REST API call queue (queueDepth={}).",
                        tenantId, nodeId, next.msg().getId(), pendingQueue.size());
                next.onFailure().accept(next.msg(), new RuntimeException("Message is no longer valid. Dropped from queue."));
                maybeReportAnomalies();
                continue; // slot released — loop to check if there's a valid next item
            }
            dispatchedCount.incrementAndGet();
            if (doHttpCall(next)) {
                return; // async HTTP call started — its callback will call tryProcess()
            }
            // synchronous failure — semaphore already released in doHttpCall, loop to try next task
        }
    }

    private void maybeReportAnomalies() {
        long now = System.currentTimeMillis();
        if (now - lastAnomalyReportAt < ANOMALY_REPORT_INTERVAL_MS) {
            return;
        }
        lastAnomalyReportAt = now;
        int queueSize = pendingQueue != null ? pendingQueue.size() : 0;
        int queueRemaining = pendingQueue != null ? pendingQueue.remainingCapacity() : Integer.MAX_VALUE;
        int availablePermits = semaphore != null ? semaphore.availablePermits() : -1;
        log.warn("[{}][{}] REST API call node anomalies: droppedQueueFull={}, droppedStale={} " +
                        "(dispatched={}, success={}, failure={}, queueDepth={}, queueRemaining={}, semaphorePermits={}).",
                tenantId, nodeId,
                droppedFullCount.get(), droppedStaleCount.get(),
                dispatchedCount.get(), successCount.get(), failureCount.get(),
                queueSize, queueRemaining, availablePermits);
    }

    /**
     * Initiates an async HTTP call for the given task.
     *
     * @return {@code true} if the async subscription was started and the semaphore slot is now
     *         owned by the callback (which will release it and call {@link #tryProcess()}).
     *         {@code false} if a synchronous exception occurred before the subscription was
     *         registered; the semaphore slot has already been released and the caller should
     *         loop rather than recurse to avoid stack overflow when many queued tasks fail
     *         synchronously (e.g. misconfigured URL pattern).
     */
    private boolean doHttpCall(PendingTask task) {
        boolean asyncStarted = false;
        try {
            String endpointUrl = TbNodeUtils.processPattern(config.getRestEndpointUrlPattern(), task.msg());
            HttpMethod method = HttpMethod.valueOf(config.getRequestMethod());
            URI uri = buildEncodedUri(endpointUrl);

            RequestBodySpec req = webClient
                    .method(method)
                    .uri(uri)
                    .headers(headers -> prepareHeaders(headers, task.msg()));

            if ((HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method) ||
                    HttpMethod.PATCH.equals(method) || HttpMethod.DELETE.equals(method)) &&
                    !config.isIgnoreRequestBody()) {
                req.body(BodyInserters.fromValue(getData(task.msg(), config.isParseToPlainText())));
            }

            req.retrieve()
                    .toEntity(String.class)
                    .publishOn(Schedulers.fromExecutor(task.ctx().getExternalCallExecutor()))
                    .doFinally(signalType -> {
                        // Runs exactly once after onComplete, onError, or cancel — the only
                        // place that releases the permit for the async path.
                        if (semaphore != null) {
                            semaphore.release();
                            tryProcess();
                        }
                    })
                    .subscribe(responseEntity -> {
                        if (responseEntity.getStatusCode().is2xxSuccessful()) {
                            successCount.incrementAndGet();
                            task.onSuccess().accept(processResponse(task.ctx(), task.msg(), responseEntity));
                        } else {
                            failureCount.incrementAndGet();
                            task.onFailure().accept(processFailureResponse(task.msg(), responseEntity), null);
                        }
                    }, throwable -> {
                        failureCount.incrementAndGet();
                        task.onFailure().accept(processException(task.msg(), throwable), processThrowable(throwable));
                    });
            asyncStarted = true;
            return true;
        } catch (Exception e) {
            failureCount.incrementAndGet();
            task.onFailure().accept(processException(task.msg(), e), processThrowable(e));
            return false;
        } finally {
            // Synchronous permit release: only when the async pipeline was never registered
            // (asyncStarted=false). If it was, doFinally owns the release.
            // tryProcess() is intentionally not called here — the caller loops iteratively.
            if (!asyncStarted && semaphore != null) {
                semaphore.release();
            }
        }
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

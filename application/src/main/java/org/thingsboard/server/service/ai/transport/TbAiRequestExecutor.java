// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.thingsboard.ai.common.channel.ChannelBodies;
import org.thingsboard.ai.common.channel.ChannelError;
import org.thingsboard.ai.common.channel.ChannelException;
import org.thingsboard.ai.common.channel.ChannelFrame;
import org.thingsboard.ai.common.channel.ChannelFrameCodec;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.channel.ChannelSession;
import org.thingsboard.ai.common.channel.TbAiChannelClient;
import org.thingsboard.ai.common.channel.TbHttpRequest;
import org.thingsboard.ai.common.channel.TbHttpResponse;
import org.thingsboard.server.config.ThingsboardSecurityConfiguration;
import org.thingsboard.server.queue.util.TbCoreComponent;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@TbCoreComponent
public class TbAiRequestExecutor {

    static final String INVALID_REQUEST = "invalid_request";
    static final String REQUEST_FAILED = "request_failed";
    static final String TIMEOUT = "timeout";
    static final String TOO_LARGE = "too_large";

    private static final String API_PREFIX = "/api/";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(35);
    private static final Set<HttpMethod> ALLOWED_METHODS = Set.of(
            HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE);
    private static final Set<String> DROPPED_REQUEST_HEADERS = Set.of(
            "host", "authorization", "x-authorization", "cookie", "connection", "content-length", "transfer-encoding",
            "x-forwarded-host", "x-forwarded-proto", "x-forwarded-port", "x-forwarded-for");
    private static final Set<String> DROPPED_RESPONSE_HEADERS = Set.of(
            "set-cookie", "content-length", "transfer-encoding", "connection");

    private final String baseUrl;
    private final WebClient webClient;

    @Autowired
    public TbAiRequestExecutor(@Value("${server.address:0.0.0.0}") String address,
                               @Value("${server.port:8080}") int port,
                               @Value("${server.ssl.enabled:false}") boolean sslEnabled) {
        this(loopbackUrl(address, port, sslEnabled), sslEnabled ? insecureLoopbackSslContext() : null);
    }

    TbAiRequestExecutor(String baseUrl, SslContext sslContext) {
        this.baseUrl = baseUrl;
        HttpClient httpClient = sslContext != null
                ? HttpClient.create().secure(spec -> spec.sslContext(sslContext))
                : HttpClient.create();
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(TbAiChannelClient.DEFAULT_MAX_FRAME_BYTES))
                .build();
    }

    public Mono<ChannelFrame> execute(ChannelFrame frame, ChannelFrameCodec codec, TbAiTurnContext context, int maxBodyBytes) {
        TbHttpRequest request;
        try {
            request = codec.fromPayload(frame.payload(), TbHttpRequest.class);
        } catch (RuntimeException e) {
            return Mono.just(error(frame, codec, INVALID_REQUEST, "Malformed tb.request payload"));
        }
        String invalidReason = validate(request);
        if (invalidReason != null) {
            log.warn("[{}][{}] Rejected AI request {} {}: {}", context.user().getTenantId(), context.user().getId(),
                    request.method(), request.path(), invalidReason);
            return Mono.just(error(frame, codec, INVALID_REQUEST, invalidReason));
        }
        Duration timeout = request.timeoutMs() != null && request.timeoutMs() > 0 ? Duration.ofMillis(request.timeoutMs()) : DEFAULT_TIMEOUT;
        String query = StringUtils.isEmpty(request.query()) ? "" : "?" + request.query();
        byte[] body = ChannelBodies.decode(request.body(), request.bodyEncoding());
        WebClient.RequestBodySpec spec = webClient.method(HttpMethod.valueOf(request.method().toUpperCase(Locale.ROOT)))
                .uri(URI.create(baseUrl + request.path() + query))
                .headers(headers -> {
                    copyRequestHeaders(request.headers(), headers);
                    context.clientRequest().forwardedHeaders().forEach(headers::set);
                    headers.set(ThingsboardSecurityConfiguration.AUTHORIZATION_HEADER, context.tbAccessToken());
                });
        WebClient.RequestHeadersSpec<?> withBody = body.length > 0 ? spec.bodyValue(body) : spec;
        return withBody.exchangeToMono(response -> response.bodyToMono(byte[].class)
                        .defaultIfEmpty(new byte[0])
                        .map(responseBody -> {
                            if (responseBody.length > maxBodyBytes) {
                                throw new DataBufferLimitException("Response body of " + responseBody.length + " bytes exceeds " + maxBodyBytes);
                            }
                            HttpHeaders responseHeaders = response.headers().asHttpHeaders();
                            String encoding = ChannelBodies.encoding(responseHeaders.getFirst(HttpHeaders.CONTENT_TYPE));
                            var reply = new TbHttpResponse(response.statusCode().value(), responseHeaders(responseHeaders),
                                    ChannelBodies.encode(responseBody, encoding), encoding);
                            return ChannelFrame.reply(ChannelProtocol.TB_RESPONSE, frame, codec.toPayload(reply));
                        }))
                .timeout(timeout)
                .onErrorResume(e -> Mono.just(failure(frame, codec, request, e)));
    }

    public void serve(ChannelSession session, ChannelFrame frame, ChannelFrameCodec codec, TbAiTurnContext context, int maxBodyBytes) {
        execute(frame, codec, context, maxBodyBytes).subscribe(reply -> {
            try {
                session.send(reply);
            } catch (ChannelException e) {
                log.debug("Dropped '{}' reply: {}", reply.type(), e.getMessage());
            }
        });
    }

    static String validate(TbHttpRequest request) {
        if (request.method() == null || !ALLOWED_METHODS.contains(HttpMethod.valueOf(request.method().toUpperCase(Locale.ROOT)))) {
            return "Method is not allowed: " + request.method();
        }
        String path = request.path();
        if (path == null || !path.startsWith(API_PREFIX)) {
            return "Path must start with " + API_PREFIX;
        }
        URI uri;
        try {
            uri = URI.create("http://loopback" + path);
        } catch (IllegalArgumentException e) {
            return "Path is not a valid URI path";
        }
        if (uri.getRawQuery() != null || uri.getRawFragment() != null || !uri.normalize().getRawPath().equals(path)
                || path.contains("//")) {
            return "Path must be a normalized relative path";
        }
        if (request.query() != null) {
            try {
                URI withQuery = URI.create("http://loopback" + path + "?" + request.query());
                if (withQuery.getRawFragment() != null) {
                    return "Query must not contain a fragment";
                }
            } catch (IllegalArgumentException e) {
                return "Query is not a valid URI query";
            }
        }
        return null;
    }

    private static void copyRequestHeaders(Map<String, List<String>> source, HttpHeaders target) {
        if (source == null) {
            return;
        }
        source.forEach((name, values) -> {
            if (!DROPPED_REQUEST_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                target.put(name, values);
            }
        });
    }

    private static Map<String, List<String>> responseHeaders(HttpHeaders headers) {
        var result = new LinkedHashMap<String, List<String>>();
        headers.forEach((name, values) -> {
            if (!DROPPED_RESPONSE_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                result.put(name, List.copyOf(values));
            }
        });
        return result;
    }

    private static ChannelFrame failure(ChannelFrame frame, ChannelFrameCodec codec, TbHttpRequest request, Throwable e) {
        if (e instanceof TimeoutException) {
            return error(frame, codec, TIMEOUT, "ThingsBoard did not answer " + request.method() + " " + request.path() + " in time");
        }
        if (e instanceof DataBufferLimitException) {
            return error(frame, codec, TOO_LARGE, "Response is larger than the channel frame limit");
        }
        log.warn("AI request {} {} failed: {}", request.method(), request.path(), e.getMessage());
        return error(frame, codec, REQUEST_FAILED, "ThingsBoard request failed");
    }

    private static ChannelFrame error(ChannelFrame frame, ChannelFrameCodec codec, String code, String message) {
        return ChannelFrame.reply(ChannelProtocol.ERROR, frame, codec.toPayload(new ChannelError(code, message)));
    }

    private static String loopbackUrl(String address, int port, boolean sslEnabled) {
        String host = StringUtils.isBlank(address) || "0.0.0.0".equals(address) || "::".equals(address) ? "127.0.0.1" : address;
        if (host.contains(":") && !host.startsWith("[")) {
            host = "[" + host + "]";
        }
        return (sslEnabled ? "https" : "http") + "://" + host + ":" + port;
    }

    private static SslContext insecureLoopbackSslContext() {
        try {
            return SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } catch (SSLException e) {
            throw new IllegalStateException("Failed to create the loopback SSL context", e);
        }
    }

}

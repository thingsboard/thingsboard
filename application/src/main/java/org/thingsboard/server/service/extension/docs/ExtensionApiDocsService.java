// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.docs;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionMode;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Fetches an extension's OpenAPI specification for the platform "API docs" page.
 *
 * The pods only admit traffic from the operator's router (NetworkPolicy; in public cloud they live in
 * a separate cluster), so TB cannot call an extension directly. The fetch goes through the router's
 * data plane instead, forwarding the caller's own credential: the router re-authorizes it via
 * /api/ext-auth exactly like any other data-plane request, so this service adds no new trust link and
 * cannot be a confused deputy.
 *
 * The body is tenant-controlled. It is never passed through: it is parsed as JSON, shape-checked to be
 * an OpenAPI document, size-capped, and re-serialized — anything the parser did not accept does not
 * reach a browser.
 */
@Slf4j
@Service
@TbCoreComponent
@ConditionalOnExpression(ExtensionMode.WHEN_ENABLED)
public class ExtensionApiDocsService {

    // Spring (springdoc) serves the spec here; FastAPI serves openapi.json. Probed in this order.
    private static final List<String> SPEC_PATHS = List.of("/v3/api-docs", "/openapi.json");

    private static final int MAX_SPEC_SIZE_BYTES = 5 * 1024 * 1024;
    private static final int READ_CHUNK_BYTES = 32 * 1024;

    // The body is written by the tenant's own container, so it gets its own reader with explicit stream
    // constraints instead of the shared one. Jackson's default nesting cap is 1000, deep enough to make a
    // recursive consumer of this document fail badly, and maxDocumentLength repeats the wire cap at the
    // parser so a chunked response cannot exceed it either.
    private static final ObjectMapper SPEC_MAPPER = JsonMapper.builder(JsonFactory.builder()
                    .streamReadConstraints(StreamReadConstraints.builder()
                            .maxNestingDepth(64)
                            .maxDocumentLength(MAX_SPEC_SIZE_BYTES)
                            .maxNameLength(4096)
                            .build())
                    .build())
            .build();

    // The in-cluster URL of the operator's router. Empty disables the endpoint: routing is
    // operator-owned, so TB cannot guess where the router lives.
    @Value("${extensions.routing.router-internal-url:}")
    private String routerInternalUrl;

    @Value("${extensions.routing.base-path:/api/extension/route}")
    private String basePath;

    // Idle timeout: connect, and time-to-response-headers. A body that keeps trickling never trips it.
    @Value("${extensions.docs.fetch-timeout-ms:10000}")
    private int fetchTimeoutMs;

    // Wall-clock ceiling for one fetch, body included. The idle timeout alone lets an upstream that
    // sends a byte every few seconds hold the request (and its thread) indefinitely.
    @Value("${extensions.docs.fetch-deadline-ms:30000}")
    private long fetchDeadlineMs;

    // Bulkhead, the same shape as the log reads in DefaultExtensionStatusService. The whole upstream
    // round trip — connect, headers, then a body read bounded only by fetchDeadlineMs — is started from
    // the caller's Tomcat thread. The per-user rate limit in the controller bounds the request RATE, not
    // how many of these run at once, so a tenant whose container accepts the connection and then goes
    // quiet could keep one request alive for the full 30 s deadline; a few tenant admins, each with their
    // own 20-per-60 s budget, are enough to fill the default 200-thread connector pool and queue every
    // REST request of every other tenant on the node. With a bounded pool and AbortPolicy the excess
    // fails immediately with 429 instead of occupying anything, and the number of concurrent upstream
    // transfers (sockets, buffers, orchestrator load) is capped whatever the arrival rate.
    //
    // This does not hand the Tomcat thread back — the caller still waits for its own answer — and it is
    // not meant to: what it removes is the unbounded pile-up.
    @Value("${extensions.docs.max-concurrent-fetches:4}")
    private int maxConcurrentFetches;

    private HttpClient httpClient;
    private ThreadPoolExecutor fetchExecutor;

    @PostConstruct
    private void init() {
        // Plain java.net.http, not RestTemplate. Both of Spring's client adapters DRAIN the remaining
        // response body when the response is closed (StreamUtils.drain in SimpleClientHttpResponse and
        // JdkClientHttpResponse alike), so rejecting an oversized or non-2xx answer early still read the
        // whole tenant-controlled body off the wire. Closing the HttpClient body stream instead cancels
        // the subscription: no more bytes are read once the decision is made.
        //
        // Redirects are never followed: a redirect from the extension would otherwise be followed with
        // the caller's credential attached — an open relay for a hostile image.
        httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofMillis(fetchTimeoutMs))
                .build();
        // No queue: a queued fetch would still be waiting when its caller's own deadline has passed, so
        // queueing only converts a fast 429 into a slow failure. Threads are created on first use.
        int threads = Math.max(1, maxConcurrentFetches);
        fetchExecutor = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(),
                ThingsBoardThreadFactory.forName("ext-api-docs"),
                new ThreadPoolExecutor.AbortPolicy());
    }

    @PreDestroy
    private void destroy() {
        if (fetchExecutor != null) {
            fetchExecutor.shutdownNow();
        }
    }

    public boolean isEnabled() {
        return StringUtils.isNotBlank(routerInternalUrl);
    }

    /**
     * @param extension     the extension (already ownership-checked by the controller)
     * @param authorization the caller's X-Authorization header, forwarded verbatim so the router's
     *                      auth_request authorizes the fetch as the caller
     */
    public JsonNode fetchApiDocs(Extension extension, String authorization) throws ThingsboardException {
        if (!isEnabled()) {
            throw new ThingsboardException("Extension API docs are not available: " +
                    "extensions.routing.router-internal-url is not configured.", ThingsboardErrorCode.GENERAL);
        }
        CompletableFuture<JsonNode> result = new CompletableFuture<>();
        try {
            fetchExecutor.execute(() -> {
                try {
                    result.complete(fetchSpec(extension, authorization));
                } catch (Throwable th) {
                    result.completeExceptionally(th);
                }
            });
        } catch (RejectedExecutionException e) {
            // 429, immediately. See the comment on maxConcurrentFetches: this is the point of the pool.
            throw new TbRateLimitsException("Too many extension API docs requests in progress. "
                    + "Please retry shortly.");
        }
        try {
            // fetchSpec enforces fetchDeadlineMs itself; the margin only keeps the caller from reporting
            // a timeout of its own while the real failure is one instant away.
            return result.get(fetchDeadlineMs + 1000L, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new ThingsboardException("Timed out while reading the OpenAPI specification of extension '"
                    + extension.getName() + "'.", ThingsboardErrorCode.GENERAL);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof ThingsboardException te) {
                throw te;
            }
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            if (cause instanceof Error err) {
                throw err;
            }
            throw new ThingsboardException(cause.getMessage(), ThingsboardErrorCode.GENERAL);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ThingsboardException("Interrupted while reading the OpenAPI specification.",
                    ThingsboardErrorCode.GENERAL);
        }
    }

    // The probe sequence itself, always on the bulkhead pool and never on a Tomcat thread.
    private JsonNode fetchSpec(Extension extension, String authorization) throws ThingsboardException {
        String prefix = routerInternalUrl.replaceAll("/+$", "") + basePath + "/" + extension.getSlug();
        // One deadline for the whole probe sequence, so two slow paths cannot double the budget.
        long deadline = System.currentTimeMillis() + fetchDeadlineMs;
        boolean sawNotFound = false;
        for (String specPath : SPEC_PATHS) {
            Fetched fetched;
            try {
                fetched = get(prefix + specPath, authorization, deadline);
            } catch (SpecTooLargeException e) {
                throw tooLarge(extension);
            } catch (IOException | InterruptedException | RuntimeException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                log.debug("[{}][{}] API docs fetch failed: {}", extension.getTenantId(), extension.getId(), e.getMessage());
                throw unreachable(extension);
            }
            int status = fetched.status();
            if (status == HttpStatus.NOT_FOUND.value()) {
                sawNotFound = true;
                continue;
            }
            if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
                throw new ThingsboardException("You don't have permission to perform this operation!",
                        ThingsboardErrorCode.PERMISSION_DENIED);
            }
            if (status >= 300 && status < 400) {
                sawNotFound = true; // a redirect is not followed: treat like "no spec here"
                continue;
            }
            if (status < 200 || status >= 300) {
                log.debug("[{}][{}] API docs fetch failed with status {}", extension.getTenantId(), extension.getId(), status);
                throw unreachable(extension);
            }
            if (fetched.body() == null) {
                sawNotFound = true;
                continue;
            }
            return parseSpec(fetched.body(), extension);
        }
        if (sawNotFound) {
            throw new ThingsboardException("Extension '" + extension.getName() +
                    "' does not expose an OpenAPI specification.", ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        throw unreachable(extension);
    }

    private record Fetched(int status, byte[] body) {}

    // Streams the body through a hard byte cap and a wall-clock deadline. The tenant controls the image
    // and can serve gigabytes at /v3/api-docs, or a byte every few seconds forever. A Content-Length
    // above the cap and any non-2xx status are rejected before a single body byte is read; a chunked
    // 2xx body is cut off at the cap plus one byte or at the deadline. Closing the stream on any of these
    // exits cancels the transfer instead of draining it.
    private Fetched get(String url, String authorization, long deadline) throws IOException, InterruptedException {
        long headersBudget = Math.max(1, Math.min(fetchTimeoutMs, deadline - System.currentTimeMillis()));
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .timeout(Duration.ofMillis(headersBudget));
        if (StringUtils.isNotBlank(authorization)) {
            request.header("X-Authorization", authorization);
        }
        HttpResponse<InputStream> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream in = response.body()) {
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                return new Fetched(status, null); // body intentionally unread
            }
            long declared = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
            if (declared > MAX_SPEC_SIZE_BYTES) {
                throw new SpecTooLargeException();
            }
            // Grow with the response instead of allocating the ceiling up front. The cap is a ceiling,
            // not a typical size — a real specification is tens of kilobytes — so `new byte[5 MiB + 1]`
            // charged every call 5 MiB of heap for the whole round trip, whatever the tenant actually
            // served, and that heap stayed held while a slow upstream ran down the 30 s deadline.
            ByteArrayOutputStream body = new ByteArrayOutputStream(
                    declared > 0 ? (int) Math.min(declared, READ_CHUNK_BYTES) : READ_CHUNK_BYTES);
            byte[] chunk = new byte[READ_CHUNK_BYTES];
            int total = 0;
            while (true) {
                if (System.currentTimeMillis() >= deadline) {
                    throw new IOException("API docs fetch exceeded the " + fetchDeadlineMs + " ms deadline");
                }
                int n = readBounded(in, chunk, 0, deadline);
                if (n == -1) {
                    break;
                }
                total += n;
                // Checked on the running total, so a chunked body that lies about its size is cut off as
                // soon as it crosses the cap rather than after filling the whole ceiling.
                if (total > MAX_SPEC_SIZE_BYTES) {
                    throw new SpecTooLargeException();
                }
                body.write(chunk, 0, n);
            }
            return new Fetched(status, total == 0 ? null : body.toByteArray());
        }
    }

    // One read that cannot outlive the deadline: the HttpClient stream has no per-read timeout, so a
    // silent upstream would park the thread in read() forever. The read runs on a helper and the caller
    // waits at most until the deadline; on expiry the stream is closed, which cancels the transfer and
    // unblocks the helper.
    private static int readBounded(InputStream in, byte[] buf, int off, long deadline) throws IOException {
        CompletableFuture<Integer> read = CompletableFuture.supplyAsync(() -> {
            try {
                return in.read(buf, off, buf.length - off);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, READ_EXECUTOR);
        try {
            return read.get(Math.max(1, deadline - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            in.close();
            throw new IOException("API docs fetch exceeded the deadline while reading the body");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof UncheckedIOException uio) {
                throw uio.getCause();
            }
            throw new IOException(cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            in.close();
            throw new IOException("Interrupted while reading the API docs body", e);
        }
    }

    // Daemon threads, created on demand and released when idle: only bounded-time reads run here.
    private static final ExecutorService READ_EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        Thread t = new Thread(runnable, "ext-api-docs-read");
        t.setDaemon(true);
        return t;
    });

    private static final class SpecTooLargeException extends RuntimeException {
        private SpecTooLargeException() {
            super("OpenAPI specification exceeds " + MAX_SPEC_SIZE_BYTES + " bytes", null, false, false);
        }
    }

    private ThingsboardException tooLarge(Extension extension) {
        return new ThingsboardException("The OpenAPI specification of extension '" + extension.getName() +
                "' exceeds the maximum supported size.", ThingsboardErrorCode.GENERAL);
    }

    // Strict parse + shape check + re-serialization: the only bytes a browser will ever see are the
    // ones Jackson produced, not the ones the tenant image sent.
    JsonNode parseSpec(byte[] body, Extension extension) throws ThingsboardException {
        JsonNode spec;
        try {
            // Deliberately NOT JacksonUtil.fromBytes(byte[]): that helper builds
            // "...cannot be transformed to Json object: " + Arrays.toString(bytes) for its exception
            // message, so a 5 MiB non-JSON body — which passes the wire cap — produced a String of about
            // 27 million characters on this Tomcat worker thread, roughly 40x the bytes the cap allows,
            // and the catch below threw it straight away. The cap bounded the wire, not the request.
            spec = SPEC_MAPPER.readTree(body);
        } catch (Exception e) {
            log.debug("Extension {} returned a document that could not be parsed: {}",
                    extension.getSlug(), e.getMessage());
            spec = null;
        }
        if (spec == null || !spec.isObject() || !(spec.hasNonNull("openapi") || spec.hasNonNull("swagger"))) {
            throw new ThingsboardException("Extension '" + extension.getName() +
                    "' returned a document that is not a valid OpenAPI specification.", ThingsboardErrorCode.GENERAL);
        }
        // Every routing field is tenant-controlled and, on this internal fetch, reflects the router's
        // in-cluster host anyway. Pin ALL of them to the canonical public data-plane path, so "Try it
        // out" stays same-origin AND inside this extension's route: a hostile spec must not be able to
        // present a platform endpoint (say /api/auth/user) as one of its own operations and have the
        // viewer execute it with their credential. OpenAPI 3 allows `servers` on the root, on every Path
        // Item and on every Operation, the innermost winning; Swagger 2 routes through host/basePath/
        // schemes instead. The UI enforces the same boundary on the final request URL as a second line.
        ObjectNode result = (ObjectNode) spec;
        String route = basePath + "/" + extension.getSlug();
        if (result.hasNonNull("openapi")) {
            result.set("servers", pinnedServers(route));
            JsonNode paths = result.get("paths");
            if (paths != null && paths.isObject()) {
                paths.forEach(pathItem -> {
                    if (pathItem == null || !pathItem.isObject()) {
                        return;
                    }
                    ObjectNode item = (ObjectNode) pathItem;
                    item.remove("servers");
                    item.forEach(operation -> {
                        if (operation != null && operation.isObject()) {
                            ((ObjectNode) operation).remove("servers");
                        }
                    });
                });
            }
        } else {
            // Swagger 2: no `servers`; the URL is schemes://host + basePath. Relative (same-origin) when
            // host and schemes are absent.
            result.remove("host");
            result.remove("schemes");
            result.remove("servers");
            result.put("basePath", route);
        }
        return result;
    }

    private static JsonNode pinnedServers(String route) {
        return JacksonUtil.newArrayNode().add(JacksonUtil.newObjectNode().put("url", route));
    }

    private ThingsboardException unreachable(Extension extension) {
        return new ThingsboardException("The API documentation of extension '" + extension.getName() +
                "' is not reachable. Make sure the extension is running.", ThingsboardErrorCode.GENERAL);
    }

}

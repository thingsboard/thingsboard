// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.docs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
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
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    private HttpClient httpClient;

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
            byte[] buf = new byte[MAX_SPEC_SIZE_BYTES + 1];
            int total = 0;
            int n;
            while (total < buf.length) {
                if (System.currentTimeMillis() >= deadline) {
                    throw new IOException("API docs fetch exceeded the " + fetchDeadlineMs + " ms deadline");
                }
                n = readBounded(in, buf, total, deadline);
                if (n == -1) {
                    break;
                }
                total += n;
            }
            if (total > MAX_SPEC_SIZE_BYTES) {
                throw new SpecTooLargeException();
            }
            return new Fetched(status, total == 0 ? null : Arrays.copyOf(buf, total));
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
            spec = JacksonUtil.fromBytes(body);
        } catch (Exception e) {
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

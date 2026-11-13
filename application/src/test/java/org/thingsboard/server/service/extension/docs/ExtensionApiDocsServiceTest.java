// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.docs;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.id.ExtensionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The body parsed here is written by the tenant's own container and arrives on the request thread, so
 * these cover what a hostile document may cost and what it may put in front of the caller.
 */
class ExtensionApiDocsServiceTest {

    private final ExtensionApiDocsService service = new ExtensionApiDocsService();

    private static Extension extension() {
        Extension e = new Extension(new ExtensionId(UUID.randomUUID()));
        e.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        e.setName("Acme Billing");
        e.setSlug("acme-billing-9f3a");
        return e;
    }

    @Test
    void anOpenApiDocumentIsParsed() throws ThingsboardException {
        JsonNode spec = service.parseSpec("{\"openapi\":\"3.0.1\",\"paths\":{}}".getBytes(StandardCharsets.UTF_8),
                extension());
        assertThat(spec.get("openapi").asText()).isEqualTo("3.0.1");
    }

    // The shared mapper allows 1000 levels, deep enough to break a recursive consumer of this document.
    // The reader used here is built with its own StreamReadConstraints, so the parse stops much earlier
    // and the caller gets the ordinary "not a valid OpenAPI specification" answer.
    @Test
    void aDocumentNestedFarBeyondAnyRealSpecIsRejected() {
        byte[] deep = ("{\"openapi\":\"3.0.1\",\"paths\":" + "[".repeat(300) + "]".repeat(300) + "}")
                .getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> service.parseSpec(deep, extension()))
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining("not a valid OpenAPI specification");
    }

    // A 5 MiB body of non-JSON passes the wire cap and then fails to parse. JacksonUtil.fromBytes(byte[])
    // would have built "... cannot be transformed to Json object: " + Arrays.toString(bytes) first — about
    // 27 million characters for this input, on the request thread, only to be discarded by the catch. The
    // assertion on the message is the part that keeps working as a guard: whatever the parser reports, no
    // part of the tenant's body may travel back to the caller.
    @Test
    void aLargeNonJsonBodyIsRejectedAndNoneOfItReachesTheCaller() {
        byte[] garbage = new byte[5 * 1024 * 1024];
        java.util.Arrays.fill(garbage, (byte) 'A');
        assertThatThrownBy(() -> service.parseSpec(garbage, extension()))
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining("not a valid OpenAPI specification")
                .hasMessageNotContaining("AAAA");
    }

    // Valid JSON that is not an OpenAPI document is refused by the shape check, not by the parser.
    @Test
    void jsonWithoutAnOpenApiOrSwaggerKeyIsRejected() {
        assertThatThrownBy(() -> service.parseSpec("{\"hello\":\"world\"}".getBytes(StandardCharsets.UTF_8),
                extension()))
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining("not a valid OpenAPI specification");
    }

    @Test
    void anArrayBodyIsRejected() {
        assertThatThrownBy(() -> service.parseSpec("[1,2,3]".getBytes(StandardCharsets.UTF_8), extension()))
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining("not a valid OpenAPI specification");
    }

    // The round trip is bounded by a deadline, not by how many of them may run at once, and the
    // controller's rate limit bounds arrivals per user rather than concurrency. A container that accepts
    // the connection and then stays quiet therefore used to hold a Tomcat worker for the whole deadline,
    // and nothing stopped the next request from doing the same. With the bulkhead the overflow is
    // refused at once, which is what keeps the rest of the REST API answering.
    @Test
    void anExtraConcurrentFetchIsRefusedAtOnceInsteadOfPilingUp() throws Exception {
        try (ServerSocket silent = new ServerSocket(0)) {
            CountDownLatch accepted = new CountDownLatch(1);
            List<Socket> held = new CopyOnWriteArrayList<>();
            Thread acceptor = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        held.add(silent.accept()); // accepted, then never answered
                        accepted.countDown();
                    }
                } catch (IOException closed) {
                    // the socket was closed by the test
                }
            });
            acceptor.setDaemon(true);
            acceptor.start();

            ExtensionApiDocsService svc = new ExtensionApiDocsService();
            ReflectionTestUtils.setField(svc, "routerInternalUrl", "http://127.0.0.1:" + silent.getLocalPort());
            ReflectionTestUtils.setField(svc, "basePath", "/api/extension/route");
            ReflectionTestUtils.setField(svc, "fetchTimeoutMs", 5000);
            ReflectionTestUtils.setField(svc, "fetchDeadlineMs", 3000L);
            ReflectionTestUtils.setField(svc, "maxConcurrentFetches", 1);
            ReflectionTestUtils.invokeMethod(svc, "init");
            try {
                Thread first = new Thread(() -> {
                    try {
                        svc.fetchApiDocs(extension(), "Bearer first");
                    } catch (Exception expected) {
                        // it never gets an answer; the point is that it occupies the only slot
                    }
                });
                first.setDaemon(true);
                first.start();

                assertThat(accepted.await(10, TimeUnit.SECONDS))
                        .as("the first fetch should have reached the silent upstream")
                        .isTrue();

                assertThatThrownBy(() -> svc.fetchApiDocs(extension(), "Bearer second"))
                        .isInstanceOf(TbRateLimitsException.class);

                first.interrupt();
            } finally {
                ReflectionTestUtils.invokeMethod(svc, "destroy");
                acceptor.interrupt();
                for (Socket s : held) {
                    try {
                        s.close();
                    } catch (IOException ignored) {
                        // best effort
                    }
                }
            }
        }
    }
}

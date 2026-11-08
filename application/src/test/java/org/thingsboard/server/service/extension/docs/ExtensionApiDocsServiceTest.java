// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.docs;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.id.ExtensionId;
import org.thingsboard.server.common.data.id.TenantId;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

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
}

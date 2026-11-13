// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.spi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.hash.Hashing;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionContainerSpec;
import org.thingsboard.server.common.data.extension.ResourceSpec;

import java.nio.charset.StandardCharsets;

/**
 * Stable digest of the container spec, stamped on the workload (K8s annotation / Docker label) so a
 * re-apply with an unchanged spec is a no-op on every substrate. Map entries are key-ordered so the
 * hash is deterministic regardless of env-var insertion order.
 *
 * <p>The spec passed here is the <em>resolved</em> one (secret references already replaced with their
 * plaintext), so rotating a referenced secret changes the digest and re-renders the workload. Because
 * the digest is also stamped on objects that can be read without {@code get secrets} (the Deployment
 * annotation, the Docker label), an operator may set {@code extensions.spec-hash-salt}: when present
 * the digest is a keyed HMAC-SHA-256 and is no longer an offline-brute-forceable fingerprint of the
 * resolved secret values. When the salt is blank the plain SHA-256 is used, so the default behaviour
 * and cross-restart idempotency are unchanged.
 */
public final class ExtensionSpecHasher {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .addMixIn(ExtensionContainerSpec.class, IgnoreReplicasMixin.class);

    private ExtensionSpecHasher() {}

    /**
     * Drops the replica count from the hashed payload. The digest is stamped into the Kubernetes pod
     * TEMPLATE, so counting replicas in it would make a pure scale change alter the template and roll
     * every healthy pod — losing their in-memory state — while the cheap desiredReplicas/liveReplicas
     * drift check that exists for exactly this case would never be reached. On Docker it is worse: a
     * hash mismatch means stop + remove + recreate, a real outage for an edit that backend documents as
     * on/off only. Scaling is converged by the replica comparison in the backends, not by the digest.
     *
     * <p>Applied as a mix-in so the spec still travels the same POJO serialization path (and the same
     * key ordering) as before, minus this one field.
     */
    private abstract static class IgnoreReplicasMixin {
        @JsonIgnore
        abstract int getReplicas();
    }

    public static String hash(Extension extension) {
        return hash(extension.getContainerSpec(), null, null);
    }

    public static String hash(Extension extension, String salt) {
        return hash(extension.getContainerSpec(), salt, null);
    }

    // renderContext carries the operator-level inputs the active backend injects into the workload
    // (tb-core URL, base/health path, network policy, ...) that are NOT part of the user spec. Folding
    // them into the digest makes a change to any of them alter the hash, so existing workloads
    // re-converge on the next apply/resync instead of being left on stale injected values.
    public static String hash(Extension extension, String salt, String renderContext) {
        return hash(extension.getContainerSpec(), salt, renderContext);
    }

    public static String hash(ExtensionContainerSpec spec) {
        return hash(spec, null, null);
    }

    public static String hash(ExtensionContainerSpec spec, String salt) {
        return hash(spec, salt, null);
    }

    // `resources` absent, {"size":null} and {"size":"SMALL"} all resolve to the same tier, but the raw
    // object serializes differently for each (nulls included), so a no-op save from a client that spells
    // the default out (the UI form always sends {size:"SMALL"}) would change the digest and recreate the
    // container / roll every pod. Hash the EFFECTIVE tier instead, so equivalent specs hash alike.
    private static JsonNode normalize(ExtensionContainerSpec spec) {
        if (spec == null) {
            return null;
        }
        ObjectNode node = MAPPER.valueToTree(spec);
        ResourceSpec resources = spec.getResources() != null ? spec.getResources() : new ResourceSpec();
        node.set("resources", MAPPER.createObjectNode().put("size", resources.effectiveSize().name()));
        return node;
    }

    public static String hash(ExtensionContainerSpec spec, String salt, String renderContext) {
        try {
            byte[] specBytes = MAPPER.writeValueAsBytes(normalize(spec));
            var hasher = (salt == null || salt.isBlank())
                    ? Hashing.sha256().newHasher()
                    : Hashing.hmacSha256(salt.getBytes(StandardCharsets.UTF_8)).newHasher();
            hasher.putBytes(specBytes);
            if (renderContext != null && !renderContext.isEmpty()) {
                // NUL-separate the context from the spec bytes (domain separation) so it can never be
                // confused with spec content. A blank/absent context leaves the digest byte-for-byte
                // identical to the previous spec-only hash.
                hasher.putByte((byte) 0).putString(renderContext, StandardCharsets.UTF_8);
            }
            return hasher.hash().toString();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize extension spec for hashing", e);
        }
    }

}

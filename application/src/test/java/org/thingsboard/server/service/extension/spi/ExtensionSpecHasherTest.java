// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.spi;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionContainerSpec;
import org.thingsboard.server.common.data.extension.ExtensionState;
import org.thingsboard.server.common.data.extension.ResourceSize;
import org.thingsboard.server.common.data.extension.ResourceSpec;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

class ExtensionSpecHasherTest {

    private Extension ext(String image, String tag, int replicas, Map<String, String> env) {
        Extension e = new Extension();
        e.setName("Acme");
        e.setState(ExtensionState.ACTIVE);
        ExtensionContainerSpec spec = new ExtensionContainerSpec();
        spec.setImage(image);
        spec.setTag(tag);
        spec.setReplicas(replicas);
        spec.setPort(8090);
        spec.setEnv(env);
        e.setContainerSpec(spec);
        return e;
    }

    @Test
    void hashIsStableAndHex() {
        String h1 = ExtensionSpecHasher.hash(ext("img", "1.0", 2, Map.of("A", "1")));
        String h2 = ExtensionSpecHasher.hash(ext("img", "1.0", 2, Map.of("A", "1")));
        assertThat(h1).isEqualTo(h2).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    void hashIgnoresEnvOrdering() {
        Map<String, String> a = new LinkedHashMap<>();
        a.put("A", "1");
        a.put("B", "2");
        Map<String, String> b = new TreeMap<>();
        b.put("B", "2");
        b.put("A", "1");
        assertThat(ExtensionSpecHasher.hash(ext("img", "1.0", 1, a)))
                .isEqualTo(ExtensionSpecHasher.hash(ext("img", "1.0", 1, b)));
    }

    @Test
    void hashIgnoresNameAndState() {
        Extension a = ext("img", "1.0", 1, null);
        Extension b = ext("img", "1.0", 1, null);
        b.setName("Different");
        b.setState(ExtensionState.FAILED);
        assertThat(ExtensionSpecHasher.hash(a)).isEqualTo(ExtensionSpecHasher.hash(b));
    }

    @Test
    void hashChangesOnTagAndImage() {
        String base = ExtensionSpecHasher.hash(ext("img", "1.0", 1, null));
        assertThat(ExtensionSpecHasher.hash(ext("img", "2.0", 1, null))).isNotEqualTo(base);
        assertThat(ExtensionSpecHasher.hash(ext("other", "1.0", 1, null))).isNotEqualTo(base);
    }

    // The digest is stamped into the K8s pod TEMPLATE, so letting the replica count into it would turn a
    // pure scale into a rolling restart of every healthy pod (and on Docker into a stop/remove/recreate
    // for an edit that backend treats as on/off only). Scaling is converged by the backends' replica
    // comparison instead, which is only reachable while the hash stays equal.
    // `resources` absent, {size:null} and {size:SMALL} are the same tier. The UI form always spells the
    // default out, so a no-op save of an API-created extension used to change the digest and recreate the
    // container / roll every pod.
    @Test
    void hashTreatsEquivalentResourceSpecsAlike() {
        ExtensionContainerSpec absent = ext("img", "1.0", 1, Map.of()).getContainerSpec();
        absent.setResources(null);
        ExtensionContainerSpec nullSize = ext("img", "1.0", 1, Map.of()).getContainerSpec();
        nullSize.setResources(new ResourceSpec());
        ExtensionContainerSpec explicit = ext("img", "1.0", 1, Map.of()).getContainerSpec();
        ResourceSpec small = new ResourceSpec();
        small.setSize(ResourceSize.SMALL);
        explicit.setResources(small);
        ExtensionContainerSpec other = ext("img", "1.0", 1, Map.of()).getContainerSpec();
        ResourceSpec large = new ResourceSpec();
        large.setSize(ResourceSize.LARGE);
        other.setResources(large);

        assertThat(ExtensionSpecHasher.hash(absent))
                .isEqualTo(ExtensionSpecHasher.hash(nullSize))
                .isEqualTo(ExtensionSpecHasher.hash(explicit))
                .isNotEqualTo(ExtensionSpecHasher.hash(other));
    }

    @Test
    void hashIgnoresReplicasSoScalingIsNotARestart() {
        String base = ExtensionSpecHasher.hash(ext("img", "1.0", 1, Map.of("A", "1")));
        assertThat(ExtensionSpecHasher.hash(ext("img", "1.0", 3, Map.of("A", "1")))).isEqualTo(base);
        assertThat(ExtensionSpecHasher.hash(ext("img", "1.0", 20, Map.of("A", "1")))).isEqualTo(base);
        // everything else about the spec still counts
        assertThat(ExtensionSpecHasher.hash(ext("img", "1.0", 3, Map.of("A", "2")))).isNotEqualTo(base);
    }

    @Test
    void blankSaltKeepsPlainSha256ButSaltKeysTheHash() {
        Extension e = ext("img", "1.0", 1, Map.of("A", "1"));
        String plain = ExtensionSpecHasher.hash(e);
        // blank/null/whitespace salt => unchanged plain SHA-256 (default behaviour, idempotency preserved)
        assertThat(ExtensionSpecHasher.hash(e, null)).isEqualTo(plain);
        assertThat(ExtensionSpecHasher.hash(e, "")).isEqualTo(plain);
        assertThat(ExtensionSpecHasher.hash(e, "   ")).isEqualTo(plain);
        // a set salt => keyed HMAC: still hex, stable, but differs from plain and is key-dependent
        String salted = ExtensionSpecHasher.hash(e, "server-salt");
        assertThat(salted).hasSize(64).matches("[0-9a-f]{64}").isNotEqualTo(plain);
        assertThat(ExtensionSpecHasher.hash(e, "server-salt")).isEqualTo(salted);
        assertThat(ExtensionSpecHasher.hash(e, "other-salt")).isNotEqualTo(salted);
        // rotation still works: changing a resolved value changes the salted hash too
        assertThat(ExtensionSpecHasher.hash(ext("img", "1.0", 1, Map.of("A", "2")), "server-salt")).isNotEqualTo(salted);
    }

}

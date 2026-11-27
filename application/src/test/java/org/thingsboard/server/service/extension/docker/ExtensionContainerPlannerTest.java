// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.docker;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionContainerSpec;
import org.thingsboard.server.common.data.extension.ExtensionState;
import org.thingsboard.server.common.data.extension.ResourceSize;
import org.thingsboard.server.common.data.extension.ResourceSpec;
import org.thingsboard.server.common.data.id.ExtensionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.extension.spi.ExtensionResourceNames;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-mapping coverage for {@link ExtensionContainerPlanner}: env (built-in TB_EXT_* + sorted user
 * env), labels (managed-by / slug / spec-hash), image ref + pull policy, port, and resource ceilings.
 */
class ExtensionContainerPlannerTest {

    private final ExtensionContainerPlanner planner =
            new ExtensionContainerPlanner("http://tb-core:8080", "/api/extension/route");

    private Extension extension(String tag, Map<String, String> env, ResourceSpec resources) {
        Extension e = new Extension(new ExtensionId(UUID.randomUUID()));
        e.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        e.setName("Acme Billing");
        e.setSlug("acme-billing-9f3a");
        e.setState(ExtensionState.PROVISIONING);
        ExtensionContainerSpec spec = new ExtensionContainerSpec();
        spec.setImage("registry.example.com/acme/billing-ext");
        spec.setTag(tag);
        spec.setPort(9000);
        spec.setEnv(env);
        spec.setResources(resources);
        e.setContainerSpec(spec);
        return e;
    }

    @Test
    void mapsNamesImageAndPort() {
        ContainerPlan plan = planner.plan(extension("1.4.0", null, null), "hash-abc");
        assertThat(plan.name()).isEqualTo("tb-ext-acme-billing-9f3a");
        assertThat(plan.repository()).isEqualTo("registry.example.com/acme/billing-ext");
        assertThat(plan.tag()).isEqualTo("1.4.0");
        assertThat(plan.imageRef()).isEqualTo("registry.example.com/acme/billing-ext:1.4.0");
        assertThat(plan.exposedPort()).isEqualTo(9000);
        assertThat(plan.pullAlways()).isFalse();
        assertThat(plan.specHash()).isEqualTo("hash-abc");
    }

    @Test
    void latestTagForcesPull() {
        assertThat(planner.plan(extension("latest", null, null), "h").pullAlways()).isTrue();
        assertThat(planner.plan(extension("LATEST", null, null), "h").pullAlways()).isTrue();
    }

    @Test
    void builtInEnvFirstThenSortedUserEnv() {
        Map<String, String> env = new LinkedHashMap<>();
        env.put("ZED", "z");
        env.put("ALPHA", "a");
        Extension ext = extension("1.0.0", env, null);

        ContainerPlan plan = planner.plan(ext, "h");

        assertThat(plan.env()).containsSubsequence(
                "TB_EXT_SLUG=acme-billing-9f3a",
                "TB_EXT_ID=" + ext.getId().getId(),
                "TB_EXT_TENANT_ID=" + ext.getTenantId().getId(),
                "TB_EXT_BASE_PATH=/api/extension/route/acme-billing-9f3a",
                "THINGSBOARD_URL=http://tb-core:8080",
                "ALPHA=a",   // user env sorted by key so the rendered list is deterministic
                "ZED=z");
    }

    @Test
    void userEnvCannotShadowBuiltIns() {
        Map<String, String> env = new LinkedHashMap<>();
        env.put("THINGSBOARD_URL", "http://attacker.example"); // collides with a built-in (not TB_EXT_ prefixed)
        env.put("ALPHA", "a");
        Extension ext = extension("1.0.0", env, null);

        ContainerPlan plan = planner.plan(ext, "h");

        // the built-in value wins and the colliding user entry is dropped (no duplicate key -> no last-wins override)
        assertThat(plan.env()).contains("THINGSBOARD_URL=http://tb-core:8080");
        assertThat(plan.env()).doesNotContain("THINGSBOARD_URL=http://attacker.example");
        assertThat(plan.env().stream().filter(s -> s.startsWith("THINGSBOARD_URL=")).count()).isEqualTo(1);
        assertThat(plan.env()).contains("ALPHA=a"); // unrelated user env still present
    }

    @Test
    void labelsCarryManagedBySlugAndSpecHash() {
        ContainerPlan plan = planner.plan(extension("1.0.0", null, null), "hash-xyz");
        assertThat(plan.labels())
                .containsEntry(ExtensionResourceNames.LABEL_MANAGED_BY, ExtensionResourceNames.MANAGED_BY)
                .containsEntry(ExtensionResourceNames.LABEL_EXTENSION_SLUG, "acme-billing-9f3a")
                .containsEntry(ExtensionResourceNames.ANNOTATION_SPEC_HASH, "hash-xyz");
    }

    @Test
    void resourceSizeLimitsConvertedToDockerUnits() {
        ResourceSpec resources = new ResourceSpec();
        resources.setSize(ResourceSize.MEDIUM); // cpuLimit "1", memory 512Mi
        ContainerPlan plan = planner.plan(extension("1.0.0", null, resources), "h");
        assertThat(plan.nanoCpus()).isEqualTo(1_000_000_000L);
        assertThat(plan.memoryBytes()).isEqualTo(512L * 1024 * 1024);
    }

    @Test
    void defaultResourcesUseTheDefaultTier() {
        // No resources set -> ResourceSpec falls back to ResourceSize.DEFAULT via effective*Limit().
        // Assert against the tier so moving the default does not leave this checking the wrong numbers.
        ContainerPlan plan = planner.plan(extension("1.0.0", null, null), "h");
        assertThat(plan.nanoCpus()).isEqualTo(ResourceQuantities.cpuToNanoCpus(ResourceSize.DEFAULT.getCpuLimit()));
        assertThat(plan.memoryBytes()).isEqualTo(ResourceQuantities.memoryToBytes(ResourceSize.DEFAULT.getMemoryLimit()));
    }

}

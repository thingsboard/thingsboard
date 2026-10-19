// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.agent;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AppTemplateRegistryTest {

    private static final AgentAppConfigType DC = AgentAppConfigType.DOCKER_COMPOSE;

    private AgentAppTemplate template(AgentApplicationType appType, String version, String nextVersion) {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setAppType(appType);
        template.setConfigType(DC);
        template.setCurrentVersion(version);
        template.setNextVersion(nextVersion);
        return template;
    }

    @Test
    void emptyRegistryReturnsNoTemplates() {
        AppTemplateRegistry registry = new AppTemplateRegistry();
        assertThat(registry.isEmpty()).isTrue();
        assertThat(registry.get(AgentApplicationType.EDGE, DC, "1")).isNull();
        assertThat(registry.latest(AgentApplicationType.EDGE, DC)).isNull();
        assertThat(registry.next(AgentApplicationType.EDGE, DC, "1")).isNull();
        assertThat(registry.list(AgentApplicationType.EDGE, DC)).isEmpty();
        assertThat(registry.all()).isEmpty();
    }

    @Test
    void getReturnsExactMatchAndNullForUnknownOrNullVersion() {
        AppTemplateRegistry registry = new AppTemplateRegistry();
        AgentAppTemplate v1 = template(AgentApplicationType.EDGE, "1", "2");
        AgentAppTemplate v2 = template(AgentApplicationType.EDGE, "2", null);
        Map<String, AgentAppTemplate> versions = new LinkedHashMap<>();
        versions.put("1", v1);
        versions.put("2", v2);
        registry.replace(AgentApplicationType.EDGE, DC, versions, v2);

        assertThat(registry.get(AgentApplicationType.EDGE, DC, "1")).isSameAs(v1);
        assertThat(registry.get(AgentApplicationType.EDGE, DC, "2")).isSameAs(v2);
        // strict lookup: an unknown version must NOT fall back to the latest template
        assertThat(registry.get(AgentApplicationType.EDGE, DC, "9")).isNull();
        assertThat(registry.get(AgentApplicationType.EDGE, DC, null)).isNull();
        // config type is part of the key
        assertThat(registry.get(AgentApplicationType.EDGE, null, "1")).isNull();
    }

    @Test
    void latestAndNextFollowTheChain() {
        AppTemplateRegistry registry = new AppTemplateRegistry();
        AgentAppTemplate v1 = template(AgentApplicationType.EDGE, "1", "2");
        AgentAppTemplate v2 = template(AgentApplicationType.EDGE, "2", null);
        registry.replace(AgentApplicationType.EDGE, DC, Map.of("1", v1, "2", v2), v2);

        assertThat(registry.latest(AgentApplicationType.EDGE, DC)).isSameAs(v2);
        assertThat(registry.next(AgentApplicationType.EDGE, DC, "1")).isEqualTo("2");
        assertThat(registry.next(AgentApplicationType.EDGE, DC, "2")).isNull();
        assertThat(registry.next(AgentApplicationType.EDGE, DC, "9")).isNull();
    }

    @Test
    void replaceWithNullLatestClearsHeadAndVersions() {
        AppTemplateRegistry registry = new AppTemplateRegistry();
        AgentAppTemplate v1 = template(AgentApplicationType.EDGE, "1", null);
        registry.replace(AgentApplicationType.EDGE, DC, Map.of("1", v1), v1);
        assertThat(registry.latest(AgentApplicationType.EDGE, DC)).isSameAs(v1);

        registry.replace(AgentApplicationType.EDGE, DC, Map.of(), null);
        assertThat(registry.latest(AgentApplicationType.EDGE, DC)).isNull();
        assertThat(registry.list(AgentApplicationType.EDGE, DC)).isEmpty();
    }

    @Test
    void retainOnlyDropsKeysMissingFromTheSyncPass() {
        AppTemplateRegistry registry = new AppTemplateRegistry();
        AgentAppTemplate edge = template(AgentApplicationType.EDGE, "1", null);
        AgentAppTemplate gw = template(AgentApplicationType.GATEWAY, "1", null);
        registry.replace(AgentApplicationType.EDGE, DC, Map.of("1", edge), edge);
        registry.replace(AgentApplicationType.GATEWAY, DC, Map.of("1", gw), gw);

        registry.retainOnly(Set.of(new AppTemplateRegistry.Key(AgentApplicationType.EDGE, DC)));

        assertThat(registry.get(AgentApplicationType.EDGE, DC, "1")).isSameAs(edge);
        assertThat(registry.get(AgentApplicationType.GATEWAY, DC, "1")).isNull();
        assertThat(registry.latest(AgentApplicationType.GATEWAY, DC)).isNull();
        assertThat(registry.all()).containsExactly(edge);
    }

    @Test
    void retainOnlyKeepsEverythingWhenTheSyncPassSawNothing() {
        AppTemplateRegistry registry = new AppTemplateRegistry();
        AgentAppTemplate edge = template(AgentApplicationType.EDGE, "1", null);
        registry.replace(AgentApplicationType.EDGE, DC, Map.of("1", edge), edge);

        registry.retainOnly(Set.of());

        assertThat(registry.get(AgentApplicationType.EDGE, DC, "1")).isSameAs(edge);
        assertThat(registry.latest(AgentApplicationType.EDGE, DC)).isSameAs(edge);
    }

    @Test
    void keysAreScopedByAppTypeAndConfigType() {
        AppTemplateRegistry registry = new AppTemplateRegistry();
        AgentAppTemplate edge = template(AgentApplicationType.EDGE, "1", null);
        AgentAppTemplate gw = template(AgentApplicationType.GATEWAY, "1", null);
        registry.replace(AgentApplicationType.EDGE, DC, Map.of("1", edge), edge);
        registry.replace(AgentApplicationType.GATEWAY, DC, Map.of("1", gw), gw);

        assertThat(registry.get(AgentApplicationType.EDGE, DC, "1")).isSameAs(edge);
        assertThat(registry.get(AgentApplicationType.GATEWAY, DC, "1")).isSameAs(gw);
        assertThat(registry.all()).containsExactlyInAnyOrder(edge, gw);
        assertThat(registry.isEmpty()).isFalse();
    }
}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.agent.AgentUpgradeInfo;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.service.agent.upgrade.DefaultAgentUpgradeVersionService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * An agent reports an image reference rather than a version number, so everything here turns on
 * what can and cannot be concluded from a tag: a pinned tag places the agent in the chain, a
 * floating one does not, and an unpublished repo must not make install commands unusable.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DefaultAgentUpgradeVersionServiceTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentId AGENT_ID = new AgentId(UUID.randomUUID());

    @Mock
    private AttributesService attributesService;

    @InjectMocks
    private DefaultAgentUpgradeVersionService service;

    @BeforeEach
    void setUp() {
        Map<String, AgentUpgradeInfo> graph = new LinkedHashMap<>();
        graph.put("1.0.0", new AgentUpgradeInfo("1.0.1"));
        graph.put("1.0.1", new AgentUpgradeInfo("1.1.0"));
        graph.put("1.1.0", new AgentUpgradeInfo(null));
        graph.put("latest", new AgentUpgradeInfo(null));
        service.updateVersionGraph(graph);
    }

    @Test
    void pinnedTagBehindTheNewestOneCanUpgrade() throws Exception {
        reportedVersion("thingsboard/tb-remote-agent:1.0.0");

        assertThat(service.isUpgradeAvailable(TENANT_ID, AGENT_ID)).isTrue();
        assertThat(service.getUpgradeImageRef(TENANT_ID, AGENT_ID))
                .contains("thingsboard/tb-remote-agent:1.1.0");
    }

    /**
     * A list resolves the target from the reference it already holds while a single-entity view reads the
     * attribute first; both must answer identically, including for the references that are deliberately
     * not placeable, or a row action and a details page can contradict each other.
     */
    @Test
    void resolvingFromAHeldReferenceMatchesResolvingFromTheAttribute() throws Exception {
        for (String imageRef : new String[]{
                "thingsboard/tb-remote-agent:1.0.0",
                "thingsboard/tb-remote-agent:1.0.1",
                "thingsboard/tb-remote-agent:1.1.0",
                "thingsboard/tb-remote-agent:latest",
                "thingsboard/tb-remote-agent:9.9.9",
                "thingsboard/tb-remote-agent@sha256:0123456789abcdef"}) {
            reportedVersion(imageRef);
            assertThat(service.getUpgradeImageRefFor(imageRef))
                    .as("held reference %s", imageRef)
                    .isEqualTo(service.getUpgradeImageRef(TENANT_ID, AGENT_ID));
        }
        assertThat(service.getUpgradeImageRefFor(null)).isEmpty();
    }

    @Test
    void theNewestPinnedTagHasNothingToUpgradeTo() throws Exception {
        reportedVersion("thingsboard/tb-remote-agent:1.1.0");

        assertThat(service.isUpgradeAvailable(TENANT_ID, AGENT_ID)).isFalse();
        assertThat(service.getUpgradeImageRef(TENANT_ID, AGENT_ID)).isEmpty();
    }

    @Test
    void floatingTagIsNeverOfferedAnUpgrade() throws Exception {
        reportedVersion("thingsboard/tb-remote-agent:latest");

        assertThat(service.isUpgradeAvailable(TENANT_ID, AGENT_ID)).isFalse();
    }

    @Test
    void unknownAndDigestPinnedReferencesAreNotPlaceable() throws Exception {
        reportedVersion("thingsboard/tb-remote-agent:9.9.9");
        assertThat(service.isUpgradeAvailable(TENANT_ID, AGENT_ID)).isFalse();

        reportedVersion("thingsboard/tb-remote-agent@sha256:0123456789abcdef");
        assertThat(service.isUpgradeAvailable(TENANT_ID, AGENT_ID)).isFalse();

        reportedVersion("thingsboard/tb-remote-agent");
        assertThat(service.isUpgradeAvailable(TENANT_ID, AGENT_ID)).isFalse();
    }

    /**
     * A digest reference carries no tag at all, so its digest must not be read as one even when the
     * hex happens to collide with a published tag - what the digest resolves to is not knowable here.
     */
    @Test
    void aDigestIsNeverReadAsATag() throws Exception {
        Map<String, AgentUpgradeInfo> graph = new LinkedHashMap<>();
        graph.put("0123456789abcdef", new AgentUpgradeInfo("1.1.0"));
        graph.put("1.0.0", new AgentUpgradeInfo("1.1.0"));
        graph.put("1.1.0", new AgentUpgradeInfo(null));
        service.updateVersionGraph(graph);

        reportedVersion("thingsboard/tb-remote-agent@sha256:0123456789abcdef");
        assertThat(service.isUpgradeAvailable(TENANT_ID, AGENT_ID)).isFalse();

        reportedVersion("thingsboard/tb-remote-agent:1.0.0@sha256:0123456789abcdef");
        assertThat(service.isUpgradeAvailable(TENANT_ID, AGENT_ID)).isFalse();
    }

    @Test
    void anAgentThatNeverReportedAVersionCanNotUpgrade() throws Exception {
        when(attributesService.find(eq(TENANT_ID), eq(AGENT_ID), eq(AttributeScope.SERVER_SCOPE),
                eq(DefaultAgentStateService.AGENT_VERSION)))
                .thenReturn(Futures.immediateFuture(Optional.empty()));

        assertThat(service.isUpgradeAvailable(TENANT_ID, AGENT_ID)).isFalse();
    }

    @Test
    void installCommandsPinTheNewestPublishedTag() {
        assertThat(service.getLatestImageRef()).isEqualTo("thingsboard/tb-remote-agent:1.1.0");
    }

    @Test
    void installCommandsFallBackToTheFloatingTagWhileNoVersionIsPublished() {
        DefaultAgentUpgradeVersionService empty = new DefaultAgentUpgradeVersionService(attributesService);

        assertThat(empty.getLatestImageRef()).isEqualTo("thingsboard/tb-remote-agent:latest");
    }

    @Test
    void anEmptyGraphNeverReplacesAKnownOne() {
        service.updateVersionGraph(new LinkedHashMap<>());

        assertThat(service.getLatestImageRef()).isEqualTo("thingsboard/tb-remote-agent:1.1.0");
    }

    private void reportedVersion(String imageRef) {
        AttributeKvEntry entry = new BaseAttributeKvEntry(
                new StringDataEntry(DefaultAgentStateService.AGENT_VERSION, imageRef), System.currentTimeMillis());
        when(attributesService.find(eq(TENANT_ID), eq(AGENT_ID), eq(AttributeScope.SERVER_SCOPE),
                eq(DefaultAgentStateService.AGENT_VERSION)))
                .thenReturn(Futures.immediateFuture(Optional.of(entry)));
    }
}

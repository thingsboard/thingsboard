// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.upgrade;

import lombok.RequiredArgsConstructor;
import org.thingsboard.common.util.TbVersionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.AgentUpgradeInfo;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.DefaultAgentStateService;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Answers whether a connected agent is running an image older than the newest published one, from the
 * version graph the update server builds out of the docker registry tags.
 * <p>
 * An agent reports its own image reference as its version, so the tag is what places it in the graph.
 * A deployment pinned to a floating tag such as {@code latest} is deliberately reported as having no
 * upgrade available: what it currently resolves to cannot be told from the tag, so claiming an upgrade
 * would be a guess. Install commands pin a concrete tag precisely so that this stays the rare case.
 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultAgentUpgradeVersionService implements AgentUpgradeVersionService {

    public static final String AGENT_IMAGE_REPO = "thingsboard/tb-remote-agent";
    private static final String FLOATING_TAG = "latest";

    private final AttributesService attributesService;

    private final Map<String, AgentUpgradeInfo> versionGraph = new ConcurrentHashMap<>();

    @Override
    public void updateVersionGraph(Map<String, AgentUpgradeInfo> graph) {
        if (graph == null || graph.isEmpty()) {
            return;
        }
        versionGraph.keySet().retainAll(graph.keySet());
        versionGraph.putAll(graph);
    }

    @Override
    public boolean isUpgradeAvailable(TenantId tenantId, AgentId agentId) throws Exception {
        return getUpgradeImageRef(tenantId, agentId).isPresent();
    }

    @Override
    public Optional<String> getUpgradeImageRef(TenantId tenantId, AgentId agentId) throws Exception {
        return getUpgradeImageRefFor(findReportedImageRef(tenantId, agentId));
    }

    @Override
    public Optional<String> getUpgradeImageRefFor(String reportedImageRef) {
        String currentTag = tagOf(reportedImageRef);
        if (currentTag == null || FLOATING_TAG.equals(currentTag) || !versionGraph.containsKey(currentTag)) {
            return Optional.empty();
        }
        String newestTag = walkToNewest(currentTag);
        return newestTag == null ? Optional.empty() : Optional.of(AGENT_IMAGE_REPO + ":" + newestTag);
    }

    @Override
    public String getLatestImageRef() {
        String newest = null;
        for (String tag : versionGraph.keySet()) {
            if (!FLOATING_TAG.equals(tag) && (newest == null || TbVersionUtils.compare(tag, newest) > 0)) {
                newest = tag;
            }
        }
        // before any version tag is published there is nothing to pin to, so keep the floating tag
        return AGENT_IMAGE_REPO + ":" + (newest == null ? FLOATING_TAG : newest);
    }

    /**
     * Follows the chain from the agent's own tag rather than jumping to the newest tag overall: the
     * chain is what keeps an upgrade inside the agent's own tag family, and it is where a stepwise
     * path would appear if one is ever needed. Returns null when the agent already runs the newest.
     */
    private String walkToNewest(String currentTag) {
        String tag = currentTag;
        for (int steps = 0; steps < versionGraph.size(); steps++) {
            AgentUpgradeInfo info = versionGraph.get(tag);
            if (info == null || info.getNextAgentVersion() == null) {
                break;
            }
            tag = info.getNextAgentVersion();
        }
        return tag.equals(currentTag) ? null : tag;
    }

    private String findReportedImageRef(TenantId tenantId, AgentId agentId) throws Exception {
        Optional<AttributeKvEntry> attribute = attributesService
                .find(tenantId, agentId, AttributeScope.SERVER_SCOPE, DefaultAgentStateService.AGENT_VERSION).get();
        return attribute.map(AttributeKvEntry::getValueAsString).orElse(null);
    }

    /** The tag of an image reference, or null when it carries a digest or no tag at all. */
    static String tagOf(String imageRef) {
        if (StringUtils.isEmpty(imageRef)) {
            return null;
        }
        int lastSlash = imageRef.lastIndexOf('/');
        if (imageRef.indexOf('@', lastSlash + 1) >= 0) {
            return null;
        }
        int lastColon = imageRef.lastIndexOf(':');
        return lastColon > lastSlash ? imageRef.substring(lastColon + 1) : null;
    }
}

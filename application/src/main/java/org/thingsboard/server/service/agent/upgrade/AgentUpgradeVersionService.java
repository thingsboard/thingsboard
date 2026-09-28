// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.upgrade;

import org.thingsboard.server.common.data.agent.AgentUpgradeInfo;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;
import java.util.Optional;

public interface AgentUpgradeVersionService {

    void updateVersionGraph(Map<String, AgentUpgradeInfo> versionGraph);

    boolean isUpgradeAvailable(TenantId tenantId, AgentId agentId) throws Exception;

    /** The image reference the agent should be upgraded to, empty when it is already the newest one. */
    Optional<String> getUpgradeImageRef(TenantId tenantId, AgentId agentId) throws Exception;

    /**
     * The same decision as {@link #getUpgradeImageRef}, for a reported image reference that the caller
     * already holds. Callers rendering a list read the reference off {@code AgentInfo} and so answer for
     * a whole page without a per-agent attribute lookup; both entry points share one rule, so a list and
     * a single-entity view can never disagree.
     */
    Optional<String> getUpgradeImageRefFor(String reportedImageRef);

    /** The newest known image reference, used to pin freshly issued install commands. */
    String getLatestImageRef();
}

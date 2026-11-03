// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.instructions;

import org.thingsboard.server.common.data.EdgeUpgradeInfo;
import org.thingsboard.server.common.data.edge.EdgeInstructions;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;

public interface EdgeUpgradeInstructionsService {

    EdgeInstructions getUpgradeInstructions(String edgeVersion, String upgradeMethod);

    void updateInstructionMap(Map<String, EdgeUpgradeInfo> upgradeVersions);

    void setPlatformEdgeVersion(String version);

    boolean isUpgradeAvailable(TenantId tenantId, EdgeId edgeId) throws Exception;

}

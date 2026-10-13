// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.edge.instructions;

import org.thingsboard.server.common.data.EdgeUpgradeInfo;
import org.thingsboard.server.common.data.edge.EdgeInstructions;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;
import java.util.Map;

public interface EdgeUpgradeInstructionsService {

    EdgeInstructions getUpgradeInstructions(String edgeVersion, String upgradeMethod);

    void updateVersionGraph(Map<String, List<EdgeUpgradeInfo>> versionGraph);

    void setPlatformEdgeVersion(String version);

    boolean isUpgradeAvailable(TenantId tenantId, EdgeId edgeId) throws Exception;

}

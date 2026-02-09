/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.iot_hub;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.thingsboard.server.common.data.id.IotHubInstalledItemId;
import org.thingsboard.server.common.data.iot_hub.DeviceInstalledItemDescriptor;
import org.thingsboard.server.service.security.model.SecurityUser;

public interface IotHubService {

    InstallItemVersionResult installItemVersion(SecurityUser user, String versionId, JsonNode data, HttpServletRequest request);

    UpdateItemVersionResult updateItemVersion(SecurityUser user, IotHubInstalledItemId installedItemId, String versionId, boolean force, HttpServletRequest request);

    InstallItemVersionResult registerDeviceInstall(SecurityUser user, String versionId, DeviceInstalledItemDescriptor descriptor);

    void deleteInstalledItem(SecurityUser user, IotHubInstalledItemId installedItemId);

    InstallPlan resolveInstallPlan(SecurityUser user, String versionId);

    InstallPlanResult installPlan(SecurityUser user, InstallPlan plan, JsonNode data, HttpServletRequest request);
}

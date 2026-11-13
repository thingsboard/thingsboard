// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.iot_hub;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.IotHubInstalledItemId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.iot_hub.DeviceInstalledItemDescriptor;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItem;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;
import java.util.UUID;

public interface IotHubService {

    InstallItemVersionResult installItemVersion(SecurityUser user, String versionId, JsonNode data, HttpServletRequest request);

    UpdateItemVersionResult updateItemVersion(SecurityUser user, IotHubInstalledItemId installedItemId, String versionId, boolean force, HttpServletRequest request);

    InstallItemVersionResult registerDeviceInstall(SecurityUser user, String versionId, DeviceInstalledItemDescriptor descriptor);

    void deleteInstalledItem(SecurityUser user, IotHubInstalledItemId installedItemId);

    InstalledItemKeptEntities findKeptEntities(TenantId tenantId, IotHubInstalledItemId installedItemId);

    /**
     * The tenant's installed items, those with an update in IoT Hub first: installed in a version other than the item's
     * published one. Without an answer from IoT Hub, in the page's sort only.
     */
    PageData<IotHubInstalledItem> findInstalledItemsUpdatesFirst(TenantId tenantId, List<String> itemTypes, UUID itemId, PageLink pageLink);

    InstallPlan resolveInstallPlan(SecurityUser user, String versionId);

    InstallPlanResult installPlan(SecurityUser user, InstallPlan plan, JsonNode data, HttpServletRequest request);

}

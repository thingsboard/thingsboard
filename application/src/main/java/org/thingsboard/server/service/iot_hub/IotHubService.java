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

    /**
     * Records a device package installed by the install wizard. With {@code replaceInstalledItemId} (an update), that
     * installed item takes the new version instead of a new installed item being created.
     */
    InstallItemVersionResult registerDeviceInstall(SecurityUser user, String versionId, DeviceInstalledItemDescriptor descriptor,
                                                   IotHubInstalledItemId replaceInstalledItemId);

    /**
     * First step of a device package update: removes the entities the package created and keeps the installed item at
     * its current version, so it is never shown with a version that is not installed. The item keeps that version even
     * when its entities are gone and the new version is never registered (the wizard was closed): Update installs it
     * again. An entity that could not be removed stays tracked and the call fails, so repeating it removes the rest.
     * Refused, before anything is removed, when {@code versionId} (the version to install) is not a version of that item.
     */
    void removeDevicePackageEntities(SecurityUser user, IotHubInstalledItemId installedItemId, String versionId);

    void deleteInstalledItem(SecurityUser user, IotHubInstalledItemId installedItemId);

    InstallPlan resolveInstallPlan(SecurityUser user, String versionId);

    InstallPlanResult installPlan(SecurityUser user, InstallPlan plan, JsonNode data, HttpServletRequest request);
}

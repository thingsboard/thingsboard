// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.iot_hub;

import org.thingsboard.server.common.data.id.IotHubInstalledItemId;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

/** Installs a device package, or updates an installed one to another version of its item. */
public interface DevicePackageService {

    /** The package's entities as they are now: whether they still exist, changed since, or are shared. */
    List<DevicePackageEntityState> check(SecurityUser user, IotHubInstalledItemId installedItemId);

    /** Installs the version in one transaction, together with its installed item, or nothing. */
    DevicePackageResult install(SecurityUser user, DevicePackageRequest request);

    /** Applies the update in one transaction, together with the new version of the installed item, or nothing. */
    DevicePackageResult update(SecurityUser user, IotHubInstalledItemId installedItemId, DevicePackageRequest request);

}

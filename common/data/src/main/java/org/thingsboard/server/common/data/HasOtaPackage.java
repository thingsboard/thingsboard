// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import org.thingsboard.server.common.data.id.OtaPackageId;

public interface HasOtaPackage {

    OtaPackageId getFirmwareId();

    OtaPackageId getSoftwareId();

    void setFirmwareId(OtaPackageId otaPackageId);

    void setSoftwareId(OtaPackageId otaPackageId);
}

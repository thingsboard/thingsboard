// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.device;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCertificateRevocation;

record DeviceCertificateRevocationEvictEvent(TenantId tenantId, String certificateHash, DeviceCertificateRevocation revocation) {
}

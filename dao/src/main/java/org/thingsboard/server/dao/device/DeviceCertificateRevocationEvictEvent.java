// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.device;

import org.thingsboard.server.common.data.id.TenantId;

record DeviceCertificateRevocationEvictEvent(TenantId tenantId, String certificateHash) {
}

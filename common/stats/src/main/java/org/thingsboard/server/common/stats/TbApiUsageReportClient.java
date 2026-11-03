// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.stats;

import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;

public interface TbApiUsageReportClient {

    void report(TenantId tenantId, CustomerId customerId, ApiUsageRecordKey key, long value);

    void report(TenantId tenantId, CustomerId customerId, ApiUsageRecordKey key);

}

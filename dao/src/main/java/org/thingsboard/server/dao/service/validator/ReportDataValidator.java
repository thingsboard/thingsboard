// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;

@Component
@AllArgsConstructor
public class ReportDataValidator extends DataValidator<Report> {

    private final TbTenantProfileCache tenantProfileCache;

    public void validateReportSize(TenantId tenantId, byte[] data) {
        int dataSize = data.length;
        if (!tenantId.isSysTenantId()) {
            DefaultTenantProfileConfiguration profileConfiguration = tenantProfileCache.get(tenantId).getDefaultProfileConfiguration();
            long maxReportSize = profileConfiguration.getMaxReportSizeInBytes();
            if (maxReportSize > 0 && dataSize > maxReportSize) {
                throw new IllegalArgumentException("Report exceeds the maximum size of " + FileUtils.byteCountToDisplaySize(maxReportSize));
            }
        }
    }
}



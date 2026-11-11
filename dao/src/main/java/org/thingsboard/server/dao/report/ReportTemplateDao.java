// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.report;

import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableCustomerEntityDao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.Map;

public interface ReportTemplateDao extends Dao<ReportTemplate>, TenantEntityDao<ReportTemplate>, ExportableCustomerEntityDao<ReportTemplate, ReportTemplateId> {

    Map<String, Map<String, Long>> countTemplateByFormatAndType();

}

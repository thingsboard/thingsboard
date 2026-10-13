// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.report;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.report.ReportTemplate;

public interface TbReportTemplateService {

    ReportTemplate save(ReportTemplate reportTemplate, User user) throws Exception;

    void delete(ReportTemplate reportTemplate, User user);

}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class ReportTemplateQuery {

    private PageLink pageLink;
    private boolean includeCustomers;
    private List<TbReportFormat> formatList;
    private List<ReportTemplateType> typeList;

}

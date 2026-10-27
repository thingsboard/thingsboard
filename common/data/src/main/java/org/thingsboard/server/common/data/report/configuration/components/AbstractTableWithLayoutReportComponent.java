// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.components;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.report.configuration.TableSortOrder;
import org.thingsboard.server.common.data.report.configuration.style.Heading;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractTableWithLayoutReportComponent extends AbstractDataWithLayoutReportComponent implements TableWithLayoutReportComponent {

    private boolean showTableHeading;

    private Heading tableHeading;

    private TableSortOrder tableSortOrder;

}

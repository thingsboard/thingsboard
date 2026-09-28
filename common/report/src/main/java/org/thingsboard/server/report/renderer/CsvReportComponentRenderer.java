// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer;

import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.components.TableReportComponent;
import org.thingsboard.server.report.context.ComponentData;

import java.util.List;

public interface CsvReportComponentRenderer<C extends TableReportComponent> {

    List<List<String>> render(C component, ComponentData componentData);

    ReportComponentType getType();

}

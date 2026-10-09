// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.components.EntityTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;


@Component
public class EntityTableRenderer extends TableWithLayoutComponentRenderer<EntityTableComponent> {

    @Override
    protected String dataSourceName() {
        return "data source";
    }

    @Override
    protected String noDataMessage() {
        return "No entities found";
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.ENTITY_TABLE;
    }

}

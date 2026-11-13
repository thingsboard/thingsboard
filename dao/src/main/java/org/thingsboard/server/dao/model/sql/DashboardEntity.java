// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.DASHBOARD_TABLE_NAME)
public final class DashboardEntity extends AbstractDashboardEntity<Dashboard> {

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.DASHBOARD_CONFIGURATION_PROPERTY)
    private JsonNode configuration;

    public DashboardEntity() {
        super();
    }

    public DashboardEntity(Dashboard dashboard) {
        super(dashboard);
        this.configuration = dashboard.getConfiguration();
    }

    @Override
    public Dashboard toData() {
        Dashboard dashboard = super.toDashboard();
        dashboard.setConfiguration(configuration);
        return dashboard;
    }
}

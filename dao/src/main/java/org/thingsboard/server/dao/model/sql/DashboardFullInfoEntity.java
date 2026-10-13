// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Immutable;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.EntityInfosConverter;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.List;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
@Entity
@Immutable
@Table(name = ModelConstants.DASHBOARD_INFO_VIEW_TABLE_NAME)
public class DashboardFullInfoEntity extends AbstractDashboardEntity<DashboardInfo> {

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.DASHBOARD_CONFIGURATION_PROPERTY)
    private JsonNode configuration;

    @Column(name = ModelConstants.OWNER_NAME_COLUMN)
    private String ownerName;

    @Convert(converter = EntityInfosConverter.class)
    @Column(name = ModelConstants.GROUPS_COLUMN)
    private List<EntityInfo> groups;

    public DashboardFullInfoEntity() {
        super();
    }

    @Override
    public DashboardInfo toData() {
        DashboardInfo dashboardInfo = new DashboardInfo(super.toDashboard(), this.ownerName, this.groups);
        dashboardInfo.setConfiguration(configuration);
        return dashboardInfo;
    }
}

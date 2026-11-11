// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.model.sql;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
@Entity
@Immutable
@Table(name = ModelConstants.DASHBOARD_INFO_VIEW_TABLE_NAME)
public class DashboardInfoEntity extends AbstractDashboardEntity<DashboardInfo> {

    public static final Map<String, String> dashboardColumnMap = new HashMap<>();

    static {
        dashboardColumnMap.put("name", "title");
    }

    @Column(name = ModelConstants.OWNER_NAME_COLUMN)
    private String ownerName;

    @Convert(converter = EntityInfosConverter.class)
    @Column(name = ModelConstants.GROUPS_COLUMN)
    private List<EntityInfo> groups;

    public DashboardInfoEntity() {
        super();
    }

    @Override
    public DashboardInfo toData() {
        return new DashboardInfo(super.toDashboard(), this.ownerName, this.groups);
    }
}

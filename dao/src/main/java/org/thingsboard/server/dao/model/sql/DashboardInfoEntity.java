/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.model.sql;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.ShortEdgeInfo;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.IOException;
import java.util.HashSet;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.DASHBOARD_COLUMN_FAMILY_NAME)
public class DashboardInfoEntity extends BaseSqlEntity<DashboardInfo> implements SearchTextEntity<DashboardInfo> {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final JavaType assignedCustomersType =
            objectMapper.getTypeFactory().constructCollectionType(HashSet.class, ShortCustomerInfo.class);
    private static final JavaType assignedEdgesType =
            objectMapper.getTypeFactory().constructCollectionType(HashSet.class, ShortEdgeInfo.class);

    @Column(name = ModelConstants.DASHBOARD_TENANT_ID_PROPERTY)
    private String tenantId;

    @Column(name = ModelConstants.DASHBOARD_TITLE_PROPERTY)
    private String title;

    @Column(name = ModelConstants.SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Column(name = ModelConstants.DASHBOARD_ASSIGNED_CUSTOMERS_PROPERTY)
    private String assignedCustomers;

    @Column(name = ModelConstants.DASHBOARD_ASSIGNED_EDGES_PROPERTY)
    private String assignedEdges;

    public DashboardInfoEntity() {
        super();
    }

    public DashboardInfoEntity(DashboardInfo dashboardInfo) {
        if (dashboardInfo.getId() != null) {
            this.setUuid(dashboardInfo.getId().getId());
        }
        if (dashboardInfo.getTenantId() != null) {
            this.tenantId = toString(dashboardInfo.getTenantId().getId());
        }
        this.title = dashboardInfo.getTitle();
        if (dashboardInfo.getAssignedCustomers() != null) {
            try {
                this.assignedCustomers = objectMapper.writeValueAsString(dashboardInfo.getAssignedCustomers());
            } catch (JsonProcessingException e) {
                log.error("Unable to serialize assigned customers to string!", e);
            }
        }
        if (dashboardInfo.getAssignedEdges() != null) {
            try {
                this.assignedEdges = objectMapper.writeValueAsString(dashboardInfo.getAssignedEdges());
            } catch (JsonProcessingException e) {
                log.error("Unable to serialize assigned edges to string!", e);
            }
        }
    }

    @Override
    public String getSearchTextSource() {
        return title;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public String getSearchText() {
        return searchText;
    }

    @Override
    public DashboardInfo toData() {
        DashboardInfo dashboardInfo = new DashboardInfo(new DashboardId(this.getUuid()));
        dashboardInfo.setCreatedTime(Uuids.unixTimestamp(this.getUuid()));
        if (tenantId != null) {
            dashboardInfo.setTenantId(new TenantId(toUUID(tenantId)));
        }
        dashboardInfo.setTitle(title);
        if (!StringUtils.isEmpty(assignedCustomers)) {
            try {
                dashboardInfo.setAssignedCustomers(objectMapper.readValue(assignedCustomers, assignedCustomersType));
            } catch (IOException e) {
                log.warn("Unable to parse assigned customers!", e);
            }
        }
        if (!StringUtils.isEmpty(assignedEdges)) {
            try {
                dashboardInfo.setAssignedEdges(objectMapper.readValue(assignedEdges, assignedEdgesType));
            } catch (IOException e) {
                log.warn("Unable to parse assigned edges!", e);
            }
        }
        return dashboardInfo;
    }

}

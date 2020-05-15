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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.IOException;
import java.util.HashSet;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.DASHBOARD_COLUMN_FAMILY_NAME)
public final class DashboardEntity extends BaseSqlEntity<Dashboard> implements SearchTextEntity<Dashboard> {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final JavaType assignedCustomersType =
            objectMapper.getTypeFactory().constructCollectionType(HashSet.class, ShortCustomerInfo.class);

    @Column(name = ModelConstants.DASHBOARD_TENANT_ID_PROPERTY)
    private String tenantId;

    @Column(name = ModelConstants.DASHBOARD_TITLE_PROPERTY)
    private String title;
    
    @Column(name = ModelConstants.SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Column(name = ModelConstants.DASHBOARD_ASSIGNED_CUSTOMERS_PROPERTY)
    private String assignedCustomers;

    @Type(type = "json")
    @Column(name = ModelConstants.DASHBOARD_CONFIGURATION_PROPERTY)
    private JsonNode configuration;

    public DashboardEntity() {
        super();
    }

    public DashboardEntity(Dashboard dashboard) {
        if (dashboard.getId() != null) {
            this.setUuid(dashboard.getId().getId());
        }
        if (dashboard.getTenantId() != null) {
            this.tenantId = toString(dashboard.getTenantId().getId());
        }
        this.title = dashboard.getTitle();
        if (dashboard.getAssignedCustomers() != null) {
            try {
                this.assignedCustomers = objectMapper.writeValueAsString(dashboard.getAssignedCustomers());
            } catch (JsonProcessingException e) {
                log.error("Unable to serialize assigned customers to string!", e);
            }
        }
        this.configuration = dashboard.getConfiguration();
    }

    @Override
    public String getSearchTextSource() {
        return title;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    @Override
    public Dashboard toData() {
        Dashboard dashboard = new Dashboard(new DashboardId(this.getUuid()));
        dashboard.setCreatedTime(Uuids.unixTimestamp(this.getUuid()));
        if (tenantId != null) {
            dashboard.setTenantId(new TenantId(toUUID(tenantId)));
        }
        dashboard.setTitle(title);
        if (!StringUtils.isEmpty(assignedCustomers)) {
            try {
                dashboard.setAssignedCustomers(objectMapper.readValue(assignedCustomers, assignedCustomersType));
            } catch (IOException e) {
                log.warn("Unable to parse assigned customers!", e);
            }
        }
        dashboard.setConfiguration(configuration);
        return dashboard;
    }
}

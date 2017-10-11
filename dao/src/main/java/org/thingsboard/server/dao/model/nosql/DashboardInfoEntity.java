/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.dao.model.nosql;

import com.datastax.driver.core.utils.UUIDs;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.SearchTextEntity;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.*;

@Table(name = DASHBOARD_COLUMN_FAMILY_NAME)
@EqualsAndHashCode
@ToString
public class DashboardInfoEntity implements SearchTextEntity<DashboardInfo> {

    @PartitionKey(value = 0)
    @Column(name = ID_PROPERTY)
    private UUID id;

    @PartitionKey(value = 1)
    @Column(name = DASHBOARD_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @PartitionKey(value = 2)
    @Column(name = DASHBOARD_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @Column(name = DASHBOARD_TITLE_PROPERTY)
    private String title;

    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;

    public DashboardInfoEntity() {
        super();
    }

    public DashboardInfoEntity(DashboardInfo dashboardInfo) {
        if (dashboardInfo.getId() != null) {
            this.id = dashboardInfo.getId().getId();
        }
        if (dashboardInfo.getTenantId() != null) {
            this.tenantId = dashboardInfo.getTenantId().getId();
        }
        if (dashboardInfo.getCustomerId() != null) {
            this.customerId = dashboardInfo.getCustomerId().getId();
        }
        this.title = dashboardInfo.getTitle();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getSearchTextSource() {
        return getTitle();
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
        DashboardInfo dashboardInfo = new DashboardInfo(new DashboardId(id));
        dashboardInfo.setCreatedTime(UUIDs.unixTimestamp(id));
        if (tenantId != null) {
            dashboardInfo.setTenantId(new TenantId(tenantId));
        }
        if (customerId != null) {
            dashboardInfo.setCustomerId(new CustomerId(customerId));
        }
        dashboardInfo.setTitle(title);
        return dashboardInfo;
    }

}
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
package org.thingsboard.server.dao.model.sql;

import com.datastax.driver.core.utils.UUIDs;
import lombok.Data;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;

import javax.persistence.*;
import java.util.UUID;

@Data
@Entity
@Table(name = ModelConstants.DASHBOARD_COLUMN_FAMILY_NAME)
public class DashboardInfoEntity implements SearchTextEntity<DashboardInfo> {

    @Transient
    private static final long serialVersionUID = -5525675905528050250L;

    @Id
    @Column(name = ModelConstants.ID_PROPERTY)
    private UUID id;

    @Column(name = ModelConstants.DASHBOARD_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.DASHBOARD_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @Column(name = ModelConstants.DASHBOARD_TITLE_PROPERTY)
    private String title;

    @Column(name = ModelConstants.SEARCH_TEXT_PROPERTY)
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
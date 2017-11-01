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
import org.thingsboard.server.common.data.Application;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.dao.model.SearchTextEntity;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.datastax.driver.mapping.annotations.Transient;


import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.model.ModelConstants.*;

@Table(name = APPLICATION_TABLE_NAME)
public final class ApplicationEntity implements SearchTextEntity<Application> {

    @Transient
    private static final long serialVersionUID = -5855480905292626926L;

    @PartitionKey(value = 0)
    @Column(name = ID_PROPERTY)
    private UUID id;

    @PartitionKey(value = 1)
    @Column(name = APPLICATION_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @PartitionKey(value = 2)
    @Column(name = APPLICATION_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @Column(name = APPLICATION_MINI_DASHBOARD_ID_PROPERTY)
    private UUID miniDashboardId;

    @Column(name = APPLICATION_DASHBOARD_ID_PROPERTY)
    private UUID dashboardId;

    @Column(name = APPLICATION_NAME)
    private String name;

    @Column(name = APPLICATION_DESCRIPTION)
    private String description;

    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Column(name = APPLICATION_RULES_COLUMN)
    private List<UUID> rules;

    @Column(name = APPLICATION_DEVICE_TYPES_COLUMN)
    private List<String> deviceTypes;


    public ApplicationEntity() {
        super();
    }

    public ApplicationEntity(Application application){
        if (application.getId() != null) {
            this.setId(application.getId().getId());
        }
        if (application.getTenantId() != null) {
            this.tenantId = application.getTenantId().getId();
        }
        if (application.getCustomerId() != null) {
            this.customerId = application.getCustomerId().getId();
        }

        if(application.getDashboardId() !=null) {
            this.dashboardId = application.getDashboardId().getId();
        }

        if(application.getMiniDashboardId() !=null) {
            this.miniDashboardId = application.getMiniDashboardId().getId();
        }

        if(application.getRules() !=null && application.getRules().size() !=0) {
            this.rules = application.getRules().stream().map(r -> (r.getId())).collect(Collectors.toList());
        }

        this.name = application.getName();
        this.description = application.getDescription();
        this.deviceTypes = application.getDeviceTypes();
    }


    @Override
    public String getSearchTextSource() {
        return name;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
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

    public UUID getMiniDashboardId() {
        return miniDashboardId;
    }

    public void setMiniDashboardId(UUID miniDashboardId) {
        this.miniDashboardId = miniDashboardId;
    }

    public UUID getDashboardId() {
        return dashboardId;
    }

    public void setDashboardId(UUID dashboardId) {
        this.dashboardId = dashboardId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSearchText() {
        return searchText;
    }

    public List<UUID> getRules() {
        return rules;
    }

    public void setRules(List<UUID> rules) {
        this.rules = rules;
    }

    public List<String> getDeviceTypes() {
        return deviceTypes;
    }

    public void setDeviceTypes(List<String> deviceTypes) {
        this.deviceTypes = deviceTypes;
    }

    @Override
    public Application toData() {
        Application application = new Application(new ApplicationId(getId()));
        application.setCreatedTime(UUIDs.unixTimestamp(getId()));
        if (tenantId != null) {
            application.setTenantId(new TenantId(tenantId));
        }
        if (customerId != null) {
            application.setCustomerId(new CustomerId(customerId));
        }

        if(dashboardId !=null) {
            application.setDashboardId(new DashboardId(dashboardId));
        }

        if(miniDashboardId !=null) {
            application.setMiniDashboardId(new DashboardId(miniDashboardId));
        }

        if(rules !=null && rules.size() !=0) {
            application.setRules(rules.stream().map(RuleId::new).collect(Collectors.toList()));
        }
        application.setName(name);
        application.setDescription(description);
        if(deviceTypes !=null) {
            application.setDeviceTypes(deviceTypes);
        }
        return application;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ApplicationEntity that = (ApplicationEntity) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (tenantId != null ? !tenantId.equals(that.tenantId) : that.tenantId != null) return false;
        if (customerId != null ? !customerId.equals(that.customerId) : that.customerId != null) return false;
        if (miniDashboardId != null ? !miniDashboardId.equals(that.miniDashboardId) : that.miniDashboardId != null)
            return false;
        if (dashboardId != null ? !dashboardId.equals(that.dashboardId) : that.dashboardId != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (searchText != null ? !searchText.equals(that.searchText) : that.searchText != null) return false;
        if (rules != null ? !rules.equals(that.rules) : that.rules != null) return false;
        return deviceTypes != null ? deviceTypes.equals(that.deviceTypes) : that.deviceTypes == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (tenantId != null ? tenantId.hashCode() : 0);
        result = 31 * result + (customerId != null ? customerId.hashCode() : 0);
        result = 31 * result + (miniDashboardId != null ? miniDashboardId.hashCode() : 0);
        result = 31 * result + (dashboardId != null ? dashboardId.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (searchText != null ? searchText.hashCode() : 0);
        result = 31 * result + (rules != null ? rules.hashCode() : 0);
        result = 31 * result + (deviceTypes != null ? deviceTypes.hashCode() : 0);
        return result;
    }
}

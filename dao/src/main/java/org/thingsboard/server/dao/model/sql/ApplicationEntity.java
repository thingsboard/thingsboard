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
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.Application;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.*;
import java.util.List;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.APPLICATION_TABLE_NAME)
public final class ApplicationEntity extends BaseSqlEntity<Application> implements SearchTextEntity<Application> {

    @Transient
    private static final long serialVersionUID = -3873737406462009031L;

    @Column(name = ModelConstants.APPLICATION_TENANT_ID_PROPERTY)
    private String tenantId;

    @Column(name = ModelConstants.APPLICATION_CUSTOMER_ID_PROPERTY)
    private String customerId;

    @Column(name = ModelConstants.APPLICATION_MINI_DASHBOARD_ID_PROPERTY)
    private String miniDashboardId;

    @Column(name = ModelConstants.APPLICATION_DASHBOARD_ID_PROPERTY)
    private String dashboardId;

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    @CollectionTable(name = ModelConstants.APPLICATION_RULES_ASSOCIATION_TABLE, joinColumns = @JoinColumn(name = ModelConstants.APPLICATION_ID_COLUMN))
    @Column(name = ModelConstants.APPLICATION_RULE_ID_COLUMN)
    private List<String> rules;

    @Column(name = ModelConstants.APPLICATION_NAME)
    private String name;

    @Column(name = ModelConstants.APPLICATION_IS_VALID)
    private Boolean isValid;

    @Column(name = ModelConstants.APPLICATION_DESCRIPTION)
    private String description;

    @Column(name = ModelConstants.SEARCH_TEXT_PROPERTY)
    private String searchText;

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    @CollectionTable(name = ModelConstants.APPLICATION_DEVICE_TYPES_TABLE, joinColumns = @JoinColumn(name = ModelConstants.APPLICATION_ID_COLUMN))
    @Column(name = ModelConstants.APPLICATION_DEVICE_TYPES)
    private List<String> deviceTypes;


    public ApplicationEntity() {
        super();
    }

    public ApplicationEntity(Application application){
        if (application.getId() != null) {
            this.setId(application.getId().getId());
        }
        if (application.getTenantId() != null) {
            this.tenantId = toString(application.getTenantId().getId());
        }
        if (application.getCustomerId() != null) {
            this.customerId = toString(application.getCustomerId().getId());
        }

        if(application.getDashboardId() !=null) {
            this.dashboardId = toString(application.getDashboardId().getId());
        }

        if(application.getMiniDashboardId() !=null) {
            this.miniDashboardId = toString(application.getMiniDashboardId().getId());
        }

        if(application.getRules() !=null && application.getRules().size() !=0) {
            this.rules = application.getRules().stream().map(r -> toString(r.getId())).collect(Collectors.toList());
        }

        this.name = application.getName();
        this.isValid = application.getIsValid();
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

    @Override
    public Application toData() {
        Application application = new Application(new ApplicationId(getId()));
        application.setCreatedTime(UUIDs.unixTimestamp(getId()));
        if (tenantId != null) {
            application.setTenantId(new TenantId(toUUID(tenantId)));
        }
        if (customerId != null) {
            application.setCustomerId(new CustomerId(toUUID(customerId)));
        }

        if(dashboardId !=null) {
           application.setDashboardId(new DashboardId(toUUID(dashboardId)));
        }

        if(miniDashboardId !=null) {
            application.setMiniDashboardId(new DashboardId(toUUID(miniDashboardId)));
        }

        if(rules !=null && rules.size() !=0) {
            application.setRules(rules.stream().map(r -> new RuleId(toUUID(r))).collect(Collectors.toList()));
        }
        application.setName(name);
        application.setIsValid(isValid);
        application.setDescription(description);
        application.setDeviceTypes(deviceTypes);
        return application;

    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ApplicationEntity that = (ApplicationEntity) o;

        if (tenantId != null ? !tenantId.equals(that.tenantId) : that.tenantId != null) return false;
        if (customerId != null ? !customerId.equals(that.customerId) : that.customerId != null) return false;
        if (miniDashboardId != null ? !miniDashboardId.equals(that.miniDashboardId) : that.miniDashboardId != null)
            return false;
        if (dashboardId != null ? !dashboardId.equals(that.dashboardId) : that.dashboardId != null) return false;
        if (rules != null ? !rules.equals(that.rules) : that.rules != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (isValid != null ? !isValid.equals(that.isValid) : that.isValid != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (searchText != null ? !searchText.equals(that.searchText) : that.searchText != null) return false;
        return deviceTypes != null ? deviceTypes.equals(that.deviceTypes) : that.deviceTypes == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (tenantId != null ? tenantId.hashCode() : 0);
        result = 31 * result + (customerId != null ? customerId.hashCode() : 0);
        result = 31 * result + (miniDashboardId != null ? miniDashboardId.hashCode() : 0);
        result = 31 * result + (dashboardId != null ? dashboardId.hashCode() : 0);
        result = 31 * result + (rules != null ? rules.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (isValid != null ? isValid.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (searchText != null ? searchText.hashCode() : 0);
        result = 31 * result + (deviceTypes != null ? deviceTypes.hashCode() : 0);
        return result;
    }
}

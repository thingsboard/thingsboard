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
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.Application;
import org.thingsboard.server.common.data.id.ApplicationId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.*;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.APPLICATION_TABLE_NAME)
public class ApplicationEntity extends BaseSqlEntity<Application> implements SearchTextEntity<Application> {

    @Transient
    private static final long serialVersionUID = -3873737406462009031L;

    @Column(name = ModelConstants.APPLICATION_TENANT_ID_PROPERTY)
    private String tenantId;

    @Column(name = ModelConstants.APPLICATION_CUSTOMER_ID_PROPERTY)
    private String customerId;

    @Type(type = "json")
    @Column(name = ModelConstants.APPLICATION_MINI_WIDGET)
    private JsonNode miniWidget;

    @Type(type = "json")
    @Column(name = ModelConstants.APPLICATION_DASHBOARD)
    private JsonNode dashboard;

    @Type(type = "json")
    @Column(name = ModelConstants.APPLICATION_RULES)
    private JsonNode rules;

    @Column(name = ModelConstants.APPLICATION_NAME)
    private String name;

    @Column(name = ModelConstants.APPLICATION_DESCRIPTION)
    private String description;

    @Column(name = ModelConstants.SEARCH_TEXT_PROPERTY)
    private String searchText;

    @ElementCollection
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

        this.miniWidget = application.getMiniWidget();
        this.dashboard = application.getDashboard();
        this.rules = application.getRules();
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
        application.setMiniWidget(miniWidget);
        application.setDashboard(dashboard);
        application.setRules(rules);
        application.setName(name);
        application.setDescription(description);
        application.setDeviceTypes(deviceTypes);
        return application;

    }
}

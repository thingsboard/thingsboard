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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.id.ApplicationId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;

public class Application extends SearchTextBased<ApplicationId> implements HasName {

    private static final long serialVersionUID = 1533755382827939542L;

    private TenantId tenantId;
    private CustomerId customerId;
    private JsonNode miniWidget;
    private JsonNode dashboard;
    private JsonNode rules;
    private String name;
    private String description;
    private List<String> deviceTypes;

    public Application() {
        super();
    }

    public Application(ApplicationId id) {
        super(id);
    }

    public Application(Application application) {
        super(application);
        this.tenantId = application.tenantId;
        this.customerId = application.customerId;
        this.miniWidget = application.miniWidget;
        this.dashboard = application.dashboard;
        this.rules = application.rules;
        this.name = application.name;
        this.description = application.description;
        this.deviceTypes = application.deviceTypes;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getSearchText() {
        return name;
    }

    public JsonNode getMiniWidget() {
        return miniWidget;
    }

    public void setMiniWidget(JsonNode miniWidget) {
        this.miniWidget = miniWidget;
    }

    public JsonNode getDashboard() {
        return dashboard;
    }

    public void setDashboard(JsonNode dashboard) {
        this.dashboard = dashboard;
    }

    public JsonNode getRules() {
        return rules;
    }

    public void setRules(JsonNode rules) {
        this.rules = rules;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getDeviceTypes() {
        return deviceTypes;
    }

    public void setDeviceTypes(List<String> deviceTypes) {
        this.deviceTypes = deviceTypes;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public void setCustomerId(CustomerId customerId) {
        this.customerId = customerId;
    }

    @Override
    public String toString() {
        return "Application{" +
                "tenantId=" + tenantId +
                ", customerId=" + customerId +
                ", miniWidget=" + miniWidget +
                ", dashboard=" + dashboard +
                ", rules=" + rules +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", deviceTypes=" + deviceTypes +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Application that = (Application) o;

        if (tenantId != null ? !tenantId.equals(that.tenantId) : that.tenantId != null) return false;
        if (customerId != null ? !customerId.equals(that.customerId) : that.customerId != null) return false;
        if (miniWidget != null ? !miniWidget.equals(that.miniWidget) : that.miniWidget != null) return false;
        if (dashboard != null ? !dashboard.equals(that.dashboard) : that.dashboard != null) return false;
        if (rules != null ? !rules.equals(that.rules) : that.rules != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        return deviceTypes != null ? deviceTypes.equals(that.deviceTypes) : that.deviceTypes == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (tenantId != null ? tenantId.hashCode() : 0);
        result = 31 * result + (customerId != null ? customerId.hashCode() : 0);
        result = 31 * result + (miniWidget != null ? miniWidget.hashCode() : 0);
        result = 31 * result + (dashboard != null ? dashboard.hashCode() : 0);
        result = 31 * result + (rules != null ? rules.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (deviceTypes != null ? deviceTypes.hashCode() : 0);
        return result;
    }
}

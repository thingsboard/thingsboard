/**
 * Copyright © 2016-2018 The Thingsboard Authors
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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.HashSet;
import java.util.Set;


public class DashboardInfo extends SearchTextBased<DashboardId> implements HasName, HasMultipleCustomers {


    private TenantId tenantId;
    private String title;
    @Getter @Setter
    private Set<ShortCustomerInfo> assignedCustomers = new HashSet<>();
    public DashboardInfo() {
        super();
    }

    public DashboardInfo(DashboardId id) {
        super(id);
    }

    public DashboardInfo(DashboardInfo dashboardInfo) {
        super(dashboardInfo);
        this.tenantId = dashboardInfo.getTenantId();
        this.title = dashboardInfo.getTitle();
        setAssignedCustomers(dashboardInfo.getAssignedCustomers());
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getName() {
        return title;
    }

    @Override
    public String getSearchText() {
        return getTitle();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((tenantId == null) ? 0 : tenantId.hashCode());
        result = prime * result + ((title == null) ? 0 : title.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        DashboardInfo other = (DashboardInfo) obj;
        if (tenantId == null) {
            if (other.tenantId != null)
                return false;
        } else if (!tenantId.equals(other.tenantId))
            return false;
        if (title == null) {
            if (other.title != null)
                return false;
        } else if (!title.equals(other.title))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DashboardInfo [tenantId=");
        builder.append(tenantId);
        builder.append(", title=");
        builder.append(title);
        builder.append("]");
        return builder.toString();
    }

}

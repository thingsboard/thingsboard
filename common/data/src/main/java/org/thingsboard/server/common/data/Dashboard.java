// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.thingsboard.server.common.data.util.DataUtils.getChildObjects;

@Schema
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"id", "createdTime", "tenantId", "customerId", "ownerId", "title", "name", "image", "assignedCustomers", "mobileHide", "mobileOrder", "configuration", "resources", "externalId", "version"})
public class Dashboard extends BaseData<DashboardId> implements GroupEntity<DashboardId>, HasName, HasTenantId, HasCustomerId, HasTitle, ExportableEntity<DashboardId>, HasImage, HasVersion {

    private static final long serialVersionUID = 872682138346187503L;

    private TenantId tenantId;

    private CustomerId customerId;

    @NoXss
    @Length(fieldName = "title")
    private String title;
    private String image;
    @Valid
    private Set<ShortCustomerInfo> assignedCustomers;
    private boolean mobileHide;
    private Integer mobileOrder;

    @Schema(description = "JSON object with main configuration of the dashboard: layouts, widgets, aliases, etc. " +
            "The JSON structure of the dashboard configuration is quite complex. " +
            "The easiest way to learn it is to export existing dashboard to JSON.")
    private JsonNode configuration;

    @Getter
    @Setter
    private DashboardId externalId;
    @Getter
    @Setter
    private Long version;

    @Getter
    @Setter
    private List<ResourceExportData> resources;

    public Dashboard() {
        super();
    }

    public Dashboard(DashboardId id) {
        super(id);
    }

    public Dashboard(DashboardInfo dashboardInfo) {
        super(dashboardInfo);
    }

    public Dashboard(Dashboard dashboard) {
        super(dashboard);
        this.tenantId = dashboard.getTenantId();
        this.customerId = dashboard.getCustomerId();
        this.title = dashboard.getTitle();
        this.image = dashboard.getImage();
        this.assignedCustomers = dashboard.getAssignedCustomers();
        this.mobileHide = dashboard.isMobileHide();
        this.mobileOrder = dashboard.getMobileOrder();
        this.configuration = dashboard.getConfiguration();
        this.externalId = dashboard.getExternalId();
        this.version = dashboard.getVersion();
        this.resources = dashboard.getResources() != null ? new ArrayList<>(dashboard.getResources()) : null;
    }

    @Schema(description = "JSON object with the dashboard Id. " +
                          "Specify existing dashboard Id to update the dashboard. " +
                          "Referencing non-existing dashboard id will cause error. " +
                          "Omit this field to create new dashboard.")
    @Override
    public DashboardId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the dashboard creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(description = "JSON object with Tenant Id. Tenant Id of the dashboard can't be changed.", accessMode = Schema.AccessMode.READ_ONLY)
    public TenantId getTenantId() {
        return tenantId;
    }

    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    @Schema(description = "JSON object with Customer Id. ")
    public CustomerId getCustomerId() {
        return customerId;
    }

    public void setCustomerId(CustomerId customerId) {
        this.customerId = customerId;
    }

    @Schema(description = "JSON object with Customer or Tenant Id", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public EntityId getOwnerId() {
        return customerId != null && !customerId.isNullUid() ? customerId : tenantId;
    }

    @Override
    public void setOwnerId(EntityId entityId) {
        if (EntityType.CUSTOMER.equals(entityId.getEntityType())) {
            this.customerId = new CustomerId(entityId.getId());
        } else {
            this.customerId = new CustomerId(CustomerId.NULL_UUID);
        }
    }

    @Schema(description = "Title of the dashboard.")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Schema(description = "Thumbnail picture for rendering of the dashboards in a grid view on mobile devices.", accessMode = Schema.AccessMode.READ_ONLY)
    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    @Schema(description = "List of assigned customers with their info.", accessMode = Schema.AccessMode.READ_ONLY)
    public Set<ShortCustomerInfo> getAssignedCustomers() {
        return assignedCustomers;
    }

    public void setAssignedCustomers(Set<ShortCustomerInfo> assignedCustomers) {
        this.assignedCustomers = assignedCustomers;
    }

    @Schema(description = "Hide dashboard from mobile devices. Useful if the dashboard is not designed for small screens.", accessMode = Schema.AccessMode.READ_ONLY)
    public boolean isMobileHide() {
        return mobileHide;
    }

    public void setMobileHide(boolean mobileHide) {
        this.mobileHide = mobileHide;
    }

    @Schema(description = "Order on mobile devices. Useful to adjust sorting of the dashboards for mobile applications", accessMode = Schema.AccessMode.READ_ONLY)
    public Integer getMobileOrder() {
        return mobileOrder;
    }

    public void setMobileOrder(Integer mobileOrder) {
        this.mobileOrder = mobileOrder;
    }


    @Schema(description = "Same as title of the dashboard. Read-only field. Update the 'title' to change the 'name' of the dashboard.", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getName() {
        return title;
    }

    public JsonNode getConfiguration() {
        return configuration;
    }

    public void setConfiguration(JsonNode configuration) {
        this.configuration = configuration;
    }

    public boolean isAssignedToCustomer(CustomerId customerId) {
        return this.assignedCustomers != null && this.assignedCustomers.contains(new ShortCustomerInfo(customerId, null, false));
    }

    public ShortCustomerInfo getAssignedCustomerInfo(CustomerId customerId) {
        if (this.assignedCustomers != null) {
            for (ShortCustomerInfo customerInfo : this.assignedCustomers) {
                if (customerInfo.getCustomerId().equals(customerId)) {
                    return customerInfo;
                }
            }
        }
        return null;
    }

    public boolean addAssignedCustomer(Customer customer) {
        ShortCustomerInfo customerInfo = customer.toShortCustomerInfo();
        if (this.assignedCustomers != null && this.assignedCustomers.contains(customerInfo)) {
            return false;
        } else {
            if (this.assignedCustomers == null) {
                this.assignedCustomers = new HashSet<>();
            }
            this.assignedCustomers.add(customerInfo);
            return true;
        }
    }

    public boolean updateAssignedCustomer(Customer customer) {
        ShortCustomerInfo customerInfo = customer.toShortCustomerInfo();
        if (this.assignedCustomers != null && this.assignedCustomers.contains(customerInfo)) {
            this.assignedCustomers.remove(customerInfo);
            this.assignedCustomers.add(customerInfo);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeAssignedCustomer(Customer customer) {
        ShortCustomerInfo customerInfo = customer.toShortCustomerInfo();
        if (this.assignedCustomers != null && this.assignedCustomers.contains(customerInfo)) {
            this.assignedCustomers.remove(customerInfo);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeAssignedCustomerInfo(ShortCustomerInfo customerInfo) {
        if (this.assignedCustomers != null && this.assignedCustomers.contains(customerInfo)) {
            this.assignedCustomers.remove(customerInfo);
            return true;
        } else {
            return false;
        }
    }

    @Override
    @JsonIgnore
    public EntityType getEntityType() {
        return EntityType.DASHBOARD;
    }

    @JsonIgnore
    public List<ObjectNode> getEntityAliasesConfig() {
        return getChildObjects("entityAliases", configuration);
    }

    @JsonIgnore
    public List<ObjectNode> getWidgetsConfig() {
        return getChildObjects("widgets", configuration);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Dashboard [tenantId=");
        builder.append(tenantId);
        builder.append(", customerId=");
        builder.append(customerId);
        builder.append(", title=");
        builder.append(title);
        builder.append("]");
        return builder.toString();
    }

}

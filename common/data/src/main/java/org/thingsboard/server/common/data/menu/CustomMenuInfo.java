// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.menu;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.Set;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomMenuInfo extends BaseData<CustomMenuId> implements HasTenantId {

    @Schema(description = "JSON object with Tenant Id that owns the menu.", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;

    @Schema(description = "JSON object with Customer Id that owns the menu.", accessMode = Schema.AccessMode.READ_ONLY)
    private CustomerId customerId;

    @NoXss
    @Length(fieldName = "name")
    @Schema(description = "Custom menu name", example = "Customer A custom menu", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull
    @Schema(description = "Custom menu scope. Possible values: SYSTEM, TENANT, CUSTOMER", example = "TENANT", requiredMode = Schema.RequiredMode.REQUIRED)
    private CMScope scope;

    @NotNull
    @Schema(description = "Custom menu assignee type. Possible values are: All (all users of specified scope), " +
            "CUSTOMERS (specified customers), USERS (specified list of users), NO_ASSIGN (no assignees), USER_GROUPS (user groups)", example = "ALL",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private CMAssigneeType assigneeType;

    @Schema(description = "User group names menu is applied to", example = "[Customer Administrators, Customer Users]]")
    private String[] userGroupNames;

    public CustomMenuInfo() {
        super();
    }

    public CustomMenuInfo(CustomMenuId id) {
        super(id);
    }

    public CustomMenuInfo(CustomMenuInfo customMenuInfo) {
        super(customMenuInfo);
        this.tenantId = customMenuInfo.getTenantId();
        this.customerId = customMenuInfo.getCustomerId();
        this.name = customMenuInfo.getName();
        this.scope = customMenuInfo.getScope();
        this.assigneeType = customMenuInfo.getAssigneeType();
        this.userGroupNames = customMenuInfo.getUserGroupNames();
    }

}

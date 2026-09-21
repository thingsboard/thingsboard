// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.targets.platform;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @Type(value = UserListFilter.class, name = "USER_LIST"),
        @Type(value = CustomerUsersFilter.class, name = "CUSTOMER_USERS"),
        @Type(value = TenantAdministratorsFilter.class, name = "TENANT_ADMINISTRATORS"),
        @Type(value = AffectedTenantAdministratorsFilter.class, name = "AFFECTED_TENANT_ADMINISTRATORS"),
        @Type(value = SystemAdministratorsFilter.class, name = "SYSTEM_ADMINISTRATORS"),
        @Type(value = AllUsersFilter.class, name = "ALL_USERS"),
        @Type(value = OriginatorEntityOwnerUsersFilter.class, name = "ORIGINATOR_ENTITY_OWNER_USERS"),
        @Type(value = AffectedUserFilter.class, name = "AFFECTED_USER")
})
public interface UsersFilter {

    @JsonIgnore
    UsersFilterType getType();

}

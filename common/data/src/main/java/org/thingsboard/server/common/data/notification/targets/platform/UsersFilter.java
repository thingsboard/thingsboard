// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.notification.targets.platform;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "USER_LIST", schema = UserListFilter.class),
                @DiscriminatorMapping(value = "CUSTOMER_USERS", schema = CustomerUsersFilter.class),
                @DiscriminatorMapping(value = "TENANT_ADMINISTRATORS", schema = TenantAdministratorsFilter.class),
                @DiscriminatorMapping(value = "AFFECTED_TENANT_ADMINISTRATORS", schema = AffectedTenantAdministratorsFilter.class),
                @DiscriminatorMapping(value = "SYSTEM_ADMINISTRATORS", schema = SystemAdministratorsFilter.class),
                @DiscriminatorMapping(value = "ALL_USERS", schema = AllUsersFilter.class),
                @DiscriminatorMapping(value = "ORIGINATOR_ENTITY_OWNER_USERS", schema = OriginatorEntityOwnerUsersFilter.class),
                @DiscriminatorMapping(value = "AFFECTED_USER", schema = AffectedUserFilter.class),
                @DiscriminatorMapping(value = "USER_GROUP_LIST", schema = UserGroupListFilter.class),
                @DiscriminatorMapping(value = "USER_ROLE", schema = UserRoleFilter.class)
        }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @Type(value = UserListFilter.class, name = "USER_LIST"),
        @Type(value = UserGroupListFilter.class, name = "USER_GROUP_LIST"),
        @Type(value = CustomerUsersFilter.class, name = "CUSTOMER_USERS"),
        @Type(value = UserRoleFilter.class, name = "USER_ROLE"),
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

/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

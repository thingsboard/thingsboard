// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.security.Authority;

import java.util.Map;
import java.util.Set;

@Data
@Schema
public class AuthorityPermissionsInfo {

    @Schema(description = "Map of permissions per authority level. Each authority (SYS_ADMIN, TENANT_ADMIN, CUSTOMER_USER) " +
            "can have different sets of resource permissions. " +
            "Format: {\"AUTHORITY\": {\"RESOURCE\": [\"OPERATION1\", \"OPERATION2\"]}}. " +
            "Example: {\"TENANT_ADMIN\": {\"DEVICE\": [\"READ\", \"WRITE\"], \"DASHBOARD\": [\"READ\"]}, " +
            "\"CUSTOMER_USER\": {\"DEVICE\": [\"READ\"]}}",
            example = "{\"TENANT_ADMIN\": {\"DEVICE\": [\"READ\", \"WRITE\"], \"ALARM\": [\"READ\", \"CREATE\"]}, " +
                    "\"CUSTOMER_USER\": {\"DEVICE\": [\"READ\"], \"ALARM\": [\"READ\"]}}")
    private Map<Authority, Map<Resource, Set<Operation>>> operationsByResource;

    public Map<Resource, Set<Operation>> getPermissionsForAuthority(Authority authority) {
        if (operationsByResource == null || authority == null) {
            return null;
        }
        return operationsByResource.get(authority);
    }

}

// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.permission;

import lombok.Getter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum Operation {

    ALL(true),
    CREATE(true, true, true),
    READ(true),
    WRITE(true, false, true),
    DELETE(true, true, true),
    RPC_CALL(true),
    READ_CREDENTIALS(true),
    WRITE_CREDENTIALS(true),
    READ_ATTRIBUTES(true),
    WRITE_ATTRIBUTES(true, false, true),
    READ_TELEMETRY(true),
    WRITE_TELEMETRY(true, false, true),
    ADD_TO_GROUP,
    REMOVE_FROM_GROUP,
    CHANGE_OWNER,
    IMPERSONATE,
    CLAIM_DEVICES,
    SHARE_GROUP(true),
    ASSIGN_TO_TENANT,
    READ_CALCULATED_FIELD(true),
    WRITE_CALCULATED_FIELD(true);

    public static Set<Operation> defaultEntityOperations = new HashSet<>(Arrays.asList(ALL, READ, WRITE,
            CREATE, DELETE, READ_ATTRIBUTES, WRITE_ATTRIBUTES, READ_TELEMETRY, WRITE_TELEMETRY, CHANGE_OWNER));

    public static Set<Operation> defaultEntityGroupOperations = new HashSet<>(Arrays.asList(ALL, READ, WRITE,
            CREATE, DELETE, READ_ATTRIBUTES, WRITE_ATTRIBUTES, READ_TELEMETRY, WRITE_TELEMETRY, ADD_TO_GROUP, REMOVE_FROM_GROUP, SHARE_GROUP));

    public static Set<Operation> crudOperations = new HashSet<>(Arrays.asList(ALL, READ, WRITE,
            CREATE, DELETE));

    public static Set<Operation> allowedForGroupRoleOperations = new HashSet<>();

    static {
        for (Operation operation : Operation.values()) {
            if (operation.isAllowedForGroupRole()) {
                allowedForGroupRoleOperations.add(operation);
            }
        }
    }

    public static Set<Operation> allowedForGroupOwnerOnlyOperations = new HashSet<>();

    static {
        for (Operation operation : Operation.values()) {
            if (operation.isAllowedForGroupOwnerOnly()) {
                allowedForGroupOwnerOnlyOperations.add(operation);
            }
        }
    }

    public static Set<Operation> allowedForGroupOwnerOnlyGroupOperations = new HashSet<>();

    static {
        for (Operation operation : Operation.values()) {
            if (operation.isGroupOperationAllowedForGroupOwnerOnly()) {
                allowedForGroupOwnerOnlyGroupOperations.add(operation);
            }
        }
    }

    public static Set<Operation> defaultCFEntityOperations = new HashSet<>(defaultEntityOperations);

    static {
        defaultCFEntityOperations.add(READ_CALCULATED_FIELD);
        defaultCFEntityOperations.add(WRITE_CALCULATED_FIELD);
    }

    @Getter
    private boolean allowedForGroupRole;

    @Getter
    private boolean allowedForGroupOwnerOnly;

    @Getter
    private boolean groupOperationAllowedForGroupOwnerOnly;

    Operation() {
        this(false, false, false);
    }

    Operation(boolean allowedForGroupRole) {
        this(allowedForGroupRole, false, false);
    }

    Operation(boolean allowedForGroupRole, boolean allowedForGroupOwnerOnly) {
        this(allowedForGroupRole, allowedForGroupOwnerOnly, false);
    }

    Operation(boolean allowedForGroupRole, boolean allowedForGroupOwnerOnly, boolean groupOperationAllowedForGroupOwnerOnly) {
        this.allowedForGroupRole = allowedForGroupRole;
        this.allowedForGroupOwnerOnly = allowedForGroupOwnerOnly;
        this.groupOperationAllowedForGroupOwnerOnly = groupOperationAllowedForGroupOwnerOnly;
    }
}

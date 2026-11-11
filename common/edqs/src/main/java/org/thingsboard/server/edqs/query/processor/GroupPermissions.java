// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.edqs.query.processor;

import lombok.Data;

import java.util.UUID;

@Data
public class GroupPermissions implements Permissions {

    protected final UUID groupId;
    protected final boolean readAttrs;
    protected final boolean readTs;

}

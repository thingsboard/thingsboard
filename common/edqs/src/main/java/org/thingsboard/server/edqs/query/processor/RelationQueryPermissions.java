// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.edqs.query.processor;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RelationQueryPermissions implements Permissions {

    private final boolean readEntity;
    private final boolean readAttrs;
    private final boolean readTs;
    private final boolean hasGroups;
    private final List<GroupPermissions> groupPermissions;

}

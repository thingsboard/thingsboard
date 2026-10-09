// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.edqs.query.processor;

import lombok.Data;

@Data
public class CombinedPermissions implements Permissions {
    private final boolean read;
    private final boolean readAttrs;
    private final boolean readTs;
}

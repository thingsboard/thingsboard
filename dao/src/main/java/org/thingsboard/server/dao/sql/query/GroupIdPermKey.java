// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.query;

import lombok.Data;

@Data
public class GroupIdPermKey {
    private final boolean read;
    private final boolean attr;
    private final boolean ts;
}

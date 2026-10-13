// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.sync.vc.data;

import lombok.Data;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.List;

@Data
public class EntityTypeExportTask {

    private final List<CustomerId> parents;
    private final EntityId ownerId;

}

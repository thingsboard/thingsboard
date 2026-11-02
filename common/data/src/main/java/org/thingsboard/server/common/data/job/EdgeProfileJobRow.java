// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.job;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EdgeProfileJobRow implements Serializable {

    private EntityId entityId;
    private EntityType groupType;

}

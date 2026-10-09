// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.thingsboard.server.common.data.id.EntityId;

public interface HasOwnerId {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    EntityId getOwnerId();

    void setOwnerId(EntityId entityId);

}

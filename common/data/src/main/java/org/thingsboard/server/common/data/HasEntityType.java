// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface HasEntityType {

    @JsonIgnore
    EntityType getEntityType();

}

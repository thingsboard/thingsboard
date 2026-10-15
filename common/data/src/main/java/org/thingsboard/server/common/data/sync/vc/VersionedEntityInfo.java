// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.sync.vc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VersionedEntityInfo {

    private final EntityId externalId;
    private String path;

    @JsonCreator
    public VersionedEntityInfo(@JsonProperty("externalId") EntityId externalId,
                               @JsonProperty("path") String path) {
        this.externalId = externalId;
        this.path = path;
    }

    public VersionedEntityInfo(EntityId externalId) {
        this.externalId = externalId;
    }

}

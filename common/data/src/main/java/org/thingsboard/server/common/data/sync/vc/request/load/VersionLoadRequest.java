// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.vc.request.load;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import static com.fasterxml.jackson.annotation.JsonSubTypes.Type;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @Type(name = "SINGLE_ENTITY", value = SingleEntityVersionLoadRequest.class),
        @Type(name = "ENTITY_TYPE", value = EntityTypeVersionLoadRequest.class)
})
@Data
public abstract class VersionLoadRequest {

    private String versionId;

    public abstract VersionLoadRequestType getType();

}

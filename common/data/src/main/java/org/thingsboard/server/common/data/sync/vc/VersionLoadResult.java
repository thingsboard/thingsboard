// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.vc;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VersionLoadResult implements Serializable {

    private static final long serialVersionUID = -1386093599856747449L;

    private List<EntityTypeLoadResult> result;
    private EntityLoadError error;
    private boolean done;

    public static VersionLoadResult empty() {
        return VersionLoadResult.builder().result(Collections.emptyList()).build();
    }

    public static VersionLoadResult success(List<EntityTypeLoadResult> result) {
        return VersionLoadResult.builder().result(result).build();
    }

    public static VersionLoadResult success(EntityTypeLoadResult result) {
        return VersionLoadResult.builder().result(List.of(result)).build();
    }

    public static VersionLoadResult error(EntityLoadError error) {
        return VersionLoadResult.builder().error(error).done(true).build();
    }

}

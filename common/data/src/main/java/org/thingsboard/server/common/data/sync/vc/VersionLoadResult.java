/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

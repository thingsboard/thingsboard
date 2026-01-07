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
package org.thingsboard.server.common.data.lwm2m;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema
@Data
public class LwM2mObject {
    @Schema(description = "LwM2M Object id.", example = "19")
    int id;
    @Schema(description = "LwM2M Object key id.", example = "19_1.0")
    String keyId;
    @Schema(description = "LwM2M Object name.", example = "BinaryAppDataContainer")
    String name;
    @Schema(description = "LwM2M Object multiple.", example = "true")
    boolean multiple;
    @Schema(description = "LwM2M Object mandatory.", example = "false")
    boolean mandatory;
    @Schema(description = "LwM2M Object instances.")
    LwM2mInstance [] instances;
}

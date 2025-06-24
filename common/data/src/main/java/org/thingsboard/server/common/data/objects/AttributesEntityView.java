/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.data.objects;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Victor Basanets on 9/05/2017.
 */
@Data
@Schema
@NoArgsConstructor
public class AttributesEntityView implements Serializable {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "List of client-side attribute keys to expose", example = "[\"currentConfiguration\"]")
    private List<String> cs = new ArrayList<>();
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "List of server-side attribute keys to expose", example = "[\"model\"]")
    private List<String> ss = new ArrayList<>();
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "List of shared attribute keys to expose", example = "[\"targetConfiguration\"]")
    private List<String> sh = new ArrayList<>();

    public AttributesEntityView(List<String> cs,
                                List<String> ss,
                                List<String> sh) {

        this.cs = new ArrayList<>(cs);
        this.ss = new ArrayList<>(ss);
        this.sh = new ArrayList<>(sh);
    }

    public AttributesEntityView(AttributesEntityView obj) {
        this(obj.getCs(), obj.getSs(), obj.getSh());
    }
}

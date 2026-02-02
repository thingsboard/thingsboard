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
package org.thingsboard.server.common.data.settings;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.HasTitle;

import java.io.Serializable;
import java.util.UUID;

@Schema
@Data
public abstract class AbstractUserDashboardInfo implements HasTitle, Serializable {

    private static final long serialVersionUID = -6461562426034242608L;

    @Schema(description = "JSON object with Dashboard id.", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID id;
    @Schema(description = "Title of the dashboard.")
    private String title;

}

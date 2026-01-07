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
package org.thingsboard.server.common.data.mobile.layout;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.Views;

@Data
public abstract class AbstractMobilePage implements MobilePage {

    @Schema(description = "Page label", example = "Air quality")
    @JsonView(Views.Public.class)
    protected String label;
    @Schema(description = "Indicates if page is visible", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonView(Views.Private.class)
    protected boolean visible;
    @Schema(description = "URL of the page icon", example = "home_icon")
    @JsonView(Views.Public.class)
    protected String icon;
}

/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.common.data.mobile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MobileMenuItem {

    @Schema(description = "Menu item label", example = "Ar quality", requiredMode = Schema.RequiredMode.REQUIRED)
    private String label;
    @Schema(description = "URL of the menu item icon", example = "home_icon")
    private String icon;
    @Schema(description = "Path to open, when user clicks the menu item", example = "/dashboard")
    private MobileMenuPath path;
    @Schema(description = "Id of the resource to open, when user clicks the menu item", example = "8a8d81b0-5975-11ef-83b1-d3209c242a36")
    private String id;

}

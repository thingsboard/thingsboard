// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

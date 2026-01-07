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
package org.thingsboard.server.common.data.widget;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serial;
import java.util.Collections;
import java.util.List;

@Data
public class WidgetTypeInfo extends BaseWidgetType {

    @Serial
    private static final long serialVersionUID = 1343617007959780969L;

    @Schema(description = "Base64 encoded widget thumbnail", accessMode = Schema.AccessMode.READ_ONLY)
    private String image;
    @NoXss
    @Schema(description = "Description of the widget type", accessMode = Schema.AccessMode.READ_ONLY)
    private String description;
    @NoXss
    @Schema(description = "Tags of the widget type", accessMode = Schema.AccessMode.READ_ONLY)
    private String[] tags;
    @NoXss
    @Schema(description = "Type of the widget (timeseries, latest, control, alarm or static)", accessMode = Schema.AccessMode.READ_ONLY)
    private String widgetType;
    @Valid
    @Schema(description = "Bundles", accessMode = Schema.AccessMode.READ_ONLY)
    private List<WidgetBundleInfo> bundles;

    public WidgetTypeInfo() {
        super();
    }

    public WidgetTypeInfo(WidgetTypeId id) {
        super(id);
    }

    public WidgetTypeInfo(BaseWidgetType baseWidgetType) {
        super(baseWidgetType);
    }

    public WidgetTypeInfo(WidgetTypeInfo widgetTypeInfo) {
        this(widgetTypeInfo, Collections.emptyList());
    }

    public WidgetTypeInfo(WidgetTypeInfo widgetTypeInfo, List<WidgetBundleInfo> bundles) {
        super(widgetTypeInfo);
        this.image = widgetTypeInfo.getImage();
        this.description = widgetTypeInfo.getDescription();
        this.tags = widgetTypeInfo.getTags();
        this.widgetType = widgetTypeInfo.getWidgetType();
        this.bundles = bundles;
    }

    public WidgetTypeInfo(WidgetTypeDetails widgetTypeDetails) {
        this(widgetTypeDetails, Collections.emptyList());
    }

    public WidgetTypeInfo(WidgetTypeDetails widgetTypeDetails, List<WidgetBundleInfo> bundles) {
        super(widgetTypeDetails);
        this.image = widgetTypeDetails.getImage();
        this.description = widgetTypeDetails.getDescription();
        this.tags = widgetTypeDetails.getTags();
        if (widgetTypeDetails.getDescriptor() != null && widgetTypeDetails.getDescriptor().has("type")) {
            this.widgetType = widgetTypeDetails.getDescriptor().get("type").asText();
        } else {
            this.widgetType = "";
        }
        this.bundles = bundles;
    }

}

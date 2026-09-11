// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.widget;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasImage;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.ResourceExportData;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"fqn", "name", "deprecated", "image", "description", "descriptor", "externalId", "resources"})
public class WidgetTypeDetails extends WidgetType implements HasName, HasTenantId, HasImage, ExportableEntity<WidgetTypeId> {

    @Schema(description = "Relative or external image URL. Replaced with image data URL (Base64) in case of relative URL and 'inlineImages' option enabled.")
    private String image;
    @NoXss
    @Length(fieldName = "description", max = 1024)
    @Schema(description = "Description of the widget")
    private String description;
    @NoXss
    @Schema(description = "Tags of the widget type")
    private String[] tags;

    private WidgetTypeId externalId;

    private List<ResourceExportData> resources;

    public WidgetTypeDetails() {
        super();
    }

    public WidgetTypeDetails(WidgetTypeId id) {
        super(id);
    }

    public WidgetTypeDetails(BaseWidgetType baseWidgetType) {
        super(baseWidgetType);
    }

    public WidgetTypeDetails(WidgetTypeDetails widgetTypeDetails) {
        super(widgetTypeDetails);
        this.image = widgetTypeDetails.getImage();
        this.description = widgetTypeDetails.getDescription();
        this.tags = widgetTypeDetails.getTags();
        this.externalId = widgetTypeDetails.getExternalId();
        this.resources = widgetTypeDetails.getResources() != null ? new ArrayList<>(widgetTypeDetails.getResources()) : null;
    }

}

// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.widget;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Data
@EqualsAndHashCode(callSuper = true)
public class BaseWidgetType extends BaseData<WidgetTypeId> implements HasName, HasTenantId, HasVersion {

    private static final long serialVersionUID = 8388684344603660756L;

    @Schema(description = "JSON object with Tenant Id.", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @NoXss
    @Length(fieldName = "fqn")
    @Schema(description = "Unique FQN that is used in dashboards as a reference widget type", accessMode = Schema.AccessMode.READ_ONLY)
    private String fqn;
    @NoXss
    @Length(fieldName = "name")
    @Schema(description = "Widget name used in search and UI", accessMode = Schema.AccessMode.READ_ONLY)
    private String name;

    @Schema(description = "Whether widget type is deprecated.", example = "true")
    private boolean deprecated;

    @Schema(description = "Whether widget type is SCADA symbol.", example = "true")
    private boolean scada;

    private Long version;

    public BaseWidgetType() {
        super();
    }

    public BaseWidgetType(WidgetTypeId id) {
        super(id);
    }

    public BaseWidgetType(BaseWidgetType widgetType) {
        super(widgetType);
        this.tenantId = widgetType.getTenantId();
        this.fqn = widgetType.getFqn();
        this.name = widgetType.getName();
        this.deprecated = widgetType.isDeprecated();
        this.scada = widgetType.isScada();
        this.version = widgetType.getVersion();
    }

    @Schema(description = "JSON object with the Widget Type Id. " +
            "Specify this field to update the Widget Type. " +
            "Referencing non-existing Widget Type Id will cause error. " +
            "Omit this field to create new Widget Type.")
    @Override
    public WidgetTypeId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the Widget Type creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

}

// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.EntityViewId;

@Data
@EqualsAndHashCode(callSuper = true)
public class EntityViewInfo extends EntityView {

    @Schema(description = "Title of the Customer that owns the entity view.", accessMode = Schema.AccessMode.READ_ONLY)
    private String customerTitle;
    @Schema(description = "Indicates special 'Public' Customer that is auto-generated to use the entity view on public dashboards.", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean customerIsPublic;

    public EntityViewInfo() {
        super();
    }

    public EntityViewInfo(EntityViewId entityViewId) {
        super(entityViewId);
    }

    public EntityViewInfo(EntityView entityView, String customerTitle, boolean customerIsPublic) {
        super(entityView);
        this.customerTitle = customerTitle;
        this.customerIsPublic = customerIsPublic;
    }
}

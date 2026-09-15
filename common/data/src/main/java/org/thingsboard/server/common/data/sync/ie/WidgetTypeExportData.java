// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.ie;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class WidgetTypeExportData extends EntityExportData<WidgetTypeDetails> {

    @Override
    public EntityType getEntityType() { return EntityType.WIDGET_TYPE; }

}

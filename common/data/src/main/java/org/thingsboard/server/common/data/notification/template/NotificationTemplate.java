// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.template;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationTemplate extends BaseData<NotificationTemplateId> implements HasTenantId, HasName, ExportableEntity<NotificationTemplateId> {

    private TenantId tenantId;
    @NoXss
    @NotEmpty
    @Length(max = 255, message = "cannot be longer than 255 chars")
    private String name;
    @NoXss
    @NotNull
    private NotificationType notificationType;
    @Valid
    @NotNull
    private NotificationTemplateConfig configuration;

    private NotificationTemplateId externalId;

    public NotificationTemplate() {
    }

    public NotificationTemplate(NotificationTemplate other) {
        super(other);
        this.tenantId = other.tenantId;
        this.name = other.name;
        this.notificationType = other.notificationType;
        this.configuration = other.configuration != null ? other.configuration.copy() : null;
        this.externalId = other.externalId;
    }

}

// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.WIDGETS_BUNDLE_TABLE_NAME)
public final class WidgetsBundleEntity extends BaseVersionedEntity<WidgetsBundle> {

    @Column(name = ModelConstants.WIDGETS_BUNDLE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.WIDGETS_BUNDLE_ALIAS_PROPERTY)
    private String alias;

    @Column(name = ModelConstants.WIDGETS_BUNDLE_TITLE_PROPERTY)
    private String title;

    @Column(name = ModelConstants.WIDGETS_BUNDLE_IMAGE_PROPERTY)
    private String image;

    @Column(name = ModelConstants.WIDGETS_BUNDLE_SCADA_PROPERTY)
    private boolean scada;

    @Column(name = ModelConstants.WIDGETS_BUNDLE_DESCRIPTION)
    private String description;

    @Column(name = ModelConstants.WIDGETS_BUNDLE_ORDER)
    private Integer order;

    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    public WidgetsBundleEntity() {
        super();
    }

    public WidgetsBundleEntity(WidgetsBundle widgetsBundle) {
        super(widgetsBundle);
        if (widgetsBundle.getTenantId() != null) {
            this.tenantId = widgetsBundle.getTenantId().getId();
        }
        this.alias = widgetsBundle.getAlias();
        this.title = widgetsBundle.getTitle();
        this.image = widgetsBundle.getImage();
        this.scada = widgetsBundle.isScada();
        this.description = widgetsBundle.getDescription();
        this.order = widgetsBundle.getOrder();
        if (widgetsBundle.getExternalId() != null) {
            this.externalId = widgetsBundle.getExternalId().getId();
        }
    }

    @Override
    public WidgetsBundle toData() {
        WidgetsBundle widgetsBundle = new WidgetsBundle(new WidgetsBundleId(id));
        widgetsBundle.setCreatedTime(createdTime);
        widgetsBundle.setVersion(version);
        if (tenantId != null) {
            widgetsBundle.setTenantId(TenantId.fromUUID(tenantId));
        }
        widgetsBundle.setAlias(alias);
        widgetsBundle.setTitle(title);
        widgetsBundle.setImage(image);
        widgetsBundle.setScada(scada);
        widgetsBundle.setDescription(description);
        widgetsBundle.setOrder(order);
        if (externalId != null) {
            widgetsBundle.setExternalId(new WidgetsBundleId(externalId));
        }
        return widgetsBundle;
    }

}

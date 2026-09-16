// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.widget.WidgetsBundleWidget;
import org.thingsboard.server.dao.model.ToData;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.WIDGETS_BUNDLE_WIDGET_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.WIDGET_TYPE_ORDER_PROPERTY;

@Data
@Entity
@Table(name = WIDGETS_BUNDLE_WIDGET_TABLE_NAME)
@IdClass(WidgetsBundleWidgetCompositeKey.class)
public final class WidgetsBundleWidgetEntity implements ToData<WidgetsBundleWidget> {

    @Id
    @Column(name = "widgets_bundle_id", columnDefinition = "uuid")
    private UUID widgetsBundleId;

    @Id
    @Column(name = "widget_type_id", columnDefinition = "uuid")
    private UUID widgetTypeId;

    @Column(name = WIDGET_TYPE_ORDER_PROPERTY)
    private int widgetTypeOrder;

    public WidgetsBundleWidgetEntity() {
        super();
    }

    public WidgetsBundleWidgetEntity(WidgetsBundleWidget widgetsBundleWidget) {
        widgetsBundleId = widgetsBundleWidget.getWidgetsBundleId().getId();
        widgetTypeId = widgetsBundleWidget.getWidgetTypeId().getId();
        widgetTypeOrder = widgetsBundleWidget.getWidgetTypeOrder();
    }

    public WidgetsBundleWidgetEntity(UUID widgetsBundleId, UUID widgetTypeId, int widgetTypeOrder) {
        this.widgetsBundleId = widgetsBundleId;
        this.widgetTypeId = widgetTypeId;
        this.widgetTypeOrder = widgetTypeOrder;
    }

    @Override
    public WidgetsBundleWidget toData() {
        WidgetsBundleWidget result = new WidgetsBundleWidget();
        result.setWidgetsBundleId(new WidgetsBundleId(widgetsBundleId));
        result.setWidgetTypeId(new WidgetTypeId(widgetTypeId));
        result.setWidgetTypeOrder(widgetTypeOrder);
        return result;
    }
}

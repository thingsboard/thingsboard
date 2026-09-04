// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.widget.BaseWidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.WIDGET_TYPE_TABLE_NAME)
public class WidgetTypeDetailsEntity extends AbstractWidgetTypeEntity<WidgetTypeDetails> {

    @Column(name = ModelConstants.WIDGET_TYPE_IMAGE_PROPERTY)
    private String image;

    @Column(name = ModelConstants.WIDGET_TYPE_DESCRIPTION_PROPERTY)
    private String description;

    @Type(StringArrayType.class)
    @Column(name = ModelConstants.WIDGET_TYPE_TAGS_PROPERTY, columnDefinition = "text[]")
    private String[] tags;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.WIDGET_TYPE_DESCRIPTOR_PROPERTY)
    private JsonNode descriptor;

    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    public WidgetTypeDetailsEntity() {
        super();
    }

    public WidgetTypeDetailsEntity(WidgetTypeDetails widgetTypeDetails) {
        super(widgetTypeDetails);
        this.image = widgetTypeDetails.getImage();
        this.description = widgetTypeDetails.getDescription();
        this.tags = widgetTypeDetails.getTags();
        this.descriptor = widgetTypeDetails.getDescriptor();
        if (widgetTypeDetails.getExternalId() != null) {
            this.externalId = widgetTypeDetails.getExternalId().getId();
        }
    }

    @Override
    public WidgetTypeDetails toData() {
        BaseWidgetType baseWidgetType = super.toBaseWidgetType();
        WidgetTypeDetails widgetTypeDetails = new WidgetTypeDetails(baseWidgetType);
        widgetTypeDetails.setImage(image);
        widgetTypeDetails.setDescription(description);
        widgetTypeDetails.setTags(tags);
        widgetTypeDetails.setDescriptor(descriptor);
        if (externalId != null) {
            widgetTypeDetails.setExternalId(new WidgetTypeId(externalId));
        }
        return widgetTypeDetails;
    }
}

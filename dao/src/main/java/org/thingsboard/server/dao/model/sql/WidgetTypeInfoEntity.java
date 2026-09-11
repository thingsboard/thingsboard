// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.widget.BaseWidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Immutable
@Table(name = ModelConstants.WIDGET_TYPE_INFO_VIEW_TABLE_NAME)
public class WidgetTypeInfoEntity extends AbstractWidgetTypeEntity<WidgetTypeInfo> {

    public static final Map<String, String> SEARCH_COLUMNS_MAP = new HashMap<>();

    static {
        SEARCH_COLUMNS_MAP.put("createdTime", "created_time");
        SEARCH_COLUMNS_MAP.put("tenantId", "tenant_id");
        SEARCH_COLUMNS_MAP.put("widgetType", "widget_type");
    }

    @Column(name = ModelConstants.WIDGET_TYPE_IMAGE_PROPERTY)
    private String image;

    @Column(name = ModelConstants.WIDGET_TYPE_DESCRIPTION_PROPERTY)
    private String description;

    @Type(StringArrayType.class)
    @Column(name = ModelConstants.WIDGET_TYPE_TAGS_PROPERTY, columnDefinition = "text[]")
    private String[] tags;

    @Column(name = ModelConstants.WIDGET_TYPE_WIDGET_TYPE_PROPERTY)
    private String widgetType;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.WIDGET_BUNDLES_PROPERTY)
    private JsonNode bundles;

    public WidgetTypeInfoEntity() {
        super();
    }

    @Override
    public WidgetTypeInfo toData() {
        BaseWidgetType baseWidgetType = super.toBaseWidgetType();
        WidgetTypeInfo widgetTypeInfo = new WidgetTypeInfo(baseWidgetType);
        widgetTypeInfo.setImage(image);
        widgetTypeInfo.setDescription(description);
        widgetTypeInfo.setTags(tags);
        widgetTypeInfo.setWidgetType(widgetType);
        widgetTypeInfo.setBundles(JacksonUtil.convertValue(bundles, new TypeReference<>() {}));
        return widgetTypeInfo;
    }

}

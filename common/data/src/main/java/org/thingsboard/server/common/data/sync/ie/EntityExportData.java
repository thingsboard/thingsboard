// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.ie;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import lombok.Data;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.sync.JsonTbEntity;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "entityType", include = As.EXISTING_PROPERTY, visible = true, defaultImpl = EntityExportData.class)
@JsonSubTypes({
        @Type(name = "DEVICE", value = DeviceExportData.class),
        @Type(name = "RULE_CHAIN", value = RuleChainExportData.class),
        @Type(name = "WIDGET_TYPE", value = WidgetTypeExportData.class),
        @Type(name = "WIDGETS_BUNDLE", value = WidgetsBundleExportData.class),
        @Type(name = "OTA_PACKAGE", value = OtaPackageExportData.class)
})
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class EntityExportData<E extends ExportableEntity<? extends EntityId>> {

    public static final Comparator<EntityRelation> relationsComparator = Comparator
            .comparing(EntityRelation::getFrom, Comparator.comparing(EntityId::getId))
            .thenComparing(EntityRelation::getTo, Comparator.comparing(EntityId::getId))
            .thenComparing(EntityRelation::getTypeGroup)
            .thenComparing(EntityRelation::getType);

    public static final Comparator<AttributeExportData> attrComparator = Comparator
            .comparing(AttributeExportData::getKey).thenComparing(AttributeExportData::getLastUpdateTs);

    public static final Comparator<CalculatedField> calculatedFieldsComparator = Comparator.comparing(CalculatedField::getName);

    @JsonProperty(index = 2)
    @JsonTbEntity
    private E entity;
    @JsonProperty(index = 1)
    private EntityType entityType;

    @JsonProperty(index = 100)
    private List<EntityRelation> relations;
    @JsonProperty(index = 101)
    private Map<String, List<AttributeExportData>> attributes;
    @JsonProperty(index = 102)
    @JsonIgnoreProperties({"id", "entityId", "createdTime", "version"})
    private List<CalculatedField> calculatedFields;

    public EntityExportData<E> sort() {
        if (relations != null && !relations.isEmpty()) {
            relations.sort(relationsComparator);
        }
        if (attributes != null && !attributes.isEmpty()) {
            attributes.values().forEach(list -> list.sort(attrComparator));
        }
        if (calculatedFields != null && !calculatedFields.isEmpty()) {
            calculatedFields.sort(calculatedFieldsComparator);
        }
        return this;
    }

    @JsonIgnore
    public EntityId getExternalId() {
        return entity.getExternalId() != null ? entity.getExternalId() : entity.getId();
    }

    @JsonIgnore
    public boolean hasCredentials() {
        return false;
    }

    @JsonIgnore
    public boolean hasAttributes() {
        return attributes != null;
    }

    @JsonIgnore
    public boolean hasRelations() {
        return relations != null;
    }

    @JsonIgnore
    public boolean hasCalculatedFields() {
        return calculatedFields != null && !calculatedFields.isEmpty();
    }

}

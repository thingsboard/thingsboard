/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.data.sync.ie;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
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
@Schema(
        description = "Base export container for ThingsBoard entities",
        discriminatorProperty = "entityType",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "CUSTOMER", schema = EntityExportData.CustomerExportData.class),
                @DiscriminatorMapping(value = "DEVICE", schema = DeviceExportData.class),
                @DiscriminatorMapping(value = "RULE_CHAIN", schema = RuleChainExportData.class),
                @DiscriminatorMapping(value = "WIDGET_TYPE", schema = WidgetTypeExportData.class),
                @DiscriminatorMapping(value = "WIDGETS_BUNDLE", schema = WidgetsBundleExportData.class),
                @DiscriminatorMapping(value = "OTA_PACKAGE", schema = OtaPackageExportData.class),
                @DiscriminatorMapping(value = "TB_RESOURCE", schema = EntityExportData.TbResourceExportData.class),
                @DiscriminatorMapping(value = "DASHBOARD", schema = EntityExportData.DashboardExportData.class),
                @DiscriminatorMapping(value = "ASSET_PROFILE", schema = EntityExportData.AssetProfileExportData.class),
                @DiscriminatorMapping(value = "ASSET", schema = EntityExportData.AssetExportData.class),
                @DiscriminatorMapping(value = "DEVICE_PROFILE", schema = EntityExportData.DeviceProfileExportData.class),
                @DiscriminatorMapping(value = "ENTITY_VIEW", schema = EntityExportData.EntityViewExportData.class),
                @DiscriminatorMapping(value = "NOTIFICATION_TEMPLATE", schema = EntityExportData.NotificationTemplateExportData.class),
                @DiscriminatorMapping(value = "NOTIFICATION_TARGET", schema = EntityExportData.NotificationTargetExportData.class),
                @DiscriminatorMapping(value = "NOTIFICATION_RULE", schema = EntityExportData.NotificationRuleExportData.class),
                @DiscriminatorMapping(value = "AI_MODEL", schema = EntityExportData.AiModelExportData.class)
        }
)
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
    @Schema(implementation = ExportableEntity.class)
    private E entity;
    @JsonProperty(index = 1)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private EntityType entityType;

    @JsonProperty(index = 100)
    @ArraySchema(schema = @Schema(implementation = EntityRelation.class))
    private List<EntityRelation> relations;
    @JsonProperty(index = 101)
    @Schema(description = "Map of attributes where key is the scope of attributes and value is the list of attributes for that scope")
    private Map<String, List<AttributeExportData>> attributes;
    @JsonProperty(index = 102)
    @JsonIgnoreProperties({"id", "entityId", "createdTime", "version"})
    @ArraySchema(schema = @Schema(implementation = CalculatedField.class))
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

    @Schema
    public static class CustomerExportData extends EntityExportData<Customer> {}
    @Schema
    public static class TbResourceExportData extends EntityExportData<TbResource> {}
    @Schema
    public static class DashboardExportData extends EntityExportData<Dashboard> {}
    @Schema
    public static class AssetProfileExportData extends EntityExportData<AssetProfile> {}
    @Schema
    public static class AssetExportData extends EntityExportData<Asset> {}
    @Schema
    public static class DeviceProfileExportData extends EntityExportData<DeviceProfile> {}
    @Schema
    public static class EntityViewExportData extends EntityExportData<EntityView> {}
    @Schema
    public static class NotificationTemplateExportData extends EntityExportData<NotificationTemplate> {}
    @Schema
    public static class NotificationTargetExportData extends EntityExportData<NotificationTarget> {}
    @Schema
    public static class NotificationRuleExportData extends EntityExportData<NotificationRule> {}
    @Schema
    public static class AiModelExportData extends EntityExportData<AiModel> {}

}

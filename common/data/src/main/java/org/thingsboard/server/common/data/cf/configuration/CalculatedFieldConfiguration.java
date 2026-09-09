// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.EntityAggregationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.aggregation.RelatedEntitiesAggregationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Schema(
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "SIMPLE", schema = SimpleCalculatedFieldConfiguration.class),
                @DiscriminatorMapping(value = "SCRIPT", schema = ScriptCalculatedFieldConfiguration.class),
                @DiscriminatorMapping(value = "GEOFENCING", schema = GeofencingCalculatedFieldConfiguration.class),
                @DiscriminatorMapping(value = "ALARM", schema = AlarmCalculatedFieldConfiguration.class),
                @DiscriminatorMapping(value = "PROPAGATION", schema = PropagationCalculatedFieldConfiguration.class),
                @DiscriminatorMapping(value = "RELATED_ENTITIES_AGGREGATION", schema = RelatedEntitiesAggregationCalculatedFieldConfiguration.class),
                @DiscriminatorMapping(value = "ENTITY_AGGREGATION", schema = EntityAggregationCalculatedFieldConfiguration.class)
        }
)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @Type(value = SimpleCalculatedFieldConfiguration.class, name = "SIMPLE"),
        @Type(value = ScriptCalculatedFieldConfiguration.class, name = "SCRIPT"),
        @Type(value = GeofencingCalculatedFieldConfiguration.class, name = "GEOFENCING"),
        @Type(value = AlarmCalculatedFieldConfiguration.class, name = "ALARM"),
        @Type(value = PropagationCalculatedFieldConfiguration.class, name = "PROPAGATION"),
        @Type(value = RelatedEntitiesAggregationCalculatedFieldConfiguration.class, name = "RELATED_ENTITIES_AGGREGATION"),
        @Type(value = EntityAggregationCalculatedFieldConfiguration.class, name = "ENTITY_AGGREGATION")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public interface CalculatedFieldConfiguration {

    @JsonIgnore
    CalculatedFieldType getType();

    Output getOutput();

    default void validate() {}

    @JsonIgnore
    default Set<EntityId> getReferencedEntities() {
        return Collections.emptySet();
    }

    default CalculatedFieldLink buildCalculatedFieldLink(TenantId tenantId, EntityId referencedEntityId, CalculatedFieldId calculatedFieldId) {
        return new CalculatedFieldLink(tenantId, referencedEntityId, calculatedFieldId);
    }

    default List<CalculatedFieldLink> buildCalculatedFieldLinks(TenantId tenantId, EntityId cfEntityId, CalculatedFieldId calculatedFieldId) {
        return getReferencedEntities().stream()
                .filter(referencedEntity -> !referencedEntity.equals(cfEntityId))
                .map(referencedEntityId -> buildCalculatedFieldLink(tenantId, referencedEntityId, calculatedFieldId))
                .collect(Collectors.toList());
    }

    @JsonIgnore
    default boolean requiresScheduledReevaluation() {
        return false;
    }

}

// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.EntityType;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class EntityIdTest {

    @Test
    public void givenConstantNullUuid_whenCompare_thenToStringEqualsPredefinedUuid() {
        Assertions.assertEquals("13814000-1dd2-11b2-8080-808080808080", EntityId.NULL_UUID.toString());
    }

    @Test
    public void allEntityIdImplementors_shouldBeInDiscriminatorMapping() {
        Schema schemaAnnotation = EntityId.class.getAnnotation(Schema.class);
        assertThat(schemaAnnotation).as("EntityId must have @Schema annotation").isNotNull();

        DiscriminatorMapping[] mappings = schemaAnnotation.discriminatorMapping();
        Map<String, Class<?>> discriminatorMap = Arrays.stream(mappings)
                .collect(Collectors.toMap(DiscriminatorMapping::value, DiscriminatorMapping::schema));

        UUID testUuid = UUID.randomUUID();
        for (EntityType entityType : EntityType.values()) {
            EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, testUuid);
            String typeName = entityType.name();

            assertThat(discriminatorMap)
                    .as("EntityId @Schema discriminatorMapping is missing entry for EntityType." + typeName)
                    .containsKey(typeName);
            assertThat(discriminatorMap.get(typeName))
                    .as("Discriminator mapping for " + typeName + " should point to " + entityId.getClass().getSimpleName())
                    .isEqualTo(entityId.getClass());
        }
    }

    @Test
    public void allEntityIdImplementors_shouldHaveAllOfEntityId() {
        UUID testUuid = UUID.randomUUID();
        for (EntityType entityType : EntityType.values()) {
            EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, testUuid);
            Class<?> idClass = entityId.getClass();
            Schema schemaAnnotation = idClass.getAnnotation(Schema.class);

            assertThat(schemaAnnotation)
                    .as(idClass.getSimpleName() + " must have @Schema annotation")
                    .isNotNull();
            assertThat(schemaAnnotation.allOf())
                    .as(idClass.getSimpleName() + " @Schema must include allOf = EntityId.class")
                    .contains(EntityId.class);
        }
    }

}
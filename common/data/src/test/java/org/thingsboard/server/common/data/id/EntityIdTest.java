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
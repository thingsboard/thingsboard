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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.EntityType;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class EntityExportDataTest {

    @Test
    public void newInstance_shouldSupportAllJsonSubTypes() {
        JsonSubTypes subTypes = EntityExportData.class.getAnnotation(JsonSubTypes.class);
        assertThat(subTypes).as("EntityExportData must have @JsonSubTypes annotation").isNotNull();

        Set<String> jsonSubTypeNames = Arrays.stream(subTypes.value())
                .map(JsonSubTypes.Type::name)
                .collect(Collectors.toSet());

        for (String typeName : jsonSubTypeNames) {
            EntityType entityType = EntityType.valueOf(typeName);
            EntityExportData<?> instance = EntityExportData.newInstance(entityType);

            assertThat(instance)
                    .as("newInstance(%s) should not return null", typeName)
                    .isNotNull();
            assertThat(instance.getEntityType())
                    .as("newInstance(%s).getEntityType() should return %s", typeName, entityType)
                    .isEqualTo(entityType);
        }
    }

}

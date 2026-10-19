// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

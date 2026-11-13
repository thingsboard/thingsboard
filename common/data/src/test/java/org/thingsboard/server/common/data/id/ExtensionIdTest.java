// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExtensionIdTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void entityIdFactoryReturnsExtensionId() {
        UUID uuid = UUID.randomUUID();
        EntityId id = EntityIdFactory.getByTypeAndUuid(EntityType.EXTENSION, uuid);
        assertThat(id).isInstanceOf(ExtensionId.class);
        assertThat(id.getEntityType()).isEqualTo(EntityType.EXTENSION);
        assertThat(id.getId()).isEqualTo(uuid);
    }

    @Test
    void entityIdFactoryByStringTypeReturnsExtensionId() {
        UUID uuid = UUID.randomUUID();
        EntityId id = EntityIdFactory.getByTypeAndUuid("EXTENSION", uuid);
        assertThat(id).isInstanceOf(ExtensionId.class);
    }

    @Test
    void jacksonRoundTrip() throws Exception {
        ExtensionId id = new ExtensionId(UUID.randomUUID());
        String json = mapper.writeValueAsString(id);
        ExtensionId parsed = mapper.readValue(json, ExtensionId.class);
        assertThat(parsed).isEqualTo(id);
        assertThat(parsed.getEntityType()).isEqualTo(EntityType.EXTENSION);
    }

    @Test
    void fromStringParsesUuid() {
        UUID uuid = UUID.randomUUID();
        assertThat(ExtensionId.fromString(uuid.toString()).getId()).isEqualTo(uuid);
    }

}

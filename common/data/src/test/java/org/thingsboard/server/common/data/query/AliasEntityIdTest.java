// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// The OpenAPI schema of AliasEntityId describes this wire format, not the getters:
// entityType holds an EntityType or an AliasEntityType name, and id is optional for an alias.
class AliasEntityIdTest {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final UUID ID = UUID.fromString("784f394c-42b6-435a-983c-b7beff2784f9");

    @Test
    void entityIdRoundTrip() throws Exception {
        AliasEntityId entityId = AliasEntityId.fromEntityId(new DeviceId(ID));

        String json = mapper.writeValueAsString(entityId);

        assertThat(mapper.readTree(json)).isEqualTo(mapper.readTree("{\"entityType\":\"DEVICE\",\"id\":\"" + ID + "\"}"));
        AliasEntityId parsed = mapper.readValue(json, AliasEntityId.class);
        assertThat(parsed.isAliasEntityId()).isFalse();
        assertThat(parsed.getEntityType()).isEqualTo(EntityType.DEVICE);
        assertThat(parsed.getId()).isEqualTo(ID);
    }

    @Test
    void aliasWithoutIdRoundTrip() throws Exception {
        String json = mapper.writeValueAsString(new AliasEntityIdImpl(AliasEntityType.CURRENT_USER, null));

        assertThat(mapper.readTree(json)).isEqualTo(mapper.readTree("{\"entityType\":\"CURRENT_USER\"}"));
        AliasEntityId parsed = mapper.readValue(json, AliasEntityId.class);
        assertThat(parsed.getAliasEntityType()).isEqualTo(AliasEntityType.CURRENT_USER);
        assertThat(parsed.getId()).isNull();
    }

    @Test
    void currentCustomerAliasCarriesDefaultCustomerId() throws Exception {
        String json = mapper.writeValueAsString(new AliasEntityIdImpl(AliasEntityType.CURRENT_CUSTOMER, ID));

        assertThat(mapper.readTree(json)).isEqualTo(mapper.readTree("{\"entityType\":\"CURRENT_CUSTOMER\",\"id\":\"" + ID + "\"}"));
        AliasEntityId parsed = mapper.readValue(json, AliasEntityId.class);
        assertThat(parsed.getAliasEntityType()).isEqualTo(AliasEntityType.CURRENT_CUSTOMER);
        assertThat(parsed.defaultEntityId()).isEqualTo(new CustomerId(ID));
    }

}

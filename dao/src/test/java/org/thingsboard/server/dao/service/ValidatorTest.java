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
package org.thingsboard.server.dao.service;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.exception.IncorrectParameterException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidatorTest {

    final DeviceId goodDeviceId = new DeviceId(UUID.fromString("18594c15-9f05-4cda-b58e-70172467c3e5"));
    final UserId nullUserId = new UserId(null);

    @Test
    void validateEntityIdTest() {
        Validator.validateEntityId(TenantId.SYS_TENANT_ID, id -> "Incorrect entityId " + id);
        Validator.validateEntityId(goodDeviceId, id -> "Incorrect entityId " + id);

        assertThatThrownBy(() -> Validator.validateEntityId(null, id -> "Incorrect entityId " + id))
                .as("EntityId is null")
                .isInstanceOf(IncorrectParameterException.class)
                .hasMessageContaining("Incorrect entityId null");

        assertThatThrownBy(() -> Validator.validateEntityId(nullUserId, id -> "Incorrect entityId " + id))
                .as("EntityId with null UUID")
                .isInstanceOf(IncorrectParameterException.class)
                .hasMessageContaining("Incorrect entityId null");
    }

    @Test
    void validateStringTest() {
        Validator.validateString("Hello", s -> "Incorrect string " + s);
        Validator.validateString(" ", s -> "Incorrect string " + s);
        Validator.validateString("\n", s -> "Incorrect string " + s);

        assertThatThrownBy(() -> Validator.validateString(null, s -> "Incorrect string " + s))
                .as("String is null")
                .isInstanceOf(IncorrectParameterException.class)
                .hasMessageContaining("Incorrect string null");

        assertThatThrownBy(() -> Validator.validateString("", s -> "Incorrect string " + s))
                .as("String is empty")
                .isInstanceOf(IncorrectParameterException.class)
                .hasMessage("Incorrect string ");

        assertThatThrownBy(() -> Validator.validateString("", s -> "Incorrect string [" + s + "]"))
                .as("String is empty []")
                .isInstanceOf(IncorrectParameterException.class)
                .hasMessage("Incorrect string []");
    }

    @Test
    void validateUUIDIdTest() {
        Validator.validateId(UUID.randomUUID(), id -> "Incorrect Id " + id);

        assertThatThrownBy(() -> Validator.validateId((UUID) null, id -> "Incorrect Id " + id))
                .as("Id is null")
                .isInstanceOf(IncorrectParameterException.class)
                .hasMessageContaining("Incorrect Id null");
    }

    @Test
    void validateUUIDBasedIdTest() {
        Validator.validateId(TenantId.SYS_TENANT_ID, id -> "Incorrect Id " + id);
        Validator.validateId(goodDeviceId, id -> "Incorrect Id " + id);

        assertThatThrownBy(() -> Validator.validateId((UUIDBased) null, id -> "Incorrect Id " + id))
                .as("Id is null")
                .isInstanceOf(IncorrectParameterException.class)
                .hasMessageContaining("Incorrect Id null");

        assertThatThrownBy(() -> Validator.validateId(nullUserId, id -> "Incorrect Id " + id))
                .as("Id with null UUIDBased")
                .isInstanceOf(IncorrectParameterException.class)
                .hasMessageContaining("Incorrect Id null");

    }

    @Test
    void validateIdsTest() {
        List<? extends UUIDBased> list = List.of(goodDeviceId);
        Validator.validateIds(list, ids -> "Incorrect Id " + ids);

        assertThatThrownBy(() -> Validator.validateIds(null, id -> "Incorrect Ids " + id))
                .as("Ids are null")
                .isInstanceOf(IncorrectParameterException.class)
                .hasMessageContaining("Incorrect Ids null");

        assertThatThrownBy(() -> Validator.validateIds(Collections.emptyList(), ids -> "Incorrect Ids " + ids))
                .as("List is empty")
                .isInstanceOf(IncorrectParameterException.class)
                .hasMessageContaining("Incorrect Ids []");

        List<UUIDBased> badList = new ArrayList<>(2);
        badList.add(goodDeviceId);
        badList.add(null);

        // Incorrect Ids [18594c15-9f05-4cda-b58e-70172467c3e5, null]
        assertThatThrownBy(() -> Validator.validateIds(badList, ids -> "Incorrect Ids " + ids))
                .as("List contains null")
                .isInstanceOf(IncorrectParameterException.class)
                .hasMessageContaining("Incorrect Ids ")
                .hasMessageContaining(goodDeviceId.getId().toString())
                .hasMessageContaining("null");

    }
}

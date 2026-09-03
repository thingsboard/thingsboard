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
package org.thingsboard.server.dao.exception;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.EntityType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.dao.exception.EntityConflictMessages.NAME;
import static org.thingsboard.server.dao.exception.EntityConflictMessages.TITLE;

class EntityConflictMessagesTest {

    @Test
    void testAlreadyExistsNamesTheEntity() {
        assertThat(EntityConflictMessages.alreadyExists(EntityType.DEVICE, NAME, "Sensor A"))
                .isEqualTo("Device with name \"Sensor A\" already exists!");
        assertThat(EntityConflictMessages.alreadyExists(EntityType.CUSTOMER, TITLE, "Customer A"))
                .isEqualTo("Customer with title \"Customer A\" already exists!");
        assertThat(EntityConflictMessages.alreadyExists(EntityType.DEVICE_PROFILE, NAME, "thermostat"))
                .isEqualTo("Device profile with name \"thermostat\" already exists!");
        assertThat(EntityConflictMessages.alreadyExists(EntityType.ENTITY_VIEW, NAME, "Meter view"))
                .isEqualTo("Entity View with name \"Meter view\" already exists!");
    }

    @Test
    void testAlreadyExistsForCalculatedField() {
        assertThat(EntityConflictMessages.alreadyExists(EntityType.CALCULATED_FIELD, NAME, "Delta"))
                .isEqualTo("Calculated field with name \"Delta\" already exists!");
    }

    @Test
    void testValueWithFormatSpecifierIsNotInterpreted() {
        // names are inserted as-is: a '%s' in a name must not turn the message into a format string
        assertThat(EntityConflictMessages.alreadyExists(EntityType.ASSET, NAME, "100% humidity %s"))
                .isEqualTo("Asset with name \"100% humidity %s\" already exists!");
    }

}

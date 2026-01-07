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
package org.thingsboard.server.common.data.cf.configuration.geofencing;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates.ENTITY_ID_LATITUDE_ARGUMENT_KEY;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates.ENTITY_ID_LONGITUDE_ARGUMENT_KEY;

public class EntityCoordinatesTest {

    @Test
    void validateToArgumentsMethodCallWithoutRefEntityId() {
        var entityCoordinates = new EntityCoordinates("xPos", "yPos");

        var arguments = entityCoordinates.toArguments();
        assertThat(arguments).isNotNull().hasSize(2);

        Argument latitudeArgument = arguments.get(ENTITY_ID_LATITUDE_ARGUMENT_KEY);
        assertThat(latitudeArgument).isNotNull();
        assertThat(latitudeArgument.getRefEntityKey()).isEqualTo(new ReferencedEntityKey("xPos", ArgumentType.TS_LATEST, null));
        assertThat(latitudeArgument.getRefEntityId()).isNull();
        assertThat(latitudeArgument.getRefDynamicSourceConfiguration()).isNull();

        Argument longitudeArgument = arguments.get(ENTITY_ID_LONGITUDE_ARGUMENT_KEY);
        assertThat(longitudeArgument).isNotNull();
        assertThat(longitudeArgument.getRefEntityKey()).isEqualTo(new ReferencedEntityKey("yPos", ArgumentType.TS_LATEST, null));
        assertThat(longitudeArgument.getRefEntityId()).isNull();
        assertThat(longitudeArgument.getRefDynamicSourceConfiguration()).isNull();
    }

}

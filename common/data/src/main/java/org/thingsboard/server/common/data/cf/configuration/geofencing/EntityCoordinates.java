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


import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;

import java.util.Map;

@Data
public class EntityCoordinates {

    public static final String ENTITY_ID_LATITUDE_ARGUMENT_KEY = "latitude";
    public static final String ENTITY_ID_LONGITUDE_ARGUMENT_KEY = "longitude";

    @NotBlank
    private final String latitudeKeyName;
    @NotBlank
    private final String longitudeKeyName;

    public Map<String, Argument> toArguments() {
        return Map.of(
                ENTITY_ID_LATITUDE_ARGUMENT_KEY, toArgument(latitudeKeyName),
                ENTITY_ID_LONGITUDE_ARGUMENT_KEY, toArgument(longitudeKeyName)
        );
    }

    private Argument toArgument(String keyName) {
        var argument = new Argument();
        argument.setRefEntityKey(new ReferencedEntityKey(keyName, ArgumentType.TS_LATEST, null));
        return argument;
    }
}

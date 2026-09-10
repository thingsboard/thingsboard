// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

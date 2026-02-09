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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates.ENTITY_ID_LATITUDE_ARGUMENT_KEY;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates.ENTITY_ID_LONGITUDE_ARGUMENT_KEY;

@ExtendWith(MockitoExtension.class)
public class GeofencingCalculatedFieldConfigurationTest {

    @Test
    void typeShouldBeGeofencing() {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        assertThat(cfg.getType()).isEqualTo(CalculatedFieldType.GEOFENCING);
    }

    @Test
    void validateShouldCallValidateOnZoneGroups() {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        EntityCoordinates entityCoordinatesMock = mock(EntityCoordinates.class);
        cfg.setEntityCoordinates(entityCoordinatesMock);
        var zoneGroupConfiguration = mock(ZoneGroupConfiguration.class);
        cfg.setZoneGroups(Map.of("someGroupName", zoneGroupConfiguration));

        cfg.validate();
        verify(zoneGroupConfiguration).validate("someGroupName");
    }

    @Test
    void validateShouldCallValidateOnZoneGroupsWithoutAnyExceptions() {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        EntityCoordinates entityCoordinatesMock = mock(EntityCoordinates.class);
        cfg.setEntityCoordinates(entityCoordinatesMock);
        var zoneGroupConfigurationA = mock(ZoneGroupConfiguration.class);
        var zoneGroupConfigurationB = mock(ZoneGroupConfiguration.class);

        String zoneGroupAName = "zoneGroupA";
        String zoneGroupBName = "zoneGroupB";

        cfg.setZoneGroups(Map.of("zoneGroupA", zoneGroupConfigurationA, "zoneGroupB", zoneGroupConfigurationB));

        assertThatCode(cfg::validate).doesNotThrowAnyException();

        verify(zoneGroupConfigurationA).validate(zoneGroupAName);
        verify(zoneGroupConfigurationB).validate(zoneGroupBName);
    }

    @Test
    void testGetArgumentsOverride() {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        cfg.setEntityCoordinates(new EntityCoordinates(ENTITY_ID_LATITUDE_ARGUMENT_KEY, ENTITY_ID_LONGITUDE_ARGUMENT_KEY));
        cfg.setZoneGroups(Map.of("allowedZones", new ZoneGroupConfiguration("perimeter", GeofencingReportStrategy.REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false)));

        Map<String, Argument> arguments = cfg.getArguments();

        assertThat(arguments).isNotNull().hasSize(3);
        assertThat(arguments).containsKeys(ENTITY_ID_LATITUDE_ARGUMENT_KEY, ENTITY_ID_LONGITUDE_ARGUMENT_KEY, "allowedZones");

        Argument latitudeArgument = arguments.get(ENTITY_ID_LATITUDE_ARGUMENT_KEY);
        assertThat(latitudeArgument).isNotNull();
        assertThat(latitudeArgument.getRefDynamicSourceConfiguration()).isNull();
        assertThat(latitudeArgument.getRefEntityId()).isNull();
        assertThat(latitudeArgument.getRefEntityKey()).isEqualTo(new ReferencedEntityKey(ENTITY_ID_LATITUDE_ARGUMENT_KEY, ArgumentType.TS_LATEST, null));

        Argument longitudeArgument = arguments.get(ENTITY_ID_LONGITUDE_ARGUMENT_KEY);
        assertThat(longitudeArgument).isNotNull();
        assertThat(longitudeArgument.getRefDynamicSourceConfiguration()).isNull();
        assertThat(longitudeArgument.getRefEntityId()).isNull();
        assertThat(longitudeArgument.getRefEntityKey()).isEqualTo(new ReferencedEntityKey(ENTITY_ID_LONGITUDE_ARGUMENT_KEY, ArgumentType.TS_LATEST, null));

        Argument allowedZonesArgument = arguments.get("allowedZones");
        assertThat(allowedZonesArgument).isNotNull();
        assertThat(allowedZonesArgument.getRefDynamicSourceConfiguration()).isNull();
        assertThat(allowedZonesArgument.getRefEntityId()).isNull();
        assertThat(allowedZonesArgument.getRefEntityKey()).isEqualTo(new ReferencedEntityKey("perimeter", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE));
    }

    @Test
    void validateShouldThrowWhenScheduledUpdateEnabledButIntervalNotSet() {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        cfg.setEntityCoordinates(mock(EntityCoordinates.class));
        cfg.setZoneGroups(Map.of("zone", mock(ZoneGroupConfiguration.class)));
        cfg.setScheduledUpdateEnabled(true);
        cfg.setScheduledUpdateInterval(null);

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh interval is required when periodic zone group refresh is enabled.");
    }

}

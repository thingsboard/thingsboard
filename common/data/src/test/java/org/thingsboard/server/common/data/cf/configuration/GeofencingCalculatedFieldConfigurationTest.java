/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.data.cf.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.thingsboard.server.common.data.cf.configuration.GeofencingCalculatedFieldConfiguration.ENTITY_ID_LATITUDE_ARGUMENT_KEY;
import static org.thingsboard.server.common.data.cf.configuration.GeofencingCalculatedFieldConfiguration.ENTITY_ID_LONGITUDE_ARGUMENT_KEY;

@ExtendWith(MockitoExtension.class)
public class GeofencingCalculatedFieldConfigurationTest {

    @Test
    void typeShouldBeGeofencing() {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        assertThat(cfg.getType()).isEqualTo(CalculatedFieldType.GEOFENCING);
    }

    @Test
    void validateShouldThrowWhenArgumentsNull() {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        cfg.setArguments(null);

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Geofencing calculated field arguments must be specified!");
    }

    @ParameterizedTest
    @MethodSource("missingCoordinateArgs")
    void validateShouldThrowWhenCoordinateArgIsMissing(String missingKey, String presentKey) {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        var arguments = new HashMap<String, Argument>();
        arguments.put(missingKey, null);
        arguments.put(presentKey, toArgument(presentKey, ArgumentType.TS_LATEST));
        cfg.setArguments(arguments);

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing required coordinates argument: " + missingKey + "!");
    }

    private static Stream<Arguments> missingCoordinateArgs() {
        return Stream.of(
                Arguments.of(ENTITY_ID_LATITUDE_ARGUMENT_KEY, ENTITY_ID_LONGITUDE_ARGUMENT_KEY),
                Arguments.of(ENTITY_ID_LONGITUDE_ARGUMENT_KEY, ENTITY_ID_LATITUDE_ARGUMENT_KEY)
        );
    }

    @ParameterizedTest
    @MethodSource("nullRefKeyCoordinateArgs")
    void validateShouldThrowWhenReferenceKeyIsNullOrTypeNull(
            String brokenKey, Argument brokenArg, String okKey) {

        var cfg = new GeofencingCalculatedFieldConfiguration();
        cfg.setArguments(Map.of(
                brokenKey, brokenArg,
                okKey, toArgument(okKey, ArgumentType.TS_LATEST)
        ));

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing or invalid reference entity key for argument: " + brokenKey);
    }

    private static Stream<Arguments> nullRefKeyCoordinateArgs() {
        return Stream.of(
                // null ref key on latitude
                Arguments.of(
                        ENTITY_ID_LATITUDE_ARGUMENT_KEY,
                        toArgument(null),
                        ENTITY_ID_LONGITUDE_ARGUMENT_KEY
                ),
                // null ref key on longitude
                Arguments.of(
                        ENTITY_ID_LONGITUDE_ARGUMENT_KEY,
                        toArgument(null),
                        ENTITY_ID_LATITUDE_ARGUMENT_KEY
                ),
                // null type on latitude
                Arguments.of(
                        ENTITY_ID_LATITUDE_ARGUMENT_KEY,
                        toArgument(new ReferencedEntityKey("latitude", null, null)),
                        ENTITY_ID_LONGITUDE_ARGUMENT_KEY
                ),
                // null type on longitude
                Arguments.of(
                        ENTITY_ID_LONGITUDE_ARGUMENT_KEY,
                        toArgument(new ReferencedEntityKey("longitude", null, null)),
                        ENTITY_ID_LATITUDE_ARGUMENT_KEY
                )
        );
    }

    @ParameterizedTest
    @MethodSource("wrongTypeCoordinateArgs")
    void validateShouldThrowWhenCoordinateHasWrongType(String wrongKey, String okKey) {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        cfg.setArguments(Map.of(
                wrongKey, toArgument(wrongKey, ArgumentType.ATTRIBUTE),
                okKey, toArgument(okKey, ArgumentType.TS_LATEST)
        ));

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Argument '" + wrongKey + "' must be of type TS_LATEST!");
    }

    private static Stream<Arguments> wrongTypeCoordinateArgs() {
        return Stream.of(
                Arguments.of(ENTITY_ID_LATITUDE_ARGUMENT_KEY, ENTITY_ID_LONGITUDE_ARGUMENT_KEY),
                Arguments.of(ENTITY_ID_LONGITUDE_ARGUMENT_KEY, ENTITY_ID_LATITUDE_ARGUMENT_KEY)
        );
    }

    @ParameterizedTest
    @MethodSource("dynamicCoordinateArgs")
    void validateShouldThrowWhenCoordinateHasDynamicSource(String dynamicKey, String okKey) {
        var cfg = new GeofencingCalculatedFieldConfiguration();

        var dynamicArg = toArgument(dynamicKey, ArgumentType.TS_LATEST);
        dynamicArg.setRefDynamicSourceConfiguration(new RelationQueryDynamicSourceConfiguration());

        cfg.setArguments(Map.of(
                dynamicKey, dynamicArg,
                okKey, toArgument(okKey, ArgumentType.TS_LATEST)
        ));

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dynamic source is not allowed for '" + dynamicKey + "' argument!");
    }

    private static Stream<Arguments> dynamicCoordinateArgs() {
        return Stream.of(
                Arguments.of(ENTITY_ID_LATITUDE_ARGUMENT_KEY, ENTITY_ID_LONGITUDE_ARGUMENT_KEY),
                Arguments.of(ENTITY_ID_LONGITUDE_ARGUMENT_KEY, ENTITY_ID_LATITUDE_ARGUMENT_KEY)
        );
    }

    @Test
    void validateShouldThrowWhenGeofencingArgumentsMissing() {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        cfg.setArguments(Map.of(
                ENTITY_ID_LATITUDE_ARGUMENT_KEY, toArgument("latitude", ArgumentType.TS_LATEST),
                ENTITY_ID_LONGITUDE_ARGUMENT_KEY, toArgument("longitude", ArgumentType.TS_LATEST)
        ));

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Geofencing calculated field must contain at least one geofencing zone group defined!");
    }

    @Test
    void validateShouldThrowWhenZoneGroupArgumentIsNull() {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        var arguments = new HashMap<String, Argument>();
        arguments.put(ENTITY_ID_LATITUDE_ARGUMENT_KEY, toArgument("latitude", ArgumentType.TS_LATEST));
        arguments.put(ENTITY_ID_LONGITUDE_ARGUMENT_KEY, toArgument("longitude", ArgumentType.TS_LATEST));
        arguments.put("someZones", null);

        cfg.setArguments(arguments);

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Geofencing calculated field must contain at least one geofencing zone group defined!");
    }

    @Test
    void validateShouldThrowWhenZoneGroupArgumentHasInvalidArgumentType() {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        var arguments = new HashMap<String, Argument>();
        arguments.put(ENTITY_ID_LATITUDE_ARGUMENT_KEY, toArgument("latitude", ArgumentType.TS_LATEST));
        arguments.put(ENTITY_ID_LONGITUDE_ARGUMENT_KEY, toArgument("longitude", ArgumentType.TS_LATEST));
        arguments.put("allowedZones", toArgument("allowedZone", ArgumentType.TS_LATEST));

        cfg.setArguments(arguments);

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Argument 'allowedZones' must be of type ATTRIBUTE!");
    }

    @Test
    void validateShouldCallDynamicSourceConfigValidationWhenZoneGroupArgumentHasDynamicSourceConfiguration() {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        var arguments = new HashMap<String, Argument>();
        arguments.put(ENTITY_ID_LATITUDE_ARGUMENT_KEY, toArgument("latitude", ArgumentType.TS_LATEST));
        arguments.put(ENTITY_ID_LONGITUDE_ARGUMENT_KEY, toArgument("longitude", ArgumentType.TS_LATEST));
        Argument allowedZonesArg = toArgument("allowedZone", ArgumentType.ATTRIBUTE);
        var refDynamicSourceConfigurationMock = mock(RelationQueryDynamicSourceConfiguration.class);
        allowedZonesArg.setRefDynamicSourceConfiguration(refDynamicSourceConfigurationMock);
        arguments.put("allowedZones", allowedZonesArg);

        ZoneGroupConfiguration allowedZoneConfiguration = new ZoneGroupConfiguration("allowedZone", Arrays.asList(GeofencingEvent.values()));
        Map<String, ZoneGroupConfiguration> zoneGroupConfigurations = Map.of("allowedZones", allowedZoneConfiguration);

        cfg.setArguments(arguments);
        cfg.setZoneGroupConfigurations(zoneGroupConfigurations);

        cfg.validate();

        verify(refDynamicSourceConfigurationMock).validate();
    }

    @Test
    void validateShouldThrowWhenZoneGroupConfigurationIsMissing() {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        var arguments = new HashMap<String, Argument>();
        arguments.put(ENTITY_ID_LATITUDE_ARGUMENT_KEY, toArgument("latitude", ArgumentType.TS_LATEST));
        arguments.put(ENTITY_ID_LONGITUDE_ARGUMENT_KEY, toArgument("longitude", ArgumentType.TS_LATEST));
        Argument allowedZonesArg = toArgument("allowedZone", ArgumentType.ATTRIBUTE);
        var refDynamicSourceConfigurationMock = mock(RelationQueryDynamicSourceConfiguration.class);
        allowedZonesArg.setRefDynamicSourceConfiguration(refDynamicSourceConfigurationMock);
        arguments.put("allowedZones", allowedZonesArg);

        cfg.setArguments(arguments);
        cfg.setZoneGroupConfigurations(null);
        cfg.setCreateRelationsWithMatchedZones(false);

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Zone groups configuration should be specified!");
    }

    @Test
    void validateShouldThrowWhenReportTelemetryPrefixDuplicate() {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        var arguments = new HashMap<String, Argument>();
        arguments.put(ENTITY_ID_LATITUDE_ARGUMENT_KEY, toArgument("latitude", ArgumentType.TS_LATEST));
        arguments.put(ENTITY_ID_LONGITUDE_ARGUMENT_KEY, toArgument("longitude", ArgumentType.TS_LATEST));
        Argument allowedZonesArg = toArgument("allowedZone", ArgumentType.ATTRIBUTE);
        Argument restrictedZonesArg = toArgument("restrictedZone", ArgumentType.ATTRIBUTE);
        var refDynamicSourceConfigurationMock = mock(RelationQueryDynamicSourceConfiguration.class);
        allowedZonesArg.setRefDynamicSourceConfiguration(refDynamicSourceConfigurationMock);
        arguments.put("allowedZones", allowedZonesArg);
        arguments.put("restrictedZones", restrictedZonesArg);

        ZoneGroupConfiguration allowedZoneConfiguration = new ZoneGroupConfiguration("theSamePrefixTest", Arrays.asList(GeofencingEvent.values()));
        ZoneGroupConfiguration restrictedZoneConfiguration = new ZoneGroupConfiguration("theSamePrefixTest", Arrays.asList(GeofencingEvent.values()));
        Map<String, ZoneGroupConfiguration> zoneGroupConfigurations = Map.of("allowedZones", allowedZoneConfiguration, "restrictedZones", restrictedZoneConfiguration);

        cfg.setArguments(arguments);
        cfg.setZoneGroupConfigurations(zoneGroupConfigurations);
        cfg.setCreateRelationsWithMatchedZones(false);

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicate report telemetry prefix found: 'theSamePrefixTest'. Must be unique!");
    }

    @Test
    void validateShouldThrowWhenZoneGroupArgumentConfigurationIsMissing() {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        var arguments = new HashMap<String, Argument>();
        arguments.put(ENTITY_ID_LATITUDE_ARGUMENT_KEY, toArgument("latitude", ArgumentType.TS_LATEST));
        arguments.put(ENTITY_ID_LONGITUDE_ARGUMENT_KEY, toArgument("longitude", ArgumentType.TS_LATEST));
        Argument allowedZonesArg = toArgument("allowedZone", ArgumentType.ATTRIBUTE);
        var refDynamicSourceConfigurationMock = mock(RelationQueryDynamicSourceConfiguration.class);
        allowedZonesArg.setRefDynamicSourceConfiguration(refDynamicSourceConfigurationMock);
        arguments.put("allowedZones", allowedZonesArg);

        ZoneGroupConfiguration allowedZoneConfiguration = new ZoneGroupConfiguration("allowedZone", Arrays.asList(GeofencingEvent.values()));
        Map<String, ZoneGroupConfiguration> zoneGroupConfigurations = Map.of("someOtherZones", allowedZoneConfiguration);

        cfg.setArguments(arguments);
        cfg.setZoneGroupConfigurations(zoneGroupConfigurations);
        cfg.setCreateRelationsWithMatchedZones(false);

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Zone group configuration is not configured for 'allowedZones' argument!");
    }

    @Test
    void validateShouldThrowWhenZoneGroupConfigurationReportEventsAreNotSpecified() {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        var arguments = new HashMap<String, Argument>();
        arguments.put(ENTITY_ID_LATITUDE_ARGUMENT_KEY, toArgument("latitude", ArgumentType.TS_LATEST));
        arguments.put(ENTITY_ID_LONGITUDE_ARGUMENT_KEY, toArgument("longitude", ArgumentType.TS_LATEST));
        Argument allowedZonesArg = toArgument("allowedZone", ArgumentType.ATTRIBUTE);
        var refDynamicSourceConfigurationMock = mock(RelationQueryDynamicSourceConfiguration.class);
        allowedZonesArg.setRefDynamicSourceConfiguration(refDynamicSourceConfigurationMock);
        arguments.put("allowedZones", allowedZonesArg);

        ZoneGroupConfiguration allowedZoneConfiguration = new ZoneGroupConfiguration("allowedZone", null);
        Map<String, ZoneGroupConfiguration> zoneGroupConfigurations = Map.of("allowedZones", allowedZoneConfiguration);

        cfg.setArguments(arguments);
        cfg.setZoneGroupConfigurations(zoneGroupConfigurations);
        cfg.setCreateRelationsWithMatchedZones(false);

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Zone group configuration report events must be specified for 'allowedZones' argument!");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "  ")
    void validateShouldThrowWhenZoneGroupConfigurationTelemetryPrefixIsBlankOrNull(String reportTelemetryPrefix) {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        var arguments = new HashMap<String, Argument>();
        arguments.put(ENTITY_ID_LATITUDE_ARGUMENT_KEY, toArgument("latitude", ArgumentType.TS_LATEST));
        arguments.put(ENTITY_ID_LONGITUDE_ARGUMENT_KEY, toArgument("longitude", ArgumentType.TS_LATEST));
        Argument allowedZonesArg = toArgument("allowedZone", ArgumentType.ATTRIBUTE);
        var refDynamicSourceConfigurationMock = mock(RelationQueryDynamicSourceConfiguration.class);
        allowedZonesArg.setRefDynamicSourceConfiguration(refDynamicSourceConfigurationMock);
        arguments.put("allowedZones", allowedZonesArg);

        ZoneGroupConfiguration allowedZoneConfiguration = new ZoneGroupConfiguration(reportTelemetryPrefix, Arrays.asList(GeofencingEvent.values()));
        Map<String, ZoneGroupConfiguration> zoneGroupConfigurations = Map.of("allowedZones", allowedZoneConfiguration);

        cfg.setArguments(arguments);
        cfg.setZoneGroupConfigurations(zoneGroupConfigurations);
        cfg.setCreateRelationsWithMatchedZones(false);

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Report telemetry prefix should be specified for 'allowedZones' argument!");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "  ")
    void validateShouldThrowWhenHasBlankOrNullZoneRelationType(String zoneRelationType) {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        var arguments = new HashMap<String, Argument>();
        arguments.put(ENTITY_ID_LATITUDE_ARGUMENT_KEY, toArgument("latitude", ArgumentType.TS_LATEST));
        arguments.put(ENTITY_ID_LONGITUDE_ARGUMENT_KEY, toArgument("longitude", ArgumentType.TS_LATEST));
        Argument allowedZonesArg = toArgument("allowedZone", ArgumentType.ATTRIBUTE);
        var refDynamicSourceConfigurationMock = mock(RelationQueryDynamicSourceConfiguration.class);
        allowedZonesArg.setRefDynamicSourceConfiguration(refDynamicSourceConfigurationMock);
        arguments.put("allowedZones", allowedZonesArg);

        ZoneGroupConfiguration allowedZoneConfiguration = new ZoneGroupConfiguration("allowedZone", Arrays.asList(GeofencingEvent.values()));
        Map<String, ZoneGroupConfiguration> zoneGroupConfigurations = Map.of("allowedZones", allowedZoneConfiguration);

        cfg.setArguments(arguments);
        cfg.setZoneGroupConfigurations(zoneGroupConfigurations);
        cfg.setCreateRelationsWithMatchedZones(true);
        cfg.setZoneRelationType(zoneRelationType);
        cfg.setZoneRelationDirection(EntitySearchDirection.TO);

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Zone relation type must be specified to create relations with matched zones!");
    }

    @Test
    void validateShouldThrowWhenNoZoneRelationDirectionSpecified() {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        var arguments = new HashMap<String, Argument>();
        arguments.put(ENTITY_ID_LATITUDE_ARGUMENT_KEY, toArgument("latitude", ArgumentType.TS_LATEST));
        arguments.put(ENTITY_ID_LONGITUDE_ARGUMENT_KEY, toArgument("longitude", ArgumentType.TS_LATEST));
        Argument allowedZonesArg = toArgument("allowedZone", ArgumentType.ATTRIBUTE);
        var refDynamicSourceConfigurationMock = mock(RelationQueryDynamicSourceConfiguration.class);
        allowedZonesArg.setRefDynamicSourceConfiguration(refDynamicSourceConfigurationMock);
        arguments.put("allowedZones", allowedZonesArg);

        ZoneGroupConfiguration allowedZoneConfiguration = new ZoneGroupConfiguration("allowedZone", Arrays.asList(GeofencingEvent.values()));
        Map<String, ZoneGroupConfiguration> zoneGroupConfigurations = Map.of("allowedZones", allowedZoneConfiguration);

        cfg.setArguments(arguments);
        cfg.setZoneGroupConfigurations(zoneGroupConfigurations);
        cfg.setCreateRelationsWithMatchedZones(true);
        cfg.setZoneRelationType("SomeRelationType");

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Zone relation direction must be specified to create relations with matched zones!");
    }

    @Test
    void scheduledUpdateDisabledWhenIntervalIsZero() {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        cfg.setScheduledUpdateIntervalSec(0);
        assertThat(cfg.isScheduledUpdateEnabled()).isFalse();
    }

    @Test
    void scheduledUpdateDisabledWhenIntervalIsGreaterThanZeroButArgumentsAreEmpty() {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        cfg.setArguments(Map.of());
        cfg.setScheduledUpdateIntervalSec(60);
        assertThat(cfg.isScheduledUpdateEnabled()).isFalse();
    }

    @Test
    void scheduledUpdateDisabledWhenIntervalIsGreaterThanZeroButDynamicArgumentsAreMissing() {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        cfg.setArguments(Map.of(ENTITY_ID_LATITUDE_ARGUMENT_KEY, toArgument("latitude", ArgumentType.TS_LATEST)));
        cfg.setScheduledUpdateIntervalSec(60);
        assertThat(cfg.isScheduledUpdateEnabled()).isFalse();
    }

    @Test
    void scheduledUpdateEnabledWhenIntervalIsGreaterThanZeroAndDynamicArgumentsPresent() {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        Argument someDynamicArgument = toArgument("someDynamicArgument", ArgumentType.ATTRIBUTE);
        someDynamicArgument.setRefDynamicSourceConfiguration(new RelationQueryDynamicSourceConfiguration());
        cfg.setArguments(Map.of("someDynamicArugument", someDynamicArgument));
        cfg.setScheduledUpdateIntervalSec(60);
        assertThat(cfg.isScheduledUpdateEnabled()).isTrue();
    }

    @Test
    void validateShouldPassOnMinimalValidConfig() {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        var args = new HashMap<String, Argument>();
        args.put(ENTITY_ID_LATITUDE_ARGUMENT_KEY, toArgument("latitude", ArgumentType.TS_LATEST));
        args.put(ENTITY_ID_LONGITUDE_ARGUMENT_KEY, toArgument("longitude", ArgumentType.TS_LATEST));
        Argument allowed = toArgument("allowed", ArgumentType.ATTRIBUTE);
        var refDynamicSourceConfigurationMock = mock(RelationQueryDynamicSourceConfiguration.class);
        allowed.setRefDynamicSourceConfiguration(refDynamicSourceConfigurationMock);
        args.put("allowedZones", allowed);
        cfg.setArguments(args);

        var zc = new ZoneGroupConfiguration("gf_allowed", Arrays.asList(GeofencingEvent.values()));
        cfg.setZoneGroupConfigurations(Map.of("allowedZones", zc));

        cfg.setCreateRelationsWithMatchedZones(true);
        cfg.setZoneRelationType("Contains");
        cfg.setZoneRelationDirection(EntitySearchDirection.FROM);

        assertThatCode(cfg::validate).doesNotThrowAnyException();
    }

    private Argument toArgument(String key, ArgumentType type) {
        var referencedEntityKey = new ReferencedEntityKey(key, type, null);
        return toArgument(referencedEntityKey);
    }

    private static Argument toArgument(ReferencedEntityKey referencedEntityKey) {
        Argument argument = new Argument();
        argument.setRefEntityKey(referencedEntityKey);
        return argument;
    }

}

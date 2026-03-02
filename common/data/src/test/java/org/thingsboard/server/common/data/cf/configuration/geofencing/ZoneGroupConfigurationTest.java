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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.CurrentOwnerDynamicSourceConfiguration;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.RelationPathQueryDynamicSourceConfiguration;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingReportStrategy.REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS;

public class ZoneGroupConfigurationTest {

    @ParameterizedTest
    @ValueSource(strings = {EntityCoordinates.ENTITY_ID_LATITUDE_ARGUMENT_KEY, EntityCoordinates.ENTITY_ID_LONGITUDE_ARGUMENT_KEY})
    void validateShouldThrowWhenUsedReservedEntityCoordinateNames(String name) {
        var zoneGroupConfiguration = new ZoneGroupConfiguration("perimeter", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);
        assertThatThrownBy(() -> zoneGroupConfiguration.validate(name))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name '" + name + "' is reserved and cannot be used for zone group!");
    }

    @ParameterizedTest
    @ValueSource(strings = "  ")
    @NullAndEmptySource
    void validateShouldThrowWhenRelationCreationEnabledAndRelationTypeIsNullEmptyOrBlank(String relationType) {
        var zoneGroupConfiguration = new ZoneGroupConfiguration("perimeter", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, true);
        zoneGroupConfiguration.setRelationType(relationType);
        assertThatThrownBy(() -> zoneGroupConfiguration.validate("allowedZonesGroup"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Relation type must be specified for 'allowedZonesGroup' zone group!");
    }

    @Test
    void validateShouldThrowWhenRelationCreationEnabledAndDirectionIsNull() {
        var zoneGroupConfiguration = new ZoneGroupConfiguration("perimeter", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, true);
        zoneGroupConfiguration.setRelationType(EntityRelation.CONTAINS_TYPE);
        zoneGroupConfiguration.setDirection(null);
        assertThatThrownBy(() -> zoneGroupConfiguration.validate("allowedZonesGroup"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Relation direction must be specified for 'allowedZonesGroup' zone group!");
    }

    @Test
    void validateShouldDoesNotThrowAnyExceptionWhenRelationCreationDisabledAndConfigValid() {
        var zoneGroupConfiguration = new ZoneGroupConfiguration("perimeter", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);
        assertThatCode(() -> zoneGroupConfiguration.validate("allowedZonesGroup")).doesNotThrowAnyException();
    }

    @Test
    void validateShouldDoesNotThrowAnyExceptionWhenRelationCreationEnabledAndConfigValid() {
        var zoneGroupConfiguration = new ZoneGroupConfiguration("perimeter", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, true);
        zoneGroupConfiguration.setRelationType(EntityRelation.CONTAINS_TYPE);
        zoneGroupConfiguration.setDirection(EntitySearchDirection.TO);
        assertThatCode(() -> zoneGroupConfiguration.validate("allowedZonesGroup")).doesNotThrowAnyException();
    }

    @Test
    void whenHasRelationQuerySourceCalled_shouldReturnTrueIfRelationQuerySourceConfigurationIsNotNull() {
        var zoneGroupConfiguration = new ZoneGroupConfiguration("perimeter", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);
        zoneGroupConfiguration.setRefDynamicSourceConfiguration(new RelationPathQueryDynamicSourceConfiguration());
        assertThat(zoneGroupConfiguration.hasRelationQuerySource()).isTrue();
    }

    @Test
    void whenHasRelationQuerySourceCalled_shouldReturnFalseIfRelationQuerySourceConfigurationIsNull() {
        var zoneGroupConfiguration = mock(ZoneGroupConfiguration.class);
        assertThat(zoneGroupConfiguration.getRefDynamicSourceConfiguration()).isNull();
        assertThat(zoneGroupConfiguration.hasRelationQuerySource()).isFalse();
    }

    @Test
    void whenHasRelationQuerySourceCalled_shouldReturnFalseIfCurrentOwnerSourceConfigured() {
        var zoneGroupConfiguration = mock(ZoneGroupConfiguration.class);
        zoneGroupConfiguration.setRefDynamicSourceConfiguration(new CurrentOwnerDynamicSourceConfiguration());
        assertThat(zoneGroupConfiguration.hasRelationQuerySource()).isFalse();
    }

    @Test
    void validateToArgumentsMethodCallWithoutRefEntityId() {
        var zoneGroupConfiguration = new ZoneGroupConfiguration("perimeter", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);
        Argument zoneGroupArgument = zoneGroupConfiguration.toArgument();
        assertThat(zoneGroupArgument).isNotNull();
        assertThat(zoneGroupArgument.getRefEntityKey()).isEqualTo(new ReferencedEntityKey("perimeter", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE));
    }

}

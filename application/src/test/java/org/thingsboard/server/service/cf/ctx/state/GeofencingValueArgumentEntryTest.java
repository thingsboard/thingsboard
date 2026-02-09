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
package org.thingsboard.server.service.cf.ctx.state;

import io.hypersistence.utils.hibernate.type.json.internal.JacksonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.geo.PerimeterDefinition;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingZoneState;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class GeofencingValueArgumentEntryTest {

    private final AssetId ZONE_1_ID = new AssetId(UUID.fromString("c0e3031c-7df1-45e4-9590-cfd621a4d714"));
    private final AssetId ZONE_2_ID = new AssetId(UUID.fromString("e7da6200-2096-4038-a343-ade9ea4fa3e4"));

    private final JsonDataEntry allowedZoneDataEntry = new JsonDataEntry("zone", "[[50.472000, 30.504000], [50.472000, 30.506000], [50.474000, 30.506000], [50.474000, 30.504000]]");
    private final BaseAttributeKvEntry allowedZoneAttributeKvEntry = new BaseAttributeKvEntry(allowedZoneDataEntry, 363L, 155L);

    private final JsonDataEntry restrictedZoneDataEntry = new JsonDataEntry("zone", "[[50.475000, 30.510000], [50.475000, 30.512000], [50.477000, 30.512000], [50.477000, 30.510000]]");
    private final BaseAttributeKvEntry restrictedZoneAttributeKvEntry = new BaseAttributeKvEntry(restrictedZoneDataEntry, 363L, 155L);

    private GeofencingArgumentEntry entry;

    @Mock
    private CalculatedFieldCtx ctx;

    @BeforeEach
    void setUp() {
        entry = new GeofencingArgumentEntry(Map.of(ZONE_1_ID, allowedZoneAttributeKvEntry, ZONE_2_ID, restrictedZoneAttributeKvEntry));
    }

    @Test
    void testArgumentEntryType() {
        assertThat(entry.getType()).isEqualTo(ArgumentEntryType.GEOFENCING);
    }

    @Test
    void testUpdateEntryWhenSingleEntryPassed() {
        assertThatThrownBy(() -> entry.updateEntry(new SingleValueArgumentEntry(), ctx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported argument entry type for geofencing argument entry: SINGLE_VALUE");
    }

    @Test
    void testUpdateEntryWhenRollingEntryPassed() {
        assertThatThrownBy(() -> entry.updateEntry(new TsRollingArgumentEntry(5, 30000L), ctx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported argument entry type for geofencing argument entry: TS_ROLLING");
    }

    @Test
    void testUpdateEntryWithTheSameTs() {
        BaseAttributeKvEntry differentValueSameTs = new BaseAttributeKvEntry(new JsonDataEntry("zone", "[[50.472001, 30.504001], [50.472001, 30.506001], [50.474001, 30.506001], [50.474001, 30.504001]]"), 363L, 156L);
        var updated = new GeofencingArgumentEntry(Map.of(ZONE_1_ID, differentValueSameTs, ZONE_2_ID, restrictedZoneAttributeKvEntry));
        assertThat(entry.updateEntry(updated, ctx)).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testUpdateEntryWhenNewVersionIsNull() {
        BaseAttributeKvEntry differentValueNewVersionIsNull = new BaseAttributeKvEntry(new JsonDataEntry("zone", "[[50.472001, 30.504001], [50.472001, 30.506001], [50.474001, 30.506001], [50.474001, 30.504001]]"), 364L, null);
        var updated = new GeofencingArgumentEntry(Map.of(ZONE_1_ID, differentValueNewVersionIsNull, ZONE_2_ID, restrictedZoneAttributeKvEntry));

        assertThat(entry.updateEntry(updated, ctx)).isTrue();
        assertThat(entry.getValue()).isInstanceOf(Map.class);

        Map<EntityId, GeofencingZoneState> value = (Map<EntityId, GeofencingZoneState>) entry.getValue();
        assertThat(value).hasSize(2);
        assertThat(value.get(ZONE_1_ID).getVersion()).isNull();
        assertThat(value.get(ZONE_1_ID).getTs()).isEqualTo(364L);
        assertThat(value.get(ZONE_1_ID).getPerimeterDefinition())
                .isEqualTo(JacksonUtil.fromString(differentValueNewVersionIsNull.getJsonValue().get(), PerimeterDefinition.class));

        assertThat(value.get(ZONE_2_ID).getVersion()).isEqualTo(155L);
        assertThat(value.get(ZONE_2_ID).getTs()).isEqualTo(363L);
        assertThat(value.get(ZONE_2_ID).getPerimeterDefinition())
                .isEqualTo(JacksonUtil.fromString(restrictedZoneAttributeKvEntry.getJsonValue().get(), PerimeterDefinition.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testUpdateEntryWhenNewVersionIsGreaterThanCurrent() {
        BaseAttributeKvEntry differentValueNewVersionIsSet = new BaseAttributeKvEntry(new JsonDataEntry("zone", "[[50.472001, 30.504001], [50.472001, 30.506001], [50.474001, 30.506001], [50.474001, 30.504001]]"), 364L, 156L);
        var updated = new GeofencingArgumentEntry(Map.of(ZONE_1_ID, differentValueNewVersionIsSet, ZONE_2_ID, restrictedZoneAttributeKvEntry));

        assertThat(entry.updateEntry(updated, ctx)).isTrue();
        assertThat(entry.getValue()).isInstanceOf(Map.class);

        Map<EntityId, GeofencingZoneState> value = (Map<EntityId, GeofencingZoneState>) entry.getValue();
        assertThat(value).hasSize(2);
        assertThat(value.get(ZONE_1_ID).getVersion()).isEqualTo(156L);
        assertThat(value.get(ZONE_1_ID).getTs()).isEqualTo(364L);
        assertThat(value.get(ZONE_1_ID).getPerimeterDefinition())
                .isEqualTo(JacksonUtil.fromString(differentValueNewVersionIsSet.getJsonValue().get(), PerimeterDefinition.class));

        assertThat(value.get(ZONE_2_ID).getVersion()).isEqualTo(155L);
        assertThat(value.get(ZONE_2_ID).getTs()).isEqualTo(363L);
        assertThat(value.get(ZONE_2_ID).getPerimeterDefinition())
                .isEqualTo(JacksonUtil.fromString(restrictedZoneAttributeKvEntry.getJsonValue().get(), PerimeterDefinition.class));
    }

    @Test
    void testUpdateEntryWhenNewVersionIsLessThanCurrent() {
        BaseAttributeKvEntry differentValueNewVersionIsSet = new BaseAttributeKvEntry(new JsonDataEntry("zone", "[[50.472001, 30.504001], [50.472001, 30.506001], [50.474001, 30.506001], [50.474001, 30.504001]]"), 364L, 154L);
        var updated = new GeofencingArgumentEntry(Map.of(ZONE_1_ID, differentValueNewVersionIsSet, ZONE_2_ID, restrictedZoneAttributeKvEntry));

        assertThat(entry.updateEntry(updated, ctx)).isFalse();
    }

    @Test
    void testUpdateEntryWhenNewTsAndVersionIsGreaterThenCurrentAndValueWasNotChanged() {
        BaseAttributeKvEntry newTsAndTheSameValue = new BaseAttributeKvEntry(allowedZoneDataEntry, 364L, 156L);
        var updated = new GeofencingArgumentEntry(Map.of(ZONE_1_ID, newTsAndTheSameValue, ZONE_2_ID, restrictedZoneAttributeKvEntry));

        assertThat(entry.updateEntry(updated, ctx)).isTrue();
    }

    @Test
    void testUpdateEntryWithOldTs() {
        BaseAttributeKvEntry oldTsAndTheSameValue = new BaseAttributeKvEntry(allowedZoneDataEntry, 362L, 156L);
        var updated = new GeofencingArgumentEntry(Map.of(ZONE_1_ID, oldTsAndTheSameValue, ZONE_2_ID, restrictedZoneAttributeKvEntry));

        assertThat(entry.updateEntry(updated, ctx)).isFalse();
    }

    @Test
    void testUpdateEntryWithNewZone() {
        final AssetId NEW_ZONE_ID = new AssetId(UUID.fromString("a3eacf1a-6af3-4e9f-87c4-502bb25c7dc3"));
        BaseAttributeKvEntry newZone = new BaseAttributeKvEntry(new JsonDataEntry("zone", "[[50.472001, 30.504001], [50.472001, 30.506001], [50.474001, 30.506001], [50.474001, 30.504001]]"), 364L, 156L);
        var updated = new GeofencingArgumentEntry(Map.of(ZONE_1_ID, allowedZoneAttributeKvEntry, ZONE_2_ID, restrictedZoneAttributeKvEntry, NEW_ZONE_ID, newZone));
        assertThat(entry.updateEntry(updated, ctx)).isTrue();
    }

    @Test
    void testIsEmpty() {
        GeofencingArgumentEntry geofencingArgumentEntry = new GeofencingArgumentEntry();
        assertThat(geofencingArgumentEntry.isEmpty()).isTrue();
    }

    @Test
    void testIsEmptyWithEmptyMap() {
        GeofencingArgumentEntry geofencingArgumentEntry = new GeofencingArgumentEntry(Map.of());
        assertThat(geofencingArgumentEntry.isEmpty()).isTrue();
    }

    @Test
    void testInvalidKvEntryDataTypeForZoneResultInEmptyArgument() {
        BaseAttributeKvEntry invalidZoneEntry = new BaseAttributeKvEntry(new StringDataEntry("zone", "someString"), 363L, 155L);
        assertThatThrownBy(() -> new GeofencingArgumentEntry(Map.of(ZONE_1_ID, invalidZoneEntry)))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid perimeter definition format for Zone with id: " + ZONE_1_ID + ". Failed to parse attribute 'zone'");
    }

    @Test
    void testNotParsableToPerimeterJsonKvEntryResultInExceptionTrowed() {
        BaseAttributeKvEntry invalidZoneEntry = new BaseAttributeKvEntry(new JsonDataEntry("zone", "\"{}\""), 363L, 155L);
        assertThatThrownBy(() -> new GeofencingArgumentEntry(Map.of(ZONE_1_ID, invalidZoneEntry)))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid perimeter definition format for Zone with id: " + ZONE_1_ID + ". Failed to parse attribute 'zone'");
    }

}

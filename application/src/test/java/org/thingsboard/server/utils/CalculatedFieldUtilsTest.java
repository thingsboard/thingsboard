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
package org.thingsboard.server.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingPresenceStatus;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldStateProto;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingZoneState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.thingsboard.server.utils.CalculatedFieldUtils.toProto;

@ExtendWith(MockitoExtension.class)
class CalculatedFieldUtilsTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("0a69e1e2-fcbc-4234-a4cd-3844bf54035c"));
    private static final CalculatedFieldId CF_ID = CalculatedFieldId.fromString("ec0e91b9-6f27-4e93-946a-5fbc2707d8bc");
    private static final DeviceId DEVICE_ID = DeviceId.fromString("1e03bd38-2010-4739-9362-160c288e36c4");

    @Test
    void toProtoAndFromProto_shouldMapGeofencingArgumentsAndZones() {
        // given
        CalculatedFieldEntityCtxId stateId = mock(CalculatedFieldEntityCtxId.class);
        given(stateId.tenantId()).willReturn(TENANT_ID);
        given(stateId.cfId()).willReturn(CF_ID);
        given(stateId.entityId()).willReturn(DEVICE_ID);

        // Build a geofencing argument with two zones (one with inside=true, one with inside=null)
        GeofencingArgumentEntry geofencingArgumentEntry = new GeofencingArgumentEntry();
        Map<EntityId, GeofencingZoneState> zoneStates = new LinkedHashMap<>();

        UUID zoneId1 = UUID.fromString("624a8fff-71a2-4847-a100-ff1cf52dbe71");
        UUID zoneId2 = UUID.fromString("e2adf6ce-9478-40b1-b0e9-4a6860cc46bb");

        AssetId z1 = new AssetId(zoneId1);
        AssetId z2 = new AssetId(zoneId2);

        JsonDataEntry zone1 = new JsonDataEntry("zone", "[[50.472000, 30.504000], [50.472000, 30.506000], [50.474000, 30.506000], [50.474000, 30.504000]]");
        JsonDataEntry zone2 = new JsonDataEntry("zone", "[[50.475000, 30.510000], [50.475000, 30.512000], [50.477000, 30.512000], [50.477000, 30.510000]]");

        BaseAttributeKvEntry zone1PerimeterAttribute = new BaseAttributeKvEntry(zone1, System.currentTimeMillis(), 0L);
        BaseAttributeKvEntry zone2PerimeterAttribute = new BaseAttributeKvEntry(zone2, System.currentTimeMillis(), 0L);

        GeofencingZoneState s1 = new GeofencingZoneState(z1, zone1PerimeterAttribute);
        s1.setLastPresence(GeofencingPresenceStatus.INSIDE);
        GeofencingZoneState s2 = new GeofencingZoneState(z2, zone2PerimeterAttribute);

        zoneStates.put(z1, s1);
        zoneStates.put(z2, s2);
        geofencingArgumentEntry.setZoneStates(zoneStates);

        // Create cf state with the geofencing argument and add it to the state map
        CalculatedFieldState state = new GeofencingCalculatedFieldState(List.of("geofencingArgumentTest"));
        state.updateState(mock(CalculatedFieldCtx.class), Map.of("geofencingArgumentTest", geofencingArgumentEntry));

        // when
        CalculatedFieldStateProto proto = toProto(stateId, state);

        // then
        CalculatedFieldState fromProto = CalculatedFieldUtils.fromProto(proto);
        assertThat(fromProto)
                .usingRecursiveComparison()
                .ignoringFields("requiredArguments")
                .isEqualTo(state);

        ArgumentEntry fromProtoArgument = fromProto.getArguments().get("geofencingArgumentTest");
        assertThat(fromProtoArgument).isInstanceOf(GeofencingArgumentEntry.class);
        GeofencingArgumentEntry fromProtoGeoArgument = (GeofencingArgumentEntry) fromProtoArgument;
        assertThat(fromProtoGeoArgument.getZoneStates()).hasSize(2);
        assertThat(fromProtoGeoArgument.getZoneStates().get(z1).getLastPresence()).isEqualTo(GeofencingPresenceStatus.INSIDE);
        assertThat(fromProtoGeoArgument.getZoneStates().get(z2).getLastPresence()).isNull();
    }

}

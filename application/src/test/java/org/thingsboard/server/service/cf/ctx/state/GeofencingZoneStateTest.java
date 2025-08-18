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
package org.thingsboard.server.service.cf.ctx.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.geo.Coordinates;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.cf.configuration.GeofencingPresenceStatus.INSIDE;
import static org.thingsboard.server.common.data.cf.configuration.GeofencingPresenceStatus.OUTSIDE;
import static org.thingsboard.server.common.data.cf.configuration.GeofencingTransitionEvent.ENTERED;
import static org.thingsboard.server.common.data.cf.configuration.GeofencingTransitionEvent.LEFT;

public class GeofencingZoneStateTest {

    private final AssetId ZONE_ID = new AssetId(UUID.fromString("628730fd-d625-417f-9c6d-ae9fe4addbdb"));

    private GeofencingZoneState state;

    @BeforeEach
    void setUp() {
        String POLYGON = "[[50.472000, 30.504000], [50.472000, 30.506000], [50.474000, 30.506000], [50.474000, 30.504000]]";
        state = new GeofencingZoneState(ZONE_ID, new BaseAttributeKvEntry(new JsonDataEntry("zone", POLYGON), 100L, 1L));
    }

    @Test
    void evaluate_initialInside_thenInsideAgain() {
        var inside = new Coordinates(50.4730, 30.5050);
        // first evaluation: no prior state -> ENTERED
        assertThat(state.evaluate(inside)).isEqualTo(new GeofencingEvalResult(ENTERED, INSIDE));
        // same position again -> INSIDE (steady state)
        assertThat(state.evaluate(inside)).isEqualTo(new GeofencingEvalResult(null, INSIDE));
    }

    @Test
    void evaluate_initialOutside_thenOutsideAgain() {
        var outside = new Coordinates(50.4760, 30.5110);
        // first evaluation: no prior state -> OUTSIDE
        assertThat(state.evaluate(outside)).isEqualTo(new GeofencingEvalResult(null, OUTSIDE));
        // same position again -> OUTSIDE (steady state)
        assertThat(state.evaluate(outside)).isEqualTo(new GeofencingEvalResult(null, OUTSIDE));
    }

    @Test
    void evaluate_inside_thenLeave() {
        var inside = new Coordinates(50.4730, 30.5050);
        var outside = new Coordinates(50.4760, 30.5110);
        // enter
        assertThat(state.evaluate(inside)).isEqualTo(new GeofencingEvalResult(ENTERED, INSIDE));
        // leave -> LEFT
        assertThat(state.evaluate(outside)).isEqualTo(new GeofencingEvalResult(LEFT, OUTSIDE));
        // still outside -> OUTSIDE
        assertThat(state.evaluate(outside)).isEqualTo(new GeofencingEvalResult(null, OUTSIDE));
    }

    @Test
    void evaluate_outside_thenEnter() {
        var outside = new Coordinates(50.4760, 30.5110);
        var inside = new Coordinates(50.4730, 30.5050);
        // start outside
        assertThat(state.evaluate(outside)).isEqualTo(new GeofencingEvalResult(null, OUTSIDE));
        // cross boundary -> ENTERED
        assertThat(state.evaluate(inside)).isEqualTo(new GeofencingEvalResult(ENTERED, INSIDE));
        // remain inside -> INSIDE
        assertThat(state.evaluate(inside)).isEqualTo(new GeofencingEvalResult(null, INSIDE));
    }

}

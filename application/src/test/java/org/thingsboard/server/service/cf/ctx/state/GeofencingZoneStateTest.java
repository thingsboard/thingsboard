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
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingEvalResult;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingZoneState;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingPresenceStatus.INSIDE;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingPresenceStatus.OUTSIDE;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingTransitionEvent.ENTERED;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingTransitionEvent.LEFT;

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

    @Test
    void update_withNewerVersion_updatesState_andResetsPresence() {
        // arrange: establish a prior presence to ensure it’s reset on update
        var inside = new Coordinates(50.4730, 30.5050);
        assertThat(state.evaluate(inside)).isNotNull(); // sets lastPresence internally

        String NEW_POLYGON = "[[50.470000, 30.502000], [50.470000, 30.503000], [50.471000, 30.503000], [50.471000, 30.502000]]";
        GeofencingZoneState newer = new GeofencingZoneState(
                ZONE_ID,
                new BaseAttributeKvEntry(new JsonDataEntry("zone", NEW_POLYGON), 200L, 2L)
        );

        // act
        boolean changed = state.update(newer);

        // assert
        assertThat(changed).isTrue();
        assertThat(state.getTs()).isEqualTo(200L);
        assertThat(state.getVersion()).isEqualTo(2L);
        assertThat(state.getPerimeterDefinition()).isNotNull();
        assertThat(state.getLastPresence()).isNull(); // must be reset on successful update
    }

    @Test
    void update_withEqualVersion_doesNothing() {
        // arrange: same version (1L) but different ts/polygon should still be ignored
        String SOME_POLYGON = "[[50.472500, 30.504500], [50.472500, 30.505500], [50.473500, 30.505500], [50.473500, 30.504500]]";
        GeofencingZoneState sameVersion = new GeofencingZoneState(
                ZONE_ID,
                new BaseAttributeKvEntry(new JsonDataEntry("zone", SOME_POLYGON), 300L, 1L)
        );

        // act
        boolean changed = state.update(sameVersion);

        // assert: nothing changes
        assertThat(changed).isFalse();
        assertThat(state.getTs()).isEqualTo(100L);
        assertThat(state.getVersion()).isEqualTo(1L);
    }

    @Test
    void update_withNullNewVersion_alwaysApplies_andCopiesNull() {
        // arrange: the implementation updates if newVersion == null
        String OTHER_POLYGON = "[[50.471000, 30.506000], [50.471000, 30.507000], [50.472000, 30.507000], [50.472000, 30.506000]]";
        GeofencingZoneState nullVersion = new GeofencingZoneState(
                ZONE_ID,
                new BaseAttributeKvEntry(new JsonDataEntry("zone", OTHER_POLYGON), 400L, null)
        );

        // act
        boolean changed = state.update(nullVersion);

        // assert: applied and version copied as null
        assertThat(changed).isTrue();
        assertThat(state.getTs()).isEqualTo(400L);
        assertThat(state.getVersion()).isNull();
        assertThat(state.getLastPresence()).isNull();
    }

    @Test
    void update_withNewVersionWhenExistingIsNull_alwaysApplies_andCopiesNew() {
        // arrange: the implementation updates if newVersion == null
        String OTHER_POLYGON = "[[50.471000, 30.506000], [50.471000, 30.507000], [50.472000, 30.507000], [50.472000, 30.506000]]";
        GeofencingZoneState newVersion = new GeofencingZoneState(
                ZONE_ID,
                new BaseAttributeKvEntry(new JsonDataEntry("zone", OTHER_POLYGON), 400L, 2L)
        );
        state.setVersion(null);

        // act
        boolean changed = state.update(newVersion);

        // assert: applied and version copied as null
        assertThat(changed).isTrue();
        assertThat(state.getTs()).isEqualTo(400L);
        assertThat(state.getVersion()).isEqualTo(2);
        assertThat(state.getLastPresence()).isNull();
    }

}

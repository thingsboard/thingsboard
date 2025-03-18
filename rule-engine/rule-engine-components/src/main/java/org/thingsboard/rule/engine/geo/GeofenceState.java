/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.rule.engine.geo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;

import static org.springframework.util.CollectionUtils.isEmpty;
import static org.thingsboard.rule.engine.geo.GeofenceStateStatus.ENTERED;
import static org.thingsboard.rule.engine.geo.GeofenceStateStatus.INSIDE;
import static org.thingsboard.rule.engine.geo.GeofenceStateStatus.LEFT;
import static org.thingsboard.rule.engine.geo.GeofenceStateStatus.OUTSIDE;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class GeofenceState {

    @EqualsAndHashCode.Include
    private EntityId geofenceId;
    private GeofenceStateStatus geofenceStateStatus;
    private Long enterTs;
    private Long insideTs;
    private Long leftTs;
    @JsonIgnore
    private boolean statusChanged;

    public GeofenceState(EntityId geofenceId) {
        this.geofenceId = geofenceId;
    }

    public void updateStatus(GeofenceDuration geofenceDuration, TbMsg tbMsg, List<EntityId> matchedGeofences) {
        long currentTime = tbMsg.getMetaDataTs();

        if (isEmpty(matchedGeofences)) {
            handleOutsideTransition(currentTime, geofenceDuration);
        } else if (matchedGeofences.contains(geofenceId)) {
            handleInsideTransition(currentTime, geofenceDuration);
        } else {
            handleOutsideTransition(currentTime, geofenceDuration);
        }
    }

    private void handleOutsideTransition(long currentTime, GeofenceDuration geofenceDuration) {
        if (LEFT.equals(geofenceStateStatus)) {
            if (hasExceededOutsideDuration(currentTime, geofenceDuration)) {
                updateStatus(OUTSIDE);
            }
        }
        if (ENTERED.equals(geofenceStateStatus) || INSIDE.equals(geofenceStateStatus)) {
            updateStatus(LEFT);
            leftTs = currentTime;
        }
    }

    private void handleInsideTransition(long currentTime, GeofenceDuration geofenceDuration) {
        if (ENTERED.equals(geofenceStateStatus)) {
            if (hasExceededInsideDuration(currentTime, geofenceDuration)) {
                updateStatus(INSIDE);
            }
        } else if (geofenceStateStatus == null) {
            updateStatus(ENTERED);
            enterTs = currentTime;
        }
    }

    private void updateStatus(GeofenceStateStatus geofenceStateStatus) {
        this.geofenceStateStatus = geofenceStateStatus;
        statusChanged = true;
    }

    private boolean hasExceededOutsideDuration(long currentTime, GeofenceDuration geofenceDuration) {
        return currentTime - leftTs >= geofenceDuration.getMinOutsideDuration();
    }

    private boolean hasExceededInsideDuration(long currentTime, GeofenceDuration geofenceDuration) {
        return currentTime - enterTs >= geofenceDuration.getMinInsideDuration();
    }

    public boolean isStatus(GeofenceStateStatus geofenceStateStatus) {
        return this.geofenceStateStatus != null && this.geofenceStateStatus.equals(geofenceStateStatus);
    }

}

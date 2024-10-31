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
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.springframework.util.CollectionUtils.isEmpty;

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

    public void updateStatus(TbGpsMultiGeofencingActionNodeConfiguration nodeConfiguration, Optional<GeofenceDurationConfig> optionalDurationConfig, TbMsg tbMsg, List<EntityId> matchedGeofences) {
        long currentTime = tbMsg.getMetaDataTs();

        if (isEmpty(matchedGeofences)) {
            handleOutsideTransition(nodeConfiguration, optionalDurationConfig, currentTime);
        } else if (matchedGeofences.contains(geofenceId)) {
            handleInsideTransition(nodeConfiguration, optionalDurationConfig, currentTime);
        } else {
            handleOutsideTransition(nodeConfiguration, optionalDurationConfig, currentTime);
        }
    }

    private void handleOutsideTransition(TbGpsMultiGeofencingActionNodeConfiguration nodeConfiguration, Optional<GeofenceDurationConfig> optionalDurationConfig, long currentTime) {
        if (GeofenceStateStatus.LEFT.equals(geofenceStateStatus)) {
            if (hasExceededOutsideDuration(currentTime - leftTs, nodeConfiguration, optionalDurationConfig)) {
                geofenceStateStatus = GeofenceStateStatus.OUTSIDE;
                setStatusChanged(true);
            }
        }
        if (GeofenceStateStatus.ENTERED.equals(geofenceStateStatus) || GeofenceStateStatus.INSIDE.equals(geofenceStateStatus)) {
            geofenceStateStatus = GeofenceStateStatus.LEFT;
            leftTs = currentTime;
            setStatusChanged(true);
        }
    }

    private void handleInsideTransition(TbGpsMultiGeofencingActionNodeConfiguration nodeConfiguration, Optional<GeofenceDurationConfig> optionalDurationConfig, long currentTime) {
        if (GeofenceStateStatus.ENTERED.equals(geofenceStateStatus)) {
            if (hasExceededInsideDuration(currentTime - enterTs, nodeConfiguration, optionalDurationConfig)) {
                geofenceStateStatus = GeofenceStateStatus.INSIDE;
                setStatusChanged(true);
            }
        } else if (geofenceStateStatus == null) {
            geofenceStateStatus = GeofenceStateStatus.ENTERED;
            enterTs = currentTime;
            setStatusChanged(true);
        }
    }

    private boolean hasExceededOutsideDuration(long elapsedTime, TbGpsMultiGeofencingActionNodeConfiguration nodeConfiguration, Optional<GeofenceDurationConfig> optionalDurationConfig) {
        Long outsideDuration = optionalDurationConfig
                .map(config -> config.getGeofenceDurationMap().get(geofenceId.getId()))
                .filter(duration -> duration.getMinOutsideDuration() != null)
                .map(GeofenceDuration::getMinOutsideDuration)
                .orElse(nodeConfiguration.getMinOutsideDuration());
        return elapsedTime >= TimeUnit.valueOf(nodeConfiguration.getMinOutsideDurationTimeUnit()).toMillis(outsideDuration);
    }

    private boolean hasExceededInsideDuration(long elapsedTime, TbGpsMultiGeofencingActionNodeConfiguration nodeConfiguration, Optional<GeofenceDurationConfig> optionalDurationConfig) {
        Long insideDuration = optionalDurationConfig
                .map(config -> config.getGeofenceDurationMap().get(geofenceId.getId()))
                .filter(duration -> duration.getMinInsideDuration() != null)
                .map(GeofenceDuration::getMinInsideDuration)
                .orElse(nodeConfiguration.getMinInsideDuration());
        return elapsedTime >= TimeUnit.valueOf(nodeConfiguration.getMinInsideDurationTimeUnit()).toMillis(insideDuration);
    }

    public boolean isStatus(GeofenceStateStatus geofenceStateStatus) {
        return this.geofenceStateStatus != null && this.geofenceStateStatus.equals(geofenceStateStatus);
    }

}

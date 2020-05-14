/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.client.object;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.response.ReadResponse;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Slf4j
public class LwM2MLocation extends BaseInstanceEnabler {

    private static final List<Integer> supportedResources = Arrays.asList(0, 1, 5);
    private static final Random RANDOM = new Random();

    private float latitude;
    private float longitude;
    private float altitude;
    private float scaleFactor;
    private float speed;
    private Date timestamp;

    public LwM2MLocation() {
        this(null, null, 1.0f, 0f, 0f);
    }

    public LwM2MLocation(Float latitude, Float longitude, float scaleFactor, float altitude, float speed) {
        if (latitude != null) {
            this.latitude = latitude + 90f;
        } else {
            this.latitude = RANDOM.nextInt(180);
        }
        if (longitude != null) {
            this.longitude = longitude + 180f;
        } else {
            this.longitude = RANDOM.nextInt(360);
        }
        this.scaleFactor = scaleFactor;
        this.altitude = altitude;
        this.speed = speed;
        timestamp = new Date();
    }

    @Override
    public ReadResponse read(ServerIdentity identity, int resourceid) {
        log.info("Read on Location resource /{}/{}/{}", getModel().id, getId(), resourceid);
        switch (resourceid) {
        case 0:
            return ReadResponse.success(resourceid, getLatitude());
        case 1:
            return ReadResponse.success(resourceid, getLongitude());
        case 2:
            return ReadResponse.success(resourceid, getAltitude());
        case 5:
            return ReadResponse.success(resourceid, getTimestamp());
        case 6:
            return ReadResponse.success(resourceid, getSpeed());
        default:
            return super.read(identity, resourceid);
        }
    }

    public void moveLocation(String nextMove) {
        switch (nextMove.charAt(0)) {
        case 'w':
            moveLatitude(1.0f);
            log.info("Move to North {}/{}", getLatitude(), getLongitude());
            break;
        case 'a':
            moveLongitude(-1.0f);
            log.info("Move to East {}/{}", getLatitude(), getLongitude());
            break;
        case 's':
            moveLatitude(-1.0f);
            log.info("Move to South {}/{}", getLatitude(), getLongitude());
            break;
        case 'd':
            moveLongitude(1.0f);
            log.info("Move to West {}/{}", getLatitude(), getLongitude());
            break;
        }
    }

    private void moveLatitude(float delta) {
        latitude = latitude + delta * scaleFactor;
        timestamp = new Date();
        fireResourcesChange(0, 5);
    }

    private void moveLongitude(float delta) {
        longitude = longitude + delta * scaleFactor;
        timestamp = new Date();
        fireResourcesChange(1, 5);
    }

    public float getLatitude() {
        return latitude - 90.0f;
    }

    public float getLongitude() {
        return longitude - 180.f;
    }

    public float getAltitude() {
        return altitude;
    }
    public Date getTimestamp() {
        return timestamp;
    }

    public float getSpeed() {
        return speed;
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }
}

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
package org.thingsboard.server.transport.lwm2m.client;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.response.ReadResponse;

import javax.security.auth.Destroyable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class LwM2mLocation extends BaseInstanceEnabler implements Destroyable {

    private float latitude;
    private float longitude;
    private float scaleFactor;
    private Date timestamp;
    protected static final Random RANDOM = new Random();
    private static final List<Integer> supportedResources = Arrays.asList(0, 1, 5);

    public LwM2mLocation() {
        this(null, null, 1.0f);
    }

    public LwM2mLocation(Float latitude, Float longitude, float scaleFactor) {

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
        timestamp = new Date();
    }

    public LwM2mLocation(Float latitude, Float longitude, float scaleFactor, ScheduledExecutorService executorService, Integer id) {
        try {
            if (id != null) this.setId(id);
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
            timestamp = new Date();
            executorService.scheduleWithFixedDelay(() -> {
                fireResourceChange(0);
                fireResourceChange(1);
            }, 10000, 10000, TimeUnit.MILLISECONDS);
        } catch (Throwable e) {
            log.error("[{}]Throwable", e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public ReadResponse read(LwM2mServer identity, int resourceId) {
        log.info("Read on Location resource /[{}]/[{}]/[{}]", getModel().id, getId(), resourceId);
        switch (resourceId) {
            case 0:
                return ReadResponse.success(resourceId, getLatitude());
            case 1:
                return ReadResponse.success(resourceId, getLongitude());
            case 5:
                return ReadResponse.success(resourceId, getTimestamp());
            default:
                return super.read(identity, resourceId);
        }
    }

    public void moveLocation(String nextMove) {
        switch (nextMove.charAt(0)) {
            case 'w':
                moveLatitude(1.0f);
                break;
            case 'a':
                moveLongitude(-1.0f);
                break;
            case 's':
                moveLatitude(-1.0f);
                break;
            case 'd':
                moveLongitude(1.0f);
                break;
        }
    }

    private void moveLatitude(float delta) {
        latitude = latitude + delta * scaleFactor;
        timestamp = new Date();
        fireResourceChange(0);
        fireResourceChange(5);
    }

    private void moveLongitude(float delta) {
        longitude = longitude + delta * scaleFactor;
        timestamp = new Date();
        fireResourceChange(1);
        fireResourceChange(5);

    }

    public float getLatitude() {
        return latitude - 90.0f;
    }

    public float getLongitude() {
        return longitude - 180.f;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }

}

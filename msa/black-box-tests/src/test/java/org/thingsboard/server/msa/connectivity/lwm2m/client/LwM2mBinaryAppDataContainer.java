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
package org.thingsboard.server.msa.connectivity.lwm2m.client;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;

import javax.security.auth.Destroyable;
import java.sql.Time;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Slf4j
public class LwM2mBinaryAppDataContainer extends BaseInstanceEnabler implements Destroyable {

    /**
     * id = 0
     * Multiple
     * base64
     */

    /**
     * Example1:
     * InNlcnZpY2VJZCI6Ik1ldGVyIiwNCiJzZXJ2aWNlRGF0YSI6ew0KImN1cnJlbnRSZWFka
     * W5nIjoiNDYuMyIsDQoic2lnbmFsU3RyZW5ndGgiOjE2LA0KImRhaWx5QWN0aXZpdHlUaW1lIjo1NzA2DQo=
     * "serviceId":"Meter",
     * "serviceData":{
     * "currentReading":"46.3",
     * "signalStrength":16,
     * "dailyActivityTime":5706
     */

    /**
     * Example2:
     * InNlcnZpY2VJZCI6IldhdGVyTWV0ZXIiLA0KImNtZCI6IlNFVF9URU1QRVJBVFVSRV9SRUFEX
     * 1BFUklPRCIsDQoicGFyYXMiOnsNCiJ2YWx1ZSI6NA0KICAgIH0sDQoNCg0K
     * "serviceId":"WaterMeter",
     * "cmd":"SET_TEMPERATURE_READ_PERIOD",
     * "paras":{
     * "value":4
     * },
     */

    Map<Integer, byte[]> data;
    private Integer priority = 0;
    private Time timestamp;
    private String description;
    private String dataFormat;
    private Integer appID = -1;
    private static final List<Integer> supportedResources = Arrays.asList(0, 1, 2, 3, 4, 5);

    public LwM2mBinaryAppDataContainer() {
    }

    public LwM2mBinaryAppDataContainer(ScheduledExecutorService executorService, Integer id) {
        try {
            if (id != null) this.setId(id);
            executorService.scheduleWithFixedDelay(() -> {
                        fireResourceChange(0);
                        fireResourceChange(2);
                    }
                    , 1800000, 1800000, TimeUnit.MILLISECONDS); // 30 MIN
        } catch (Throwable e) {
            log.error("[{}]Throwable", e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public ReadResponse read(LwM2mServer identity, int resourceId) {
        try {
            switch (resourceId) {
                case 0:
                    ReadResponse response = ReadResponse.success(resourceId, getData(), ResourceModel.Type.OPAQUE);
                    return response;
                case 1:
                    return ReadResponse.success(resourceId, getPriority());
                case 2:
                    return ReadResponse.success(resourceId, getTimestamp());
                case 3:
                    return ReadResponse.success(resourceId, getDescription());
                case 4:
                    return ReadResponse.success(resourceId, getDataFormat());
                case 5:
                    return ReadResponse.success(resourceId, getAppID());
                default:
                    return super.read(identity, resourceId);
            }
        } catch (Exception e) {
            return ReadResponse.badRequest(e.getMessage());
        }
    }

    @Override
    public WriteResponse write(LwM2mServer identity, boolean replace, int resourceId, LwM2mResource value) {
        log.info("Write on Device resource /[{}]/[{}]/[{}]", getModel().id, getId(), resourceId);
        switch (resourceId) {
            case 0:
                if (setData(value, replace)) {
                    return WriteResponse.success();
                } else {
                    WriteResponse.badRequest("Invalidate value ...");
                }
            case 1:
                setPriority((Integer) (value.getValue() instanceof Long ? ((Long) value.getValue()).intValue() : value.getValue()));
                fireResourceChange(resourceId);
                return WriteResponse.success();
            case 2:
                setTimestamp(((Date) value.getValue()).getTime());
                fireResourceChange(resourceId);
                return WriteResponse.success();
            case 3:
                setDescription((String) value.getValue());
                fireResourceChange(resourceId);
                return WriteResponse.success();
            case 4:
                setDataFormat((String) value.getValue());
                fireResourceChange(resourceId);
                return WriteResponse.success();
            case 5:
                setAppID((Integer) value.getValue());
                fireResourceChange(resourceId);
                return WriteResponse.success();
            default:
                return super.write(identity, replace, resourceId, value);
        }
    }

    private Integer getAppID() {
        return this.appID;
    }

    private void setAppID(Integer appId) {
        this.appID = appId;
    }

    private void setDataFormat(String value) {
        this.dataFormat = value;
    }

    private String getDataFormat() {
        return this.dataFormat == null ? "OPAQUE" : this.dataFormat;
    }

    private void setDescription(String value) {
        this.description = value;
    }

    private String getDescription() {
//        return this.description == null ? "meter reading" : this.description;
        return this.description;
    }

    private void setTimestamp(long time) {
        this.timestamp = new Time(time);
    }

    private Time getTimestamp() {
        return this.timestamp != null ? this.timestamp : new Time(new Date().getTime());
    }

    private boolean setData(LwM2mResource value, boolean replace) {
        try {
            if (value instanceof LwM2mMultipleResource) {
                if (replace || this.data == null) {
                    this.data = new HashMap<>();
                }
                value.getInstances().values().forEach(v -> {
                    this.data.put(v.getId(), (byte[]) v.getValue());
                });
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private Map<Integer, byte[]> getData() {
        if (data == null) {
            this.data = new HashMap<>();
            this.data.put(0, new byte[]{(byte) 0xAC});
        }
        return data;
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }

    @Override
    public void destroy() {
    }

    private int getPriority() {
        return this.priority;
    }

    private void setPriority(int value) {
        this.priority = value;
    }
}

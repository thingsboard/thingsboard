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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.Destroyable;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.argument.Arguments;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Data
public class SimpleLwM2MDevice extends BaseInstanceEnabler implements Destroyable {


    private static final Random RANDOM = new Random();
    private static final int min = 5;
    private static final int max = 50;
    private static final  PrimitiveIterator.OfInt randomIterator = new Random().ints(min,max + 1).iterator();
    private static final List<Integer> supportedResources = Arrays.asList(0, 1, 2, 3, 6, 7, 8, 9, 10, 11, 13, 14, 15, 16, 17, 18, 19, 20, 21);
    private int countReadObserveAfterUpdateRegistrationSuccess_3_0_9;
    private int countUpdateRegistrationSuccessLast;
    private LwM2MTestClient lwM2MTestClient;

    public SimpleLwM2MDevice() {
    }

    public SimpleLwM2MDevice(ScheduledExecutorService executorService) {
        try {
            executorService.scheduleWithFixedDelay(() -> {
                        fireResourceChange(9);
                    }
                    , 1, 1, TimeUnit.SECONDS); // 30 MIN
//                    , 1800000, 1800000, TimeUnit.MILLISECONDS); // 30 MIN
        } catch (Throwable e) {
            log.error("[{}]Throwable", e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public ReadResponse read(LwM2mServer identity, int resourceId) {
        if (!identity.isSystem())
            log.trace("Read on Device resource /{}/{}/{}", getModel().id, getId(), resourceId);
        switch (resourceId) {
            case 0:
                return ReadResponse.success(resourceId, getManufacturer());
            case 1:
                return ReadResponse.success(resourceId, getModelNumber());
            case 2:
                return ReadResponse.success(resourceId, getSerialNumber());
            case 3:
                return ReadResponse.success(resourceId, getFirmwareVersion());
            case 6:
                return ReadResponse.success(resourceId, getAvailablePowerSources(), ResourceModel.Type.INTEGER);
            case 9:
                return ReadResponse.success(resourceId, getBatteryLevel());
            case 10:
                return ReadResponse.success(resourceId, getMemoryFree());
            case 11:
                Map<Integer, Long> errorCodes = new HashMap<>();
                errorCodes.put(0, getErrorCode());
                return ReadResponse.success(resourceId, errorCodes, ResourceModel.Type.INTEGER);
            case 14:
                return ReadResponse.success(resourceId, getUtcOffset());
            case 15:
                return ReadResponse.success(resourceId, getTimezone());
            case 16:
                return ReadResponse.success(resourceId, getSupportedBinding());
            case 17:
                return ReadResponse.success(resourceId, getDeviceType());
            case 18:
                return ReadResponse.success(resourceId, getHardwareVersion());
            case 19:
                return ReadResponse.success(resourceId, getSoftwareVersion());
            case 20:
                return ReadResponse.success(resourceId, getBatteryStatus());
            case 21:
                return ReadResponse.success(resourceId, getMemoryTotal());
            default:
                return super.read(identity, resourceId);
        }
    }

    @Override
    public ExecuteResponse execute(LwM2mServer identity, int resourceId, Arguments arguments) {
        String withArguments = "";
        if (!arguments.isEmpty())
            withArguments = " with arguments " + arguments;
        log.info("Execute on Device resource /{}/{}/{} {}", getModel().id, getId(), resourceId, withArguments);
        return ExecuteResponse.success();
    }

    @Override
    public WriteResponse write(LwM2mServer identity, boolean replace, int resourceId, LwM2mResource value) {
        log.info("Write on Device resource /{}/{}/{}", getModel().id, getId(), resourceId);

        switch (resourceId) {
            case 13:
                return WriteResponse.notFound();
            case 14:
                setUtcOffset((String) value.getValue());
                fireResourceChange(resourceId);
                return WriteResponse.success();
            case 15:
                setTimezone((String) value.getValue());
                fireResourceChange(resourceId);
                return WriteResponse.success();
            default:
                return super.write(identity, replace, resourceId, value);
        }
    }

    private String getManufacturer() {
        return "Thingsboard Demo Lwm2mDevice";
    }

    private String getModelNumber() {
        return "Model 500";
    }

    private String getSerialNumber() {
        return "Thingsboard-500-000-0001";
    }

    private String getFirmwareVersion() {
        return "1.0.2";
    }

    private long getErrorCode() {
        return 0;
    }

    private Map<Integer, Long> getAvailablePowerSources() {
        Map<Integer, Long> availablePowerSources = new HashMap<>();
        availablePowerSources.put(0, 1L);
        availablePowerSources.put(1, 2L);
        availablePowerSources.put(2, 5L);
        return availablePowerSources;
    }

    private int getBatteryLevel() {
        int batteryLevel = randomIterator.nextInt();
        if (countUpdateRegistrationSuccessLast != this.lwM2MTestClient.getCountUpdateRegistrationSuccess()) {
            countUpdateRegistrationSuccessLast = this.lwM2MTestClient.getCountUpdateRegistrationSuccess();
            countReadObserveAfterUpdateRegistrationSuccess_3_0_9 = 0;
        }
        countReadObserveAfterUpdateRegistrationSuccess_3_0_9++;
        this.lwM2MTestClient.setCountReadObserveAfterUpdateRegistrationSuccess(countReadObserveAfterUpdateRegistrationSuccess_3_0_9);
        log.info("Read on Device resource /{}/{}/9 batteryLevel = {}, cntReadAfterUpdateReg [{}] ", getModel().id, getId(), batteryLevel, countReadObserveAfterUpdateRegistrationSuccess_3_0_9);
        return randomIterator.nextInt();
    }

    private long getMemoryFree() {
        return Runtime.getRuntime().freeMemory() / 1024;
    }

    private String utcOffset = new SimpleDateFormat("X").format(Calendar.getInstance().getTime());

    private String getUtcOffset() {
        return utcOffset;
    }

    private void setUtcOffset(String t) {
        utcOffset = t;
    }

    private String timeZone = TimeZone.getDefault().getID();

    private String getTimezone() {
        return timeZone;
    }

    private void setTimezone(String t) {
        timeZone = t;
    }

    private String getSupportedBinding() {
        return "U";
    }

    private String getDeviceType() {
        return "Demo";
    }

    private String getHardwareVersion() {
        return "1.0.1";
    }

    private String getSoftwareVersion() {
        return "1.0.2";
    }

    private int getBatteryStatus() {
        return RANDOM.nextInt(7);
    }

    private long getMemoryTotal() {
        return Runtime.getRuntime().totalMemory() / 1024;
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }

    protected void setLwM2MTestClient(LwM2MTestClient lwM2MTestClient){
        this.lwM2MTestClient = lwM2MTestClient;
    }

    @Override
    public void destroy() {
    }
}

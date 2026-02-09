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
import org.eclipse.leshan.client.LeshanClient;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.send.ManualDataSender;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.argument.Arguments;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;

import javax.security.auth.Destroyable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class LwM2mTemperatureSensor extends BaseInstanceEnabler implements Destroyable {

    private static final String UNIT_CELSIUS = "cel";
    private double currentTemp = 20d;
    private double minMeasuredValue = currentTemp;
    private double maxMeasuredValue = currentTemp;

    private LeshanClient leshanClient;
    private List<Double> containingValues;
    protected static final Random RANDOM = new Random();
    private static final List<Integer> supportedResources = Arrays.asList(5601, 5602, 5700, 5701);

    public LwM2mTemperatureSensor() {

    }

    public LwM2mTemperatureSensor(ScheduledExecutorService executorService, Integer id) {
        try {
            if (id != null) this.setId(id);
        executorService.scheduleWithFixedDelay(this::adjustTemperature, 2000, 2000, TimeUnit.MILLISECONDS);
        } catch (Throwable e) {
            log.error("[{}]Throwable", e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public synchronized ReadResponse read(LwM2mServer identity, int resourceId) {
        log.info("Read on Temperature resource /[{}]/[{}]/[{}]", getModel().id, getId(), resourceId);
        switch (resourceId) {
            case 5601:
                return ReadResponse.success(resourceId, getTwoDigitValue(minMeasuredValue));
            case 5602:
                return ReadResponse.success(resourceId, getTwoDigitValue(maxMeasuredValue));
            case 5700:
                if (identity == LwM2mServer.SYSTEM)  {
                    setTemperature();
                    setData();
                    return ReadResponse.success(resourceId, getTwoDigitValue(currentTemp));
                } else if (this.getId() == 12 && this.leshanClient != null)  {
                    containingValues = new ArrayList<>();
                    sendCollected(5700);
                    return ReadResponse.success(resourceId, getData());
                } else {
                    return ReadResponse.success(resourceId, getTwoDigitValue(currentTemp));
                }
            case 5701:
                return ReadResponse.success(resourceId, UNIT_CELSIUS);
            default:
                return super.read(identity, resourceId);
        }
    }

    @Override
    public synchronized ExecuteResponse execute(LwM2mServer identity, int resourceId, Arguments arguments) {
        log.info("Execute on Temperature resource /[{}]/[{}]/[{}]", getModel().id, getId(), resourceId);
        switch (resourceId) {
            case 5605:
                resetMinMaxMeasuredValues();
                return ExecuteResponse.success();
            default:
                return super.execute(identity, resourceId, arguments);
        }
    }

    private double getTwoDigitValue(double value) {
        BigDecimal toBeTruncated = BigDecimal.valueOf(value);
        return toBeTruncated.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private void adjustTemperature() {
        setTemperature();
        Integer changedResource = adjustMinMaxMeasuredValue(currentTemp);
        fireResourceChange(5700);
        if (changedResource != null) {
            fireResourceChange(changedResource);
        }
    }

    private void setTemperature(){
        float delta = (RANDOM.nextInt(20) - 10) / 10f;
        currentTemp += delta;
    }
    private synchronized Integer adjustMinMaxMeasuredValue(double newTemperature) {
        if (newTemperature > maxMeasuredValue) {
            maxMeasuredValue = newTemperature;
            return 5602;
        } else if (newTemperature < minMeasuredValue) {
            minMeasuredValue = newTemperature;
            return 5601;
        } else {
            return null;
        }
    }

    private void resetMinMaxMeasuredValues() {
        minMeasuredValue = currentTemp;
        maxMeasuredValue = currentTemp;
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }

    protected void setLeshanClient(LeshanClient leshanClient){
        this.leshanClient = leshanClient;
    }

    @Override
    public void destroy() {
    }

    private void sendCollected(int resourceId) {
        try {
            LwM2mServer registeredServer = this.leshanClient.getRegisteredServers().values().iterator().next();
            ManualDataSender sender = this.leshanClient.getSendService().getDataSender(ManualDataSender.DEFAULT_NAME,
                    ManualDataSender.class);
            sender.collectData(Arrays.asList(getPathForCollectedValue(resourceId)));
            Thread.sleep(1000);
            sender.collectData(Arrays.asList(getPathForCollectedValue(resourceId)));
            sender.sendCollectedData(registeredServer, ContentFormat.SENML_JSON, 1000, false);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    private LwM2mPath getPathForCollectedValue(int resourceId) {
        return new LwM2mPath(3303, this.getId(), resourceId);
    }

    private double getData() {
        if (containingValues.size() > 1) {
            Integer t0 = Math.toIntExact(Math.round(containingValues.get(0) * 100));
            Integer t1 = Math.toIntExact(Math.round(containingValues.get(1) * 100));
            long to_t1 = (((long) t0) << 32) | (t1 & 0xffffffffL);
            return Double.longBitsToDouble(to_t1);
        } else {
            return currentTemp;
        }

    }

    private void setData() {
        if (containingValues == null){
            containingValues = new ArrayList<>();
        }
        containingValues.add(getTwoDigitValue(currentTemp));
    }
}


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
package org.thingsboard.server.transport.coap.client;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.data.CoapDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.PowerMode;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.coap.TransportConfigurationContainer;
import org.thingsboard.server.transport.coap.adaptors.CoapTransportAdaptor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Data
public class TbCoapClientState {

    private final DeviceId deviceId;
    private final Lock lock;

    private volatile TransportConfigurationContainer configuration;
    private volatile CoapTransportAdaptor adaptor;
    private volatile ValidateDeviceCredentialsResponse credentials;
    private volatile TransportProtos.SessionInfoProto session;
    private volatile DefaultCoapClientContext.CoapSessionListener listener;
    private volatile TbCoapObservationState attrs;
    private volatile TbCoapObservationState rpc;
    private volatile int contentFormat;

    private TransportProtos.AttributeUpdateNotificationMsg missedAttributeUpdates;

    private DeviceProfileId profileId;

    @Getter
    private PowerMode powerMode;
    @Getter
    private Long psmActivityTimer;
    @Getter
    private Long edrxCycle;
    @Getter
    private Long pagingTransmissionWindow;
    @Getter
    @Setter
    private boolean asleep;
    @Getter
    private long lastUplinkTime;
    @Getter
    @Setter
    private Future<Void> sleepTask;

    private boolean firstEdrxDownlink = true;

    public TbCoapClientState(DeviceId deviceId) {
        this.deviceId = deviceId;
        this.lock = new ReentrantLock();
    }

    public void init(ValidateDeviceCredentialsResponse credentials) {
        this.credentials = credentials;
        this.profileId = credentials.getDeviceInfo().getDeviceProfileId();
        this.powerMode = credentials.getDeviceInfo().getPowerMode();
        this.edrxCycle = credentials.getDeviceInfo().getEdrxCycle();
        this.psmActivityTimer = credentials.getDeviceInfo().getPsmActivityTimer();
        this.pagingTransmissionWindow = credentials.getDeviceInfo().getPagingTransmissionWindow();
    }

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }

    public long updateLastUplinkTime(long ts) {
        if (ts > lastUplinkTime) {
            this.lastUplinkTime = ts;
            this.firstEdrxDownlink = true;
        }
        return lastUplinkTime;
    }

    public boolean checkFirstDownlink() {
        boolean result = firstEdrxDownlink;
        firstEdrxDownlink = false;
        return result;
    }

    public void onDeviceUpdate(Device device) {
        this.profileId = device.getDeviceProfileId();
        var data = device.getDeviceData();
        if (data.getTransportConfiguration() != null && data.getTransportConfiguration().getType().equals(DeviceTransportType.COAP)) {
            CoapDeviceTransportConfiguration configuration = (CoapDeviceTransportConfiguration) data.getTransportConfiguration();
            this.powerMode = configuration.getPowerMode();
            this.edrxCycle = configuration.getEdrxCycle();
            this.psmActivityTimer = configuration.getPsmActivityTimer();
            this.pagingTransmissionWindow = configuration.getPagingTransmissionWindow();
        }
    }

    public void addQueuedNotification(TransportProtos.AttributeUpdateNotificationMsg msg) {
        if (missedAttributeUpdates == null) {
            missedAttributeUpdates = msg;
        } else {
            Map<String, TransportProtos.TsKvProto> updatedAttrs = new HashMap<>(missedAttributeUpdates.getSharedUpdatedCount() + msg.getSharedUpdatedCount());
            Set<String> deletedKeys = new HashSet<>(missedAttributeUpdates.getSharedDeletedCount() + msg.getSharedDeletedCount());
            for (TransportProtos.TsKvProto oldUpdatedAttrs : missedAttributeUpdates.getSharedUpdatedList()) {
                updatedAttrs.put(oldUpdatedAttrs.getKv().getKey(), oldUpdatedAttrs);
            }
            deletedKeys.addAll(msg.getSharedDeletedList());
            for (TransportProtos.TsKvProto newUpdatedAttrs : msg.getSharedUpdatedList()) {
                updatedAttrs.put(newUpdatedAttrs.getKv().getKey(), newUpdatedAttrs);
            }
            deletedKeys.addAll(msg.getSharedDeletedList());
            for (String deletedKey : msg.getSharedDeletedList()) {
                updatedAttrs.remove(deletedKey);
            }
            missedAttributeUpdates = TransportProtos.AttributeUpdateNotificationMsg.newBuilder().addAllSharedUpdated(updatedAttrs.values()).addAllSharedDeleted(deletedKeys).build();
        }
    }

    public TransportProtos.AttributeUpdateNotificationMsg getAndClearMissedUpdates() {
        var result = this.missedAttributeUpdates;
        this.missedAttributeUpdates = null;
        return result;
    }
}

/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.coap.TransportConfigurationContainer;
import org.thingsboard.server.transport.coap.adaptors.CoapTransportAdaptor;

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

    private volatile TbCoapObservationState attrs;
    private volatile TbCoapObservationState rpc;

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

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }

    public long updateLastUplinkTime(){
        this.lastUplinkTime = System.currentTimeMillis();
        this.firstEdrxDownlink = true;
        return lastUplinkTime;
    }

}

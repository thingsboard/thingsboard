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
package org.thingsboard.server.transport.snmp.session;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.CommunityTarget;
import org.snmp4j.Target;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OctetString;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.data.SnmpDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.SnmpDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.transport.session.DeviceAwareSessionContext;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class DeviceSessionCtx extends DeviceAwareSessionContext {

    private final AtomicInteger msgIdSeq = new AtomicInteger(0);

    //TODO: temp implementation
    @Getter
    @Setter
    private SnmpDeviceTransportConfiguration transportConfiguration;
    @Getter
    @Setter
    private SnmpSessionListener snmpSessionListener = new SnmpSessionListener();
    @Getter
    @Setter
    private Target target;

    public DeviceSessionCtx(UUID sessionId) {
        super(sessionId);
    }

    @Override
    public int nextMsgId() {
        return msgIdSeq.incrementAndGet();
    }

    @Override
    public void onProfileUpdate(DeviceProfile deviceProfile) {
        super.onProfileUpdate(deviceProfile);
        //TODO: Cancel futures, update profile and start new features.
    }

    public void initTarget(SnmpDeviceProfileTransportConfiguration config) {
        CommunityTarget communityTarget = new CommunityTarget();
        communityTarget.setAddress(GenericAddress.parse(GenericAddress.TYPE_UDP + ":" + transportConfiguration.getAddress() + "/" + transportConfiguration.getPort()));
        communityTarget.setVersion(getSnmpVersion(transportConfiguration.getProtocolVersion()));
        communityTarget.setCommunity(new OctetString(transportConfiguration.getCommunity()));
        communityTarget.setTimeout(config.getTimeoutMs());
        communityTarget.setRetries(config.getRetries());

        this.target = communityTarget;
    }

    //TODO: replace with enum, wtih preliminary disussion of type version in config (string or integer)
    private int getSnmpVersion(String configSnmpVersion) {
        switch (configSnmpVersion) {
            case ("v1"):
                return SnmpConstants.version1;
            case ("v2c"):
                return SnmpConstants.version2c;
            case ("v3"):
                return SnmpConstants.version3;
            default:
                return -1;
        }
    }
}

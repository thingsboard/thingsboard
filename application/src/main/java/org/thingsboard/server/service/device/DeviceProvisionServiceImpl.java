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
package org.thingsboard.server.service.device;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.cluster.SendToClusterMsg;
import org.thingsboard.server.common.msg.system.ServiceToRuleEngineMsg;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProvisionService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.device.provision.ProvisionProfile;
import org.thingsboard.server.dao.device.provision.ProvisionRequest;
import org.thingsboard.server.dao.device.provision.ProvisionResponse;
import org.thingsboard.server.dao.device.provision.ProvisionResponseStatus;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class DeviceProvisionServiceImpl implements DeviceProvisionService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ReentrantLock deviceCreationLock = new ReentrantLock();
    private final Set<ProvisionProfile> set = ConcurrentHashMap.newKeySet();

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceCredentialsService deviceCredentialsService;

    @Autowired
    private ActorService actorService;

    @Override
    public ProvisionProfile saveProvisionProfile(ProvisionProfile provisionProfile) {
        boolean added = set.add(provisionProfile);
        if (!added) {
            log.debug("[{}] The profile has already been added!", provisionProfile);
            throw new RuntimeException("The profile has already been added!");
        }
        return provisionProfile;
    }

    @Override
    public ProvisionResponse provisionDevice(ProvisionRequest provisionRequest) {
        ProvisionProfile targetProfile = null;
        for (ProvisionProfile profile : set) {
            if (profile.getCredentials().equals(provisionRequest.getCredentials())) {
                log.debug("[{}] Found provision profile!", profile);
                targetProfile = profile;
                break;
            }
        }
        if (targetProfile == null) {
            return new ProvisionResponse(null, ProvisionResponseStatus.NOT_FOUND);
        }
        Device device = getOrCreateDevice(provisionRequest, targetProfile);
        set.remove(targetProfile);
        return new ProvisionResponse(deviceCredentialsService.findDeviceCredentialsByDeviceId(device.getTenantId(), device.getId()), ProvisionResponseStatus.SUCCESS);
    }

    private Device getOrCreateDevice(ProvisionRequest provisionRequest, ProvisionProfile profile) {
        Device device = deviceService.findDeviceByTenantIdAndName(profile.getTenantId(), provisionRequest.getDeviceName());
        if (device == null) {
            deviceCreationLock.lock();
            try {
                return processGetOrCreateDevice(provisionRequest, profile);
            } finally {
                deviceCreationLock.unlock();
            }
        }
        return device;
    }

    private Device processGetOrCreateDevice(ProvisionRequest provisionRequest, ProvisionProfile profile) {
        Device device = deviceService.findDeviceByTenantIdAndName(profile.getTenantId(), provisionRequest.getDeviceName());
        if (device == null) {
            device = new Device();
            device.setName(provisionRequest.getDeviceName());
            device.setType(provisionRequest.getDeviceType());
            device.setTenantId(profile.getTenantId());
            if (profile.getCustomerId() != null) {
                device.setCustomerId(profile.getCustomerId());
            }
            device = deviceService.saveDevice(device);

            actorService.onDeviceAdded(device);
            pushDeviceCreatedEventToRuleEngine(device);

            if (!StringUtils.isEmpty(provisionRequest.getX509CertPubKey())) {
                DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(profile.getTenantId(), device.getId());
                deviceCredentials.setCredentialsType(DeviceCredentialsType.X509_CERTIFICATE);
                deviceCredentials.setCredentialsValue(provisionRequest.getX509CertPubKey());
                deviceCredentialsService.updateDeviceCredentials(profile.getTenantId(), deviceCredentials);
            }
        }
        return device;
    }

    private void pushDeviceCreatedEventToRuleEngine(Device device) {
        try {
            ObjectNode entityNode = mapper.valueToTree(device);
            TbMsg msg = new TbMsg(UUIDs.timeBased(), DataConstants.ENTITY_CREATED, device.getId(), deviceActionTbMsgMetaData(device), mapper.writeValueAsString(entityNode), null, null, 0L);
            actorService.onMsg(new SendToClusterMsg(device.getId(), new ServiceToRuleEngineMsg(device.getTenantId(), msg)));
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("[{}] Failed to push device action to rule engine: {}", device.getId(), DataConstants.ENTITY_CREATED, e);
        }
    }

    private TbMsgMetaData deviceActionTbMsgMetaData(Device device) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("tenantId", device.getTenantId().toString());
        CustomerId customerId = device.getCustomerId();
        if (customerId != null && !customerId.isNullUid()) {
            metaData.putValue("customerId", customerId.toString());
        }
        return metaData;
    }
}

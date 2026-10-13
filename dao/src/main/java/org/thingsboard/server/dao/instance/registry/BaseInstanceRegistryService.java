// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.instance.registry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.license.client.InstanceRegistry;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaseInstanceRegistryService implements InstanceRegistryService {

    private final InstanceRegistryDao instanceRegistryDao;

    @Override
    public InstanceRegistry save(InstanceRegistry instanceRegistry) {
        return instanceRegistryDao.save(instanceRegistry);
    }

    @Override
    public InstanceRegistry findByServiceId(String serviceId) {
        return instanceRegistryDao.findByServiceId(serviceId);
    }

    @Override
    public List<InstanceRegistry> findAll() {
        return instanceRegistryDao.findAll();
    }

    @Override
    public void deleteByServiceId(String serviceId) {
        instanceRegistryDao.deleteByServiceId(serviceId);
    }

}

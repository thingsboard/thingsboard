// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.instance.registry;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.license.client.InstanceRegistry;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.instance.registry.InstanceRegistryDao;
import org.thingsboard.server.dao.model.sql.InstanceRegistryEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;

@Component
@SqlDao
@RequiredArgsConstructor
public class JpaInstanceRegistryDao implements InstanceRegistryDao {

    private final InstanceRegistryRepository instanceRegistryRepository;

    @Override
    public InstanceRegistry save(InstanceRegistry instanceRegistry) {
        return DaoUtil.getData(instanceRegistryRepository.save(new InstanceRegistryEntity(instanceRegistry)));
    }

    @Override
    public InstanceRegistry findByServiceId(String serviceId) {
        return DaoUtil.getData(instanceRegistryRepository.findById(serviceId));
    }

    @Override
    public List<InstanceRegistry> findAll() {
        return DaoUtil.convertDataList(instanceRegistryRepository.findAll());
    }

    @Override
    public void deleteByServiceId(String serviceId) {
        instanceRegistryRepository.deleteById(serviceId);
    }

}

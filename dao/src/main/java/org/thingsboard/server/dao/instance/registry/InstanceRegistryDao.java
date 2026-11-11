// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.instance.registry;

import org.thingsboard.license.client.InstanceRegistry;

import java.util.List;

public interface InstanceRegistryDao {
    InstanceRegistry save(InstanceRegistry instanceRegistry);

    InstanceRegistry findByServiceId(String serviceId);

    List<InstanceRegistry> findAll();

    void deleteByServiceId(String serviceId);

}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.instance.registry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.thingsboard.server.dao.model.sql.InstanceRegistryEntity;

public interface InstanceRegistryRepository extends JpaRepository<InstanceRegistryEntity, String> {

}

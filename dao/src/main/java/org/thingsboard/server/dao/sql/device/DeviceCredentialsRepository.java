package org.thingsboard.server.dao.sql.device;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.dao.model.sql.DeviceCredentialsEntity;

import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@ConditionalOnProperty(prefix = "sql", value = "enabled", havingValue = "true", matchIfMissing = false)
public interface DeviceCredentialsRepository extends CrudRepository<DeviceCredentialsEntity, UUID> {

    DeviceCredentialsEntity findByDeviceId(UUID deviceId);

    DeviceCredentialsEntity findByCredentialsId(String credentialsId);
}

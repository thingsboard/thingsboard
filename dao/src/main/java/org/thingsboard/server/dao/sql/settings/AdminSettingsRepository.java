package org.thingsboard.server.dao.sql.settings;

import org.springframework.data.repository.CrudRepository;
import org.thingsboard.server.dao.model.ToData;
import org.thingsboard.server.dao.model.sql.AdminSettingsEntity;

import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public interface AdminSettingsRepository extends CrudRepository<AdminSettingsEntity, UUID> {

    AdminSettingsEntity findByKey(String key);
}

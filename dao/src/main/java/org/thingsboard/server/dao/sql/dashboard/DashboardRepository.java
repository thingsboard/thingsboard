package org.thingsboard.server.dao.sql.dashboard;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.repository.CrudRepository;
import org.thingsboard.server.dao.model.sql.DashboardEntity;

import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@ConditionalOnProperty(prefix = "sql", value = "enabled", havingValue = "true", matchIfMissing = false)
public interface DashboardRepository extends CrudRepository<DashboardEntity, UUID> {
}

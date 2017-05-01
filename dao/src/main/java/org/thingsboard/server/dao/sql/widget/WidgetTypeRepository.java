package org.thingsboard.server.dao.sql.widget;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.repository.CrudRepository;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.dao.model.sql.WidgetTypeEntity;

import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 4/29/2017.
 */
@ConditionalOnProperty(prefix = "sql", value = "enabled", havingValue = "true", matchIfMissing = false)
public interface WidgetTypeRepository extends CrudRepository<WidgetTypeEntity, UUID> {

    List<WidgetTypeEntity> findByTenantIdAndBundleAlias(UUID tenantId, String bundleAlias);

    WidgetTypeEntity findByTenantIdAndBundleAliasAndAlias(UUID tenantId, String bundleAlias, String alias);
}

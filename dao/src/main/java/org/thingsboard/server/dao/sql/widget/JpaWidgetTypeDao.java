package org.thingsboard.server.dao.sql.widget;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.WidgetTypeEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.widget.WidgetTypeDao;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.WIDGET_TYPE_COLUMN_FAMILY_NAME;

/**
 * Created by Valerii Sosliuk on 4/29/2017.
 */
@Component
@ConditionalOnProperty(prefix = "sql", value = "enabled", havingValue = "true", matchIfMissing = false)
public class JpaWidgetTypeDao extends JpaAbstractDao<WidgetTypeEntity, WidgetType> implements WidgetTypeDao {

    @Autowired
    private WidgetTypeRepository widgetTypeRepository;

    @Override
    protected Class<WidgetTypeEntity> getEntityClass() {
        return WidgetTypeEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return WIDGET_TYPE_COLUMN_FAMILY_NAME;
    }

    @Override
    protected CrudRepository<WidgetTypeEntity, UUID> getCrudRepository() {
        return widgetTypeRepository;
    }

    @Override
    public List<WidgetType> findWidgetTypesByTenantIdAndBundleAlias(UUID tenantId, String bundleAlias) {
        return DaoUtil.convertDataList(widgetTypeRepository.findByTenantIdAndBundleAlias(tenantId, bundleAlias));
    }

    @Override
    public WidgetType findByTenantIdBundleAliasAndAlias(UUID tenantId, String bundleAlias, String alias) {
        return DaoUtil.getData(widgetTypeRepository.findByTenantIdAndBundleAliasAndAlias(tenantId, bundleAlias, alias));
    }
}

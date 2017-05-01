package org.thingsboard.server.dao.sql.tenant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.TenantEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.TENANT_COLUMN_FAMILY_NAME;

/**
 * Created by Valerii Sosliuk on 4/30/2017.
 */
@Component
@ConditionalOnProperty(prefix = "sql", value = "enabled", havingValue = "true", matchIfMissing = false)
public class JpaTenantDao extends JpaAbstractDao<TenantEntity, Tenant> implements TenantDao {

    @Autowired
    private TenantRepository tenantRepository;

    @Override
    protected Class<TenantEntity> getEntityClass() {
        return TenantEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return TENANT_COLUMN_FAMILY_NAME;
    }

    @Override
    protected CrudRepository<TenantEntity, UUID> getCrudRepository() {
        return tenantRepository;
    }

    @Override
    protected boolean isSearchTextDao() {
        return true;
    }

    @Override
    public List<Tenant> findTenantsByRegion(String region, TextPageLink pageLink) {
        if (pageLink.getIdOffset() == null) {
            return DaoUtil.convertDataList(tenantRepository.findByRegionFirstPage(pageLink.getLimit(), region, pageLink.getTextSearch()));
        } else {
            return DaoUtil.convertDataList(tenantRepository.findByRegionNextPage(pageLink.getLimit(), region, pageLink.getTextSearch(), pageLink.getIdOffset()));
        }
    }
}

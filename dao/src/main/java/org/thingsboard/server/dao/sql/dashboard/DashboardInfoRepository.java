package org.thingsboard.server.dao.sql.dashboard;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.thingsboard.server.dao.model.sql.DashboardInfoEntity;

import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@ConditionalOnProperty(prefix = "sql", value = "enabled", havingValue = "true", matchIfMissing = false)
public interface DashboardInfoRepository extends CrudRepository<DashboardInfoEntity, UUID> {

    @Query(nativeQuery = true, value = "SELECT * FROM DASHBOARD WHERE TENANT_ID = ?2 " +
            "AND LOWER(SEARCH_TEXT) LIKE LOWER(CONCAT(?3, '%')) " +
            "ORDER BY ID LIMIT ?1")
    List<DashboardInfoEntity> findByTenantIdFirstPage(int limit, UUID tenantId, String textSearch);

    @Query(nativeQuery = true, value = "SELECT * FROM DASHBOARD WHERE TENANT_ID = ?2 " +
            "AND LOWER(SEARCH_TEXT) LIKE LOWER(CONCAT(?3, '%')) " +
            "AND ID > ?4 ORDER BY ID LIMIT ?1")
    List<DashboardInfoEntity> findByTenantIdNextPage(int limit, UUID tenantId, String textSearch, UUID idOffset);

    @Query(nativeQuery = true, value = "SELECT * FROM DASHBOARD WHERE TENANT_ID = ?2 " +
            "AND CUSTOMER_ID = ?3 AND LOWER(SEARCH_TEXT) LIKE LOWER(CONCAT(?4, '%')) " +
            "ORDER BY ID LIMIT ?1")
    List<DashboardInfoEntity> findByTenantIdAndCustomerIdFirstPage(int limit, UUID tenantId, UUID customerId, String textSearch);

    @Query(nativeQuery = true, value = "SELECT * FROM DASHBOARD WHERE TENANT_ID = ?2 " +
            "AND CUSTOMER_ID = ?3 AND LOWER(SEARCH_TEXT) LIKE LOWER(CONCAT(?4, '%')) " +
            "AND ID > ?5 ORDER BY ID LIMIT ?1")
    List<DashboardInfoEntity> findByTenantIdAndCustomerIdNextPage(int limit, UUID tenantId, UUID customerId, String textSearch, UUID idOffset);
}

package org.thingsboard.server.dao.sql.device;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.concurrent.ListenableFuture;
import org.thingsboard.server.dao.model.sql.DeviceEntity;

import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@ConditionalOnProperty(prefix = "sql", value = "enabled", havingValue = "true", matchIfMissing = false)
public interface DeviceRepository extends CrudRepository<DeviceEntity, UUID> {

    @Query(nativeQuery = true, value = "SELECT * FROM DEVICE WHERE TENANT_ID = ?2 " +
            "AND CUSTOMER_ID = ?3 " +
            "AND LOWER(SEARCH_TEXT) LIKE LOWER(CONCAT(?4, '%')) " +
            "ORDER BY ID LIMIT ?1")
    List<DeviceEntity> findByTenantIdAndCustomerIdFirstPage(int limit, UUID tenantId, UUID customerId, String searchText);

    @Query(nativeQuery = true, value = "SELECT * FROM DEVICE WHERE TENANT_ID = ?2 " +
            "AND CUSTOMER_ID = ?3 " +
            "AND LOWER(SEARCH_TEXT) LIKE LOWER(CONCAT(?4, '%')) " +
            "AND ID > ?5 ORDER BY ID LIMIT ?1")
    List<DeviceEntity> findByTenantIdAndCustomerIdNextPage(int limit, UUID tenantId, UUID customerId, String searchText, UUID idOffset);

    @Query(nativeQuery = true, value = "SELECT * FROM DEVICE WHERE TENANT_ID = ?2 " +
            "AND LOWER(SEARCH_TEXT) LIKE LOWER(CONCAT(?3, '%')) " +
            "ORDER BY ID LIMIT ?1")
    List<DeviceEntity> findByTenantIdFirstPage(int limit, UUID tenantId, String textSearch);

    @Query(nativeQuery = true, value = "SELECT * FROM DEVICE WHERE TENANT_ID = ?2 " +
            "AND LOWER(SEARCH_TEXT) LIKE LOWER(CONCAT(?3, '%')) " +
            "AND ID > ?4 ORDER BY ID LIMIT ?1")
    List<DeviceEntity> findByTenantIdNextPage(int limit, UUID tenantId, String textSearch, UUID idOffset);

    DeviceEntity findByTenantIdAndName(UUID tenantId, String name);

    List<DeviceEntity> findDevicesByTenantIdAndCustomerIdAndIdIn(UUID tenantId, UUID customerId, List<UUID> deviceIds);

    List<DeviceEntity> findDevicesByTenantId(UUID tenantId);
    List<DeviceEntity> findDevicesByTenantIdAndIdIn(UUID tenantId, List<UUID> deviceIds);
}

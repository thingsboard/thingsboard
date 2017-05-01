package org.thingsboard.server.dao.sql.tenant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.thingsboard.server.dao.model.sql.TenantEntity;

import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 4/30/2017.
 */
@ConditionalOnProperty(prefix = "sql", value = "enabled", havingValue = "true", matchIfMissing = false)
public interface TenantRepository extends CrudRepository<TenantEntity, UUID> {


    @Query(nativeQuery = true, value = "SELECT * FROM TENANT WHERE REGION = ?2 " +
            "AND LOWER(SEARCH_TEXT) LIKE LOWER(CONCAT(?3, '%')) " +
            "ORDER BY ID LIMIT ?1")
    List<TenantEntity> findByRegionFirstPage(int limit, String region, String textSearch);


    @Query(nativeQuery = true, value = "SELECT * FROM TENANT WHERE REGION = ?2 " +
            "AND LOWER(SEARCH_TEXT) LIKE LOWER(CONCAT(?3, '%')) " +
            "AND ID > ?4 ORDER BY ID LIMIT ?1")
    List<TenantEntity> findByRegionNextPage(int limit, String region, String textSearch, UUID idOffset);
}

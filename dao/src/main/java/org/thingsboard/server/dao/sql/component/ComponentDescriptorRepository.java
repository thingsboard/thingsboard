package org.thingsboard.server.dao.sql.component;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.dao.model.ToData;
import org.thingsboard.server.dao.model.sql.ComponentDescriptorEntity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@ConditionalOnProperty(prefix = "sql", value = "enabled", havingValue = "true", matchIfMissing = false)
public interface ComponentDescriptorRepository extends CrudRepository<ComponentDescriptorEntity, UUID> {

    ComponentDescriptorEntity findByClazz(String clazz);

    @Query(nativeQuery = true, value = "SELECT * FROM COMPONENT_DESCRIPTOR WHERE TYPE = ?2 " +
            "AND LOWER(SEARCH_TEXT) LIKE LOWER(CONCAT(?3, '%')) " +
            "ORDER BY ID LIMIT ?1")
    List<ComponentDescriptorEntity> findByTypeFirstPage(int limit, int type, String textSearch);

    @Query(nativeQuery = true, value = "SELECT * FROM COMPONENT_DESCRIPTOR WHERE TYPE = ?2 " +
            "AND LOWER(SEARCH_TEXT) LIKE LOWER(CONCAT(?3, '%')) " +
            "AND ID > ?4 ORDER BY ID LIMIT ?1")
    List<ComponentDescriptorEntity> findByTypeNextPage(int limit, int type, String textSearch, UUID idOffset);

    @Query(nativeQuery = true, value = "SELECT * FROM COMPONENT_DESCRIPTOR WHERE TYPE = ?2 " +
            "AND SCOPE = ?3 AND LOWER(SEARCH_TEXT) LIKE LOWER(CONCAT(?4, '%')) " +
            "ORDER BY ID LIMIT ?1")
    List<ComponentDescriptorEntity> findByScopeAndTypeFirstPage(int limit, int type, int scope, String textSearch);

    @Query(nativeQuery = true, value = "SELECT * FROM COMPONENT_DESCRIPTOR WHERE TYPE = ?2 " +
            "AND SCOPE = ?3 AND LOWER(SEARCH_TEXT) LIKE LOWER(CONCAT(?4, '%')) " +
            "AND ID > ?5 ORDER BY ID LIMIT ?1")
    List<ComponentDescriptorEntity> findByScopeAndTypeNextPage(int limit, int type, int scope, String textSearch, UUID idOffset);

    void deleteByClazz(String clazz);
}

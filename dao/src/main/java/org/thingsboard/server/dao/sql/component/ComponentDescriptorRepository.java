// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.component;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.dao.model.sql.ComponentDescriptorEntity;

import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public interface ComponentDescriptorRepository extends JpaRepository<ComponentDescriptorEntity, UUID> {

    ComponentDescriptorEntity findByClazz(String clazz);

    @Query("SELECT cd FROM ComponentDescriptorEntity cd WHERE cd.type = :type " +
            "AND (:textSearch IS NULL OR ilike(cd.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<ComponentDescriptorEntity> findByType(@Param("type") ComponentType type,
                                               @Param("textSearch") String textSearch,
                                               Pageable pageable);

    @Query("SELECT cd FROM ComponentDescriptorEntity cd WHERE cd.type = :type AND cd.scope = :scope " +
            "AND (:textSearch IS NULL OR ilike(cd.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<ComponentDescriptorEntity> findByScopeAndType(@Param("type") ComponentType type,
                                                       @Param("scope") ComponentScope scope,
                                                       @Param("textSearch") String textSearch,
                                                       Pageable pageable);

    @Transactional
    @Modifying
    @Query("DELETE FROM ComponentDescriptorEntity cd where cd.clazz = :clazz")
    void deleteByClazz(@Param("clazz") String clazz);
}

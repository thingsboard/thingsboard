// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.extension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.ExtensionEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ExtensionRepository extends JpaRepository<ExtensionEntity, UUID>, ExportableEntityRepository<ExtensionEntity> {

    Optional<ExtensionEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    ExtensionEntity findBySlug(String slug);

    ExtensionEntity findByTenantIdAndName(UUID tenantId, String name);

    // Bulk read by primary key for the status collector, which otherwise issued one SELECT per registered
    // extension on every poll. Callers must re-check the tenant of every row: this query is not scoped.
    List<ExtensionEntity> findByIdIn(Collection<UUID> ids);

    ExtensionEntity findByTenantIdAndExternalId(UUID tenantId, UUID externalId);

    // Lists extensions whose container spec references a given secret placeholder; powers secret
    // deletion-protection (SecretServiceImpl.findEntitiesBySecret) via HasSecretsEntityDao.
    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(e.id, 'EXTENSION', e.name) " +
           "FROM ExtensionEntity e WHERE e.tenantId = :tenantId AND ilike(e.containerSpec, CONCAT('%', :placeholder, '%'))")
    List<EntityInfo> findByTenantIdAndSecretPlaceholder(@Param("tenantId") UUID tenantId,
                                                        @Param("placeholder") String placeholder);

    @Query("SELECT e FROM ExtensionEntity e WHERE e.tenantId = :tenantId " +
           "AND (:textSearch IS NULL OR ilike(e.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<ExtensionEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                         @Param("textSearch") String textSearch,
                                         Pageable pageable);

    @Query("SELECT e.id FROM ExtensionEntity e WHERE e.tenantId = :tenantId")
    Page<UUID> findIdsByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT e.externalId FROM ExtensionEntity e WHERE e.id = :id")
    Optional<UUID> getExternalIdById(@Param("id") UUID id);

    Long countByTenantId(UUID tenantId);

    @Transactional
    @Modifying
    @Query("DELETE FROM ExtensionEntity e WHERE e.tenantId = :tenantId AND e.id IN (:ids)")
    int deleteByTenantIdAndIdIn(@Param("tenantId") UUID tenantId, @Param("ids") Set<UUID> ids);

    // Compare-and-set on state, backing ExtensionService.updateStateIf. The status poller and the
    // reconciler both decide on a snapshot taken before a slow orchestrator call, so their write must
    // re-check the state it was derived from; a read-modify-write cannot do that safely, because under
    // READ COMMITTED a user transition can commit between the check and the write, and a JVM-local
    // lock cannot serialize core nodes. A single conditional statement is atomic by itself, so no row
    // lock is held. Native SQL because a JPQL bulk update cannot assign the @Version column, which is
    // bumped here so entity versioning stays consistent with the normal save path (COALESCE guards the
    // nullable column). Returns 0 when the row has moved on.
    @Transactional
    @Modifying
    @Query(value = "UPDATE extension SET state = :newState, last_error = :lastError, last_error_at = :lastErrorAt, " +
                   "version = COALESCE(version, 0) + 1 " +
                   "WHERE id = :id AND tenant_id = :tenantId AND state = :expectedState", nativeQuery = true)
    int updateStateIf(@Param("tenantId") UUID tenantId,
                      @Param("id") UUID id,
                      @Param("expectedState") String expectedState,
                      @Param("newState") String newState,
                      @Param("lastError") String lastError,
                      @Param("lastErrorAt") Long lastErrorAt);

}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * Common contract of a versioned batch upsert repository: the same entity batch can be flushed either through the
 * repository's own (coordinator) {@code JdbcTemplate} or through a caller-supplied target one (used by Citus smart
 * routing to write straight to the worker owning a shard). Both {@code attribute_kv} and {@code ts_kv_latest}
 * repositories expose this pair, so the shared KV write-queue wiring can accept the repository once and pick the
 * overload for the active mode instead of being handed two loose method references.
 *
 * @param <E> the entity type persisted by this repository
 */
public interface VersionedInsertRepository<E> {

    List<Long> saveOrUpdate(List<E> entities);

    List<Long> saveOrUpdate(JdbcTemplate target, List<E> entities);

}

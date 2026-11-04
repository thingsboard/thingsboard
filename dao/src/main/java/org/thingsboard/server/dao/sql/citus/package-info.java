// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
/**
 * Citus (distributed PostgreSQL) support for the SQL DAO layer.
 *
 * <h2>Static audit: no KV write shares a transaction with a reference-table write</h2>
 *
 * <p><b>Topology (see {@link CitusTables} for the authoritative lists).</b> Under Citus the KV fact
 * tables {@code attribute_kv} and {@code ts_kv_latest} are hash-distributed by {@code entity_id};
 * {@code device}, {@code asset} and {@code entity_view} are hash-distributed on their {@code id}, and
 * {@code alarm} and {@code entity_alarm} are hash-distributed on {@code originator_id} — all
 * co-located with the KV anchor group. Roughly ~25 dimension tables (including {@code key_dictionary},
 * {@code relation}, the profile tables, and the rest of {@link CitusTables#REFERENCE_TABLES}) are
 * converted to replicated reference tables. The partitioned {@code blob_entity}, {@code report} and
 * {@code alarm_comment} stay plain coordinator-local tables.
 *
 * <p><b>What was audited.</b> A single database transaction that writes BOTH a distributed KV table AND
 * a reference table would be promoted to a distributed (2PC) transaction across worker nodes. This
 * audit confirms that the hot write path for the two KV tables never mixes a KV-table write with a
 * reference-table (e.g. {@code key_dictionary}) write inside one transaction.
 *
 * <h3>Enumerated KV write paths (each single-table)</h3>
 * <ul>
 *   <li><b>{@code attribute_kv} upsert.</b> {@code JpaAttributeDao#save} builds an
 *       {@code AttributeKvEntity} and hands it to a {@code TbSqlBlockingQueueWrapper} via
 *       {@code addToQueue}. The queue worker ({@code TbSqlBlockingQueue}) invokes the registered
 *       save function {@code AttributeKvInsertRepository#saveOrUpdate} (inherited from
 *       {@code AbstractVersionedInsertRepository}). That method opens the only transaction
 *       ({@code transactionTemplate.execute}) and runs solely the batch UPDATE / INSERT ... ON
 *       CONFLICT statements against {@code attribute_kv}. No other table is touched in that scope.</li>
 *   <li><b>{@code ts_kv_latest} upsert.</b> {@code SqlTimeseriesLatestDao#getSaveLatestFuture}
 *       (reached from {@code saveLatest}) builds a {@code TsKvLatestEntity} and enqueues it on its
 *       own {@code TbSqlBlockingQueueWrapper}. The queue worker invokes
 *       {@code SqlLatestInsertTsRepository#saveOrUpdate} (also from
 *       {@code AbstractVersionedInsertRepository}), whose single {@code transactionTemplate.execute}
 *       runs only batch UPDATE / INSERT ... ON CONFLICT statements against {@code ts_kv_latest}.</li>
 *   <li><b>{@code attribute_kv} deletes.</b> {@code JpaAttributeDao#removeAllWithVersions} and
 *       {@code JpaAttributeDao#removeAllByEntityId} run a {@code DELETE FROM attribute_kv ...} (the
 *       latter annotated {@code @Transactional}); both touch only {@code attribute_kv}.</li>
 *   <li><b>{@code ts_kv_latest} delete.</b> {@code SqlTimeseriesLatestDao#getRemoveLatestFuture}
 *       runs a {@code DELETE FROM ts_kv_latest ...} inside {@code transactionTemplate.execute}
 *       touching only {@code ts_kv_latest}.</li>
 * </ul>
 *
 * <p><b>Isolation through the blocking queue.</b> {@code TbSqlBlockingQueueWrapper#add} routes the
 * entity to a {@code TbSqlBlockingQueue}, whose worker loop calls {@code saveFunction.apply(...)}
 * directly with no enclosing transaction. The only transactional boundary in the save path is the
 * per-repository {@code AbstractVersionedInsertRepository#saveOrUpdate}, which issues statements
 * against exactly one KV table. The key_dictionary lookup/insert ({@code keyDictionaryDao
 * .getOrSaveKeyId}, a reference table) is resolved on the caller thread BEFORE the entity is
 * enqueued, so it is never part of the queued KV transaction.
 *
 * <h3>Conclusion</h3>
 * <p><b>Audit clean.</b> No service or DAO method writes a KV table and a reference table in the
 * same transaction. Every {@code attribute_kv} / {@code ts_kv_latest} write enumerated above is
 * single-table, and the queued upsert path is isolated by {@code TbSqlBlockingQueueWrapper} /
 * {@code TbSqlBlockingQueue}.
 *
 * <p>This single-table property is what keeps hot-path KV writes single-shard under Citus
 * (no cross-node 2PC / distributed transaction).
 *
 * <h2>Version application: per-row versions and the delete-tombstone fix</h2>
 *
 * <p><b>Background.</b> The {@code version} column on {@code attribute_kv} / {@code ts_kv_latest}
 * is used by EDQS ({@code VersionsStore}) and by the versioned cache
 * ({@code VersionedTbCache}) to apply the newest write per object key and reject outdated ones.
 * In plain mode {@code version} comes from a single global sequence ({@code nextval(..._version_seq)})
 * and is globally monotonic. In Citus mode that sequence cannot be shared cheaply across shards, so
 * upserts use a PER-ROW increment ({@code table.version + 1}, starting at 1) — monotonic per key but
 * not global.
 *
 * <p><b>The delete-tombstone problem.</b> Both EDQS and the versioned cache treat a delete as a
 * version-tagged tombstone: a later write only wins if its version exceeds the tombstone's. With
 * per-row versions, a delete followed by a re-create of the same key produces a re-create whose
 * version restarts low and would be wrongly rejected (re-created data invisible) until the cache TTL
 * expires. (In plain mode the global sequence makes the re-create's version exceed the tombstone, so
 * the problem does not arise.)
 *
 * <p><b>The fix (clear-on-delete).</b>
 * <ul>
 *   <li><b>Delete-SQL version source.</b> {@code JpaAttributeDao#removeAllWithVersions} and
 *       {@code SqlTimeseriesLatestDao#getRemoveLatestFuture} return the deleted row's own per-row
 *       {@code version} ({@code RETURNING version}) in Citus mode instead of
 *       {@code RETURNING nextval(..._version_seq)} — dropping the global sequence from the Citus
 *       delete path. Plain mode is unchanged.</li>
 *   <li><b>Versioned cache.</b> In Citus mode {@code CachedAttributesService#removeAll} and
 *       {@code CachedRedisSqlTimeseriesLatestDao#removeLatest} call the unversioned
 *       {@code cache.evict(key)} (full clear) instead of {@code cache.evict(key, version)} (a
 *       version-tagged tombstone), so a re-create is not blocked. Plain mode is unchanged.</li>
 *   <li><b>EDQS.</b> {@code VersionsStore#remove} is called on an accepted delete (in
 *       {@code EdqsProcessor#process} and the {@code KafkaEdqsStateService} backup consumer) so the
 *       key's stored version is cleared rather than retaining the delete's version. This is applied
 *       UNCONDITIONALLY (all modes, no Citus flag): EDQS delivers a given key's events in order
 *       (single partition per entity), so a stale out-of-order delete is still rejected by the
 *       {@code isNew} gate before {@code remove} is reached, and clearing on an accepted delete is
 *       safe. Because EDQS is not Citus-aware, no Citus flag needs to reach the standalone EDQS
 *       service.</li>
 * </ul>
 *
 * <h3>Residual limitation: concurrent delete + re-create race</h3>
 * <p>Dropping the global sequence removes the cross-key order oracle it provided. For the COMMON
 * case — a sequential delete then re-create of a key — clear-on-delete is correct. For a genuinely
 * CONCURRENT delete and re-create of the same key, EDQS / the cache can momentarily settle on the
 * wrong final state (the deletes run on the caller thread while saves run through the async write
 * queue, so their commit/notify order is not guaranteed, and per-row versions are no longer globally
 * comparable across the delete boundary). This is an accepted residual limitation under Citus; it
 * self-heals once the EDQS versions-cache / cache evict TTL expires. Plain mode is unaffected because
 * its global sequence remains a true order oracle.
 */
package org.thingsboard.server.dao.sql.citus;

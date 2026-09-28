// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class DefaultCitusSchemaService implements CitusSchemaService {

    // The entity_alarm -> alarm foreign key. Both tables are distributed on originator_id, so this FK must be the
    // co-located composite form (originator_id, alarm_id) -> alarm(originator_id, id); the original single-column
    // (alarm_id) -> alarm(id) form is invalid once alarm's PK is (originator_id, id). It is therefore excluded from
    // the generic capture/re-add path (which would re-add the stale single-column def verbatim and fail) and
    // re-added explicitly in its composite form after both tables are distributed. See applyDistribution().
    private static final String ENTITY_ALARM_FK = "fk_entity_alarm_id";
    private static final String ENTITY_ALARM_TABLE = "entity_alarm";

    // SQL expression building a foreign key's structural identity (child|parent|ordered-child-columns) from
    // pg_constraint row "con" joined to pg_class aliases "child" and "parent". Its '|'/',' format mirrors
    // CitusTables#foreignKeyStructuralKey and must stay in sync with it. Shared by existingForeignKeyStructures()
    // and dropForeignKeys() so a format change cannot update one query and miss the other.
    private static final String FK_STRUCTURAL_KEY_SQL =
            "child.relname || '|' || parent.relname || '|' || " +
                    "(select string_agg(a.attname::text, ',' order by cols.ord) " +
                    " from unnest(con.conkey) with ordinality as cols(attnum, ord) " +
                    " join pg_attribute a on a.attrelid = con.conrelid and a.attnum = cols.attnum)";

    private final JdbcTemplate jdbcTemplate;
    private final CitusSettings settings;

    @Override
    public void applyDistribution() {
        log.info("Applying Citus distribution (shardCount={})", settings.getShardCount());

        Set<String> alreadyDistributed = currentlyDistributed();

        List<String> managedTables = new ArrayList<>(distributedTables());
        managedTables.addAll(referenceTables());

        guardAgainstPartialState(managedTables, alreadyDistributed);
        guardAgainstMissingAlarmUpgrade(alreadyDistributed);

        // Crash-recovery FK repair, run before anything is dropped. Two crash windows slip past the partial-state
        // guard above (it needs at least one managed table distributed AND one not): a crash during the prior run's
        // final FK re-add loop leaves EVERYTHING distributed, and a crash after dropManagedForeignKeys() but before
        // the first successful conversion leaves NOTHING distributed — that re-run looks like a clean first run, its
        // own drop phase finds (and captures) nothing because the FKs are already gone, and the managed FKs would be
        // silently lost for good. readdMissingForeignKeys() re-derives the expected set from the static catalog and
        // is an idempotent no-op when the FKs are present, so it is harmless on a genuinely clean first run too
        // (plain-Postgres FKs between still-local tables are legal). See its javadoc for the full rationale.
        // NOTE: the catalog-based repair covers ONLY the managed FKs declared in CitusTables.MANAGED_FOREIGN_KEYS.
        // An operator-added (non-catalog) FK whose child is a managed table is dropped by dropForeignKeys() like any
        // other, but in these two crash windows there is no source to restore it from — it is permanently and
        // silently lost (see readdMissingForeignKeys()). Operators must capture custom FKs before converting.
        readdMissingForeignKeys();
        if (primaryKeyIncludesColumn("alarm", "originator_id")) {
            // The composite entity_alarm -> alarm FK is repaired the same way but out of band (it is not in the
            // managed-FK catalog) and, unlike the catalog FKs, it is NOT valid in every state this repair can run
            // in: it references alarm(originator_id, id), which only exists once rewriteAlarmGroupPrimaryKeys() has
            // widened alarm's primary key. Before that (a clean first run, or a crash before the PK rewrite) the
            // composite form cannot exist or be added, and skipping loses nothing — the re-add at the end of this
            // run rebuilds it after the PK rewrite.
            readdEntityAlarmCompositeForeignKey();
        }

        // Must stay above the fully-distributed early return below: the indexes are independent of the conversion
        // and have to reach already-converted clusters too, not just first-time conversions.
        ensureCitusOnlyIndexes();

        if (managedTables.stream().allMatch(alreadyDistributed::contains)) {
            // Fully-distributed re-run: the normal phases below would all be no-ops (every table is skipped via
            // alreadyDistributed), and the crash repair above has already reconciled any FK lost mid re-add.
            log.info("Citus distribution already applied");
            return;
        }

        List<String> foreignKeysToReadd = dropManagedForeignKeys(managedTables, alreadyDistributed);
        dropIncompatibleUniques(alreadyDistributed);
        rewriteAlarmGroupPrimaryKeys(alreadyDistributed);
        createReferenceTables(alreadyDistributed);
        createDistributedTables(alreadyDistributed);
        readdForeignKeys(foreignKeysToReadd);
        readdEntityAlarmCompositeForeignKey();

        log.info("Citus distribution applied");
    }

    /**
     * Fails loudly if the database is in a partial/inconsistent distribution state from a crashed prior run.
     *
     * <p>This conversion is NOT crash-safe: {@link #dropManagedForeignKeys} tears down every managed FK up front
     * and they are only re-added at the very end. A crash in between leaves some managed tables already
     * distributed (so the conversion phases skip them, never re-capturing their FKs) while their dropped FKs are
     * gone for good — a permanent, silent FK loss. We do not attempt to persist/replay the drop; instead we
     * detect that exact partial state and fail loudly. The signature is: at least one managed table is already
     * distributed (a prior run started) AND at least one not-yet-distributed managed table remains (the prior run
     * did not finish). On a clean DB nothing is distributed; on a fully applied DB everything is, so a normal
     * idempotent re-run still passes this guard. The two crash gaps that share those signatures (everything
     * distributed, or nothing distributed yet) are repaired by the start-of-run {@link #readdMissingForeignKeys()}
     * call instead — but ONLY for the catalog-declared managed FKs: an operator-added (non-catalog) FK on a
     * managed table that was already dropped in either gap has no source to be restored from and is permanently,
     * silently lost (see {@link #readdMissingForeignKeys()}).
     */
    private void guardAgainstPartialState(List<String> managedTables, Set<String> alreadyDistributed) {
        boolean anyDistributed = managedTables.stream().anyMatch(alreadyDistributed::contains);
        boolean anyNotDistributed = managedTables.stream().anyMatch(t -> !alreadyDistributed.contains(t));
        if (anyDistributed && anyNotDistributed) {
            List<String> distributed = managedTables.stream().filter(alreadyDistributed::contains).toList();
            List<String> remaining = managedTables.stream().filter(t -> !alreadyDistributed.contains(t)).toList();
            throw new IllegalStateException(
                    "Citus distribution is in a partial/inconsistent state: a previous applyDistribution() run " +
                    "did not complete. Already-distributed managed tables " + distributed + " had their managed " +
                    "foreign keys dropped but those FKs may not have been re-added, while these managed tables are " +
                    "still not distributed: " + remaining + ". Resuming would skip FK re-capture on the " +
                    "already-distributed tables and permanently lose those foreign keys. Recovery: restore from a " +
                    "clean database (drop and re-create the schema, then re-run distribution) or manually restore " +
                    "the dropped managed foreign keys (and finish the remaining conversions) before retrying. " +
                    "applyDistribution() does not auto-replay a partial run.");
        }
    }

    /**
     * Fails loudly when an existing populated database is converted before the 4.3.1.4 upgrade has prepared the
     * alarm-group tables. {@link #rewriteAlarmGroupPrimaryKeys} has two prerequisites that nothing else enforces
     * and that only that upgrade establishes:
     *
     * <ul>
     *   <li>{@code alarm.originator_id} and {@code entity_alarm.originator_id} must exist and contain no NULLs —
     *       the widened {@code ADD PRIMARY KEY} fails otherwise. On a genuinely pre-4.3.1.4 database
     *       {@code entity_alarm} has NO {@code originator_id} column at all (that upgrade adds it and backfills it
     *       from {@code alarm}), so column existence is checked first — a missing column is the strongest possible
     *       signal the upgrade never ran, and probing it for NULLs would only surface as a cryptic
     *       "column does not exist" SQL error instead of pointing the operator at the missing upgrade.</li>
     *   <li>The legacy {@code alarm_comment -> alarm} foreign key must already be dropped — it depends on the
     *       single-column {@code alarm_pkey}, so {@code DROP CONSTRAINT alarm_pkey} fails while it exists, and
     *       {@link #dropManagedForeignKeys} never drops it because {@code alarm_comment} is not a managed
     *       table.</li>
     * </ul>
     *
     * All checks are cheap catalog/EXISTS probes and are skipped once the table is already distributed or its
     * primary key already leads with {@code originator_id} (an already-converted cluster, or a re-run resuming past
     * the PK rewrite): the widened PK enforces NOT NULL and removes the unique key the legacy FK depended on, so
     * the conditions cannot hold there and probing would only add a pointless scan to every idempotent re-run.
     */
    private void guardAgainstMissingAlarmUpgrade(Set<String> alreadyDistributed) {
        boolean alarmNeedsPrimaryKeyRewrite = needsPrimaryKeyRewrite("alarm", alreadyDistributed);
        if (alarmNeedsPrimaryKeyRewrite) {
            guardAgainstUnpreparedOriginatorColumn("alarm");
        }
        if (needsPrimaryKeyRewrite(ENTITY_ALARM_TABLE, alreadyDistributed)) {
            guardAgainstUnpreparedOriginatorColumn(ENTITY_ALARM_TABLE);
        }
        if (alarmNeedsPrimaryKeyRewrite && alarmCommentForeignKeyToAlarmExists()) {
            throw new IllegalStateException(
                    "Cannot convert to Citus: the legacy alarm_comment -> alarm foreign key is still present and " +
                    "would block rewriting alarm's primary key (alarm_comment is not a managed table, so the " +
                    "conversion never drops its foreign keys). Run the 4.3.1.4 (or later) upgrade first (it drops " +
                    "this foreign key), then re-run the postgres-to-citus conversion.");
        }
    }

    /**
     * Whether {@link #rewriteAlarmGroupPrimaryKeys} will actually rewrite {@code table}'s primary key on this run:
     * the table is in the distributed set, not yet distributed, and its PK does not already lead with
     * {@code originator_id}. This is the gate for every {@link #guardAgainstMissingAlarmUpgrade} probe.
     */
    private boolean needsPrimaryKeyRewrite(String table, Set<String> alreadyDistributed) {
        return shouldConvert(table, alreadyDistributed) && !primaryKeyIncludesColumn(table, "originator_id");
    }

    /**
     * The per-table half of {@link #guardAgainstMissingAlarmUpgrade}: fails loudly when {@code table} is not ready
     * for the {@code originator_id}-leading primary key rewrite — either the column does not exist at all (a
     * genuinely pre-4.3.1.4 database; checked first so the NULL probe below cannot blow up with a raw
     * "column does not exist" SQL error) or it still contains NULLs (the 4.3.1.4 backfill never ran/finished).
     * <p>
     * The NULL probe is the hard gate for the conversion: it fails loudly whenever any {@code originator_id} row is
     * still NULL, so the primary key can never be rewritten to lead with a column that holds NULLs. The 4.3.1.4
     * migration backfills pre-existing rows once and then logs detection/repair guidance; on a cluster already past
     * that version gate, any stragglers left by a rolling upgrade must be repaired manually (per that guidance)
     * before the postgres-to-citus conversion is retried.
     */
    private void guardAgainstUnpreparedOriginatorColumn(String table) {
        if (!columnExists(table, "originator_id")) {
            throw new IllegalStateException(
                    "Cannot convert to Citus: " + table + " has no originator_id column, so its primary key " +
                    "cannot be rewritten to lead with the originator_id distribution column. Run the 4.3.1.4 " +
                    "(or later) upgrade first (it adds and backfills entity_alarm.originator_id), then re-run " +
                    "the postgres-to-citus conversion.");
        }
        if (hasNullOriginatorRows(table)) {
            throw new IllegalStateException(
                    "Cannot convert to Citus: " + table + " contains rows with NULL originator_id, so its " +
                    "primary key cannot be rewritten to lead with the originator_id distribution column. " +
                    "Run the 4.3.1.4 (or later) upgrade first (it backfills entity_alarm.originator_id from " +
                    "alarm), then re-run the postgres-to-citus conversion.");
        }
    }

    /**
     * Whether {@code table}'s primary key includes {@code column}. Used to detect whether
     * {@link #rewriteAlarmGroupPrimaryKeys} has already widened an alarm-group primary key to lead with
     * {@code originator_id}; returns {@code false} when the table (or its PK) does not exist. Matched via
     * {@code to_regclass}, which resolves the bare table name through the session search_path exactly like the
     * unqualified DDL this class executes (and yields NULL — matching nothing — when the table is absent), so a
     * same-named table in another schema (a backup schema, a restored dump) can never satisfy the probe.
     */
    private boolean primaryKeyIncludesColumn(String table, String column) {
        Boolean includes = jdbcTemplate.queryForObject(
                "select exists (select 1 from pg_constraint con " +
                        "join pg_attribute a on a.attrelid = con.conrelid and a.attnum = any (con.conkey) " +
                        "where con.contype = 'p' and con.conrelid = to_regclass(?) and a.attname = ?)",
                Boolean.class, table, column);
        return Boolean.TRUE.equals(includes);
    }

    /**
     * Whether {@code table} has a column named {@code column}, scoped to {@code current_schema()} — the schema
     * the unqualified DDL this class executes lands in. Backs the {@link #guardAgainstUnpreparedOriginatorColumn}
     * missing-column check; returns {@code false} when the table itself does not exist.
     */
    private boolean columnExists(String table, String column) {
        Boolean exists = jdbcTemplate.queryForObject(
                "select exists (select 1 from information_schema.columns " +
                        "where table_schema = current_schema() and table_name = ? and column_name = ?)",
                Boolean.class, table, column);
        return Boolean.TRUE.equals(exists);
    }

    private boolean hasNullOriginatorRows(String table) {
        // table is a trusted internal constant, never user input — safe to inline. EXISTS stops at the first
        // offending row, so on a healthy backfilled table this costs a single scan at most, once per conversion.
        // The caller has already verified the originator_id column exists (see guardAgainstUnpreparedOriginatorColumn).
        Boolean hasNulls = jdbcTemplate.queryForObject(
                "select exists (select 1 from " + table + " where originator_id is null)", Boolean.class);
        return Boolean.TRUE.equals(hasNulls);
    }

    /**
     * Whether any {@code alarm_comment -> alarm} foreign key is still present. Matched structurally (child/parent
     * relation, not constraint name) so a renamed legacy constraint is still caught; the relations resolve via
     * {@code to_regclass} (search_path-scoped, NULL when absent — see {@link #primaryKeyIncludesColumn}), so this
     * returns {@code false} when {@code alarm_comment} does not exist at all and ignores same-named tables in
     * other schemas.
     */
    private boolean alarmCommentForeignKeyToAlarmExists() {
        Boolean exists = jdbcTemplate.queryForObject(
                "select exists (select 1 from pg_constraint con " +
                        "where con.contype = 'f' " +
                        "and con.conrelid = to_regclass('alarm_comment') and con.confrelid = to_regclass('alarm'))",
                Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Pre-conversion constraint surgery: drops every foreign key whose child is one of the not-yet-distributed
     * managed tables, returning the {@code ALTER TABLE ... ADD CONSTRAINT} statements that recreate them verbatim.
     * These FKs have to be torn down BEFORE any conversion and rebuilt AFTER every conversion (via
     * {@link #readdForeignKeys}), for two reasons:
     *
     * <ul>
     *   <li>A FK from a soon-to-be-distributed table into a soon-to-be-reference table: Citus 12
     *       ({@code enable_local_reference_table_foreign_keys}, on by default) reacts to
     *       {@code create_reference_table} by auto-adding any still-local table that has a foreign key to the new
     *       reference table into Citus metadata. The distributed table (e.g. {@code device}, which FKs
     *       {@code device_profile}/{@code ota_package}) would then be silently captured as a Citus-managed table,
     *       and the later {@code create_distributed_table} on it fails with "table ... is already distributed".</li>
     *   <li>A FK FROM a soon-to-be-reference table that carries a cascading ON DELETE/ON UPDATE action
     *       (CASCADE / SET NULL / SET DEFAULT) into another managed table that is still LOCAL at the moment
     *       {@code create_reference_table} runs (e.g. {@code ota_package -> device_profile},
     *       {@code device_profile -> ota_package}, which mutually reference each other, so no ordering clears
     *       both). Citus rejects a reference-&gt;local FK with a cascading action: "foreign keys from reference
     *       tables to local tables can only be defined with NO ACTION or RESTRICT behaviors".</li>
     * </ul>
     *
     * <p>We drop EVERY foreign key whose child is in the managed set up front and re-add them all after the
     * conversions complete. By then every managed table is distributed or reference, so each re-added FK is one of
     * distributed-&gt;reference, reference-&gt;reference (incl. ON DELETE CASCADE) or distributed-&gt;distributed
     * — all legal. (No reference table in this schema FKs into a distributed table, so the illegal
     * reference-&gt;distributed case never arises on re-add.) Already-distributed tables are skipped: their
     * FKs/uniques were handled on the prior run, and leaving the (already re-added) foreign keys in place keeps a
     * second applyDistribution() run a no-op.
     */
    private List<String> dropManagedForeignKeys(List<String> managedTables, Set<String> alreadyDistributed) {
        List<String> foreignKeysToReadd = new ArrayList<>();
        for (String table : managedTables) {
            if (alreadyDistributed.contains(table)) {
                continue;
            }
            foreignKeysToReadd.addAll(dropForeignKeys(table));
        }
        return foreignKeysToReadd;
    }

    /**
     * Drops the UNIQUE constraints that omit the distribution column from every not-yet-distributed distributed
     * table. Citus rejects distributing a table whose UNIQUE/EXCLUDE constraint omits the distribution key (it
     * cannot be enforced across shards). These constraints cannot be recreated on the distributed table; the
     * invariants they guarded (e.g. per-tenant device name / external_id uniqueness) are enforced at the
     * application layer instead (see DeviceDataValidator). Only distributed tables are processed — reference
     * tables enforce uniqueness cluster-wide and keep their UNIQUE constraints.
     */
    private void dropIncompatibleUniques(Set<String> alreadyDistributed) {
        for (String table : distributedTables()) {
            if (alreadyDistributed.contains(table)) {
                continue;
            }
            dropUniqueConstraintsWithoutColumn(table, CitusTables.distributionColumn(table));
        }
    }

    /**
     * Widens the alarm-group primary keys to lead with the {@code originator_id} distribution column, the Citus-only
     * surgery that must run before {@link #createDistributedTables} can distribute {@code alarm}/{@code entity_alarm}
     * on {@code originator_id}. Citus refuses to distribute a table on a column its primary key does not include: it
     * enforces PK/unique constraints per shard and could not guarantee them across shards if they omitted the
     * distribution key — the same rule that forces {@link #dropIncompatibleUniques}. A primary key cannot be altered
     * in place, so the narrow {@code (id)} / {@code (entity_id, alarm_id)} PK is dropped and re-added in the wider form
     * (leading with {@code originator_id} rather than merely including it is a separate by-id-lookup concern — see the
     * {@code idx_alarm_id} comment below). Safe to run here because {@link #dropManagedForeignKeys} has already torn down every
     * managed foreign key, so nothing references {@code alarm(id)} at this point (the {@code entity_alarm -> alarm}
     * FK is re-added in its composite form afterwards by {@link #readdEntityAlarmCompositeForeignKey}). Gated via
     * {@link #shouldConvert} so re-runs / already-converted clusters (whose tables are already distributed) are left
     * untouched. The base schema-entities.sql keeps the original plain-Postgres PKs; these widened PKs are
     * Citus-only.
     * <p>
     * Each rewrite is a {@code DROP CONSTRAINT IF EXISTS} followed by {@code ADD CONSTRAINT}, both idempotent so the
     * conversion self-heals across a crash anywhere in this step. A crash after the ADD (before the table is
     * distributed) re-enters here on the next run — the DROP removes the widened PK and the ADD re-creates it
     * unchanged. A crash BETWEEN the DROP and the ADD leaves the table with no primary key at all; the {@code IF
     * EXISTS} makes the re-entry's DROP a no-op and the ADD re-creates the PK, so the window is fully recoverable
     * (without {@code IF EXISTS} that re-entry would instead fail on the missing constraint and wedge the
     * conversion). Once the table is distributed {@link #shouldConvert} short-circuits this step entirely.
     */
    private void rewriteAlarmGroupPrimaryKeys(Set<String> alreadyDistributed) {
        if (shouldConvert("alarm", alreadyDistributed)) {
            log.info("Rewriting alarm primary key to (originator_id, id) for distribution on originator_id");
            jdbcTemplate.execute("ALTER TABLE alarm DROP CONSTRAINT IF EXISTS alarm_pkey");
            jdbcTemplate.execute("ALTER TABLE alarm ADD CONSTRAINT alarm_pkey PRIMARY KEY (originator_id, id)");
            // The widened PK leads with originator_id, so it can no longer serve a lookup by id alone (a btree is
            // ordered by its leading column first, and id is now the trailing column scattered under every distinct
            // originator_id). GET /api/alarm/info/{alarmId} carries only the id, so without a dedicated index every
            // such lookup degrades to a per-shard sequential scan of the alarm table (tens of seconds on a cold
            // cache). A non-unique index leading with id restores a per-shard index scan: the lookup still fans out
            // to all shards (id carries no originator, so Citus cannot prune to one), but each shard probes its index
            // instead of scanning. Created before create_distributed_table below so Citus recreates it on every
            // shard. Citus-only: plain PostgreSQL keeps the single-column alarm_pkey on id, which already serves
            // by-id lookups, so the base schema deliberately omits this index (a duplicate there would only add
            // write/storage overhead on the write-heavy alarm table).
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_alarm_id ON alarm (id)");
        }
        if (shouldConvert(ENTITY_ALARM_TABLE, alreadyDistributed)) {
            log.info("Rewriting entity_alarm primary key to (originator_id, entity_id, alarm_id) for distribution on originator_id");
            jdbcTemplate.execute("ALTER TABLE entity_alarm DROP CONSTRAINT IF EXISTS entity_alarm_pkey");
            jdbcTemplate.execute("ALTER TABLE entity_alarm ADD CONSTRAINT entity_alarm_pkey PRIMARY KEY (originator_id, entity_id, alarm_id)");
        }
    }

    /**
     * Creates the indexes that only a Citus deployment needs, independently of whether this run converts anything.
     * <p>
     * Unlike the conversion phases, this is NOT gated on {@link #shouldConvert}, and it sits above
     * {@link #applyDistribution}'s fully-distributed early return: a cluster converted by an earlier build is past
     * that return, so an index added later would never reach it otherwise. Every statement must therefore be
     * idempotent ({@code IF NOT EXISTS}) and valid in both states — on a not-yet-distributed table the index is
     * created locally and Citus recreates it on each shard during {@code create_distributed_table}, and on an
     * already-distributed table Citus propagates the {@code CREATE INDEX} to every shard.
     * <p>
     * Note that {@code CREATE INDEX} takes a lock that blocks writes to the table while it builds, so on a cluster
     * with a large pre-existing alarm table the migration that first introduces this index pays that cost once.
     */
    private void ensureCitusOnlyIndexes() {
        // The alarm page lists a tenant's ACTIVE alarms newest-first (cleared = false, ORDER BY created_time DESC).
        // The query carries no originator_id, so Citus cannot prune to one shard and fans the scan out to all of
        // them. The base schema's idx_alarm_tenant_created_time covers (tenant_id, created_time DESC) but is not
        // partial: every shard has to walk index entries for that tenant's cleared alarms as well and discard them,
        // and cleared alarms accumulate without bound while the active set stays small. Restricting the index to
        // active rows lets each shard read only entries that can qualify and stop once the page is filled.
        // Citus-only by choice: the base schema is deliberately left untouched so existing non-Citus deployments
        // keep their current index set (and neither pay the build on upgrade nor the extra write cost) — plain
        // PostgreSQL serves this page from a single node without the fan-out that makes the partial index matter.
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_alarm_tenant_created_time_active " +
                "ON alarm (tenant_id, created_time DESC) WHERE cleared = false");
    }

    /**
     * Converts every not-yet-distributed reference table via {@code create_reference_table}, replicating it to
     * every worker.
     */
    private void createReferenceTables(Set<String> alreadyDistributed) {
        for (String table : referenceTables()) {
            if (alreadyDistributed.contains(table)) {
                log.info("Reference table {} already distributed, skipping", table);
                continue;
            }
            log.info("Creating reference table {}", table);
            // create_reference_table returns a 'void' typed value (an empty PGobject), not a JDBC
            // NULL, so it cannot be mapped to Void.class. We use execute(...) and inline the table
            // name — it is a trusted internal constant from CitusTables, never user input.
            jdbcTemplate.execute("SELECT create_reference_table('" + table + "')");
        }
    }

    /**
     * Hash-distributes every not-yet-distributed distributed table into a single co-location group.
     *
     * <p>This relies on every entry in DISTRIBUTED_TABLES being a mutually co-located, hash-distributed table on a
     * type-compatible (uuid) distribution column — the KV tables on 'entity_id', device on 'id'. The anchor table
     * establishes the colocation group with the configured shard_count, and every other table colocates with it
     * (and therefore inherits the same shard count). The anchor is computed up front as any already-distributed
     * table in the group, so a partial resume colocates the remaining tables with whichever one survived
     * regardless of iteration order — relying on loop order would mis-handle a resume where only a later table
     * (e.g. ts_kv_latest) was distributed before the crash: the first not-yet-distributed table would then be
     * created with shard_count and no colocate_with, splitting the colocation group and silently mis-routing the
     * co-located reads/writes. We deliberately do NOT issue {@code SET citus.shard_count} here: that is a session
     * GUC, and in production the JdbcTemplate is backed by a HikariCP pool where each call may use a different
     * pooled connection — the GUC would be discarded and create_distributed_table would silently fall back to the
     * default shard count. Inlining shard_count as a literal in the statement is pooled-connection-safe and
     * behaves identically on the single-connection test harness. Note: Citus rejects specifying both
     * colocate_with and shard_count, so only the anchor table carries shard_count.
     */
    private void createDistributedTables(Set<String> alreadyDistributed) {
        List<String> distributedTables = distributedTables();
        String anchor = distributedTables.stream()
                .filter(alreadyDistributed::contains)
                .findFirst()
                .orElse(null);
        for (String table : distributedTables) {
            if (alreadyDistributed.contains(table)) {
                log.info("Distributed table {} already distributed, skipping", table);
                continue;
            }
            String distColumn = CitusTables.distributionColumn(table);
            String sql;
            if (anchor == null) {
                // No table distributed yet: this one establishes the colocation group with the configured
                // shard count. shardCount is a trusted internal int from settings, not user input — safe to inline.
                sql = "SELECT create_distributed_table('" + table + "', '" + distColumn + "', shard_count => " + settings.getShardCount() + ")";
                anchor = table;
            } else {
                // Other tables inherit the shard count by co-locating with the anchor table. Each table keeps
                // its own distribution column (device on 'id', the KV tables on 'entity_id') — co-location only
                // requires type-compatible distribution columns (all uuid), not identical column names.
                // anchor is a trusted internal constant from CitusTables — safe to inline.
                sql = "SELECT create_distributed_table('" + table + "', '" + distColumn + "', colocate_with => '" + anchor + "')";
            }
            // create_distributed_table returns regclass, which (like the void above) does not map to
            // Void.class; the table name is a trusted constant so we inline it and use execute(...).
            log.info("Creating distributed table {} ({})", table, sql);
            jdbcTemplate.execute(sql);
        }
    }

    /**
     * Re-adds the foreign keys dropped by {@link #dropManagedForeignKeys}. Every managed table is now distributed
     * or reference, so each captured FK becomes a legal distributed-&gt;reference, reference-&gt;reference (incl.
     * ON DELETE CASCADE) or distributed-&gt;distributed foreign key that Citus propagates to all shards.
     */
    private void readdForeignKeys(List<String> foreignKeysToReadd) {
        for (String addStatement : foreignKeysToReadd) {
            log.info("Re-adding foreign key: {}", addStatement);
            jdbcTemplate.execute(addStatement);
        }
    }

    /**
     * Re-adds the {@code entity_alarm -> alarm} foreign key in its co-located composite form
     * {@code (originator_id, alarm_id) -> alarm(originator_id, id)} when it is not already present. As a
     * distributed-&gt;distributed FK Citus propagates it to all shards, preserving {@code ON DELETE CASCADE}
     * natively per shard.
     * <p>
     * This FK is deliberately excluded from the generic managed-FK catalog and the live capture/re-add path (see
     * {@link #ENTITY_ALARM_FK}): the plain-schema single-column {@code (alarm_id) -> alarm(id)} form is invalid once
     * {@code alarm}'s primary key is widened to {@code (originator_id, id)}, so {@link #dropForeignKeys} drops it
     * without capturing it and it is rebuilt here in composite form after both tables are distributed. The presence
     * check keeps this idempotent, so it doubles as the crash-recovery repair for this one FK — the analogue of
     * {@link #readdMissingForeignKeys()}, which cannot cover it because {@link CitusTables#MANAGED_FOREIGN_KEYS}
     * mirrors the single-column plain schema. Guarded on distributed-set membership so the minimal unit-test
     * fixtures that omit entity_alarm skip it. The start-of-run crash-repair call is additionally guarded by the
     * caller on the widened alarm primary key: the composite form is only addable once
     * {@link #rewriteAlarmGroupPrimaryKeys} has run, whereas the end-of-run call here always runs after it.
     */
    private void readdEntityAlarmCompositeForeignKey() {
        if (!distributedTables().contains(ENTITY_ALARM_TABLE)
                || existingForeignKeyStructures().contains(
                        CitusTables.foreignKeyStructuralKey(ENTITY_ALARM_TABLE, "alarm", "originator_id,alarm_id"))) {
            return;
        }
        String compositeFk = "ALTER TABLE " + ENTITY_ALARM_TABLE + " ADD CONSTRAINT " + ENTITY_ALARM_FK +
                " FOREIGN KEY (originator_id, alarm_id) REFERENCES alarm(originator_id, id) ON DELETE CASCADE";
        log.info("Re-adding composite co-located foreign key: {}", compositeFk);
        jdbcTemplate.execute(compositeFk);
    }

    /**
     * Crash-recovery repair for the two FK-loss gaps that slip past {@link #guardAgainstPartialState}, run at the
     * start of every {@link #applyDistribution} before anything is dropped. Each {@code ADD CONSTRAINT} in
     * {@link #readdForeignKeys} runs in its own autocommit, so a crash partway through the prior run's final re-add
     * loop leaves every managed table already distributed but some managed foreign keys
     * dropped-and-never-re-added — and the normal phases are all no-ops on a fully-distributed re-run. Symmetrically,
     * a crash after {@link #dropManagedForeignKeys} but before the first successful conversion leaves NOTHING
     * distributed, so the re-run looks like a clean first run and its own drop phase finds (and captures) nothing.
     * The guard cannot catch either state (it needs some tables distributed and some not) — in both, those FKs would
     * be lost forever. The dropped FK is gone from the catalog with no live source to re-capture from, so we
     * re-derive the expected managed FK set from {@link CitusTables#MANAGED_FOREIGN_KEYS} (a static,
     * install-invariant declaration that mirrors schema-entities.sql and is byte-identical to
     * {@code pg_get_constraintdef} output) and re-add only the ones actually missing from the catalog — presence is
     * decided structurally, not by constraint name (see {@link #existingForeignKeyStructures()}). Present FKs
     * are left untouched, keeping this idempotent: on a healthy DB (clean first run or fully-distributed re-run) it
     * finds nothing missing and does nothing. Every catalog FK is legal in both states this can run in: between
     * still-local tables it is a plain PostgreSQL foreign key, and on a fully-distributed cluster it is one of the
     * distributed-&gt;reference / reference-&gt;reference / distributed-&gt;distributed forms Citus accepts. The
     * composite {@code entity_alarm -> alarm} FK is handled separately by
     * {@link #readdEntityAlarmCompositeForeignKey} (it is not in the catalog and is not valid in every state — see
     * the guarded call site in {@link #applyDistribution}).
     *
     * <p><b>Operator-added (non-catalog) foreign keys are NOT recoverable.</b> {@link #dropForeignKeys} drops EVERY
     * FK whose child is a managed table — including custom DBA-added constraints — capturing their re-add DDL only
     * in memory. On the happy path they are re-added verbatim, but in both crash windows this repair covers, a
     * custom FK that was already dropped has no live catalog row left and no entry in the static catalog, so it is
     * never restored and the re-run then completes normally: permanent, silent referential-integrity loss with no
     * error. Operators who added their own FKs to managed tables must capture and re-add them manually (or back up
     * their definitions) before running the postgres-to-citus conversion.
     */
    private void readdMissingForeignKeys() {
        Set<String> existing = existingForeignKeyStructures();
        for (CitusTables.ManagedForeignKey fk : managedForeignKeys()) {
            if (existing.contains(fk.structuralKey())) {
                continue;
            }
            String addStatement = fk.addStatement();
            log.warn("Re-adding managed foreign key {} missing after a crash during a prior run's FK re-add: {}",
                    fk.name(), addStatement);
            jdbcTemplate.execute(addStatement);
        }
    }

    /**
     * Structural identities ({@link CitusTables#foreignKeyStructuralKey}: child table + parent table + ordered
     * child column list) of all foreign key constraints currently present in the current schema. Used by
     * {@link #readdMissingForeignKeys()} and {@link #readdEntityAlarmCompositeForeignKey()} to detect which managed
     * FKs are missing after a crashed prior run. Presence is decided structurally, NOT by constraint name: an
     * equivalent live FK under a different (historical or auto-generated) name must count as present — a name-keyed
     * check would see it as missing and add a DUPLICATE constraint next to it (a long ACCESS EXCLUSIVE validation
     * on a big table, then two identical FKs forever). Scoped to {@code current_schema()} — where the unqualified
     * DDL this class executes lands — so foreign keys on same-named tables in other schemas (a backup schema, a
     * restored dump) can neither fake presence nor leak in. The key is built by the shared
     * {@link #FK_STRUCTURAL_KEY_SQL} fragment, whose {@code '|'}/{@code ','} format mirrors
     * {@link CitusTables#foreignKeyStructuralKey} and must stay in sync.
     */
    private Set<String> existingForeignKeyStructures() {
        List<String> keys = jdbcTemplate.queryForList(
                "select " + FK_STRUCTURAL_KEY_SQL + " " +
                        "from pg_constraint con " +
                        "join pg_class child on child.oid = con.conrelid " +
                        "join pg_class parent on parent.oid = con.confrelid " +
                        "join pg_namespace ns on ns.oid = child.relnamespace " +
                        "where con.contype = 'f' and ns.nspname = current_schema()", String.class);
        return Set.copyOf(keys);
    }

    /**
     * Drops every foreign key whose child is {@code table}, returning the {@code ALTER TABLE ... ADD CONSTRAINT}
     * statements that recreate them verbatim (captured via {@code pg_get_constraintdef} before the drop). All such
     * FKs are torn down before any conversion and re-added after every conversion completes — see
     * {@link #dropManagedForeignKeys} for why both the inbound (distributed-&gt;reference) and the cascading
     * outbound (reference-&gt;local) cases require this. Statements/identifiers come from Citus metadata and the
     * trusted internal table lists, never user input, so they are safe to inline. The child table resolves via
     * {@code to_regclass} (search_path-scoped — see {@link #primaryKeyIncludesColumn}) so a same-named table in
     * another schema never has its foreign keys captured or dropped.
     */
    private List<String> dropForeignKeys(String table) {
        // Also select each FK's structural identity (child|parent|ordered-columns via the shared
        // FK_STRUCTURAL_KEY_SQL, same format as CitusTables#foreignKeyStructuralKey) so the entity_alarm->alarm single-column
        // FK can be matched by SHAPE below. parent.relname is unqualified (no schema): safe because con.conrelid is
        // already resolved via to_regclass(?) (search_path-scoped, see the javadoc), so only FKs of the intended child
        // are considered, and entity_alarm is not expected to reference an "alarm" table in a foreign schema.
        List<Map<String, Object>> fks = jdbcTemplate.queryForList(
                "select con.conname as name, pg_get_constraintdef(con.oid) as def, " +
                        FK_STRUCTURAL_KEY_SQL + " as structural_key " +
                        "from pg_constraint con " +
                        "join pg_class child on child.oid = con.conrelid " +
                        "join pg_class parent on parent.oid = con.confrelid " +
                        "where con.contype = 'f' and con.conrelid = to_regclass(?)", table);
        // The plain-schema single-column (alarm_id) -> alarm(id) form, regardless of the constraint's name.
        String entityAlarmSingleColumnFkKey = CitusTables.foreignKeyStructuralKey(ENTITY_ALARM_TABLE, "alarm", "alarm_id");
        List<String> addStatements = new ArrayList<>();
        for (Map<String, Object> fk : fks) {
            String name = (String) fk.get("name");
            log.info("Dropping foreign key {} on {} (re-added after distribution)", name, table);
            jdbcTemplate.execute("ALTER TABLE " + table + " DROP CONSTRAINT " + name);
            if (entityAlarmSingleColumnFkKey.equals(fk.get("structural_key"))) {
                // Dropped (so the PK rewrite can run) but deliberately NOT captured for generic verbatim re-add --
                // see ENTITY_ALARM_FK; it is re-added explicitly in its composite form in applyDistribution() after
                // both tables are distributed. Matched by SHAPE (entity_alarm -> alarm on the single alarm_id column),
                // not by constraint name, so a restored/renamed dump carrying the same single-column FK under any other
                // name is still skipped -- capturing it would re-add the invalid single-column form alongside the
                // composite one applyDistribution() adds. This branch only matters during the initial conversion (and a
                // partial-resume that re-enters before entity_alarm was distributed): on a full re-run,
                // applyDistribution()'s drop loop skips dropForeignKeys(entity_alarm) once it is already
                // distributed, so the composite FK is left in place untouched.
                continue;
            }
            addStatements.add("ALTER TABLE " + table + " ADD CONSTRAINT " + name + " " + fk.get("def"));
        }
        return addStatements;
    }

    /**
     * Drops every UNIQUE constraint on {@code table} whose column set does not include {@code distributionColumn}.
     * Citus refuses to distribute a table carrying such a constraint, and it cannot be recreated afterwards. See
     * {@link #dropIncompatibleUniques} for the rationale. The primary key is left untouched (contype 'p'); for the
     * distributed tables here it already contains the distribution column.
     */
    private void dropUniqueConstraintsWithoutColumn(String table, String distributionColumn) {
        List<String> constraints = jdbcTemplate.queryForList(
                "select con.conname from pg_constraint con " +
                        "where con.conrelid = to_regclass(?) and con.contype = 'u' " +
                        "and not exists (select 1 from unnest(con.conkey) k " +
                        "  join pg_attribute a on a.attrelid = con.conrelid and a.attnum = k " +
                        "  where a.attname = ?)", String.class, table, distributionColumn);
        for (String name : constraints) {
            log.info("Dropping distribution-incompatible unique constraint {} on {} " +
                    "(enforced at application layer)", name, table);
            jdbcTemplate.execute("ALTER TABLE " + table + " DROP CONSTRAINT " + name);
        }
    }

    // The alarm-group tables (alarm, entity_alarm) need Citus-only surgery beyond the generic conversion: their
    // primary keys are widened to lead with originator_id and the entity_alarm -> alarm FK is re-added in its
    // co-located composite form (see ENTITY_ALARM_FK). Apply that surgery only when the table is actually in the
    // distributed set (the minimal unit-test fixture omits these tables) AND is not already distributed (so re-runs
    // and already-converted clusters are left untouched) -- the same gate the conversion loops use.
    private boolean shouldConvert(String table, Set<String> alreadyDistributed) {
        return distributedTables().contains(table) && !alreadyDistributed.contains(table);
    }

    private Set<String> currentlyDistributed() {
        // Compare on the bare relation name (relname), not logicalrelid::regclass::text: the latter renders the name
        // relative to the current search_path and yields a schema-qualified name (e.g. "public.attribute_kv") whenever
        // the table's schema is not on the path. That would make the bare-name contains() checks in applyDistribution()
        // miss, re-issue create_distributed_table/create_reference_table, and break idempotency. relname is always the
        // unqualified name regardless of search_path. Scoped to current_schema() (where this class's unqualified DDL
        // lands) so a same-named distributed table in another schema cannot make a managed table look converted.
        List<String> names = jdbcTemplate.queryForList(
                "select c.relname from pg_dist_partition p join pg_class c on c.oid = p.logicalrelid " +
                        "join pg_namespace ns on ns.oid = c.relnamespace where ns.nspname = current_schema()",
                String.class);
        return Set.copyOf(names);
    }

    // overridable in tests to limit the reference set to the tables present
    protected List<String> referenceTables() {
        return CitusTables.REFERENCE_TABLES;
    }

    protected List<String> distributedTables() {
        return CitusTables.DISTRIBUTED_TABLES;
    }

    // overridable in tests to limit the managed foreign keys to those present in the test fixture's schema
    protected List<CitusTables.ManagedForeignKey> managedForeignKeys() {
        return CitusTables.MANAGED_FOREIGN_KEYS;
    }

}

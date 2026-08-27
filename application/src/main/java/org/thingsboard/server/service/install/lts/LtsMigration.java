/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.install.lts;

/**
 * One LTS migration, keyed by the version it ships in (e.g. "4.2.2.3").
 * The runner runs the version's data/upgrade/lts/&lt;version&gt;/schema_update.sql by convention
 * (if present), then calls {@link #apply()} for any programmatic work beyond SQL.
 * Most migrations are SQL-only and only override {@link #getVersion()}.
 * <p>
 * Ordering caveat for a multi-version run (see {@link LtsMigrationService#applyMigrations}): on the no-downtime path
 * every migration's schema_update.sql + {@link #apply()} runs, in version order, BEFORE any migration's
 * {@link #applyAfterCommit()}. So migration N+1's schema/apply executes ahead of migration N's backfill. A migration
 * must therefore not depend, in its schema_update.sql or {@link #apply()}, on the completed result of an earlier
 * migration's {@link #applyAfterCommit()} within the same run.
 * <p>
 * The converse constraint also holds: because all schema transactions commit before any {@link #applyAfterCommit()}
 * runs, migration N's backfill executes against a schema that already includes every later selected migration's DDL.
 * {@link #applyAfterCommit()} must therefore tolerate the schema changes of every later migration in the same run --
 * equivalently, later DDL must not drop, rename, or retype anything an earlier migration's backfill reads or writes.
 */
public interface LtsMigration {

    String getVersion();

    /**
     * Programmatic data migration that runs in the SAME transaction as the version's schema_update.sql
     * (see {@link LtsMigrationService#applyMigrations}). Use for work that must be atomic with the schema
     * change. Do NOT put heavy table rewrites here: on the no-downtime path the schema_update.sql DDL holds
     * its locks until this transaction commits, so a long apply() keeps those locks held while the node serves.
     * <p>
     * Must be idempotent: the version is not recorded until {@link #applyAfterCommit()} also succeeds, so a crash
     * after this transaction commits but before the version is recorded re-runs this method on the next startup.
     */
    default void apply() {
    }

    /**
     * Programmatic data migration that runs AFTER the version's schema transaction has committed, outside any
     * transaction (see {@link LtsMigrationService#applyMigrations} and {@link LtsMigrationService#runDataMigrations}).
     * Use for heavy or long-running data backfills that must not extend the lifetime of the schema-change locks:
     * the implementation is responsible for chunking the work into its own small, self-committing transactions so
     * it only ever holds row-level locks and never blocks concurrent readers/writers.
     * <p>
     * Must be idempotent and resumable: {@link LtsMigrationService#applyMigrations} records the version only after
     * this method returns, so if the node crashes partway through, the migration is selected again on the next
     * startup and this method runs again from the start -- it must skip already-migrated rows rather than redo or
     * double-count them.
     */
    default void applyAfterCommit() {
    }
}

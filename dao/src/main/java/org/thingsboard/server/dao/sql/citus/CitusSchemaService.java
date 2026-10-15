// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

public interface CitusSchemaService {

    /**
     * Idempotently converts the schema to the Citus layout (see {@link DefaultCitusSchemaService}):
     * converts the full {@link CitusTables#REFERENCE_TABLES} set (~24 dimension tables) to reference
     * tables and hash-distributes the {@link CitusTables#DISTRIBUTED_TABLES} into a single co-location
     * group — the KV anchor group ({@code attribute_kv}/{@code ts_kv_latest} on {@code entity_id}),
     * {@code device}/{@code asset}/{@code entity_view} on {@code id}, and {@code alarm}/{@code entity_alarm}
     * on {@code originator_id}, whose primary keys are first rewritten to lead with the distribution column.
     * ALL managed foreign keys are dropped up front and re-added only after every conversion completes, so
     * the operation is NOT crash-safe: {@link DefaultCitusSchemaService} pairs it with a partial-state guard
     * that fails a resumed partial run loudly and a start-of-run crash repair for the two crash windows the
     * guard cannot see — see its javadoc for the crash-safety caveats. Safe to re-run once complete (tables
     * already present in pg_dist_partition are skipped).
     */
    void applyDistribution();

}

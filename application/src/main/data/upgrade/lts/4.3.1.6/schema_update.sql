--
-- SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
-- SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
-- SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
--

-- A copy of the tb_cluster block in dao/src/main/resources/sql/schema-entities.sql, because the no-downtime patch
-- path (SystemPatchApplier) runs only these per-version scripts and never that file. It has a version to itself so
-- that losing the CREATE TABLE race against the licence context's self-heal - PostgreSQL's IF NOT EXISTS is not
-- atomic - rolls back nothing but this idempotent DDL.

CREATE TABLE IF NOT EXISTS tb_cluster (
    cluster_id uuid NOT NULL,
    license_claim_token varchar,
    CONSTRAINT tb_cluster_pkey PRIMARY KEY (cluster_id)
);

-- PE has carried tb_cluster since the offline licence client, so on an existing deployment the CREATE above is a
-- no-op and cannot add the column to the table already there.
ALTER TABLE tb_cluster ADD COLUMN IF NOT EXISTS license_claim_token varchar;

-- Allows at most one row in the table, no matter how many nodes race to insert.
CREATE UNIQUE INDEX IF NOT EXISTS tb_cluster_single_row ON tb_cluster ((true));

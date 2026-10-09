--
-- SPDX-FileCopyrightText: Copyright The Thingsboard Authors
-- SPDX-License-Identifier: Apache-2.0
--

-- TB_CLUSTER START

-- The patch path never runs schema-entities.sql, so tb_cluster is created here too.
-- Keep this block identical to the one in schema-entities.sql.

CREATE TABLE IF NOT EXISTS tb_cluster (
    cluster_id uuid NOT NULL,
    license_claim_token varchar,
    license_secret varchar,
    CONSTRAINT tb_cluster_pkey PRIMARY KEY (cluster_id)
);

-- Backstop against two concurrent install jobs both minting an id: a unique index on a constant
-- expression allows at most one row in the table, no matter how many rows race to insert.
CREATE UNIQUE INDEX IF NOT EXISTS tb_cluster_single_row ON tb_cluster ((true));

-- TB_CLUSTER END

-- TB_CLUSTER LICENSE SECRET START

-- The CREATE above is a no-op on a database that already carries tb_cluster.
ALTER TABLE tb_cluster ADD COLUMN IF NOT EXISTS license_secret varchar;

-- TB_CLUSTER LICENSE SECRET END

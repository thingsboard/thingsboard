--
-- SPDX-FileCopyrightText: Copyright The Thingsboard Authors
-- SPDX-License-Identifier: Apache-2.0
--

CREATE TABLE IF NOT EXISTS thingsboard.ts_kv_cf (
    entity_type text, -- (DEVICE, CUSTOMER, TENANT)
    entity_id timeuuid,
    key text,
    partition bigint,
    ts bigint,
    bool_v boolean,
    str_v text,
    long_v bigint,
    dbl_v double,
    json_v text,
    PRIMARY KEY (( entity_type, entity_id, key, partition ), ts)
);

CREATE TABLE IF NOT EXISTS thingsboard.ts_kv_partitions_cf (
    entity_type text, -- (DEVICE, CUSTOMER, TENANT)
    entity_id timeuuid,
    key text,
    partition bigint,
    PRIMARY KEY (( entity_type, entity_id, key ), partition)
) WITH CLUSTERING ORDER BY ( partition ASC )
  AND compaction = { 'class' :  'LeveledCompactionStrategy'  };

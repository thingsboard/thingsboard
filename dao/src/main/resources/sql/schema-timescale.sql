--
-- Copyright © 2016-2019 The Thingsboard Authors
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;

CREATE TABLE IF NOT EXISTS tenant_ts_kv (
    tenant_id varchar(31) NOT NULL,
    entity_id varchar(31) NOT NULL,
    key varchar(255) NOT NULL,
    ts bigint NOT NULL,
    bool_v boolean,
    str_v varchar(10000000),
    long_v bigint,
    dbl_v double precision,
    CONSTRAINT ts_kv_pkey PRIMARY KEY (tenant_id, entity_id, key, ts)
);

SELECT create_hypertable('tenant_ts_kv', 'ts', chunk_time_interval => 86400000, if_not_exists => true);
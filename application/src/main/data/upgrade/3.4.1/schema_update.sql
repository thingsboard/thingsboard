--
-- Copyright © 2016-2022 The Thingsboard Authors
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

CREATE TABLE IF NOT EXISTS tmp_audit_log (
    id uuid NOT NULL,
    created_time bigint NOT NULL,
    tenant_id uuid,
    customer_id uuid,
    entity_id uuid,
    entity_type varchar(255),
    entity_name varchar(255),
    user_id uuid,
    user_name varchar(255),
    action_type varchar(255),
    action_data varchar(1000000),
    action_status varchar(255),
    action_failure_details varchar(1000000)
) PARTITION BY RANGE (created_time);
CREATE INDEX IF NOT EXISTS idx_tmp_audit_log_tenant_id_and_created_time ON tmp_audit_log(tenant_id, created_time DESC);

CREATE OR REPLACE PROCEDURE rename_old_audit_logs_partitions()
    LANGUAGE plpgsql AS
$$
DECLARE
    table_partition RECORD;
BEGIN
    FOR table_partition IN SELECT tablename AS name, split_part(tablename, '_', 3) AS partition_ts
    FROM pg_tables WHERE tablename LIKE 'audit_log_%'
    LOOP
        EXECUTE format('ALTER TABLE %s RENAME TO old_audit_log_%s', table_partition.name, table_partition.partition_ts);
    END LOOP;
END;
$$;

CREATE OR REPLACE PROCEDURE migrate_audit_logs(IN start_time_ms BIGINT, IN end_time_ms BIGINT, IN partition_size_ms BIGINT)
    LANGUAGE plpgsql AS
$$
DECLARE
    p RECORD;
    partition_end_ts BIGINT;
BEGIN
    FOR p IN SELECT DISTINCT (created_time - created_time % partition_size_ms) AS partition_ts FROM audit_log
    WHERE created_time >= start_time_ms AND created_time < end_time_ms
    LOOP
        partition_end_ts = p.partition_ts + partition_size_ms;
        RAISE NOTICE '[audit_log] Partition to create : [%-%]', p.partition_ts, partition_end_ts;
        EXECUTE format('CREATE TABLE IF NOT EXISTS audit_log_%s PARTITION OF tmp_audit_log ' ||
         'FOR VALUES FROM ( %s ) TO ( %s )', p.partition_ts, p.partition_ts, partition_end_ts);
    END LOOP;

    INSERT INTO tmp_audit_log
    SELECT * FROM audit_log
    WHERE created_time >= start_time_ms AND created_time < end_time_ms;
END;
$$;

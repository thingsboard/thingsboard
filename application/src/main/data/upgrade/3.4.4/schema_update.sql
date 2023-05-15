--
-- Copyright © 2016-2023 The Thingsboard Authors
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

-- USER CREDENTIALS START

ALTER TABLE user_credentials
    ADD COLUMN IF NOT EXISTS additional_info varchar NOT NULL DEFAULT '{}';

UPDATE user_credentials
    SET additional_info = json_build_object('userPasswordHistory', (u.additional_info::json -> 'userPasswordHistory'))
    FROM tb_user u WHERE user_credentials.user_id = u.id AND u.additional_info::jsonb ? 'userPasswordHistory';

UPDATE tb_user SET additional_info = tb_user.additional_info::jsonb - 'userPasswordHistory' WHERE additional_info::jsonb ? 'userPasswordHistory';

-- USER CREDENTIALS END

-- ALARM ASSIGN TO USER START

ALTER TABLE alarm ADD COLUMN IF NOT EXISTS assign_ts BIGINT DEFAULT 0;
ALTER TABLE alarm ADD COLUMN IF NOT EXISTS assignee_id UUID;

CREATE INDEX IF NOT EXISTS idx_alarm_tenant_assignee_created_time ON alarm(tenant_id, assignee_id, created_time DESC);

-- ALARM ASSIGN TO USER END

-- ALARM STATUS REFACTORING START

ALTER TABLE alarm ADD COLUMN IF NOT EXISTS acknowledged boolean;
ALTER TABLE alarm ADD COLUMN IF NOT EXISTS cleared boolean;

ALTER TABLE alarm ADD COLUMN IF NOT EXISTS status varchar; -- to avoid failure of the subsequent upgrade.
UPDATE alarm SET acknowledged = true, cleared = true WHERE status = 'CLEARED_ACK';
UPDATE alarm SET acknowledged = true, cleared = false WHERE status = 'ACTIVE_ACK';
UPDATE alarm SET acknowledged = false, cleared = true WHERE status = 'CLEARED_UNACK';
UPDATE alarm SET acknowledged = false, cleared = false WHERE status = 'ACTIVE_UNACK';

-- Drop index by 'status' column and replace with new indexes that has only active alarms;
DROP INDEX IF EXISTS idx_alarm_originator_alarm_type_active;
CREATE INDEX IF NOT EXISTS idx_alarm_originator_alarm_type_active
    ON alarm USING btree (originator_id, type) WHERE cleared = false;

DROP INDEX IF EXISTS idx_alarm_tenant_alarm_type_active;
CREATE INDEX IF NOT EXISTS idx_alarm_tenant_alarm_type_active
    ON alarm USING btree (tenant_id, type) WHERE cleared = false;

-- Cover index by alarm type to optimize propagated alarm queries;
DROP INDEX IF EXISTS idx_entity_alarm_entity_id_alarm_type_created_time_alarm_id;
CREATE INDEX IF NOT EXISTS idx_entity_alarm_entity_id_alarm_type_created_time_alarm_id ON entity_alarm
USING btree (tenant_id, entity_id, alarm_type, created_time DESC) INCLUDE(alarm_id);

DROP INDEX IF EXISTS idx_alarm_tenant_status_created_time;
ALTER TABLE alarm DROP COLUMN IF EXISTS status;

-- Update old alarms and set their state to clear, if there are newer alarms.
UPDATE alarm a
SET cleared = TRUE
WHERE cleared = FALSE
  AND id != (SELECT l.id
             FROM alarm l
             WHERE l.tenant_id = a.tenant_id
               AND l.originator_id = a.originator_id
               AND l.type = a.type
             ORDER BY l.created_time DESC, l.id
             LIMIT 1);

-- ALARM STATUS REFACTORING END

-- ALARM COMMENTS START

CREATE TABLE IF NOT EXISTS alarm_comment (
    id uuid NOT NULL,
    created_time bigint NOT NULL,
    alarm_id uuid NOT NULL,
    user_id uuid,
    type varchar(255) NOT NULL,
    comment varchar(10000),
    CONSTRAINT fk_alarm_comment_alarm_id FOREIGN KEY (alarm_id) REFERENCES alarm(id) ON DELETE CASCADE
) PARTITION BY RANGE (created_time);
CREATE INDEX IF NOT EXISTS idx_alarm_comment_alarm_id ON alarm_comment(alarm_id);

-- ALARM COMMENTS END

-- NOTIFICATIONS START

CREATE TABLE IF NOT EXISTS notification_target (
    id UUID NOT NULL CONSTRAINT notification_target_pkey PRIMARY KEY,
    created_time BIGINT NOT NULL,
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    configuration VARCHAR(10000) NOT NULL,
    CONSTRAINT uq_notification_target_name UNIQUE (tenant_id, name)
);
CREATE INDEX IF NOT EXISTS idx_notification_target_tenant_id_created_time ON notification_target(tenant_id, created_time DESC);

CREATE TABLE IF NOT EXISTS notification_template (
    id UUID NOT NULL CONSTRAINT notification_template_pkey PRIMARY KEY,
    created_time BIGINT NOT NULL,
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    configuration VARCHAR(10000000) NOT NULL,
    CONSTRAINT uq_notification_template_name UNIQUE (tenant_id, name)
);
CREATE INDEX IF NOT EXISTS idx_notification_template_tenant_id_created_time ON notification_template(tenant_id, created_time DESC);

CREATE TABLE IF NOT EXISTS notification_rule (
    id UUID NOT NULL CONSTRAINT notification_rule_pkey PRIMARY KEY,
    created_time BIGINT NOT NULL,
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    template_id UUID NOT NULL CONSTRAINT fk_notification_rule_template_id REFERENCES notification_template(id),
    trigger_type VARCHAR(50) NOT NULL,
    trigger_config VARCHAR(1000) NOT NULL,
    recipients_config VARCHAR(10000) NOT NULL,
    additional_config VARCHAR(255),
    CONSTRAINT uq_notification_rule_name UNIQUE (tenant_id, name)
);
CREATE INDEX IF NOT EXISTS idx_notification_rule_tenant_id_trigger_type_created_time ON notification_rule(tenant_id, trigger_type, created_time DESC);

CREATE TABLE IF NOT EXISTS notification_request (
    id UUID NOT NULL CONSTRAINT notification_request_pkey PRIMARY KEY,
    created_time BIGINT NOT NULL,
    tenant_id UUID NOT NULL,
    targets VARCHAR(10000) NOT NULL,
    template_id UUID,
    template VARCHAR(10000000),
    info VARCHAR(1000),
    additional_config VARCHAR(1000),
    originator_entity_id UUID,
    originator_entity_type VARCHAR(32),
    rule_id UUID NULL,
    status VARCHAR(32),
    stats VARCHAR(10000)
);
CREATE INDEX IF NOT EXISTS idx_notification_request_tenant_id_user_created_time ON notification_request(tenant_id, created_time DESC)
    WHERE originator_entity_type = 'USER';
CREATE INDEX IF NOT EXISTS idx_notification_request_rule_id_originator_entity_id ON notification_request(rule_id, originator_entity_id)
    WHERE originator_entity_type = 'ALARM';
CREATE INDEX IF NOT EXISTS idx_notification_request_status ON notification_request(status)
    WHERE status = 'SCHEDULED';

CREATE TABLE IF NOT EXISTS notification (
    id UUID NOT NULL,
    created_time BIGINT NOT NULL,
    request_id UUID NULL CONSTRAINT fk_notification_request_id REFERENCES notification_request(id) ON DELETE CASCADE,
    recipient_id UUID NOT NULL CONSTRAINT fk_notification_recipient_id REFERENCES tb_user(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    subject VARCHAR(255),
    body VARCHAR(1000) NOT NULL,
    additional_config VARCHAR(1000),
    status VARCHAR(32)
) PARTITION BY RANGE (created_time);
CREATE INDEX IF NOT EXISTS idx_notification_id ON notification(id);
CREATE INDEX IF NOT EXISTS idx_notification_recipient_id_created_time ON notification(recipient_id, created_time DESC);

-- NOTIFICATIONS END

ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS phone VARCHAR(255);

CREATE TABLE IF NOT EXISTS user_settings (
    user_id uuid NOT NULL,
    type VARCHAR(50) NOT NULL,
    settings varchar(10000),
    CONSTRAINT fk_user_id FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE,
    CONSTRAINT user_settings_pkey PRIMARY KEY (user_id, type)
);

-- TTL DROP PARTITIONS FUNCTIONS UPDATE START

DROP PROCEDURE IF EXISTS drop_partitions_by_max_ttl(character varying, bigint, bigint);
DROP FUNCTION IF EXISTS get_partition_by_max_ttl_date;

CREATE OR REPLACE FUNCTION get_partition_by_system_ttl_date(IN partition_type varchar, IN date timestamp, OUT partition varchar) AS
$$
BEGIN
    CASE
        WHEN partition_type = 'DAYS' THEN
            partition := 'ts_kv_' || to_char(date, 'yyyy') || '_' || to_char(date, 'MM') || '_' || to_char(date, 'dd');
        WHEN partition_type = 'MONTHS' THEN
            partition := 'ts_kv_' || to_char(date, 'yyyy') || '_' || to_char(date, 'MM');
        WHEN partition_type = 'YEARS' THEN
            partition := 'ts_kv_' || to_char(date, 'yyyy');
        ELSE
            partition := NULL;
        END CASE;
    IF partition IS NOT NULL THEN
        IF NOT EXISTS(SELECT
                      FROM pg_tables
                      WHERE schemaname = 'public'
                        AND tablename = partition) THEN
            partition := NULL;
            RAISE NOTICE 'Failed to found partition by ttl';
        END IF;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE PROCEDURE drop_partitions_by_system_ttl(IN partition_type varchar, IN system_ttl bigint, INOUT deleted bigint)
    LANGUAGE plpgsql AS
$$
DECLARE
    date                       timestamp;
    partition_by_max_ttl_date  varchar;
    partition_by_max_ttl_month varchar;
    partition_by_max_ttl_day   varchar;
    partition_by_max_ttl_year  varchar;
    partition                  varchar;
    partition_year             integer;
    partition_month            integer;
    partition_day              integer;

BEGIN
    if system_ttl IS NOT NULL AND system_ttl > 0 THEN
        date := to_timestamp(EXTRACT(EPOCH FROM current_timestamp) - system_ttl);
        partition_by_max_ttl_date := get_partition_by_system_ttl_date(partition_type, date);
        RAISE NOTICE 'Date by max ttl: %', date;
        RAISE NOTICE 'Partition by max ttl: %', partition_by_max_ttl_date;
        IF partition_by_max_ttl_date IS NOT NULL THEN
            CASE
                WHEN partition_type = 'DAYS' THEN
                    partition_by_max_ttl_year := SPLIT_PART(partition_by_max_ttl_date, '_', 3);
                    partition_by_max_ttl_month := SPLIT_PART(partition_by_max_ttl_date, '_', 4);
                    partition_by_max_ttl_day := SPLIT_PART(partition_by_max_ttl_date, '_', 5);
                WHEN partition_type = 'MONTHS' THEN
                    partition_by_max_ttl_year := SPLIT_PART(partition_by_max_ttl_date, '_', 3);
                    partition_by_max_ttl_month := SPLIT_PART(partition_by_max_ttl_date, '_', 4);
                ELSE
                    partition_by_max_ttl_year := SPLIT_PART(partition_by_max_ttl_date, '_', 3);
                END CASE;
            IF partition_by_max_ttl_year IS NULL THEN
                RAISE NOTICE 'Failed to remove partitions by max ttl date due to partition_by_max_ttl_year is null!';
            ELSE
                IF partition_type = 'YEARS' THEN
                    FOR partition IN SELECT tablename
                                     FROM pg_tables
                                     WHERE schemaname = 'public'
                                       AND tablename like 'ts_kv_' || '%'
                                       AND tablename != 'ts_kv_latest'
                                       AND tablename != 'ts_kv_dictionary'
                                       AND tablename != 'ts_kv_indefinite'
                                       AND tablename != partition_by_max_ttl_date
                        LOOP
                            partition_year := SPLIT_PART(partition, '_', 3)::integer;
                            IF partition_year < partition_by_max_ttl_year::integer THEN
                                RAISE NOTICE 'Partition to delete by max ttl: %', partition;
                                EXECUTE format('DROP TABLE IF EXISTS %I', partition);
                                deleted := deleted + 1;
                            END IF;
                        END LOOP;
                ELSE
                    IF partition_type = 'MONTHS' THEN
                        IF partition_by_max_ttl_month IS NULL THEN
                            RAISE NOTICE 'Failed to remove months partitions by max ttl date due to partition_by_max_ttl_month is null!';
                        ELSE
                            FOR partition IN SELECT tablename
                                             FROM pg_tables
                                             WHERE schemaname = 'public'
                                               AND tablename like 'ts_kv_' || '%'
                                               AND tablename != 'ts_kv_latest'
                                               AND tablename != 'ts_kv_dictionary'
                                               AND tablename != 'ts_kv_indefinite'
                                               AND tablename != partition_by_max_ttl_date
                                LOOP
                                    partition_year := SPLIT_PART(partition, '_', 3)::integer;
                                    IF partition_year > partition_by_max_ttl_year::integer THEN
                                        RAISE NOTICE 'Skip iteration! Partition: % is valid!', partition;
                                        CONTINUE;
                                    ELSE
                                        IF partition_year < partition_by_max_ttl_year::integer THEN
                                            RAISE NOTICE 'Partition to delete by max ttl: %', partition;
                                            EXECUTE format('DROP TABLE IF EXISTS %I', partition);
                                            deleted := deleted + 1;
                                        ELSE
                                            partition_month := SPLIT_PART(partition, '_', 4)::integer;
                                            IF partition_year = partition_by_max_ttl_year::integer THEN
                                                IF  partition_month >= partition_by_max_ttl_month::integer THEN
                                                    RAISE NOTICE 'Skip iteration! Partition: % is valid!', partition;
                                                    CONTINUE;
                                                ELSE
                                                    RAISE NOTICE 'Partition to delete by max ttl: %', partition;
                                                    EXECUTE format('DROP TABLE IF EXISTS %I', partition);
                                                    deleted := deleted + 1;
                                                END IF;
                                            END IF;
                                        END IF;
                                    END IF;
                                END LOOP;
                        END IF;
                    ELSE
                        IF partition_type = 'DAYS' THEN
                            IF partition_by_max_ttl_month IS NULL THEN
                                RAISE NOTICE 'Failed to remove days partitions by max ttl date due to partition_by_max_ttl_month is null!';
                            ELSE
                                IF partition_by_max_ttl_day IS NULL THEN
                                    RAISE NOTICE 'Failed to remove days partitions by max ttl date due to partition_by_max_ttl_day is null!';
                                ELSE
                                    FOR partition IN SELECT tablename
                                                     FROM pg_tables
                                                     WHERE schemaname = 'public'
                                                       AND tablename like 'ts_kv_' || '%'
                                                       AND tablename != 'ts_kv_latest'
                                                       AND tablename != 'ts_kv_dictionary'
                                                       AND tablename != 'ts_kv_indefinite'
                                                       AND tablename != partition_by_max_ttl_date
                                        LOOP
                                            partition_year := SPLIT_PART(partition, '_', 3)::integer;
                                            IF partition_year > partition_by_max_ttl_year::integer THEN
                                                RAISE NOTICE 'Skip iteration! Partition: % is valid!', partition;
                                                CONTINUE;
                                            ELSE
                                                IF partition_year < partition_by_max_ttl_year::integer THEN
                                                    RAISE NOTICE 'Partition to delete by max ttl: %', partition;
                                                    EXECUTE format('DROP TABLE IF EXISTS %I', partition);
                                                    deleted := deleted + 1;
                                                ELSE
                                                    partition_month := SPLIT_PART(partition, '_', 4)::integer;
                                                    IF partition_month > partition_by_max_ttl_month::integer THEN
                                                        RAISE NOTICE 'Skip iteration! Partition: % is valid!', partition;
                                                        CONTINUE;
                                                    ELSE
                                                        IF partition_month < partition_by_max_ttl_month::integer THEN
                                                            RAISE NOTICE 'Partition to delete by max ttl: %', partition;
                                                            EXECUTE format('DROP TABLE IF EXISTS %I', partition);
                                                            deleted := deleted + 1;
                                                        ELSE
                                                            partition_day := SPLIT_PART(partition, '_', 5)::integer;
                                                            IF partition_day >= partition_by_max_ttl_day::integer THEN
                                                                RAISE NOTICE 'Skip iteration! Partition: % is valid!', partition;
                                                                CONTINUE;
                                                            ELSE
                                                                IF partition_day < partition_by_max_ttl_day::integer THEN
                                                                    RAISE NOTICE 'Partition to delete by max ttl: %', partition;
                                                                    EXECUTE format('DROP TABLE IF EXISTS %I', partition);
                                                                    deleted := deleted + 1;
                                                                END IF;
                                                            END IF;
                                                        END IF;
                                                    END IF;
                                                END IF;
                                            END IF;
                                        END LOOP;
                                END IF;
                            END IF;
                        END IF;
                    END IF;
                END IF;
            END IF;
        END IF;
    END IF;
END
$$;

-- TTL DROP PARTITIONS FUNCTIONS UPDATE END

-- RULE NODE SINGLETON MODE SUPPORT

ALTER TABLE rule_node ADD COLUMN IF NOT EXISTS singleton_mode bool DEFAULT false;

UPDATE rule_node SET singleton_mode = true WHERE type IN ('org.thingsboard.rule.engine.mqtt.azure.TbAzureIotHubNode', 'org.thingsboard.rule.engine.mqtt.TbMqttNode');

ALTER TABLE component_descriptor ADD COLUMN IF NOT EXISTS clustering_mode varchar(255) DEFAULT 'ENABLED';

UPDATE component_descriptor SET clustering_mode = 'USER_PREFERENCE' WHERE clazz = 'org.thingsboard.rule.engine.mqtt.TbMqttNode';

UPDATE component_descriptor SET clustering_mode = 'SINGLETON' WHERE clazz = 'org.thingsboard.rule.engine.mqtt.azure.TbAzureIotHubNode';


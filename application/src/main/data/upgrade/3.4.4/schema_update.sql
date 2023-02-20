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


-- ALARM ASSIGN TO USER START

ALTER TABLE alarm ADD COLUMN IF NOT EXISTS assign_ts BIGINT;
ALTER TABLE alarm ADD COLUMN IF NOT EXISTS assignee_id UUID;

CREATE INDEX IF NOT EXISTS idx_alarm_tenant_assignee_created_time ON alarm(tenant_id, assignee_id, created_time DESC);

-- ALARM ASSIGN TO USER END

-- ALARM STATUS REFACTORING START

ALTER TABLE alarm ADD COLUMN IF NOT EXISTS acknowledged boolean;
ALTER TABLE alarm ADD COLUMN IF NOT EXISTS cleared boolean;

UPDATE alarm SET acknowledged = true, cleared = true WHERE status = 'CLEARED_ACK';
UPDATE alarm SET acknowledged = true, cleared = false WHERE status = 'ACTIVE_ACK';
UPDATE alarm SET acknowledged = false, cleared = true WHERE status = 'CLEARED_UNACK';
UPDATE alarm SET acknowledged = false, cleared = false WHERE status = 'ACTIVE_UNACK';

-- Drop index by 'status' column and replace with new one that has only active alarms;
DROP INDEX IF EXISTS idx_alarm_tenant_status_created_time;
CREATE INDEX IF NOT EXISTS idx_alarm_tenant_alarm_type_created_time_active
    ON alarm USING btree (tenant_id, type, created_time DESC) WHERE cleared = false;

-- Cover index by alarm type to optimize propagated alarm queries;
CREATE INDEX IF NOT EXISTS idx_entity_alarm_entity_id_alarm_type_created_time_alarm_id ON entity_alarm
USING btree (tenant_id, entity_id, alarm_type, created_time DESC) INCLUDE(alarm_id);

ALTER TABLE alarm DROP COLUMN status;

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

CREATE TABLE IF NOT EXISTS user_settings (
    user_id uuid NOT NULL CONSTRAINT user_settings_pkey PRIMARY KEY,
    settings varchar(100000),
    CONSTRAINT fk_user_id FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE
);

-- ALARM COMMENTS END

-- ALARM INFO VIEW

DROP VIEW IF EXISTS alarm_info;
CREATE VIEW alarm_info AS
SELECT a.*,
(CASE WHEN a.acknowledged AND a.cleared THEN 'CLEARED_ACK'
      WHEN NOT a.acknowledged AND a.cleared THEN 'CLEARED_UNACK'
      WHEN a.acknowledged AND NOT a.cleared THEN 'ACTIVE_ACK'
      WHEN NOT a.acknowledged AND NOT a.cleared THEN 'ACTIVE_UNACK' END) as status,
COALESCE(CASE WHEN a.originator_type = 0 THEN (select title from tenant where id = a.originator_id)
              WHEN a.originator_type = 1 THEN (select title from customer where id = a.originator_id)
              WHEN a.originator_type = 2 THEN (select email from tb_user where id = a.originator_id)
              WHEN a.originator_type = 3 THEN (select title from dashboard where id = a.originator_id)
              WHEN a.originator_type = 4 THEN (select name from asset where id = a.originator_id)
              WHEN a.originator_type = 5 THEN (select name from device where id = a.originator_id)
              WHEN a.originator_type = 9 THEN (select name from entity_view where id = a.originator_id)
              WHEN a.originator_type = 13 THEN (select name from device_profile where id = a.originator_id)
              WHEN a.originator_type = 14 THEN (select name from asset_profile where id = a.originator_id)
              WHEN a.originator_type = 18 THEN (select name from edge where id = a.originator_id) END
    , 'Deleted') originator_name,
COALESCE(CASE WHEN a.originator_type = 0 THEN (select title from tenant where id = a.originator_id)
              WHEN a.originator_type = 1 THEN (select COALESCE(title, email) from customer where id = a.originator_id)
              WHEN a.originator_type = 2 THEN (select email from tb_user where id = a.originator_id)
              WHEN a.originator_type = 3 THEN (select title from dashboard where id = a.originator_id)
              WHEN a.originator_type = 4 THEN (select COALESCE(label, name) from asset where id = a.originator_id)
              WHEN a.originator_type = 5 THEN (select COALESCE(label, name) from device where id = a.originator_id)
              WHEN a.originator_type = 9 THEN (select name from entity_view where id = a.originator_id)
              WHEN a.originator_type = 13 THEN (select name from device_profile where id = a.originator_id)
              WHEN a.originator_type = 14 THEN (select name from asset_profile where id = a.originator_id)
              WHEN a.originator_type = 18 THEN (select COALESCE(label, name) from edge where id = a.originator_id) END
    , 'Deleted') as originator_label,
u.first_name as assignee_first_name, u.last_name as assignee_last_name, u.email as assignee_email
FROM alarm a
LEFT JOIN tb_user u ON u.id = a.assignee_id;


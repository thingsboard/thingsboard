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

CREATE TABLE IF NOT EXISTS entity_alarm (
    tenant_id uuid NOT NULL,
    entity_type varchar(32),
    entity_id uuid NOT NULL,
    created_time bigint NOT NULL,
    alarm_type varchar(255) NOT NULL,
    customer_id uuid,
    alarm_id uuid,
    CONSTRAINT entity_alarm_pkey PRIMARY KEY (entity_id, alarm_id),
    CONSTRAINT fk_entity_alarm_id FOREIGN KEY (alarm_id) REFERENCES alarm(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_alarm_tenant_status_created_time ON alarm(tenant_id, status, created_time DESC);
CREATE INDEX IF NOT EXISTS idx_entity_alarm_created_time ON entity_alarm(tenant_id, entity_id, created_time DESC);
CREATE INDEX IF NOT EXISTS idx_entity_alarm_alarm_id ON entity_alarm(alarm_id);

INSERT INTO entity_alarm(tenant_id, entity_type, entity_id, created_time, alarm_type, customer_id, alarm_id)
SELECT tenant_id,
       CASE
           WHEN originator_type = 0 THEN 'TENANT'
           WHEN originator_type = 1 THEN 'CUSTOMER'
           WHEN originator_type = 2 THEN 'USER'
           WHEN originator_type = 3 THEN 'DASHBOARD'
           WHEN originator_type = 4 THEN 'ASSET'
           WHEN originator_type = 5 THEN 'DEVICE'
           WHEN originator_type = 6 THEN 'ALARM'
           WHEN originator_type = 7 THEN 'RULE_CHAIN'
           WHEN originator_type = 8 THEN 'RULE_NODE'
           WHEN originator_type = 9 THEN 'ENTITY_VIEW'
           WHEN originator_type = 10 THEN 'WIDGETS_BUNDLE'
           WHEN originator_type = 11 THEN 'WIDGET_TYPE'
           WHEN originator_type = 12 THEN 'TENANT_PROFILE'
           WHEN originator_type = 13 THEN 'DEVICE_PROFILE'
           WHEN originator_type = 14 THEN 'API_USAGE_STATE'
           WHEN originator_type = 15 THEN 'TB_RESOURCE'
           WHEN originator_type = 16 THEN 'OTA_PACKAGE'
           WHEN originator_type = 17 THEN 'EDGE'
           WHEN originator_type = 18 THEN 'RPC'
           else 'UNKNOWN'
           END,
       originator_id,
       created_time,
       type,
       customer_id,
       id
FROM alarm
ON CONFLICT DO NOTHING;

INSERT INTO entity_alarm(tenant_id, entity_type, entity_id, created_time, alarm_type, customer_id, alarm_id)
SELECT a.tenant_id, r.from_type, r.from_id, created_time, type, customer_id, id
FROM alarm a
         INNER JOIN relation r ON r.relation_type_group = 'ALARM' and r.relation_type = 'ANY' and a.id = r.to_id
ON CONFLICT DO NOTHING;

DELETE FROM relation r WHERE r.relation_type_group = 'ALARM';
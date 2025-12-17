--
-- Copyright © 2016-2025 The Thingsboard Authors
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

DROP VIEW IF EXISTS device_info_active_attribute_view CASCADE;
CREATE OR REPLACE VIEW device_info_active_attribute_view AS
SELECT d.*
       , c.title as customer_title
       , COALESCE((c.additional_info::json->>'isPublic')::bool, FALSE) as customer_is_public
       , d.type as device_profile_name
       , COALESCE(da.bool_v, FALSE) as active
FROM device d
         LEFT JOIN customer c ON c.id = d.customer_id
         LEFT JOIN attribute_kv da ON da.entity_id = d.id AND da.attribute_type = 2 AND da.attribute_key = (select key_id from key_dictionary  where key = 'active');

DROP VIEW IF EXISTS device_info_active_ts_view CASCADE;
CREATE OR REPLACE VIEW device_info_active_ts_view AS
SELECT d.*
       , c.title as customer_title
       , COALESCE((c.additional_info::json->>'isPublic')::bool, FALSE) as customer_is_public
       , d.type as device_profile_name
       , COALESCE(dt.bool_v, FALSE) as active
FROM device d
         LEFT JOIN customer c ON c.id = d.customer_id
         LEFT JOIN ts_kv_latest dt ON dt.entity_id = d.id and dt.key = (select key_id from key_dictionary where key = 'active');

DROP VIEW IF EXISTS device_info_view CASCADE;
CREATE OR REPLACE VIEW device_info_view AS SELECT * FROM device_info_active_attribute_view;

DROP VIEW IF EXISTS alarm_info CASCADE;
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
              WHEN a.originator_type = 1 THEN (select COALESCE(NULLIF(title, ''), email) from customer where id = a.originator_id)
              WHEN a.originator_type = 2 THEN (select email from tb_user where id = a.originator_id)
              WHEN a.originator_type = 3 THEN (select title from dashboard where id = a.originator_id)
              WHEN a.originator_type = 4 THEN (select COALESCE(NULLIF(label, ''), name) from asset where id = a.originator_id)
              WHEN a.originator_type = 5 THEN (select COALESCE(NULLIF(label, ''), name) from device where id = a.originator_id)
              WHEN a.originator_type = 9 THEN (select name from entity_view where id = a.originator_id)
              WHEN a.originator_type = 13 THEN (select name from device_profile where id = a.originator_id)
              WHEN a.originator_type = 14 THEN (select name from asset_profile where id = a.originator_id)
              WHEN a.originator_type = 18 THEN (select COALESCE(NULLIF(label, ''), name) from edge where id = a.originator_id) END
    , 'Deleted') as originator_label,
u.first_name as assignee_first_name, u.last_name as assignee_last_name, u.email as assignee_email
FROM alarm a
LEFT JOIN tb_user u ON u.id = a.assignee_id;

DROP VIEW IF EXISTS edge_active_attribute_view CASCADE;
CREATE OR REPLACE VIEW edge_active_attribute_view AS
SELECT ee.id
        , ee.created_time
        , ee.additional_info
        , ee.customer_id
        , ee.root_rule_chain_id
        , ee.type
        , ee.name
        , ee.label
        , ee.routing_key
        , ee.secret
        , ee.tenant_id
        , ee.version
FROM edge ee
        JOIN attribute_kv ON ee.id = attribute_kv.entity_id
        JOIN key_dictionary ON attribute_kv.attribute_key = key_dictionary.key_id
WHERE attribute_kv.bool_v = true AND key_dictionary.key = 'active'
ORDER BY ee.id;

DROP VIEW IF EXISTS widget_type_info_view CASCADE;
CREATE OR REPLACE VIEW widget_type_info_view AS
SELECT t.*,
       COALESCE((t.descriptor::json->>'type')::text, '') as widget_type,
       array_to_json(ARRAY(
           SELECT json_build_object('id', wb.widgets_bundle_id, 'name', b.title)
           FROM widgets_bundle_widget wb
           JOIN widgets_bundle b ON wb.widgets_bundle_id = b.id
           WHERE wb.widget_type_id = t.id
           ORDER BY b.title
       )) AS bundles
FROM widget_type t;

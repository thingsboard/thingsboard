--
-- Copyright © 2016-2024 The Thingsboard Authors
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

-- NOTIFICATIONS UPDATE START

ALTER TABLE notification ADD COLUMN IF NOT EXISTS delivery_method VARCHAR(50) NOT NULL default 'WEB';

DROP INDEX IF EXISTS idx_notification_recipient_id_created_time;
DROP INDEX IF EXISTS idx_notification_recipient_id_unread;

CREATE INDEX IF NOT EXISTS idx_notification_delivery_method_recipient_id_created_time ON notification(delivery_method, recipient_id, created_time DESC);
CREATE INDEX IF NOT EXISTS idx_notification_delivery_method_recipient_id_unread ON notification(delivery_method, recipient_id) WHERE status <> 'READ';

-- migrate attributes from attribute_kv_old to attribute_kv
DO
$$
DECLARE
    row_num_old integer;
    row_num integer;
BEGIN
    IF EXISTS(SELECT 1 FROM information_schema.tables WHERE table_name = 'attribute_kv_old') THEN
        INSERT INTO attribute_kv(entity_id, attribute_type, attribute_key, bool_v, str_v, long_v, dbl_v, json_v, last_update_ts)
            SELECT a.entity_id, CASE
                        WHEN a.attribute_type = 'CLIENT_SCOPE' THEN 1
                        WHEN a.attribute_type = 'SERVER_SCOPE' THEN 2
                        WHEN a.attribute_type = 'SHARED_SCOPE' THEN 3
                        ELSE 0
                        END,
                k.key_id,  a.bool_v, a.str_v, a.long_v, a.dbl_v, a.json_v, a.last_update_ts
                FROM attribute_kv_old a INNER JOIN key_dictionary k ON (a.attribute_key = k.key);
        SELECT COUNT(*) INTO row_num_old FROM attribute_kv_old;
        SELECT COUNT(*) INTO row_num FROM attribute_kv;
        RAISE NOTICE 'Migrated % of % rows', row_num, row_num_old;

        IF row_num != 0 THEN
            DROP TABLE IF EXISTS attribute_kv_old;
        ELSE
           RAISE EXCEPTION 'Table attribute_kv is empty';
        END IF;

        CREATE INDEX IF NOT EXISTS idx_attribute_kv_by_key_and_last_update_ts ON attribute_kv(entity_id, attribute_key, last_update_ts desc);
    END IF;
EXCEPTION
    WHEN others THEN
        ROLLBACK;
        RAISE EXCEPTION 'Error during COPY: %', SQLERRM;
END
$$;

-- NOTIFICATIONS UPDATE END

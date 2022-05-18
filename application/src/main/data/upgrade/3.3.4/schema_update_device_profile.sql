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

ALTER TABLE device_profile
    ADD COLUMN IF NOT EXISTS default_queue_id uuid;

DO
$$
    BEGIN
        IF EXISTS
            (SELECT column_name
             FROM information_schema.columns
             WHERE table_name = 'device_profile'
               AND column_name = 'default_queue_name'
            )
        THEN
            UPDATE device_profile
            SET default_queue_id = q.id
            FROM queue as q
            WHERE default_queue_name = q.name;
        END IF;
    END
$$;

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'fk_default_queue_device_profile') THEN
            ALTER TABLE device_profile
                ADD CONSTRAINT fk_default_queue_device_profile FOREIGN KEY (default_queue_id) REFERENCES queue (id);
        END IF;
    END;
$$;

ALTER TABLE device_profile
    DROP COLUMN IF EXISTS default_queue_name;

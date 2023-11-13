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

-- RESOURCES UPDATE START

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name = 'resource' AND column_name = 'data' AND data_type = 'bytea') THEN
            ALTER TABLE resource RENAME COLUMN data TO base64_data;
            ALTER TABLE resource ADD COLUMN data bytea;
            UPDATE resource SET data = decode(base64_data, 'base64') WHERE base64_data IS NOT NULL;
            ALTER TABLE resource DROP COLUMN base64_data;
        END IF;
    END;
$$;
ALTER TABLE resource ADD COLUMN IF NOT EXISTS media_type varchar(255);

-- RESOURCES UPDATE END

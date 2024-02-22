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

-- RESOURCES UPDATE START

ALTER TABLE resource ADD COLUMN IF NOT EXISTS descriptor varchar;
ALTER TABLE resource ADD COLUMN IF NOT EXISTS preview bytea;
ALTER TABLE resource ADD COLUMN IF NOT EXISTS external_id uuid;
ALTER TABLE resource ADD COLUMN IF NOT EXISTS is_public boolean default true;
ALTER TABLE resource ADD COLUMN IF NOT EXISTS public_resource_key varchar(32) unique;

CREATE INDEX IF NOT EXISTS idx_resource_etag ON resource(tenant_id, etag);
CREATE INDEX IF NOT EXISTS idx_resource_type_public_resource_key ON resource(resource_type, public_resource_key);

CREATE OR REPLACE FUNCTION generate_resource_public_key()
RETURNS text AS $$
DECLARE
  chars text := 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
  result text := '';
BEGIN
  FOR i IN 1..32 LOOP
    result := result || substr(chars, floor(random()*62)::int + 1, 1);
  END LOOP;
  RETURN result;
END;
$$ LANGUAGE plpgsql;

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name = 'resource' AND column_name = 'data' AND data_type = 'bytea') THEN
            ALTER TABLE resource RENAME COLUMN data TO base64_data;
            ALTER TABLE resource ADD COLUMN data bytea;
            UPDATE resource SET data = decode(base64_data, 'base64') WHERE base64_data IS NOT NULL;
            ALTER TABLE resource DROP COLUMN base64_data;
        ELSE
            UPDATE resource SET public_resource_key = generate_resource_public_key() WHERE resource_type = 'IMAGE' AND public_resource_key IS NULL;
        END IF;
    END;
$$;

DROP FUNCTION generate_resource_public_key;

-- RESOURCES UPDATE END

CREATE INDEX IF NOT EXISTS idx_edge_event_tenant_id_edge_id_created_time ON edge_event(tenant_id, edge_id, created_time DESC);
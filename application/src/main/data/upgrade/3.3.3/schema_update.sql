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

DELETE from ota_package as op WHERE NOT EXISTS(SELECT * FROM device_profile dp where op.device_profile_id = dp.id);

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'fk_device_profile_ota_package') THEN
            ALTER TABLE ota_package
                ADD CONSTRAINT fk_device_profile_ota_package
                    FOREIGN KEY (device_profile_id) REFERENCES device_profile (id)
                        ON DELETE CASCADE;
        END IF;
    END;
$$;

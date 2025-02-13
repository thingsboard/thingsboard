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

-- UPDATE SAVE TIME SERIES NODES START

DO $$
    BEGIN
        -- Check if the rule_node table exists
        IF EXISTS (
            SELECT 1
            FROM information_schema.tables
            WHERE table_name = 'rule_node'
        ) THEN

            UPDATE rule_node
            SET configuration = (
                (configuration::jsonb - 'skipLatestPersistence')
                    || jsonb_build_object(
                        'processingSettings', jsonb_build_object(
                                'type',       'ADVANCED',
                                'timeseries', jsonb_build_object('type', 'ON_EVERY_MESSAGE'),
                                'latest',     jsonb_build_object('type', 'SKIP'),
                                'webSockets', jsonb_build_object('type', 'ON_EVERY_MESSAGE')
                                               )
                       )
                )::text,
                configuration_version = 1
            WHERE type = 'org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode'
              AND configuration_version = 0
              AND configuration::jsonb ->> 'skipLatestPersistence' = 'true';

            UPDATE rule_node
            SET configuration = (
                (configuration::jsonb - 'skipLatestPersistence')
                    || jsonb_build_object(
                        'processingSettings', jsonb_build_object(
                                'type', 'ON_EVERY_MESSAGE'
                                               )
                       )
                )::text,
                configuration_version = 1
            WHERE type = 'org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode'
              AND configuration_version = 0
              AND (configuration::jsonb ->> 'skipLatestPersistence' != 'true' OR configuration::jsonb ->> 'skipLatestPersistence' IS NULL);

        END IF;
    END;
$$;

-- UPDATE SAVE TIME SERIES NODES END

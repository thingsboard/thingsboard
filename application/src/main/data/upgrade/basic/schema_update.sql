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

-- UPDATE SAVE TIME SERIES NODES START

UPDATE rule_node
SET configuration = (
    (configuration::jsonb - 'skipLatestPersistence')
        || jsonb_build_object(
            'processingSettings', jsonb_build_object(
                    'type',       'ADVANCED',
                    'timeseries',       jsonb_build_object('type', 'ON_EVERY_MESSAGE'),
                    'latest',           jsonb_build_object('type', 'SKIP'),
                    'webSockets',       jsonb_build_object('type', 'ON_EVERY_MESSAGE'),
                    'calculatedFields', jsonb_build_object('type', 'ON_EVERY_MESSAGE')
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

-- UPDATE SAVE TIME SERIES NODES END

-- UPDATE SAVE ATTRIBUTES NODES START

UPDATE rule_node
SET configuration = (
    configuration::jsonb
        || jsonb_build_object(
            'processingSettings', jsonb_build_object('type', 'ON_EVERY_MESSAGE')
           )
    )::text,
    configuration_version = 3
WHERE type = 'org.thingsboard.rule.engine.telemetry.TbMsgAttributesNode'
  AND configuration_version = 2;

-- UPDATE SAVE ATTRIBUTES NODES END

ALTER TABLE api_usage_state ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;

-- UPDATE TENANT PROFILE CALCULATED FIELD LIMITS START

UPDATE tenant_profile
SET profile_data = profile_data
    || jsonb_build_object(
                           'configuration', profile_data->'configuration' || jsonb_build_object(
                    'maxCalculatedFieldsPerEntity', COALESCE(profile_data->'configuration'->>'maxCalculatedFieldsPerEntity', '5')::bigint,
                    'maxArgumentsPerCF', COALESCE(profile_data->'configuration'->>'maxArgumentsPerCF', '10')::bigint,
                    'maxDataPointsPerRollingArg', COALESCE(profile_data->'configuration'->>'maxDataPointsPerRollingArg', '1000')::bigint,
                    'maxStateSizeInKBytes', COALESCE(profile_data->'configuration'->>'maxStateSizeInKBytes', '32')::bigint,
                    'maxSingleValueArgumentSizeInKBytes', COALESCE(profile_data->'configuration'->>'maxSingleValueArgumentSizeInKBytes', '2')::bigint
                                                                             )
       )
WHERE profile_data->'configuration'->>'maxCalculatedFieldsPerEntity' IS NULL;

-- UPDATE TENANT PROFILE CALCULATED FIELD LIMITS END

-- UPDATE TENANT PROFILE DEBUG DURATION START

UPDATE tenant_profile
SET profile_data = jsonb_set(profile_data, '{configuration,maxDebugModeDurationMinutes}', '15', true)
WHERE
    profile_data->'configuration' ? 'maxDebugModeDurationMinutes' = false
    OR (profile_data->'configuration'->>'maxDebugModeDurationMinutes')::int = 0;

-- UPDATE TENANT PROFILE DEBUG DURATION END

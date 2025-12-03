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

-- UPDATE TENANT PROFILE CONFIGURATION START

UPDATE tenant_profile
SET profile_data = jsonb_set(
    profile_data,
    '{configuration}',
    jsonb_build_object(
        'minAllowedScheduledUpdateIntervalInSecForCF', 60,
        'maxRelationLevelPerCfArgument', 10,
        'maxRelatedEntitiesToReturnPerCfArgument', 100,
        'minAllowedDeduplicationIntervalInSecForCF', 10,
        'minAllowedAggregationIntervalInSecForCF', 60,
        'minAllowedIntermediateAggregationIntervalInSecForCF', 300,
        'cfReevaluationCheckInterval', 60,
        'alarmsReevaluationInterval', 60
    )
    ||
    jsonb_strip_nulls(profile_data -> 'configuration')
)
WHERE NOT (
    jsonb_strip_nulls(profile_data -> 'configuration') ?& ARRAY[
        'minAllowedScheduledUpdateIntervalInSecForCF',
        'maxRelationLevelPerCfArgument',
        'maxRelatedEntitiesToReturnPerCfArgument',
        'minAllowedDeduplicationIntervalInSecForCF',
        'minAllowedAggregationIntervalInSecForCF',
        'minAllowedIntermediateAggregationIntervalInSecForCF',
        'cfReevaluationCheckInterval',
        'alarmsReevaluationInterval'
    ]
);

-- UPDATE TENANT PROFILE CONFIGURATION END

-- CALCULATED FIELD UNIQUE CONSTRAINT UPDATE START

ALTER TABLE calculated_field DROP CONSTRAINT IF EXISTS calculated_field_unq_key;
ALTER TABLE calculated_field ADD CONSTRAINT calculated_field_unq_key UNIQUE (entity_id, type, name);

-- CALCULATED FIELD UNIQUE CONSTRAINT UPDATE END

-- CALCULATED FIELD OUTPUT STRATEGY UPDATE START

UPDATE calculated_field
SET configuration = jsonb_set(
        configuration::jsonb,
        '{output}',
        (configuration::jsonb -> 'output')
            || jsonb_build_object(
                'strategy',
                jsonb_build_object(
                        'type', 'RULE_CHAIN'
                )
               ),
        false
                    )
WHERE (configuration::jsonb -> 'output' -> 'strategy') IS NULL;

-- CALCULATED FIELD OUTPUT STRATEGY UPDATE END

-- REMOVAL OF CALCULATED FIELD LINKS PERSISTENCE START

DROP TABLE IF EXISTS calculated_field_link;
ANALYZE calculated_field;

-- REMOVAL OF CALCULATED FIELD LINKS PERSISTENCE END

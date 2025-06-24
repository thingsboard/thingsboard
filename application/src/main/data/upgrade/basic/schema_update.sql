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

-- UPDATE TENANT PROFILE CASSANDRA RATE LIMITS START

UPDATE tenant_profile
SET profile_data = jsonb_set(
        profile_data,
        '{configuration}',
        (
            (profile_data -> 'configuration') - 'cassandraQueryTenantRateLimitsConfiguration'
                ||
            COALESCE(
                    CASE
                        WHEN profile_data -> 'configuration' ->
                             'cassandraQueryTenantRateLimitsConfiguration' IS NOT NULL THEN
                            jsonb_build_object(
                                    'cassandraReadQueryTenantCoreRateLimits',
                                    profile_data -> 'configuration' -> 'cassandraQueryTenantRateLimitsConfiguration',
                                    'cassandraWriteQueryTenantCoreRateLimits',
                                    profile_data -> 'configuration' -> 'cassandraQueryTenantRateLimitsConfiguration',
                                    'cassandraReadQueryTenantRuleEngineRateLimits',
                                    profile_data -> 'configuration' -> 'cassandraQueryTenantRateLimitsConfiguration',
                                    'cassandraWriteQueryTenantRuleEngineRateLimits',
                                    profile_data -> 'configuration' -> 'cassandraQueryTenantRateLimitsConfiguration'
                            )
                        END,
                    '{}'::jsonb
            )
            )
                   )
WHERE profile_data -> 'configuration' ? 'cassandraQueryTenantRateLimitsConfiguration';

-- UPDATE TENANT PROFILE CASSANDRA RATE LIMITS END

-- UPDATE NOTIFICATION RULE CASSANDRA RATE LIMITS START

UPDATE notification_rule
SET trigger_config = REGEXP_REPLACE(
        trigger_config,
        '"CASSANDRA_QUERIES"',
        '"CASSANDRA_WRITE_QUERIES_CORE","CASSANDRA_READ_QUERIES_CORE","CASSANDRA_WRITE_QUERIES_RULE_ENGINE","CASSANDRA_READ_QUERIES_RULE_ENGINE","CASSANDRA_WRITE_QUERIES_MONOLITH","CASSANDRA_READ_QUERIES_MONOLITH"',
        'g'
                     )
WHERE trigger_type = 'RATE_LIMITS'
  AND trigger_config LIKE '%"CASSANDRA_QUERIES"%';

-- UPDATE NOTIFICATION RULE CASSANDRA RATE LIMITS END

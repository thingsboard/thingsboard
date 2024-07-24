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

-- USER CREDENTIALS UPDATE START

ALTER TABLE user_credentials ADD COLUMN IF NOT EXISTS activate_token_exp_time BIGINT;
-- Setting 24-hour TTL for existing activation tokens
UPDATE user_credentials SET activate_token_exp_time = cast(extract(EPOCH FROM NOW()) * 1000 AS BIGINT) + 86400000
    WHERE activate_token IS NOT NULL AND activate_token_exp_time IS NULL;

ALTER TABLE user_credentials ADD COLUMN IF NOT EXISTS reset_token_exp_time BIGINT;
-- Setting 24-hour TTL for existing password reset tokens
UPDATE user_credentials SET reset_token_exp_time = cast(extract(EPOCH FROM NOW()) * 1000 AS BIGINT) + 86400000
    WHERE reset_token IS NOT NULL AND reset_token_exp_time IS NULL;

UPDATE admin_settings SET json_value = (json_value::jsonb || '{"userActivationTokenTtl":24,"passwordResetTokenTtl":24}'::jsonb)::varchar
    WHERE key = 'securitySettings';

-- USER CREDENTIALS UPDATE END

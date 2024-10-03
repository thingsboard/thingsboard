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

ALTER TABLE user_credentials ADD COLUMN IF NOT EXISTS last_login_ts BIGINT;
UPDATE user_credentials c SET last_login_ts = (SELECT (additional_info::json ->> 'lastLoginTs')::bigint FROM tb_user u WHERE u.id = c.user_id)
  WHERE last_login_ts IS NULL;

ALTER TABLE user_credentials ADD COLUMN IF NOT EXISTS failed_login_attempts INT;
UPDATE user_credentials c SET failed_login_attempts = (SELECT (additional_info::json ->> 'failedLoginAttempts')::int FROM tb_user u WHERE u.id = c.user_id)
  WHERE failed_login_attempts IS NULL;

UPDATE tb_user SET additional_info = (additional_info::jsonb - 'lastLoginTs' - 'failedLoginAttempts' - 'userCredentialsEnabled')::text
  WHERE additional_info IS NOT NULL AND additional_info != 'null';

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

CREATE TABLE IF NOT EXISTS alarm_comment (
    id uuid NOT NULL,
    created_time bigint NOT NULL,
    alarm_id uuid NOT NULL,
    user_id uuid,
    type varchar(255) NOT NULL,
    comment varchar(10000),
    CONSTRAINT fk_alarm_comment_alarm_id FOREIGN KEY (alarm_id) REFERENCES alarm(id) ON DELETE CASCADE
) PARTITION BY RANGE (created_time);
CREATE INDEX IF NOT EXISTS idx_alarm_comment_alarm_id ON alarm_comment(alarm_id);

CREATE TABLE IF NOT EXISTS user_settings (
    user_id uuid NOT NULL CONSTRAINT user_settings_pkey PRIMARY KEY,
    settings varchar(100000),
    CONSTRAINT fk_user_id FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE
);

ALTER TABLE user_credentials
    ADD COLUMN IF NOT EXISTS additional_info varchar NULL;
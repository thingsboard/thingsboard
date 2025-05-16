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

CREATE TABLE ai_settings (
    id           UUID          NOT NULL PRIMARY KEY,
    created_time BIGINT        NOT NULL,
    tenant_id    UUID          NOT NULL,
    version      BIGINT        NOT NULL DEFAULT 1,
    name         VARCHAR(255)  NOT NULL,
    provider     VARCHAR(255)  NOT NULL,
    model        VARCHAR(255)  NOT NULL,
    api_key      VARCHAR(1000) NOT NULL,
    CONSTRAINT ai_settings_name_unq_key UNIQUE (tenant_id, name)
);

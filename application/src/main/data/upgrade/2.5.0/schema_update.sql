--
-- Copyright © 2016-2020 The Thingsboard Authors
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

DROP TABLE IF EXISTS provision_profile;

CREATE TABLE IF NOT EXISTS provision_profile (
    id varchar(31) NOT NULL CONSTRAINT provision_profile_pkey PRIMARY KEY,
    key varchar(255),
    secret varchar(255),
    customer_id varchar(31),
    tenant_id varchar(31),
    provision_request_validation_strategy_type varchar(255),
    CONSTRAINT provision_profile_unq_key UNIQUE (key)
);
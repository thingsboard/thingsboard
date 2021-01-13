--
-- Copyright © 2016-2021 The Thingsboard Authors
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

CREATE TABLE IF NOT EXISTS entity_views (
    id varchar(31) NOT NULL CONSTRAINT entity_views_pkey PRIMARY KEY,
    entity_id varchar(31),
    entity_type varchar(255),
    tenant_id varchar(31),
    customer_id varchar(31),
    name varchar(255),
    keys varchar(255),
    start_ts bigint,
    end_ts bigint,
    search_text varchar(255),
    additional_info varchar
);

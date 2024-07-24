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

-- KV VERSIONING UPDATE START

CREATE SEQUENCE IF NOT EXISTS attribute_kv_version_seq cache 1000;
CREATE SEQUENCE IF NOT EXISTS ts_kv_latest_version_seq cache 1000;

ALTER TABLE attribute_kv ADD COLUMN version bigint default 0;
ALTER TABLE ts_kv_latest ADD COLUMN version bigint default 0;

-- KV VERSIONING UPDATE END

-- RELATION VERSIONING UPDATE START

CREATE SEQUENCE IF NOT EXISTS relation_version_seq cache 1000;
ALTER TABLE relation ADD COLUMN version bigint default 0;

-- RELATION VERSIONING UPDATE END

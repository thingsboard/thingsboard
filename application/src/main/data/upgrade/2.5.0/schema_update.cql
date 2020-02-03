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

DROP MATERIALIZED VIEW IF EXISTS thingsboard.provision_profile_by_key;

DROP TABLE IF EXISTS thingsboard.provision_profile;

CREATE TABLE IF NOT EXISTS thingsboard.provision_profile (
    id timeuuid PRIMARY KEY,
    tenant_id timeuuid,
    customer_id timeuuid,
    key text,
    secret text,
    pre_provision_allowed boolean,
);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.provision_profile_by_key AS
	SELECT *
	from thingsboard.provision_profile
	WHERE key IS NOT NULL AND id IS NOT NULL
	PRIMARY KEY ( key, id );
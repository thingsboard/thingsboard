--
-- Copyright © 2016-2018 The Thingsboard Authors
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

DROP MATERIALIZED VIEW IF EXISTS thingsboard.entity_views_by_tenant_and_name;
DROP MATERIALIZED VIEW IF EXISTS thingsboard.entity_views_by_tenant_and_entity;
DROP MATERIALIZED VIEW IF EXISTS thingsboard.entity_views_by_tenant_and_customer;
DROP MATERIALIZED VIEW IF EXISTS thingsboard.entity_views_by_tenant_and_customer_and_entity;

DROP TABLE IF EXISTS thingsboard.entity_views;

CREATE TABLE IF NOT EXISTS thingsboard.entity_views (
    id timeuuid,
    entity_id timeuuid,
    entity_type text,
    tenant_id timeuuid,
    customer_id timeuuid,
    name text,
    keys text,
    ts_begin bigint,
    ts_end bigint,
    search_text text,
    additional_info text,
    PRIMARY KEY (id, entity_id, tenant_id, customer_id)
);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.entity_views_by_tenant_and_name AS
    SELECT *
    from thingsboard.entity_views
    WHERE entity_id IS NOT NULL
    AND tenant_id IS NOT NULL
    AND customer_id IS NOT NULL
    AND keys IS NOT NULL
    AND ts_begin IS NOT NULL
    AND ts_end IS NOT NULL
    AND name IS NOT NULL
    AND id IS NOT NULL
    PRIMARY KEY (tenant_id, name, id, entity_id, customer_id)
    WITH CLUSTERING ORDER BY (name ASC, id DESC, entity_id DESC, customer_id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.entity_views_by_tenant_and_entity AS
    SELECT *
    from thingsboard.entity_views
    WHERE entity_id IS NOT NULL
    AND tenant_id IS NOT NULL
    AND customer_id IS NOT NULL
    AND keys IS NOT NULL
    AND ts_begin IS NOT NULL
    AND ts_end IS NOT NULL
    AND name IS NOT NULL
    AND id IS NOT NULL
    PRIMARY KEY (tenant_id, entity_id, id, customer_id, name)
    WITH CLUSTERING ORDER BY (entity_id ASC, customer_id ASC, id DESC, name DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.entity_views_by_tenant_and_customer AS
    SELECT *
    from thingsboard.entity_views
    WHERE entity_id IS NOT NULL
    AND tenant_id IS NOT NULL
    AND customer_id IS NOT NULL
    AND keys IS NOT NULL
    AND ts_begin IS NOT NULL
    AND ts_end IS NOT NULL
    AND name IS NOT NULL
    AND id IS NOT NULL
    PRIMARY KEY (tenant_id, customer_id, id, entity_id, name)
    WITH CLUSTERING ORDER BY (customer_id ASC, id DESC, entity_id DESC, name DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.entity_views_by_tenant_and_customer_and_entity AS
    SELECT *
    from thingsboard.entity_views
    WHERE entity_id IS NOT NULL
    AND tenant_id IS NOT NULL
    AND customer_id IS NOT NULL
    AND keys IS NOT NULL
    AND ts_begin IS NOT NULL
    AND ts_end IS NOT NULL
    AND name IS NOT NULL
    AND id IS NOT NULL
    PRIMARY KEY (tenant_id, customer_id, entity_id, id, name)
    WITH CLUSTERING ORDER BY (customer_id ASC, entity_id DESC, id DESC, name DESC);

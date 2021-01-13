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

CREATE TABLE IF NOT EXISTS thingsboard.entity_views (
    id timeuuid,
    entity_id timeuuid,
    entity_type text,
    tenant_id timeuuid,
    customer_id timeuuid,
    name text,
    keys text,
    start_ts bigint,
    end_ts bigint,
    search_text text,
    additional_info text,
    PRIMARY KEY (id, entity_id, tenant_id, customer_id)
);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.entity_view_by_tenant_and_name AS
    SELECT *
    from thingsboard.entity_views
    WHERE tenant_id IS NOT NULL
        AND entity_id IS NOT NULL
        AND customer_id IS NOT NULL
        AND name IS NOT NULL
        AND id IS NOT NULL
    PRIMARY KEY (tenant_id, name, id, customer_id, entity_id)
    WITH CLUSTERING ORDER BY (name ASC, id DESC, customer_id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.entity_view_by_tenant_and_search_text AS
    SELECT *
    from thingsboard.entity_views
    WHERE tenant_id IS NOT NULL
      AND entity_id IS NOT NULL
      AND customer_id IS NOT NULL
      AND search_text IS NOT NULL
      AND id IS NOT NULL
    PRIMARY KEY (tenant_id, search_text, id, customer_id, entity_id)
    WITH CLUSTERING ORDER BY (search_text ASC, id DESC, customer_id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.entity_view_by_tenant_and_customer AS
    SELECT *
    from thingsboard.entity_views
    WHERE tenant_id IS NOT NULL
      AND customer_id IS NOT NULL
      AND entity_id IS NOT NULL
      AND search_text IS NOT NULL
      AND id IS NOT NULL
    PRIMARY KEY (tenant_id, customer_id, search_text, id, entity_id)
    WITH CLUSTERING ORDER BY (customer_id DESC, search_text ASC, id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.entity_view_by_tenant_and_entity_id AS
    SELECT *
    from thingsboard.entity_views
    WHERE tenant_id IS NOT NULL
      AND customer_id IS NOT NULL
      AND entity_id IS NOT NULL
      AND search_text IS NOT NULL
      AND id IS NOT NULL
    PRIMARY KEY (tenant_id, entity_id, customer_id, search_text, id)
    WITH CLUSTERING ORDER BY (entity_id DESC, customer_id DESC, search_text ASC, id DESC);
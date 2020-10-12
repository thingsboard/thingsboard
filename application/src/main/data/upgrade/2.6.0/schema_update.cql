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

DROP MATERIALIZED VIEW IF EXISTS thingsboard.rule_chain_by_tenant_and_search_text;

DROP TABLE IF EXISTS thingsboard.rule_chain;

CREATE TABLE IF NOT EXISTS thingsboard.rule_chain (
    id uuid,
    tenant_id uuid,
    name text,
    type text,
    search_text text,
    first_rule_node_id uuid,
    root boolean,
    debug_mode boolean,
    configuration text,
    additional_info text,
    PRIMARY KEY (id, tenant_id, type)
);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.rule_chain_by_tenant_and_search_text AS
    SELECT *
    from thingsboard.rule_chain
    WHERE tenant_id IS NOT NULL AND search_text IS NOT NULL AND id IS NOT NULL AND type IS NOT NULL
    PRIMARY KEY ( tenant_id, search_text, id, type )
    WITH CLUSTERING ORDER BY ( search_text ASC, id DESC );

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.rule_chain_by_tenant_by_type_and_search_text AS
    SELECT *
    from thingsboard.rule_chain
    WHERE tenant_id IS NOT NULL AND search_text IS NOT NULL AND id IS NOT NULL AND type IS NOT NULL
    PRIMARY KEY ( tenant_id, type, search_text, id )
    WITH CLUSTERING ORDER BY ( type ASC, search_text ASC, id DESC );

CREATE TABLE IF NOT EXISTS thingsboard.edge (
    id timeuuid,
    tenant_id timeuuid,
    customer_id timeuuid,
    root_rule_chain_id timeuuid,
    type text,
    name text,
    label text,
    search_text text,
    routing_key text,
    secret text,
    edge_license_key text,
    cloud_endpoint text,
    configuration text,
    additional_info text,
    PRIMARY KEY (id, tenant_id, customer_id, type)
);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.edge_by_tenant_and_name AS
    SELECT *
    from thingsboard.edge
    WHERE tenant_id IS NOT NULL AND customer_id IS NOT NULL AND type IS NOT NULL AND name IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( tenant_id, name, id, customer_id, type)
    WITH CLUSTERING ORDER BY ( name ASC, id DESC, customer_id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.edge_by_routing_key AS
    SELECT *
    from thingsboard.edge
    WHERE tenant_id IS NOT NULL AND customer_id IS NOT NULL AND type IS NOT NULL AND routing_key IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( routing_key, tenant_id, id, customer_id, type)
    WITH CLUSTERING ORDER BY ( tenant_id DESC, id DESC, customer_id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.edge_by_tenant_and_search_text AS
    SELECT *
    from thingsboard.edge
    WHERE tenant_id IS NOT NULL AND customer_id IS NOT NULL AND type IS NOT NULL AND search_text IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( tenant_id, search_text, id, customer_id, type)
    WITH CLUSTERING ORDER BY ( search_text ASC, id DESC, customer_id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.edge_by_tenant_by_type_and_search_text AS
    SELECT *
    from thingsboard.edge
    WHERE tenant_id IS NOT NULL AND customer_id IS NOT NULL AND type IS NOT NULL AND search_text IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( tenant_id, type, search_text, id, customer_id)
    WITH CLUSTERING ORDER BY ( type ASC, search_text ASC, id DESC, customer_id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.edge_by_customer_and_search_text AS
    SELECT *
    from thingsboard.edge
    WHERE tenant_id IS NOT NULL AND customer_id IS NOT NULL AND type IS NOT NULL AND search_text IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( customer_id, tenant_id, search_text, id, type )
    WITH CLUSTERING ORDER BY ( tenant_id DESC, search_text ASC, id DESC );

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.edge_by_customer_by_type_and_search_text AS
    SELECT *
    from thingsboard.edge
    WHERE tenant_id IS NOT NULL AND customer_id IS NOT NULL AND type IS NOT NULL AND search_text IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( customer_id, tenant_id, type, search_text, id )
    WITH CLUSTERING ORDER BY ( tenant_id DESC, type ASC, search_text ASC, id DESC );

CREATE TABLE IF NOT EXISTS thingsboard.edge_event (
	id timeuuid,
	tenant_id timeuuid,
    edge_id timeuuid,
	edge_event_type text,
	edge_event_action text,
	edge_event_uid text,
	entity_id timeuuid,
	body text,
	PRIMARY KEY ((tenant_id, edge_id), edge_event_type, edge_event_uid)
);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.edge_event_by_id AS
    SELECT *
    FROM thingsboard.edge_event
    WHERE tenant_id IS NOT NULL AND edge_id IS NOT NULL AND edge_event_type IS NOT NULL
    AND id IS NOT NULL AND edge_event_uid IS NOT NULL
    PRIMARY KEY ((tenant_id, edge_id), id, edge_event_type, edge_event_uid)
    WITH CLUSTERING ORDER BY (id ASC);
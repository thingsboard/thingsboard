--
-- SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
-- SPDX-License-Identifier: BUSL-1.1
--

-- Tenant-scoped paging over calculated fields (WHERE tenant_id = ? ORDER BY id LIMIT ?), issued for every tenant
-- when the calculated field actors initialize, otherwise scans the whole table and sorts on each call.
CREATE INDEX IF NOT EXISTS idx_calculated_field_tenant_id_id ON calculated_field(tenant_id, id);

-- Edge status page lists the notification requests of one edge (WHERE originator_entity_id = ? ORDER BY created_time DESC),
-- otherwise scans every notification request of the tenant. Partial, so only edge-originated requests are indexed.
CREATE INDEX IF NOT EXISTS idx_notification_request_edge_originator_created_time ON notification_request(originator_entity_id, created_time DESC)
    WHERE originator_entity_type = 'EDGE';

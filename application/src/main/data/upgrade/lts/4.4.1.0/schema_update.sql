--
-- SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
-- SPDX-License-Identifier: BUSL-1.1
--

-- Tenant-scoped paging over calculated fields (WHERE tenant_id = ? ORDER BY id LIMIT ?), issued for every tenant
-- when the calculated field actors initialize, otherwise scans the whole table and sorts on each call.
CREATE INDEX IF NOT EXISTS idx_calculated_field_tenant_id_id ON calculated_field(tenant_id, id);

-- Keyset paging over a tenant's devices and assets (WHERE tenant_id = ? AND id > ? ORDER BY id LIMIT ?), issued when
-- tb-core and the calculated field actors initialize. On a live system with many devices the indexes can be created
-- beforehand with CREATE INDEX CONCURRENTLY under the same names; these statements then skip them.
CREATE INDEX IF NOT EXISTS idx_device_tenant_id_id ON device(tenant_id, id);
CREATE INDEX IF NOT EXISTS idx_asset_tenant_id_id ON asset(tenant_id, id);

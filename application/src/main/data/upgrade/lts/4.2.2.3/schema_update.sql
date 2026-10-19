--
-- SPDX-FileCopyrightText: Copyright The Thingsboard Authors
-- SPDX-License-Identifier: Apache-2.0
--

-- IOT HUB INSTALLED ITEM START

CREATE TABLE IF NOT EXISTS iot_hub_installed_item (
    id              UUID          NOT NULL PRIMARY KEY,
    created_time    BIGINT        NOT NULL,
    tenant_id       UUID          NOT NULL,
    item_id         UUID          NOT NULL,
    item_version_id UUID          NOT NULL,
    item_name       VARCHAR       NOT NULL,
    item_type       VARCHAR       NOT NULL,
    version         VARCHAR       NOT NULL,
    descriptor      JSONB         NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_iot_hub_installed_item_tenant_id ON iot_hub_installed_item(tenant_id);

CREATE INDEX IF NOT EXISTS idx_iot_hub_installed_item_item_type ON iot_hub_installed_item(tenant_id, item_type);

CREATE INDEX IF NOT EXISTS idx_iot_hub_installed_item_item_id ON iot_hub_installed_item(tenant_id, item_id);

-- IOT HUB INSTALLED ITEM END

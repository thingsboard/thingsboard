--
-- SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
-- SPDX-License-Identifier: BUSL-1.1
--

-- DEVICE CERTIFICATE REVOCATION START

CREATE TABLE IF NOT EXISTS device_certificate_revocation (
    id uuid NOT NULL CONSTRAINT device_certificate_revocation_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid NOT NULL,
    device_id uuid,
    certificate_hash varchar(255) NOT NULL,
    serial_number varchar(255),
    issuer varchar,
    subject varchar,
    not_before bigint NOT NULL,
    not_after bigint NOT NULL,
    reason varchar(255),
    revoked_by uuid,
    CONSTRAINT device_certificate_revocation_hash_unq_key UNIQUE (tenant_id, certificate_hash)
);

-- DEVICE CERTIFICATE REVOCATION END

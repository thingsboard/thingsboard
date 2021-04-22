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

CREATE TABLE IF NOT EXISTS resource (
    id uuid NOT NULL CONSTRAINT resource_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid NOT NULL,
    title varchar(255) NOT NULL,
    resource_type varchar(32) NOT NULL,
    resource_key varchar(255) NOT NULL,
    search_text varchar(255),
    file_name varchar(255) NOT NULL,
    data varchar,
    CONSTRAINT resource_unq_key UNIQUE (tenant_id, resource_type, resource_key)
);

CREATE TABLE IF NOT EXISTS firmware (
    id uuid NOT NULL CONSTRAINT firmware_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid NOT NULL,
    title varchar(255) NOT NULL,
    version varchar(255) NOT NULL,
    file_name varchar(255),
    content_type varchar(255),
    checksum_algorithm varchar(32),
    checksum varchar(1020),
    data bytea,
    additional_info varchar,
    search_text varchar(255),
    CONSTRAINT firmware_tenant_title_version_unq_key UNIQUE (tenant_id, title, version)
);

ALTER TABLE device_profile
    ADD COLUMN IF NOT EXISTS firmware_id uuid;

ALTER TABLE device
    ADD COLUMN IF NOT EXISTS firmware_id uuid;

DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_firmware_device_profile') THEN
            ALTER TABLE device_profile
                ADD CONSTRAINT fk_firmware_device_profile
                    FOREIGN KEY (firmware_id) REFERENCES firmware(id);
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_firmware_device') THEN
            ALTER TABLE device
                ADD CONSTRAINT fk_firmware_device
                    FOREIGN KEY (firmware_id) REFERENCES firmware(id);
        END IF;
    END;
$$;

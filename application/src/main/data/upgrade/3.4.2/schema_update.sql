--
-- Copyright © 2016-2022 The Thingsboard Authors
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


CREATE TABLE IF NOT EXISTS notification_target (
    id UUID NOT NULL CONSTRAINT notification_target_pkey PRIMARY KEY,
    created_time BIGINT NOT NULL,
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    configuration varchar(1000) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_notification_target_tenant_id_and_created_time ON notification_target(tenant_id, created_time DESC);

CREATE TABLE IF NOT EXISTS notification_rule (
    id UUID NOT NULL CONSTRAINT notification_rule_pkey PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS notification_request (
    id UUID NOT NULL CONSTRAINT notification_request_pkey PRIMARY KEY,
    created_time BIGINT NOT NULL,
    tenant_id UUID NOT NULL,
    target_id UUID NOT NULL CONSTRAINT fk_notification_request_target_id REFERENCES notification_target(id),
    notification_reason VARCHAR NOT NULL,
    text_template VARCHAR NOT NULL,
    notification_info VARCHAR(1000),
    notification_severity VARCHAR(32),
    additional_config VARCHAR(1000),
    status VARCHAR(32),
    rule_id UUID NULL CONSTRAINT fk_notification_request_rule_id REFERENCES notification_rule(id),
    alarm_id UUID
);
CREATE INDEX IF NOT EXISTS idx_notification_request_tenant_id_and_created_time ON notification_request(tenant_id, created_time DESC);

CREATE TABLE IF NOT EXISTS notification (
    id UUID NOT NULL,
    created_time BIGINT NOT NULL,
    request_id UUID NOT NULL CONSTRAINT fk_notification_request_id
        REFERENCES notification_request(id) ON DELETE CASCADE,
    recipient_id UUID NOT NULL,
    reason VARCHAR NOT NULL,
    text VARCHAR NOT NULL,
    info VARCHAR(1000),
    severity VARCHAR(32),
    status VARCHAR(32)
) PARTITION BY RANGE (created_time);
CREATE INDEX IF NOT EXISTS idx_notification_id ON notification(id);
CREATE INDEX IF NOT EXISTS idx_notification_notification_request_id ON notification(request_id);
CREATE INDEX IF NOT EXISTS idx_notification_recipient_id_and_created_time ON notification(recipient_id, created_time DESC);

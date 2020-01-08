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

DROP INDEX IF EXISTS idx_alarm_originator_alarm_type;

CREATE INDEX IF NOT EXISTS idx_alarm_originator_alarm_type ON alarm(originator_id, type, start_ts DESC);

CREATE INDEX IF NOT EXISTS idx_device_customer_id ON device(tenant_id, customer_id);

CREATE INDEX IF NOT EXISTS idx_device_customer_id_and_type ON device(tenant_id, customer_id, type);

CREATE INDEX IF NOT EXISTS idx_device_type ON device(tenant_id, type);

CREATE INDEX IF NOT EXISTS idx_asset_customer_id ON asset(tenant_id, customer_id);

CREATE INDEX IF NOT EXISTS idx_asset_customer_id_and_type ON asset(tenant_id, customer_id, type);

CREATE INDEX IF NOT EXISTS idx_asset_type ON asset(tenant_id, type);
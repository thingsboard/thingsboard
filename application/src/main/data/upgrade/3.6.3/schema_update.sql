--
-- Copyright © 2016-2024 The Thingsboard Authors
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

-- NOTIFICATIONS UPDATE START

ALTER TABLE notification ADD COLUMN IF NOT EXISTS delivery_method VARCHAR(50) NOT NULL default 'WEB';

DROP INDEX IF EXISTS idx_notification_recipient_id_created_time;
DROP INDEX IF EXISTS idx_notification_recipient_id_unread;

CREATE INDEX IF NOT EXISTS idx_notification_delivery_method_recipient_id_created_time ON notification(delivery_method, recipient_id, created_time DESC);
CREATE INDEX IF NOT EXISTS idx_notification_delivery_method_recipient_id_unread ON notification(delivery_method, recipient_id) WHERE status <> 'READ';

-- NOTIFICATIONS UPDATE END

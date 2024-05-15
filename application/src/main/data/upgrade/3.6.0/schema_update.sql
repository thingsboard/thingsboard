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

ALTER TABLE widget_type
    ADD COLUMN IF NOT EXISTS tags text[];

ALTER TABLE widgets_bundle
    ADD COLUMN IF NOT EXISTS widgets_bundle_order int;

ALTER TABLE api_usage_state ADD COLUMN IF NOT EXISTS tbel_exec varchar(32);
UPDATE api_usage_state SET tbel_exec = js_exec WHERE tbel_exec IS NULL;

ALTER TABLE notification DROP CONSTRAINT IF EXISTS fk_notification_request_id;
ALTER TABLE notification DROP CONSTRAINT IF EXISTS fk_notification_recipient_id;
CREATE INDEX IF NOT EXISTS idx_notification_notification_request_id ON notification(request_id);
CREATE INDEX IF NOT EXISTS idx_notification_request_tenant_id ON notification_request(tenant_id);

-- DELETE invalid records from M:N widgets_bundle_widget table caused by the bug in previous upgrade script;
DELETE
FROM widgets_bundle_widget wbw
WHERE (SELECT tenant_id FROM widgets_bundle wb WHERE wb.id = wbw.widgets_bundle_id) !=
      (SELECT tenant_id FROM widget_type wt WHERE wt.id = wbw.widget_type_id);


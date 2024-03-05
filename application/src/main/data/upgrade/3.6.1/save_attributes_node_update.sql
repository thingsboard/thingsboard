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

UPDATE rule_node SET
    configuration = (configuration::jsonb || jsonb_build_object(
        'notifyDevice',
        CASE WHEN configuration::jsonb ->> 'notifyDevice' = 'false' THEN false ELSE true END,
        'sendAttributesUpdatedNotification',
        CASE WHEN configuration::jsonb ->> 'sendAttributesUpdatedNotification' = 'true' THEN true ELSE false END,
        'updateAttributesOnlyOnValueChange',
        CASE WHEN configuration::jsonb ->> 'updateAttributesOnlyOnValueChange' = 'false' THEN false ELSE true END)::jsonb)::varchar,
    configuration_version = 2
WHERE type = 'org.thingsboard.rule.engine.telemetry.TbMsgAttributesNode' AND configuration_version = 1;

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

-- FIX DASHBOARD TEMPLATES AFTER ANGULAR MIGRATION TO VER.15

UPDATE dashboard SET configuration = REPLACE(configuration, 'mat-button mat-icon-button', 'mat-icon-button')
        WHERE configuration like '%mat-button mat-icon-button%';

UPDATE widget_type SET descriptor = REPLACE(descriptor, 'mat-button mat-icon-button', 'mat-icon-button')
        WHERE descriptor like '%mat-button mat-icon-button%';

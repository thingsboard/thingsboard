///
/// Copyright © 2016-2025 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component } from '@angular/core';
import { EntityTabsComponent } from '../../components/entity/entity-tabs.component';
import { CalculatedFieldEventBody, DebugEventType, EventType } from '@shared/models/event.models';
import type {
  CalculatedFieldsTableConfig,
  CalculatedFieldsTableEntity
} from '@home/components/calculated-fields/calculated-fields-table-config';
import { debugCfActionEnabled } from '@shared/models/calculated-field.models';

@Component({
  selector: 'tb-alarm-rules-tabs',
  templateUrl: './alarm-rules-tabs.component.html',
  styleUrls: []
})
export class AlarmRulesTabsComponent extends EntityTabsComponent<CalculatedFieldsTableEntity> {

  readonly DebugEventType = DebugEventType;
  readonly EventType = EventType;

  constructor() {
    super();
  }

  get debugActionDisabled(): boolean {
    return !debugCfActionEnabled(this.entity);
  };

  onDebugEventSelected(event: CalculatedFieldEventBody) {
    (this.entitiesTableConfig as CalculatedFieldsTableConfig).getTestScriptDialog(this.entity, JSON.parse(event.arguments))
      .subscribe((expression) => {
      });
  };
}

// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { EntityTabsComponent } from '../../components/entity/entity-tabs.component';
import { CalculatedFieldEventBody, DebugEventType, EventType } from '@shared/models/event.models';
import type {
  AlarmRulesTableConfig,
  AlarmRuleTableEntity
} from '@home/components/alarm-rules/alarm-rules-table-config';
import { debugCfActionEnabled } from '@shared/models/calculated-field.models';

@Component({
    selector: 'tb-alarm-rules-tabs',
    templateUrl: './alarm-rules-tabs.component.html',
    styleUrls: [],
    standalone: false
})
export class AlarmRulesTabsComponent extends EntityTabsComponent<AlarmRuleTableEntity> {

  readonly DebugEventType = DebugEventType;
  readonly EventType = EventType;

  constructor() {
    super();
  }

  get debugActionDisabled(): boolean {
    return !debugCfActionEnabled(this.entity);
  };

  onDebugEventSelected(event: CalculatedFieldEventBody) {
    (this.entitiesTableConfig as AlarmRulesTableConfig).getTestScriptDialog(this.entity, JSON.parse(event.arguments))
      .subscribe((_expression) => { });
  };
}

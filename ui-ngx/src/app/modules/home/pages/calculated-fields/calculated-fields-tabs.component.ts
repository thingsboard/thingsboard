// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { EntityTabsComponent } from '../../components/entity/entity-tabs.component';
import { CalculatedFieldEventBody, DebugEventType, EventType } from '@shared/models/event.models';
import type {
  CalculatedFieldsTableConfig,
  CalculatedFieldsTableEntity
} from '@home/components/calculated-fields/calculated-fields-table-config';
import { debugCfActionEnabled } from '@shared/models/calculated-field.models';

@Component({
    selector: 'tb-calculated-fields-tabs',
    templateUrl: './calculated-fields-tabs.component.html',
    styleUrls: [],
    standalone: false
})
export class CalculatedFieldsTabsComponent extends EntityTabsComponent<CalculatedFieldsTableEntity> {

  readonly DebugEventType = DebugEventType;
  readonly EventType = EventType;

  constructor() {
    super();
  }

  get debugActionDisabled(): boolean {
    return !debugCfActionEnabled(this.entity);
  };

  onDebugEventSelected(event: CalculatedFieldEventBody) {
    (this.entitiesTableConfig as CalculatedFieldsTableConfig).getTestScriptDialog(this.entity, JSON.parse(event.arguments), false)
      .subscribe((expression) => {
        (this.entitiesTableConfig as CalculatedFieldsTableConfig).getTable();
        const entityDetailsPanel = this.entitiesTableConfig.getTable().entityDetailsPanel;
        entityDetailsPanel.onToggleEditMode(true);
        entityDetailsPanel.selectedTab = 0;
        setTimeout(() => {
          entityDetailsPanel.detailsForm.get('configuration').setValue({...this.entity.configuration, expression});
          entityDetailsPanel.detailsForm.get('configuration').markAsDirty();
        });
      });
  };
}

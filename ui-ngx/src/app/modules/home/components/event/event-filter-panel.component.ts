///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { Component, Inject, InjectionToken } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { OverlayRef } from '@angular/cdk/overlay';
import { EntityType } from '@shared/models/entity-type.models';
import { FilterEvent } from '@shared/models/event.models';

export const EVENT_FILTER_PANEL_DATA = new InjectionToken<any>('AlarmFilterPanelData');

export interface EventFilterPanelData {
  filterParams: FilterEvent;
  columns: Array<FilterEntityColumn>;
}

export interface FilterEntityColumn {
  key: string;
  title: string;
}


@Component({
  selector: 'tb-event-filter-panel',
  templateUrl: './event-filter-panel.component.html',
  styleUrls: ['./event-filter-panel.component.scss']
})
export class EventFilterPanelComponent {

  private readonly excludeColumns = ['createdTime'];

  private readonly convertNameMap = new Map<string, string>([
    ['data', 'dataSearch'],
    ['metadata', 'metadataSearch'],
    ['entity', 'entityName'],
    ['error', 'isError']
  ]);

  eventFilterFormGroup: FormGroup;

  result: EventFilterPanelData;

  eventTypes = ['IN', 'OUT'];
  entityTypes = Object.keys(EntityType);

  showColumns: FilterEntityColumn[] = [];

  constructor(@Inject(EVENT_FILTER_PANEL_DATA)
              public data: EventFilterPanelData,
              public overlayRef: OverlayRef,
              private fb: FormBuilder) {
    this.eventFilterFormGroup = this.fb.group({});
    this.data.columns.forEach((column) => {
      if (!this.excludeColumns.includes(column.key)) {
        if (this.convertNameMap.has(column.key)) {
          column.key = this.convertNameMap.get(column.key);
        }
        this.showColumns.push(column);
        this.eventFilterFormGroup.addControl(column.key, this.fb.control(this.data.filterParams[column.key] || null));
      }
    });
  }

  update() {
    const filter = Object.fromEntries(Object.entries(this.eventFilterFormGroup.value).filter(([_, v]) => v != null && v !== ''));
    this.result = {
      filterParams: filter,
      columns: this.data.columns
    };
    this.overlayRef.dispose();
  }

  cancel() {
    this.overlayRef.dispose();
  }
}


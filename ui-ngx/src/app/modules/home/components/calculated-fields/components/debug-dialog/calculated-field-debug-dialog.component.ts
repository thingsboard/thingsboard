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

import { AfterViewInit, Component, Inject, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';
import { CalculatedFieldEventBody, DebugEventType, Event, EventType } from '@shared/models/event.models';
import { EventTableComponent } from '@home/components/event/event-table.component';
import {
  CalculatedField,
  CalculatedFieldTestScriptFn,
  CalculatedFieldType
} from '@shared/models/calculated-field.models';

export interface CalculatedFieldDebugDialogData {
  tenantId: string;
  value: CalculatedField;
  getTestScriptDialogFn: CalculatedFieldTestScriptFn;
}

@Component({
  selector: 'tb-calculated-field-debug-dialog',
  styleUrls: ['calculated-field-debug-dialog.component.scss'],
  templateUrl: './calculated-field-debug-dialog.component.html',
})
export class CalculatedFieldDebugDialogComponent extends DialogComponent<CalculatedFieldDebugDialogComponent, string> implements AfterViewInit {

  @ViewChild(EventTableComponent, {static: true}) eventsTable: EventTableComponent;

  readonly DebugEventType = DebugEventType;
  readonly debugEventTypes = DebugEventType;
  readonly EventType = EventType;

  dialogTitle: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: CalculatedFieldDebugDialogData,
              protected dialogRef: MatDialogRef<CalculatedFieldDebugDialogComponent, string>) {
    super(store, router, dialogRef);
    this.dialogTitle = this.data.value.type === CalculatedFieldType.ALARM ? 'alarm-rule.debugging' : 'calculated-fields.debugging';
  }

  ngAfterViewInit(): void {
    this.eventsTable.entitiesTable.cellActionDescriptors[0].isEnabled = (event => {
      return (this.data.value.type === CalculatedFieldType.SCRIPT ||
        (this.data.value.type === CalculatedFieldType.PROPAGATION && this.data.value.configuration.applyExpressionToResolvedArguments)
      ) && !!(event as Event).body.arguments
    });
    this.eventsTable.entitiesTable.updateData();
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  onDebugEventSelected(event: CalculatedFieldEventBody): void {
    this.data.getTestScriptDialogFn(this.data.value, JSON.parse(event.arguments))
      .subscribe(expression => this.dialogRef.close(expression));
  }
}

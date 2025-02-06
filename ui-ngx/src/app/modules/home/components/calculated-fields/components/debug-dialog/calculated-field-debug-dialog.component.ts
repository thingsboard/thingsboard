///
/// Copyright © 2016-2024 The Thingsboard Authors
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
import { DebugEventType, EventType } from '@shared/models/event.models';
import { EventTableComponent } from '@home/components/event/event-table.component';
import { CalculatedFieldDebugDialogData } from '@shared/models/calculated-field.models';

@Component({
  selector: 'tb-calculated-field-debug-dialog',
  templateUrl: './calculated-field-debug-dialog.component.html',
})
export class CalculatedFieldDebugDialogComponent extends DialogComponent<CalculatedFieldDebugDialogComponent, null> implements AfterViewInit {

  @ViewChild(EventTableComponent, {static: true}) eventsTable: EventTableComponent;

  readonly DebugEventType = DebugEventType;
  readonly debugEventTypes = DebugEventType;
  readonly EventType = EventType;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: CalculatedFieldDebugDialogData,
              protected dialogRef: MatDialogRef<CalculatedFieldDebugDialogComponent, null>) {
    super(store, router, dialogRef);
  }

  ngAfterViewInit(): void {
    this.eventsTable.entitiesTable.updateData();
  }

  cancel(): void {
    this.dialogRef.close(null);
  }
}

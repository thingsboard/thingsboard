///
/// Copyright © 2016-2026 The Thingsboard Authors
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
import { DialogComponent } from '@shared/components/dialog.component';
import { EventTableComponent } from '@home/components/event/event-table.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { DebugEventType, EventType } from '@shared/models/event.models';
import { EntityId } from '@shared/models/id/entity-id';

export interface EventsDialogData {
  title: string;
  tenantId: string;
  entityId: EntityId;
  debugEventTypes: Array<DebugEventType>;
  defaultEventType: DebugEventType;
  disabledEventTypes?: Array<EventType | DebugEventType>;
  functionTestButtonLabel?: string;
  onDebugEventSelected?: (event: any, dialogRef: MatDialogRef<EventsDialogComponent, string>) => void;
  debugActionDisabled?: boolean;
}

@Component({
    selector: 'tb-debug-dialog',
    templateUrl: './events-dialog.component.html',
    styleUrl: './events-dialog.component.scss',
    standalone: false
})
export class EventsDialogComponent extends DialogComponent<EventsDialogComponent, string> implements AfterViewInit{

  @ViewChild(EventTableComponent, {static: true}) eventsTable: EventTableComponent;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: EventsDialogData,
              protected dialogRef: MatDialogRef<EventsDialogComponent, string>) {
    super(store, router, dialogRef);
  }

 ngAfterViewInit() {
   this.eventsTable.entitiesTable.updateData();
 }

  cancel(): void {
    this.dialogRef.close(null);
  }

  onDebugEventSelected(event: any): void {
    if(this.data.onDebugEventSelected) {
      this.data.onDebugEventSelected(event, this.dialogRef);
    }
  }
}

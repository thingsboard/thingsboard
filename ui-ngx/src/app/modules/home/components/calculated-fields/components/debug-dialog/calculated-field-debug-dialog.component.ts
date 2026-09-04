// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    standalone: false
})
export class CalculatedFieldDebugDialogComponent extends DialogComponent<CalculatedFieldDebugDialogComponent, string> implements AfterViewInit {

  @ViewChild(EventTableComponent, {static: true}) eventsTable: EventTableComponent;

  readonly DebugEventType = DebugEventType;
  readonly debugEventTypes = DebugEventType;
  readonly EventType = EventType;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: CalculatedFieldDebugDialogData,
              protected dialogRef: MatDialogRef<CalculatedFieldDebugDialogComponent, string>) {
    super(store, router, dialogRef);
  }

  ngAfterViewInit(): void {
    this.eventsTable.entitiesTable.cellActionDescriptors[0].isEnabled = (event => this.data.value.type === CalculatedFieldType.SCRIPT && !!(event as Event).body.arguments);
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

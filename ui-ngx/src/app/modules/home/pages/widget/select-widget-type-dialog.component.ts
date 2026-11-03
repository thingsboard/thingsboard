// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { widgetType, widgetTypesData } from '@shared/models/widget.models';

@Component({
    selector: 'tb-select-widget-type-dialog',
    templateUrl: './select-widget-type-dialog.component.html',
    styleUrls: ['./select-widget-type-dialog.component.scss'],
    standalone: false
})
export class SelectWidgetTypeDialogComponent extends
  DialogComponent<SelectWidgetTypeDialogComponent, widgetType> {

  widgetTypes = widgetType;

  allWidgetTypes = Object.keys(widgetType);

  widgetTypesDataMap = widgetTypesData;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<SelectWidgetTypeDialogComponent, widgetType>) {
    super(store, router, dialogRef);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  typeSelected(type: widgetType) {
    this.dialogRef.close(type);
  }
}

///
/// Copyright © 2016-2020 The Thingsboard Authors
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
import { MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { widgetType, widgetTypesData } from '@shared/models/widget.models';

@Component({
  selector: 'tb-select-widget-type-dialog',
  templateUrl: './select-widget-type-dialog.component.html',
  styleUrls: ['./select-widget-type-dialog.component.scss']
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

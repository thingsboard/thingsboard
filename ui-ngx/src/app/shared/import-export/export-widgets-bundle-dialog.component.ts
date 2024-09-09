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

import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormControl } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { isDefinedAndNotNull } from '@core/utils';

export interface ExportWidgetsBundleDialogData {
  widgetsBundle: WidgetsBundle;
  includeBundleWidgetsInExport: boolean;
  ignoreLoading?: boolean;
}

export interface ExportWidgetsBundleDialogResult {
  exportWidgets: boolean;
}

@Component({
  selector: 'tb-export-widgets-bundle-dialog',
  templateUrl: './export-widgets-bundle-dialog.component.html',
  providers: [],
  styleUrls: []
})
export class ExportWidgetsBundleDialogComponent extends DialogComponent<ExportWidgetsBundleDialogComponent, ExportWidgetsBundleDialogResult>
  implements OnInit {

  widgetsBundle: WidgetsBundle;

  ignoreLoading = false;

  exportWidgetsFormControl = new FormControl(true);

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ExportWidgetsBundleDialogData,
              public dialogRef: MatDialogRef<ExportWidgetsBundleDialogComponent, ExportWidgetsBundleDialogResult>) {
    super(store, router, dialogRef);
    this.widgetsBundle = data.widgetsBundle;
    this.ignoreLoading = data.ignoreLoading;
    if (isDefinedAndNotNull(data.includeBundleWidgetsInExport)) {
      this.exportWidgetsFormControl.patchValue(data.includeBundleWidgetsInExport, {emitEvent: false});
    }
  }

  ngOnInit(): void {
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  export(): void {
    this.dialogRef.close({
      exportWidgets: this.exportWidgetsFormControl.value
    });
  }
}

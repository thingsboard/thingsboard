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

import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';

export interface ColorPickerDialogData {
  color: string;
  colorClearButton: boolean;
}

export interface ColorPickerDialogResult {
  color?: string;
  canceled?: boolean;
}

@Component({
  selector: 'tb-color-picker-dialog',
  templateUrl: './color-picker-dialog.component.html',
  styleUrls: ['./color-picker-dialog.component.scss']
})
export class ColorPickerDialogComponent extends DialogComponent<ColorPickerDialogComponent, ColorPickerDialogResult> {

  color: string;
  colorClearButton: boolean;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ColorPickerDialogData,
              public dialogRef: MatDialogRef<ColorPickerDialogComponent, ColorPickerDialogResult>) {
    super(store, router, dialogRef);
    this.color = data.color;
    this.colorClearButton = data.colorClearButton;
  }

  selectColor(color: string) {
    this.dialogRef.close({color});
  }

  cancel(): void {
    this.dialogRef.close({canceled: true});
  }

}

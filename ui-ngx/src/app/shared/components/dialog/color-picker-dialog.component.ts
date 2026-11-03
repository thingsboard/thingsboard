// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    styleUrls: ['./color-picker-dialog.component.scss'],
    standalone: false
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

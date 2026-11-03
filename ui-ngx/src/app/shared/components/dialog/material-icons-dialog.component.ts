// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';

export interface MaterialIconsDialogData {
  icon: string;
  iconClearButton: boolean;
}

export interface MaterialIconsDialogResult {
  icon?: string;
  canceled?: boolean;
}

@Component({
    selector: 'tb-material-icons-dialog',
    templateUrl: './material-icons-dialog.component.html',
    providers: [],
    styleUrls: ['./material-icons-dialog.component.scss'],
    standalone: false
})
export class MaterialIconsDialogComponent extends DialogComponent<MaterialIconsDialogComponent, MaterialIconsDialogResult> {

  selectedIcon: string;
  iconClearButton: boolean;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: MaterialIconsDialogData,
              public dialogRef: MatDialogRef<MaterialIconsDialogComponent, MaterialIconsDialogResult>) {
    super(store, router, dialogRef);
    this.selectedIcon = data.icon;
    this.iconClearButton = data.iconClearButton;
  }

  selectIcon(icon: string) {
    this.dialogRef.close({icon});
  }

  cancel(): void {
    this.dialogRef.close({canceled: true});
  }

}

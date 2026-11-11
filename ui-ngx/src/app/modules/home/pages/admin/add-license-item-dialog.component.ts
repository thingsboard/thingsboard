// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export interface AddLicenseItemDialogData {
  itemName: string;
  add: boolean;
  isPerpetual: boolean;
  licensePortalUrl: string;
  disabledClose?: boolean;
}

@Component({
    selector: 'tb-add-license-item-dialog',
    templateUrl: './add-license-item-dialog.component.html',
    styleUrls: ['./add-license-item-dialog.component.scss'],
    standalone: false
})
export class AddLicenseItemDialogComponent extends DialogComponent<AddLicenseItemDialogComponent, boolean> {

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddLicenseItemDialogData,
              public dialogRef: MatDialogRef<AddLicenseItemDialogComponent, boolean>) {
    super(store, router, dialogRef);
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  refresh(): void {
    this.dialogRef.close(true);
  }

  goToLicensePortal() {
    window.open(this.data.licensePortalUrl, '_blank');
  }

}

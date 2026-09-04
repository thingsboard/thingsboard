// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { CustomMobilePage } from '@shared/models/mobile-app.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MatDialogRef } from '@angular/material/dialog';
import { FormBuilder } from '@angular/forms';
import { deepTrim } from '@core/utils';

@Component({
    selector: 'tb-add-mobile-page-dialog',
    templateUrl: './add-mobile-page-dialog.component.html',
    styleUrls: ['./add-mobile-page-dialog.component.scss'],
    standalone: false
})
export class AddMobilePageDialogComponent extends DialogComponent<AddMobilePageDialogComponent, CustomMobilePage> {

  customMobilePage = this.fb.control<CustomMobilePage>(null);

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<AddMobilePageDialogComponent, CustomMobilePage>,
              private fb: FormBuilder) {
    super(store, router, dialogRef);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save() {
    if (this.customMobilePage.valid) {
      const pageItem = deepTrim(this.customMobilePage.value);
      this.dialogRef.close(pageItem);
    }
  }
}

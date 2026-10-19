// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject, OnInit } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { CMAssigneeType } from '@shared/models/custom-menu.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { SecretStorageType } from '@shared/models/secret-storage.models';
import { UntypedFormBuilder, Validators } from '@angular/forms';

export interface EditSecretValueDialogData {
  type: SecretStorageType;
}

@Component({
    selector: 'tb-edit-secret-value-dialog',
    templateUrl: './edit-secret-value-dialog.component.html',
    styleUrls: [],
    standalone: false
})
export class EditSecretValueDialogComponent extends
  DialogComponent<EditSecretValueDialogComponent, boolean> implements OnInit {

  SecretStorageType = SecretStorageType;
  type: SecretStorageType = SecretStorageType.TEXT;

  valueFormControl = this.fb.control(null, [Validators.required]);

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private fb: UntypedFormBuilder,
              @Inject(MAT_DIALOG_DATA) public data: EditSecretValueDialogData,
              public dialogRef: MatDialogRef<EditSecretValueDialogComponent, boolean>,
              public translate: TranslateService) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.type = this.data.type;
  }

  cancel() {
    this.dialogRef.close(false);
  }

  submit() {
    this.dialogRef.close(this.valueFormControl.value);
  }
}

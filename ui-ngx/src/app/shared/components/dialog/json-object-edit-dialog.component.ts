// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { isNotEmptyStr } from '@core/utils';

export interface JsonObjectEditDialogData {
  jsonValue: object;
  required?: boolean;
  title?: string;
  saveLabel?: string;
  cancelLabel?: string;
  fillHeight?: boolean;
}

@Component({
    selector: 'tb-object-edit-dialog',
    templateUrl: './json-object-edit-dialog.component.html',
    styleUrls: [],
    standalone: false
})
export class JsonObjectEditDialogComponent extends DialogComponent<JsonObjectEditDialogComponent, object> {

  jsonFormGroup: FormGroup;
  title = this.translate.instant('details.edit-json');
  saveButtonLabel = this.translate.instant('action.save');
  cancelButtonLabel = this.translate.instant('action.cancel');

  required = this.data.required === true;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: JsonObjectEditDialogData,
              public dialogRef: MatDialogRef<JsonObjectEditDialogComponent, object>,
              public fb: FormBuilder,
              private translate: TranslateService) {
    super(store, router, dialogRef);
    if (isNotEmptyStr(this.data.title)) {
      this.title = this.data.title;
    }
    if (isNotEmptyStr(this.data.saveLabel)) {
      this.saveButtonLabel = this.data.saveLabel;
    }
    if (isNotEmptyStr(this.data.cancelLabel)) {
      this.cancelButtonLabel = this.data.cancelLabel;
    }
    this.jsonFormGroup = this.fb.group({
      json: [this.data.jsonValue, []]
    });
  }

  cancel(): void {
    this.dialogRef.close(undefined);
  }

  add(): void {
    this.dialogRef.close(this.jsonFormGroup.get('json').value);
  }
}

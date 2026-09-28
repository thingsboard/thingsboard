// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { FormProperty } from '@shared/models/dynamic-form.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup } from '@angular/forms';

export interface DynamicFormDialogData<V> {
  title: string;
  properties: FormProperty[];
  value: V;
}

@Component({
    selector: 'tb-dynamic-form-dialog',
    templateUrl: './dynamic-form-dialog.component.html',
    styleUrls: [],
    standalone: false
})
export class DynamicFormDialogComponent<V> extends DialogComponent<DynamicFormDialogComponent<V>, V> {

  title: string;
  properties: FormProperty[];

  dynamicFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: DynamicFormDialogData<V>,
              public dialogRef: MatDialogRef<DynamicFormDialogComponent<V>, V>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);
    this.title = this.data.title;
    this.properties = this.data.properties;
    this.dynamicFormGroup = this.fb.group({
      value: [this.data.value, []]
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.dialogRef.close(this.dynamicFormGroup.get('value').value);
  }
}

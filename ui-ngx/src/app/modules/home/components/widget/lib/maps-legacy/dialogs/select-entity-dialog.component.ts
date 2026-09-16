// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FormattedData } from '@shared/models/widget.models';

export interface SelectEntityDialogData {
  entities: FormattedData[];
}

@Component({
    selector: 'tb-select-entity-dialog',
    templateUrl: './select-entity-dialog.component.html',
    styleUrls: ['./select-entity-dialog.component.scss'],
    standalone: false
})
export class SelectEntityDialogComponent extends DialogComponent<SelectEntityDialogComponent, FormattedData> {
  selectEntityFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: SelectEntityDialogData,
              public dialogRef: MatDialogRef<SelectEntityDialogComponent, FormattedData>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);

    this.selectEntityFormGroup = this.fb.group(
      {
        entity: ['', Validators.required]
      }
    );
  }

  save(): void {
    this.dialogRef.close(this.selectEntityFormGroup.value.entity);
  }
}

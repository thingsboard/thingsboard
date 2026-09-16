// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormControl } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { isDefinedAndNotNull } from '@core/utils';

export interface ExportResourceDialogData {
  title: string;
  prompt: string;
  include?: boolean;
  ignoreLoading?: boolean;
}

export interface ExportResourceDialogDialogResult {
  include: boolean;
}

@Component({
    selector: 'tb-export-resource-dialog',
    templateUrl: './export-resource-dialog.component.html',
    styleUrls: [],
    standalone: false
})
export class ExportResourceDialogComponent extends DialogComponent<ExportResourceDialogComponent, ExportResourceDialogDialogResult> {

  ignoreLoading = false;

  title: string;
  prompt: string

  includeResourcesFormControl = new FormControl(true);

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) private data: ExportResourceDialogData,
              public dialogRef: MatDialogRef<ExportResourceDialogComponent, ExportResourceDialogDialogResult>) {
    super(store, router, dialogRef);
    this.ignoreLoading = this.data.ignoreLoading;
    this.title = this.data.title;
    this.prompt = this.data.prompt;
    if (isDefinedAndNotNull(this.data.include)) {
      this.includeResourcesFormControl.patchValue(this.data.include, {emitEvent: false});
    }
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  export(): void {
    this.dialogRef.close({
      include: this.includeResourcesFormControl.value
    });
  }
}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { FormBuilder, Validators } from '@angular/forms';

export interface AiRenameDialogData {
  title: string;
  value: string;
}

@Component({
  selector: 'tb-input-dialog',
  templateUrl: './ai-rename-dialog.component.html',
  styles: ':host{min-width: 100%;max-width: 100%;width: 420px;display: grid;grid-template-rows: min-content minmax(auto, 1fr) min-content;height: 100%}',
  standalone: false
})
export class AiRenameDialogComponent extends DialogComponent<AiRenameDialogComponent, string> {

  valueControl = this.fb.control('', [Validators.required]);

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<AiRenameDialogComponent>,
              @Inject(MAT_DIALOG_DATA) public data: AiRenameDialogData,
              private fb: FormBuilder) {
    super(store, router, dialogRef);
    this.valueControl.patchValue(data.value || '');
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  confirm(): void {
    this.dialogRef.close(this.valueControl.value.trim());
  }
}

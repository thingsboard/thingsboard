///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component } from '@angular/core';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';

export interface MoveWidgetsDialogResult {
  cols: number;
  rows: number;
}

@Component({
  selector: 'tb-move-widgets-dialog',
  templateUrl: './move-widgets-dialog.component.html',
  providers: [],
  styleUrls: []
})
export class MoveWidgetsDialogComponent extends DialogComponent<MoveWidgetsDialogComponent, MoveWidgetsDialogResult> {

  moveWidgetsFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<MoveWidgetsDialogComponent, MoveWidgetsDialogResult>,
              private fb: UntypedFormBuilder,
              private dialog: MatDialog) {
    super(store, router, dialogRef);

    this.moveWidgetsFormGroup = this.fb.group({
        cols: [0, [Validators.required]],
        rows: [0, [Validators.required]]
      }
    );
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  move(): void {
    const result: MoveWidgetsDialogResult = this.moveWidgetsFormGroup.value;
    this.dialogRef.close(result);
  }
}

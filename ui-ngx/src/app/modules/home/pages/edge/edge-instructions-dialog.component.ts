///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { Component, Inject } from '@angular/core';
import { MAT_LEGACY_DIALOG_DATA as MAT_DIALOG_DATA, MatLegacyDialogRef as MatDialogRef } from "@angular/material/legacy-dialog";
import { DialogComponent } from "@shared/components/dialog.component";
import { Store } from "@ngrx/store";
import { AppState } from "@core/core.state";
import { Router } from "@angular/router";

export interface EdgeInstructionsData {
  instructions: string;
}

@Component({
  selector: 'tb-edge-instructions',
  templateUrl: './edge-instructions-dialog.component.html'
})
export class EdgeInstructionsDialogComponent extends DialogComponent<EdgeInstructionsDialogComponent, EdgeInstructionsData> {

  instructions: string = this.data.instructions;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<EdgeInstructionsDialogComponent, EdgeInstructionsData>,
              @Inject(MAT_DIALOG_DATA) public data: EdgeInstructionsData) {
    super(store, router, dialogRef);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }
}

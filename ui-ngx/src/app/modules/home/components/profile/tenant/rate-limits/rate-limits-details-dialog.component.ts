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

import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';

export interface RateLimitsDetailsDialogData {
  rateLimits: string;
  title: string;
  readonly: boolean;
}

@Component({
  templateUrl: './rate-limits-details-dialog.component.html'
})
export class RateLimitsDetailsDialogComponent extends DialogComponent<RateLimitsDetailsDialogComponent> {

  editDetailsFormGroup: UntypedFormGroup;

  rateLimits: string = this.data.rateLimits;

  title: string = this.data.title;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: RateLimitsDetailsDialogData,
              public dialogRef: MatDialogRef<RateLimitsDetailsDialogComponent>,
              private fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
    this.editDetailsFormGroup = this.fb.group({
      rateLimits: [this.rateLimits, []]
    });
    if (this.data.readonly) {
      this.editDetailsFormGroup.disable();
    }
  }

  save(): void {
    this.dialogRef.close(this.editDetailsFormGroup.get('rateLimits').value);
  }
}

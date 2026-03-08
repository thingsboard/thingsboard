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

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

import { Component, ViewEncapsulation } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormGroup } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';

@Component({
  selector: 'tb-getting-started-completed-dialog',
  templateUrl: './getting-started-completed-dialog.component.html',
  styleUrls: ['./getting-started-completed-dialog.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class GettingStartedCompletedDialogComponent extends
  DialogComponent<GettingStartedCompletedDialogComponent, void> {

  addDocLinkFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<GettingStartedCompletedDialogComponent, void>) {
    super(store, router, dialogRef);
    dialogRef.addPanelClass('tb-getting-started-completed-dialog');
  }

  close(): void {
    this.dialogRef.close();
  }
}

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
import { MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';

@Component({
  selector: 'tb-iot-hub-pe-required-dialog',
  standalone: false,
  templateUrl: './iot-hub-pe-required-dialog.component.html',
  styleUrls: ['./iot-hub-pe-required-dialog.component.scss']
})
export class TbIotHubPeRequiredDialogComponent extends DialogComponent<TbIotHubPeRequiredDialogComponent> {

  constructor(
    protected store: Store<AppState>,
    protected router: Router,
    protected dialogRef: MatDialogRef<TbIotHubPeRequiredDialogComponent>
  ) {
    super(store, router, dialogRef);
  }

  close(): void {
    this.dialogRef.close();
  }

  upgradeInstance(): void {
    window.open('https://thingsboard.io/docs/pe/installation/upgrade-from-ce/', '_blank');
  }
}

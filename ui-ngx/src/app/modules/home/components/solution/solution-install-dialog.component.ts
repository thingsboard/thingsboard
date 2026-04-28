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
import { Router } from '@angular/router';
import { SolutionTemplateInstalledItemDescriptor } from '@shared/models/iot-hub/iot-hub-installed-item.models';

export interface SolutionInstallDialogData {
  descriptor: SolutionTemplateInstalledItemDescriptor;
  instructions?: boolean;
}

@Component({
  selector: 'tb-solution-install-dialog',
  templateUrl: './solution-install-dialog.component.html',
  styleUrls: ['./solution-install-dialog.component.scss'],
  standalone: false
})
export class SolutionInstallDialogComponent {

  details: string;
  dashboardId: string | null;
  instructions: boolean;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: SolutionInstallDialogData,
    private dialogRef: MatDialogRef<SolutionInstallDialogComponent>,
    private router: Router
  ) {
    this.details = data.descriptor.details || '';
    this.dashboardId = data.descriptor.dashboardId?.id || null;
    this.instructions = !!data.instructions;
  }

  gotoMainDashboard(): void {
    if (this.dashboardId) {
      this.dialogRef.close();
      this.router.navigateByUrl(`/dashboards/${this.dashboardId}`);
    }
  }

  close(): void {
    this.dialogRef.close();
  }
}

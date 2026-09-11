// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { SolutionTemplateInstalledItemDescriptor } from '@shared/models/iot-hub/iot-hub-installed-item.models';
import {
  replaceItemLinkPlaceholders
} from '@home/components/iot-hub/iot-hub-markdown.utils';

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
    this.details = replaceItemLinkPlaceholders(data.descriptor.details || '');
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

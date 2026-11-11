// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export interface AiNoTelemetryModalData {
  checkConnectivity?: () => void;
  hideSendTelemetry?: boolean;
}
@Component({
  selector: 'tb-ai-no-telemetry-modal',
  templateUrl: './ai-no-telemetry-modal.component.html',
  styleUrl: './ai-no-telemetry-modal.component.scss',
  standalone: false,
})
export class AiNoTelemetryModalComponent extends
  DialogComponent<AiNoTelemetryModalComponent> {

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AiNoTelemetryModalData,
              public dialogRef: MatDialogRef<AiNoTelemetryModalComponent>,) {
    super(store, router, dialogRef);
  }

  cancel() {
    this.dialogRef.close();
  }

  moveToCheckConnectivityModal() {
    this.dialogRef.close();
    this.data.checkConnectivity?.();
  }
}

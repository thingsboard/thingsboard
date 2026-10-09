// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { DeviceService } from '@core/http/device.service';
import { Device } from '@shared/models/device.models';
import { EntityType } from '@shared/models/entity-type.models';

// Mirrors the 'Add Gateway' dialog of the gateways dashboard (gateways_dashboard.json).
@Component({
  selector: 'tb-agent-gateway-create-dialog',
  templateUrl: './agent-gateway-create-dialog.component.html',
  styleUrls: [],
  standalone: false
})
export class AgentGatewayCreateDialogComponent
  extends DialogComponent<AgentGatewayCreateDialogComponent, Device> {

  createFormGroup: UntypedFormGroup;
  entityType = EntityType;
  submitting = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private deviceService: DeviceService,
              private fb: UntypedFormBuilder,
              public dialogRef: MatDialogRef<AgentGatewayCreateDialogComponent, Device>) {
    super(store, router, dialogRef);
    this.createFormGroup = this.fb.group({
      name: ['', [Validators.required, Validators.pattern(/.*\S.*/)]],
      type: ['', [Validators.required]]
    });
  }

  cancel() {
    this.dialogRef.close(undefined);
  }

  create() {
    if (this.createFormGroup.invalid || this.submitting) {
      return;
    }
    this.submitting = true;
    const formValues = this.createFormGroup.value;
    const device = {
      name: formValues.name.trim(),
      type: formValues.type,
      additionalInfo: { gateway: true }
    } as Device;
    this.deviceService.saveDevice(device).subscribe({
      next: saved => this.dialogRef.close(saved),
      error: () => {
        this.submitting = false;
      }
    });
  }
}

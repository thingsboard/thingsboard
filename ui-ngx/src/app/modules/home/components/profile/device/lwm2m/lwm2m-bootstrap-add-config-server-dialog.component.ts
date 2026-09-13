// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MatDialogRef } from '@angular/material/dialog';
import {
  ServerConfigType,
  ServerConfigTypeTranslationMap
} from '@home/components/profile/device/lwm2m/lwm2m-profile-config.models';

@Component({
    selector: 'tb-profile-lwm2m-bootstrap-add-config-server-dialog',
    templateUrl: './lwm2m-bootstrap-add-config-server-dialog.component.html',
    standalone: false
})
export class Lwm2mBootstrapAddConfigServerDialogComponent extends DialogComponent<Lwm2mBootstrapAddConfigServerDialogComponent> {

  addConfigServerFormGroup: UntypedFormGroup;

  serverTypes = Object.values(ServerConfigType);
  serverConfigTypeNamesMap = ServerConfigTypeTranslationMap;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private fb: UntypedFormBuilder,
              public dialogRef: MatDialogRef<Lwm2mBootstrapAddConfigServerDialogComponent, boolean>,
  ) {
    super(store, router, dialogRef);
    this.addConfigServerFormGroup = this.fb.group({
      serverType: [ServerConfigType.LWM2M]
    });
  }

  addServerConfig() {
    this.dialogRef.close(this.addConfigServerFormGroup.get('serverType').value === ServerConfigType.BOOTSTRAP);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }
}



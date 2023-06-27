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

import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { FormBuilder, FormControl } from '@angular/forms';

export interface GatewayRemoteConfigurationDialogData {
  gatewayName: string;
}

@Component({
  selector: 'tb-activation-link-dialog',
  templateUrl: './gateway-remote-configuration-dialog.html'
})

export class GatewayRemoteConfigurationDialogComponent extends DialogComponent<GatewayRemoteConfigurationDialogComponent,
  boolean> implements OnInit {

  gatewayName: string;

  gatewayControl: FormControl;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: GatewayRemoteConfigurationDialogData,
              public dialogRef: MatDialogRef<GatewayRemoteConfigurationDialogComponent, boolean>,
              private fb: FormBuilder) {
    super(store, router, dialogRef);
    this.gatewayName = this.data.gatewayName;
    this.gatewayControl = this.fb.control(null);
  }

  ngOnInit(): void {
  }

  close(): void {
    this.dialogRef.close();
  }

  turnOff(): void {
    this.dialogRef.close(true);
  }
}

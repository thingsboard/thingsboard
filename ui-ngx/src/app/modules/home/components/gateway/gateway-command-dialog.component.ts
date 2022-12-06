///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {Router} from '@angular/router';
import {DialogComponent} from '@app/shared/components/dialog.component';
import {TranslateService} from '@ngx-translate/core';
import {Device, DeviceCredentials} from "@shared/models/device.models";

export interface GatewayCommandDialogData {
  device: Device,
  credentials: DeviceCredentials
}

enum OsType {
  linux = 'linux',
  macos = 'macos',
  windows = 'win'
}

@Component({
  selector: 'tb-gateway-command-dialog',
  templateUrl: './gateway-command-dialog.component.html',
  styleUrls: []
})
export class GatewayCommandDialogComponent extends DialogComponent<GatewayCommandDialogComponent> implements OnInit {

  constructor(protected router: Router,
              protected store: Store<AppState>,
              @Inject(MAT_DIALOG_DATA) public data: GatewayCommandDialogData,
              public dialogRef: MatDialogRef<GatewayCommandDialogComponent, boolean>,) {
    super(store, router, dialogRef);
  }

  ngOnInit() {
  }

  close(): void {
    this.dialogRef.close();
  }

}

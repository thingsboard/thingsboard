///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
///
/// NOTICE: All information contained herein is, and remains
/// the property of ThingsBoard, Inc. and its suppliers,
/// if any.  The intellectual and technical concepts contained
/// herein are proprietary to ThingsBoard, Inc.
/// and its suppliers and may be covered by U.S. and Foreign Patents,
/// patents in process, and are protected by trade secret or copyright law.
///
/// Dissemination of this information or reproduction of this material is strictly forbidden
/// unless prior written permission is obtained from COMPANY.
///
/// Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
/// managers or contractors who have executed Confidentiality and Non-disclosure agreements
/// explicitly covering such access.
///
/// The copyright notice above does not evidence any actual or intended publication
/// or disclosure  of  this source code, which includes
/// information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
/// ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
/// OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
/// THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
/// AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
/// THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
/// DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
/// OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
///

import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { TranslateService } from '@ngx-translate/core';
import {Device, DeviceCredentials} from "@shared/models/device.models";
import {ActionNotificationShow} from "@core/notification/notification.actions";
import {FormControl} from "@angular/forms";

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
  linuxCode: string;
  windowsCode: string;
  selectedOSControll: FormControl;
  osTypes = OsType;

  constructor(protected router: Router,
              protected store: Store<AppState>,
              private translate: TranslateService,
              @Inject(MAT_DIALOG_DATA) public data: GatewayCommandDialogData,
              public dialogRef: MatDialogRef<GatewayCommandDialogComponent, boolean>,) {
    super(store, router, dialogRef);
    const ACCESS_TOKEN = data.credentials.credentialsId;
    const HOST = window.location.hostname;
    this.selectedOSControll = new FormControl('');
    // @ts-ignore
    const platform = window.navigator?.userAgentData?.platform || window.navigator.platform,
      macosPlatforms = ['Macintosh', 'MacIntel', 'MacPPC', 'Mac68K'],
      windowsPlatforms = ['Win32', 'Win64', 'Windows', 'WinCE'];
    if (macosPlatforms.indexOf(platform) !== -1) {
      this.selectedOSControll.setValue(OsType.macos);
    } else if (windowsPlatforms.indexOf(platform) !== -1) {
      this.selectedOSControll.setValue(OsType.windows);
    } else if (/Linux/.test(platform)) {
      this.selectedOSControll.setValue(OsType.linux);
    }
    this.linuxCode = "docker run -it -v ~/.tb-gateway/logs:/thingsboard_gateway/logs -v " +
      "~/.tb-gateway/extensions:/thingsboard_gateway/extensions -v ~/.tb-gateway/config:/thingsboard_gateway/config --name tb-gateway -e host=" +
      HOST +
      " -e port=1883 -e accessToken=" +
      ACCESS_TOKEN +
      " --restart always thingsboard/tb-gateway";
    this.windowsCode = "docker run -it -v %HOMEPATH%/tb-gateway/config:/thingsboard_gateway/config -v " +
      "%HOMEPATH%/tb-gateway/extensions:/thingsboard_gateway/extensions -v %HOMEPATH%/tb-gateway/logs:/thingsboard_gateway/logs " +
      "--name tb-gateway -e host=" +
      HOST +
      " -e port=1883 -e accessToken=" +
      ACCESS_TOKEN +
      " --restart always thingsboard/tb-gateway";

  }

  ngOnInit(): void {
  }

  close(): void {
    this.dialogRef.close();
  }

  onDockerCodeCopied() {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('gateway.command-copied-message'),
        type: 'success',
        target: 'dockerCommandDialogContent',
        duration: 1200,
        verticalPosition: 'bottom',
        horizontalPosition: 'left'
      }));
  }
}

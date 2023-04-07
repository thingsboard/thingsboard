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

import { Component, Input, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { DeviceService } from '@core/http/device.service';
import { helpBaseUrl } from '@shared/models/constants';

enum OsType {
  linux = 'linux',
  macos = 'macos',
  windows = 'win'
}

@Component({
  selector: 'tb-gateway-command',
  templateUrl: './device-gateway-command.component.html',
  styleUrls: []
})

export class DeviceGatewayCommandComponent implements OnInit {

  @Input()
  token: string;

  @Input()
  deviceId: string;

  linuxCode: string;
  windowsCode: string;
  selectedOSCControl: FormControl;
  osTypes = OsType;
  helpLink: string = helpBaseUrl + '/docs/iot-gateway/install/docker-installation/';

  constructor(protected router: Router,
              protected store: Store<AppState>,
              private translate: TranslateService,
              private deviceService: DeviceService) {
  }


  ngOnInit(): void {
    const HOST = window.location.hostname;
    if (this.deviceId) {
      this.deviceService.getDeviceCredentials(this.deviceId).subscribe(credentials => {
        this.token = credentials.credentialsId;
        this.createRunCode(HOST);
      });
    }
    this.selectedOSCControl = new FormControl('');
    // @ts-ignore
    const platform = window.navigator?.userAgentData?.platform || window.navigator.platform,
      macosPlatforms = ['Macintosh', 'MacIntel', 'MacPPC', 'Mac68K'],
      windowsPlatforms = ['Win32', 'Win64', 'Windows', 'WinCE'];
    if (macosPlatforms.indexOf(platform) !== -1) {
      this.selectedOSCControl.setValue(OsType.macos);
    } else if (windowsPlatforms.indexOf(platform) !== -1) {
      this.selectedOSCControl.setValue(OsType.windows);
    } else if (/Linux/.test(platform)) {
      this.selectedOSCControl.setValue(OsType.linux);
    }
    this.createRunCode(HOST);
  }

  createRunCode(HOST) {
    this.linuxCode = 'docker run -it -v ~/.tb-gateway/logs:/thingsboard_gateway/logs -v ' +
      '~/.tb-gateway/extensions:/thingsboard_gateway/extensions -v ~/.tb-gateway/config:/thingsboard_gateway/config --name tb-gateway -e host=' +
      HOST +
      ' -e port=1883 -e accessToken=' +
      this.token +
      ' --restart always thingsboard/tb-gateway';
    this.windowsCode = 'docker run -it -v %HOMEPATH%/tb-gateway/config:/thingsboard_gateway/config -v ' +
      '%HOMEPATH%/tb-gateway/extensions:/thingsboard_gateway/extensions -v %HOMEPATH%/tb-gateway/logs:/thingsboard_gateway/logs ' +
      '--name tb-gateway -e host=' +
      HOST +
      ' -e port=1883 -e accessToken=' +
      this.token +
      ' --restart always thingsboard/tb-gateway';
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

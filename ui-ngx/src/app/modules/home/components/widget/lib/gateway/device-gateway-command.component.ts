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

import { ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { DeviceService } from '@core/http/device.service';
import { helpBaseUrl } from '@shared/models/constants';


@Component({
  selector: 'tb-gateway-command',
  templateUrl: './device-gateway-command.component.html',
  styleUrls: ['./device-gateway-command.component.scss']
})

export class DeviceGatewayCommandComponent implements OnInit {

  @Input()
  token: string;

  @Input()
  deviceId: string;

  linuxCode: string;
  windowsCode: string;

  helpLink: string = helpBaseUrl + '/docs/iot-gateway/install/docker-installation/';

  tabIndex = 0;

  constructor(protected router: Router,
              protected store: Store<AppState>,
              private translate: TranslateService,
              private cd: ChangeDetectorRef,
              private deviceService: DeviceService) {
  }


  ngOnInit(): void {
    if (this.deviceId) {
      this.deviceService.getDevicePublishLaunchCommands(this.deviceId).subscribe(commands => {
        this.createRunCode(commands.mqtt);
        this.cd.detectChanges();
      });
    }
    // @ts-ignore
    const platform = window.navigator?.userAgentData?.platform || window.navigator.platform,
      macosPlatforms = ['Macintosh', 'MacIntel', 'MacPPC', 'Mac68K'],
      windowsPlatforms = ['Win32', 'Win64', 'Windows', 'WinCE'];
    if (macosPlatforms.indexOf(platform) !== -1) {
      this.tabIndex = 1;
    } else if (windowsPlatforms.indexOf(platform) !== -1) {
      this.tabIndex = 0;
    } else if (/Linux/.test(platform)) {
      this.tabIndex = 1;
    }
  }

  createRunCode(commands) {
    this.linuxCode = commands.linux;
    this.windowsCode = commands.windows;
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

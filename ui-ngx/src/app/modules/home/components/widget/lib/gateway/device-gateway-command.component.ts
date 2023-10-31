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
import { DeviceService } from '@core/http/device.service';
import { helpBaseUrl } from '@shared/models/constants';
import { getOS } from '@core/utils';
import { PublishLaunchCommand } from '@shared/models/device.models';

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

  commands: PublishLaunchCommand;

  helpLink: string = helpBaseUrl + '/docs/iot-gateway/install/docker-installation/';

  tabIndex = 0;

  constructor(private cd: ChangeDetectorRef,
              private deviceService: DeviceService) {
  }


  ngOnInit(): void {
    if (this.deviceId) {
      this.deviceService.getDevicePublishLaunchCommands(this.deviceId).subscribe(commands => {
        this.commands = commands;
        this.cd.detectChanges();
      });
    }
    const currentOS = getOS();
    switch (currentOS) {
      case 'linux':
      case 'android':
        this.tabIndex = 1;
        break;
      case 'macos':
      case 'ios':
        this.tabIndex = 2;
        break;
      case 'windows':
        this.tabIndex = 0;
        break;
      default:
        this.tabIndex = 1;
    }
  }
}

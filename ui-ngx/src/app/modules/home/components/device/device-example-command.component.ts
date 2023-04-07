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
import { helpBaseUrl } from '@shared/models/constants';

@Component({
  selector: 'tb-device-example-command',
  templateUrl: './device-example-command.component.html',
  styleUrls: ['./device-example-command.component.scss']
})

export class DeviceExampleCommandComponent implements OnInit {

  @Input()
  token: string;

  helpLink: string = helpBaseUrl + '/docs/reference/protocols/';

  mqttCode: string;

  coapCode: string;

  httpCode: string;

  mqttSetup: string;

  coapSetup: string;

  httpSetup: string;

  protocolCtrl: FormControl;

  constructor(protected router: Router,
              protected store: Store<AppState>,
              private translate: TranslateService) {
  }


  ngOnInit(): void {
    const HOST = window.location.hostname;
    this.mqttCode = `mosquitto_pub -d -q 1 -h ${HOST} -t "v1/devices/me/telemetry" -u "${this.token}" -m "{"temperature":42}"`;
    this.coapCode = `cat telemetry-data.json | coap post coap://${HOST}/api/v1/${this.token}/telemetry`;
    this.httpCode = `curl -v -X POST --data "{"temperature":42,"humidity":73}" http://${HOST}/api/v1/${this.token}/telemetry --header "Content-Type:application/json"`;

    this.protocolCtrl = new FormControl('mqtt');
    const mqttLink = '<a href="https://thingsboard.io/docs/getting-started-guides/helloworld/?connectdevice=mqtt-windows"> Mqtt Guide</a>';

    // @ts-ignore
    const platform = window.navigator?.userAgentData?.platform || window.navigator.platform,
      macosPlatforms = ['Macintosh', 'MacIntel', 'MacPPC', 'Mac68K'],
      windowsPlatforms = ['Win32', 'Win64', 'Windows', 'WinCE'];
    if (macosPlatforms.indexOf(platform) !== -1) {
      this.mqttSetup = 'brew install mosquitto-clients';
      this.coapSetup = 'npm install coap-cli -g';
      this.httpSetup = 'brew install curl';
    } else if (windowsPlatforms.indexOf(platform) !== -1) {
      this.mqttSetup = mqttLink;
      this.coapSetup = 'npm install coap-cli -g';
      this.httpSetup = 'not required, available by default in windows 10+';
    } else if (/Linux/.test(platform)) {
      this.mqttSetup = 'sudo apt-get install mosquitto-clients';
      this.coapSetup = 'npm install coap-cli -g';
      this.httpSetup = 'sudo apt-get install curl';
    }
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

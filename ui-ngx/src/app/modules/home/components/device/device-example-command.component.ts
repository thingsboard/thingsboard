///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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

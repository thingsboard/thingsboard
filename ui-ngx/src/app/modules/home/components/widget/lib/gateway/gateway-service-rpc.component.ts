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

import { AfterViewInit, Component, Input } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { WidgetContext } from '@home/models/widget-component.models';
import { ContentType } from '@shared/models/constants';

@Component({
  selector: 'tb-gateway-service-rpc',
  templateUrl: './gateway-service-rpc.component.html',
  styleUrls: ['./gateway-service-rpc.component.scss']
})
export class GatewayServiceRPCComponent implements AfterViewInit {

  @Input()
  ctx: WidgetContext;

  contentTypes = ContentType;

  commandForm: FormGroup;

  isConnector: boolean;

  RPCCommands: Array<string> = [
    'Ping',
    'Stats',
    'Devices',
    'Update',
    'Version',
    'Restart',
    'Reboot'
  ];

  private connectorType: string;

  constructor(private fb: FormBuilder) {
    this.commandForm = this.fb.group({
      command: [null,[Validators.required]],
      time: [60, [Validators.required, Validators.min(1)]],
      params: [""],
      result: [null]
    });
  }

  ngAfterViewInit() {
    this.isConnector = this.ctx.settings.isConnector;
    if (!this.isConnector) {
      this.commandForm.get('command').setValue(this.RPCCommands[0]);
    } else {
      this.commandForm.get('params').addValidators(Validators.required);
      this.connectorType = this.ctx.stateController.getStateParams().connector_rpc.value.type;
    }
  }

  sendCommand() {
    const formValues = this.commandForm.value;
    const commandPrefix = this.isConnector ? `${this.connectorType}_` : 'gateway_';
    this.ctx.controlApi.sendTwoWayCommand(commandPrefix+formValues.command.toLowerCase(), formValues.params, formValues.time).subscribe({
      next: resp => this.commandForm.get('result').setValue(JSON.stringify(resp)),
      error: error => {
        console.log(error);
        this.commandForm.get('result').setValue(JSON.stringify(error.error));
      }
    });
  }
}

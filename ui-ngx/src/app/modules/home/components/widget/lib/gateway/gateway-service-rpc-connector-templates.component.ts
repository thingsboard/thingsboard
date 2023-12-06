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

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { ConnectorType, RPCTemplate } from '@home/components/widget/lib/gateway/gateway-widget.models';
import { TranslateService } from '@ngx-translate/core';
import { KeyValue } from '@angular/common';

@Component({
  selector: 'tb-gateway-service-rpc-connector-templates',
  templateUrl: './gateway-service-rpc-connector-templates.component.html',
  styleUrls: ['./gateway-service-rpc-connector-templates.component.scss']
})
export class GatewayServiceRPCConnectorTemplatesComponent implements OnInit {

  @Input()
  connectorType: ConnectorType;

  @Output()
  saveTemplate: EventEmitter<any> = new EventEmitter();

  rpcTemplates: Array<RPCTemplate> = [];

  constructor(private translate: TranslateService) {
    this.rpcTemplates.push(
      {
        name: 'Test Template',
        config: {
          fieldString: 'string',
          fieldNumber: 666,
          fieldBool: true,
          fieldArray: [111, 222, 333, "String", "444"],
          fieldObj: {
            subKey1: 'dasd',
            subKey2: 666,
          }
        }
      }
    )
  }


  ngOnInit() {

  }

  public useTemplate($event: Event): void {
    $event.stopPropagation();
    console.log("useTemplate")
  }

  public copyTemplate($event: Event): void {
    $event.stopPropagation();
    console.log("copyTemplate")
  }

  public deleteTemplate($event: Event): void {
    $event.stopPropagation();
    console.log("deleteTemplate")
  }

  public originalOrder = (a: KeyValue<string, any>, b: KeyValue<string, any>): number => {
    return 0;
  }
  public isObject(value: any) {
    return value !== null && typeof value === 'object' && !Array.isArray(value);
  }
}

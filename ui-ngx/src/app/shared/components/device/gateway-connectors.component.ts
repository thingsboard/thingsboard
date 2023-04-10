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
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { FormArray, FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { EntityId } from '@shared/models/id/entity-id';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { AttributeService } from '@core/http/attribute.service';
import { DeviceService } from '@core/http/device.service';
import { TranslateService } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';


export interface gatewayConnector {
  name: string;
  type: string;
  configuration: string;
  jsonConfiguration: string;
  key: string;
}

@Component({
  selector: 'tb-gateway-connector',
  templateUrl: './gateway-connectors.component.html',
  styleUrls: ['./gateway-connectors.component.scss']
})
export class GatewayConnectorComponent implements OnInit {

  gatewayConnectorsGroup: FormGroup;

  gatewayConnectorDefaultTypes: Array<string> =
    ['mqtt',
    'modbus',
    'grpc',
    'opcua',
    'opcua_asyncio',
    'ble',
    'request',
    'can',
    'bacnet',
    'odbc',
    'rest',
    'snmp',
    'ftp',
    'socket',
    'xmpp',
    'ocpp'
  ];

  selectedConnector: number;

  @Input()
  device: EntityId;

  @Input()
  dialogRef: MatDialogRef<any>;

  logSelector: FormControl;


  constructor(protected router: Router,
              protected store: Store<AppState>,
              protected fb: FormBuilder,
              protected translate: TranslateService,
              protected attributeService: AttributeService,
              protected deviceService: DeviceService,
              private cd: ChangeDetectorRef,
              public dialog: MatDialog) {
  }

  ngOnInit() {
    this.gatewayConnectorsGroup = this.fb.group({
      connectors: this.fb.array([], [Validators.required])
    });
    this.getConnectorsData();
  }

  cancel(): void {
    if (this.dialogRef) {
      this.dialogRef.close();
    }
  }

  getConnectorsData(): void {
    forkJoin([
      this.attributeService.getEntityAttributes(this.device, AttributeScope.SHARED_SCOPE,['implementedConnectors']),
      this.attributeService.getEntityAttributes(this.device, AttributeScope.SERVER_SCOPE,['implementedConnectors']),
      this.attributeService.getEntityAttributes(this.device, AttributeScope.SHARED_SCOPE,['connectorTypes'])
    ]).subscribe(attributes=>{
      if (attributes[0].length) {
        attributes[0][0].value.forEach(connector=>this.addConnector(true, connector));
      }
      if (attributes[1].length) {
        attributes[1][0].value.forEach(connector=>this.addConnector(false, connector));
      }
      if(attributes[2].length) {
        attributes[1][0].value.forEach(type=> {
          if (this.gatewayConnectorDefaultTypes.indexOf(type) === -1) {
            this.gatewayConnectorDefaultTypes.push(type);
          }
        });
      }
      this.cd.detectChanges();
    });
  }

  saveConnectors(): void {
    const connectors = this.gatewayConnectorsGroup.value.connectors;
    const activeConnectors = connectors.filter(connector=>connector.active);
    const inactiveConnectors = connectors.filter(connector=>!connector.active);
    forkJoin([
      this.attributeService.saveEntityAttributes(this.device, AttributeScope.SERVER_SCOPE,
        [{key: 'implementedConnectors', value: inactiveConnectors}]),
      this.attributeService.saveEntityAttributes(this.device, AttributeScope.SHARED_SCOPE,
        [{key: 'implementedConnectors', value: activeConnectors}])
    ]).subscribe(_=> {
      if (this.dialogRef) {
        this.dialogRef.close();
      }
    });
  }

  connectorsFormArray(): FormArray {
    return this.gatewayConnectorsGroup.get('connectors') as FormArray;
  }

  addConnector(active?: boolean,connector?: gatewayConnector)  {
    const newConnector = this.fb.group({
      active: [!!active],
      name: [connector?.name || '', [Validators.required]],
      type: [connector?.type || '', [Validators.required]],
      configuration: [connector?.configuration || '', [Validators.required]],
      jsonConfiguration: [connector?.jsonConfiguration || {}, [Validators.required]],
      key: [connector?.key || 'auto', [Validators.required]],
    });
    const connectorsFormArray = this.connectorsFormArray();
    connectorsFormArray.push(newConnector);
    this.selectConnector(connectorsFormArray.length-1);
  }

  removeConnector(index: number): void {
    this.connectorsFormArray().removeAt(index);
    if (index !== 0) {
      this.selectedConnector = index - 1;
    } else {
      this.selectedConnector = undefined;
    }
  }

  selectConnector(index: number): void {
    this.selectedConnector = index;
  }

  getJsonControl(selectedConnector: number): FormControl {
    return this.connectorsFormArray().at(selectedConnector).get('jsonConfiguration') as FormControl;
  }

  generatePanelTitle(connectorControl): string{
    const connectorValues = connectorControl.value;
    const activeTxt = this.translate.instant(connectorValues.active ? 'gateway.connectors-active' : 'gateway.connectors-inactive');
    let title = `${connectorValues.name} | ${connectorValues.type} `;
    if (connectorValues.type === 'grpc') {
      title += `| ${connectorValues.key} `;
    }
    title += `| ${activeTxt}`;
    return title;
  }
}

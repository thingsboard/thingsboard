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
import { FormArray, FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import {
  BACnetObjectTypes, BACnetObjectTypesTranslates,
  BACnetRequestTypes, BACnetRequestTypesTranslates, BLEMethods, BLEMethodsTranslates,
  CANByteOrders,
  ConnectorType, GatewayConnectorDefaultTypesTranslates, HTTPMethods,
  ModbusCommandTypes,
  RPCCommand,
  SNMPMethods, SNMPMethodsTranslations,
  SocketEncodings,
  SocketMethodProcessings, SocketMethodProcessingsTranslates
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { TranslateService } from "@ngx-translate/core";

@Component({
  selector: 'tb-gateway-service-rpc-connector',
  templateUrl: './gateway-service-rpc-connector.component.html',
  styleUrls: ['./gateway-service-rpc-connector.component.scss']
})
export class GatewayServiceRPCConnectorComponent implements OnInit {

  @Input()
  connectorType: ConnectorType;

  @Output()
  sendCommand: EventEmitter<RPCCommand> = new EventEmitter();

  commandForm: FormGroup;

  codesArray: Array<number> = [1, 2, 3, 4, 5, 6, 15, 16];

  readonly ConnectorType = ConnectorType;

  modbusCommandTypes = Object.values(ModbusCommandTypes) as ModbusCommandTypes[];
  bACnetRequestTypes = Object.values(BACnetRequestTypes) as BACnetRequestTypes[];
  bACnetObjectTypes = Object.values(BACnetObjectTypes) as BACnetObjectTypes[];
  bLEMethods = Object.values(BLEMethods) as BLEMethods[];
  cANByteOrders = Object.values(CANByteOrders) as CANByteOrders[];
  socketMethodProcessings = Object.values(SocketMethodProcessings) as SocketMethodProcessings[];
  socketEncodings = Object.values(SocketEncodings) as SocketEncodings[];
  sNMPMethods = Object.values(SNMPMethods) as SNMPMethods[];
  hTTPMethods = Object.values(HTTPMethods) as HTTPMethods[];

  bACnetRequestTypesTranslates = BACnetRequestTypesTranslates;
  bACnetObjectTypesTranslates = BACnetObjectTypesTranslates;
  bLEMethodsTranslates = BLEMethodsTranslates;
  SocketMethodProcessingsTranslates = SocketMethodProcessingsTranslates;
  SNMPMethodsTranslations = SNMPMethodsTranslations;
  gatewayConnectorDefaultTypesTranslates = GatewayConnectorDefaultTypesTranslates;

  urlPattern = new RegExp(
    '^(https?:\\/\\/)?' + // protocol
    '((([a-z\\d]([a-z\\d-]*[a-z\\d])*)\\.)+[a-z]{2,}|' + // domain name
    '((\\d{1,3}\\.){3}\\d{1,3}))' + // OR IP (v4) address
    '(\\:\\d+)?(\\/[-a-z\\d%_.~+]*)*' + // port and path
    '(\\?[;&a-z\\d%_.~+=-]*)?' + // query string
    '(\\#[-a-z\\d_]*)?$', // fragment locator
    'i'
  );

  constructor(private fb: FormBuilder,
              private translate: TranslateService) {
  }


  ngOnInit() {
    this.commandForm = this.connectorParamsFormGroupByType(this.connectorType)
  }

  connectorParamsFormGroupByType(type: ConnectorType): FormGroup {
    let formGroup: FormGroup;

    switch (type) {
      case ConnectorType.MQTT:
        formGroup = this.fb.group({
          methodFilter: [null, [Validators.required]],
          requestTopicExpression: [null, [Validators.required]],
          responseTopicExpression: [null, []],
          responseTimeout: [null, [Validators.min(10), Validators.pattern("^[0-9]*$")]],
          valueExpression: [null, [Validators.required]],
        })
        break;
      case ConnectorType.MODBUS:
        formGroup = this.fb.group({
          tag: [null, [Validators.required]],
          type: [null, [Validators.required]],
          functionCode: [null, [Validators.required]],
          address: [null, [Validators.required, Validators.min(0), Validators.pattern("^[0-9]*$")]],
          objectsCount: [null, [Validators.required, Validators.min(0), Validators.pattern("^[0-9]*$")]]
        })
        break;
      case ConnectorType.BACNET:
        formGroup = this.fb.group({
          method: [null, [Validators.required]],
          requestType: [null, [Validators.required]],
          requestTimeout: [null, [Validators.required, Validators.min(10), Validators.pattern("^[0-9]*$")]],
          objectType: [null, []],
          identifier: [null, [Validators.required, Validators.min(1), Validators.pattern("^[0-9]*$")]],
          propertyId: [null, [Validators.required]]
        })
        break;
      case ConnectorType.BLE:
        formGroup = this.fb.group({
          methodRPC: [null, [Validators.required]],
          characteristicUUID: [null, [Validators.required]],
          methodProcessing: [null, [Validators.required]],
          withResponse: [false, []]
        })
        break;
      case ConnectorType.CAN:
        formGroup = this.fb.group({
          method: [null, [Validators.required]],
          nodeID: [null, [Validators.required, Validators.min(0), Validators.pattern("^[0-9]*$")]],
          isExtendedID: [false, []],
          isFD: [false, []],
          bitrateSwitch: [false, []],
          dataLength: [null, [Validators.min(1), Validators.pattern("^[0-9]*$")]],
          dataByteorder: [null, []],
          dataBefore: [null, []],
          dataAfter: [null, []],
          dataInHEX: [null, []],
          dataExpression: [null, []]
        })
        break;
      case ConnectorType.FTP:
        formGroup = this.fb.group({
          methodFilter: [null, [Validators.required]],
          valueExpression: [null, [Validators.required]]
        })
        break;
      case ConnectorType.OCPP:
        formGroup = this.fb.group({
          methodRPC: [null, [Validators.required]],
          valueExpression: [null, [Validators.required]],
          withResponse: [false, []]
        })
        break;
      case ConnectorType.SOCKET:
        formGroup = this.fb.group({
          methodRPC: [null, [Validators.required]],
          methodProcessing: [null, [Validators.required]],
          encoding: [null, [Validators.required]],
          withResponse: [false, []]
        })
        break;
      case ConnectorType.XMPP:
        formGroup = this.fb.group({
          methodRPC: [null, [Validators.required]],
          valueExpression: [null, [Validators.required]],
          withResponse: [false, []]
        })
        break;
      case ConnectorType.SNMP:
        formGroup = this.fb.group({
          requestFilter: [null, [Validators.required]],
          method: [null, [Validators.required]],
          oid: this.fb.array([], [Validators.required])
        })
        break;
      case ConnectorType.REST:
        formGroup = this.fb.group({
          methodFilter: [null, [Validators.required]],
          HTTPMethod: [null, [Validators.required]],
          requestUrlExpression: [null, [Validators.required, Validators.pattern(this.urlPattern)]],
          responseTimeout: [null, [Validators.required, Validators.min(10), Validators.pattern("^[0-9]*$")]],
          timeout: [null, [Validators.required, Validators.min(10), Validators.pattern("^[0-9]*$")]],
          tries: [null, [Validators.required, Validators.min(1), Validators.pattern("^[0-9]*$")]],
          valueExpression: [null, [Validators.required]],
          httpHeaders: this.fb.array([]),
          security: this.fb.array([])
        })
        break;
      case ConnectorType.REQUEST:
        formGroup = this.fb.group({
          methodFilter: [null, [Validators.required]],
          httpMethod: [null, [Validators.required]],
          requestUrlExpression: [null, [Validators.required, Validators.pattern(this.urlPattern)]],
          responseTimeout: [null, [Validators.required, Validators.min(10), Validators.pattern("^[0-9]*$")]],
          timeout: [null, [Validators.required, Validators.min(10), Validators.pattern("^[0-9]*$")]],
          tries: [null, [Validators.required, Validators.min(1), Validators.pattern("^[0-9]*$")]],
          requestValueExpression: [null, [Validators.required]],
          responseValueExpression: [null, []],
          httpHeaders: this.fb.array([]),
        })
        break;
      case ConnectorType.OPCUA:
      case ConnectorType.OPCUA_ASYNCIO:
        formGroup = this.fb.group({
          method: [null, [Validators.required]],
          arguments: this.fb.array([]),
        })
    }
    return formGroup;
  }

  addSNMPoid(value: string = null) {
    const oidsFA = this.commandForm.get('oid') as FormArray;
    if (oidsFA) {
      oidsFA.push(this.fb.control(value, [Validators.required]));
    }
  }

  removeSNMPoid(index: number) {
    const oidsFA = this.commandForm.get('oid') as FormArray;
    oidsFA.removeAt(index);
  }

  addHTTPHeader(value: { headerName: string, value: string } = {headerName: null, value: null}) {
    const headerFA = this.commandForm.get('httpHeaders') as FormArray;
    const formGroup = this.fb.group({
      headerName: [value.headerName, [Validators.required]],
      value: [value.value, [Validators.required]]
    })
    if (headerFA) {
      headerFA.push(formGroup);
    }
  }

  removeHTTPHeader(index: number) {
    const oidsFA = this.commandForm.get('httpHeaders') as FormArray;
    oidsFA.removeAt(index);
  }

  addHTTPSecurity(value: { securityName: string, value: string } = {securityName: null, value: null}) {
    const securityFA = this.commandForm.get('security') as FormArray;
    const formGroup = this.fb.group({
      securityName: [value.securityName, [Validators.required]],
      value: [value.value, [Validators.required]]
    })
    if (securityFA) {
      securityFA.push(formGroup);
    }
  }

  removeHTTPSecurity(index: number) {
    const oidsFA = this.commandForm.get('security') as FormArray;
    oidsFA.removeAt(index);
  }

  getFormArrayControls(path: string) {
    return (this.commandForm.get(path) as FormArray).controls as FormControl[];
  }

  addOCPUAArguments(value: string = null) {
    const oidsFA = this.commandForm.get('arguments') as FormArray;
    if (oidsFA) {
      oidsFA.push(this.fb.control(value));
    }
  }

  removeOCPUAArguments(index: number) {
    const oidsFA = this.commandForm.get('arguments') as FormArray;
    oidsFA.removeAt(index);
  }
}

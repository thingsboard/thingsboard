///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import {
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output
} from '@angular/core';
import {
  AbstractControl, FormControl,
  FormGroup,
  UntypedFormArray,
  UntypedFormBuilder,
  Validators
} from '@angular/forms';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import {
  DeviceAttributesRequests,
  DeviceDataKey, ExpressionType,
  mappingValueTypesMap,
  noLeadTrailSpacesRegex, RequestsType,
  RpcMethodsMapping, SocketAttributeUpdates, SocketEncoding, SocketValueKey,
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-device-data-keys-panel',
  templateUrl: './device-data-keys-panel.component.html',
  styleUrls: ['./device-data-keys-panel.component.scss'],
  providers: []
})
export class DeviceDataKeysPanelComponent extends PageComponent implements OnInit {

  @Input()
  panelTitle: string;

  @Input()
  addKeyTitle: string;

  @Input()
  deleteKeyTitle: string;

  @Input()
  noKeysText: string;

  @Input()
  keys: Array<DeviceDataKey> | { [key: string]: any };

  @Input()
  keysType: SocketValueKey;

  @Input()
  valueTypes: Map<string, any> = mappingValueTypesMap;

  @Input()
  @coerceBoolean()
  rawData = false;

  @Input()
  popover: TbPopoverComponent<DeviceDataKeysPanelComponent>;

  @Output()
  keysDataApplied = new EventEmitter<Array<DeviceDataKey> | { [key: string]: unknown }>();

  SocketValueKey = SocketValueKey;

  socketEncoding = Object.values(SocketEncoding);

  requestsType = Object.values(RequestsType);

  expressionType = Object.values(ExpressionType);

  dataKeyType: DataKeyType;

  keysListFormArray: UntypedFormArray;

  errorText = '';

  constructor(private fb: UntypedFormBuilder,
              protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit(): void {
    this.keysListFormArray = this.prepareKeysFormArray(this.keys);
  }

  trackByKey(index: number, keyControl: AbstractControl): any {
    return keyControl;
  }

  addKey(): void {
    let dataKeyFormGroup: FormGroup;
    if (this.keysType === SocketValueKey.RPC_METHODS) {
      dataKeyFormGroup = this.fb.group({
        methodRPC: ['', [Validators.required]],
        encoding: [SocketEncoding.UTF16, [Validators.required]],
        withResponse: [true]
      });
    } else if (this.keysType === SocketValueKey.ATTRIBUTES_UPDATES) {
      dataKeyFormGroup = this.fb.group({
        encoding: [SocketEncoding.UTF16, [Validators.required]],
        attributeOnThingsBoard: ['', [Validators.required]],
      });
    } else if (this.keysType === SocketValueKey.ATTRIBUTES_REQUESTS) {
      dataKeyFormGroup = this.fb.group({
        type: [RequestsType.Shared, [Validators.required]],
        expressionType : [ExpressionType.Constant, [Validators.required]],
        requestExpression: ['', []],
        attributeNameExpression: ['', []]
      });
    } else {
      dataKeyFormGroup = this.fb.group({
        key: ['name', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
        byteFrom: ['0', [Validators.required]],
        byteTo: ['-1', [Validators.required]],
      });
    }
    this.keysListFormArray.push(dataKeyFormGroup);
  }

  deleteKey($event: Event, index: number): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.keysListFormArray.removeAt(index);
    this.keysListFormArray.markAsDirty();
  }

  cancel(): void {
    this.popover?.hide();
  }

  applyKeysData(): void {
    const keys = this.keysListFormArray.value;
    this.keysDataApplied.emit(keys);
  }

  private prepareKeysFormArray(keys: Array<DeviceDataKey | RpcMethodsMapping | SocketAttributeUpdates | DeviceAttributesRequests> | {
    [key: string]: any
  }): UntypedFormArray {
    const keysControlGroups: Array<AbstractControl> = [];
    if (keys) {
      keys.forEach((keyData) => {
        let dataKeyFormGroup: FormGroup;
        if (this.keysType === SocketValueKey.RPC_METHODS) {
          dataKeyFormGroup = this.fb.group({
            methodRPC: [keyData.methodRPC, [Validators.required]],
            encoding: [keyData.encoding, [Validators.required]],
            withResponse: [true]
          });
        } else if (this.keysType === SocketValueKey.ATTRIBUTES_REQUESTS) {
          dataKeyFormGroup = this.fb.group({
            type: [keyData.type, [Validators.required]],
            expressionType: [keyData.expressionType, [Validators.required]],
            requestExpression: [keyData.requestExpression, []],
            attributeNameExpression: [keyData.attributeNameExpression, []]
          });
        } else if (this.keysType === SocketValueKey.ATTRIBUTES_UPDATES) {
          dataKeyFormGroup = this.fb.group({
            encoding: [keyData.encoding, [Validators.required]],
            attributeOnThingsBoard: [keyData.attributeOnThingsBoard, [Validators.required]],
          });
        } else {
          const {key, byteFrom, byteTo} = keyData;
          dataKeyFormGroup = this.fb.group({
            key: [key, [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
            byteFrom: [byteFrom, [Validators.required]],
            byteTo: [byteTo, [Validators.required]]
          });
        }
        keysControlGroups.push(dataKeyFormGroup);
      });
    }
    return this.fb.array(keysControlGroups);
  }

  valueTitle(keyControl: FormControl): string {
    const controlName = this.keysType === SocketValueKey.RPC_METHODS
      ? 'methodRPC'
      : this.keysType === SocketValueKey.ATTRIBUTES_UPDATES
        ? 'attributeOnThingsBoard'
        : this.keysType === SocketValueKey.ATTRIBUTES_REQUESTS
          ? 'requestExpression'
          : 'value';

    const value = keyControl.get(controlName)?.value;

    if (isDefinedAndNotNull(value)) {
      return typeof value === 'object' ? JSON.stringify(value) : value;
    }

    return '';
  }
}

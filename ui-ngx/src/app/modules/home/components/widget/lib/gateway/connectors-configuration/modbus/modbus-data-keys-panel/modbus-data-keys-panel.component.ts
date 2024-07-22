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

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { AbstractControl, FormGroup, UntypedFormArray, UntypedFormBuilder, Validators } from '@angular/forms';
import { TbPopoverComponent } from '@shared/components/popover.component';
import {
  ModbusDataType,
  ModbusFunctionCodeTranslationsMap,
  ModbusObjectCountByDataType,
  ModbusRegisterType,
  ModbusValue,
  ModbusValueKey,
  noLeadTrailSpacesRegex,
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { GatewayHelpLinkPipe } from '@home/pipes/public-api';
import { generateSecret } from '@core/utils';

@Component({
  selector: 'tb-modbus-data-keys-panel',
  templateUrl: './modbus-data-keys-panel.component.html',
  styleUrls: ['./modbus-data-keys-panel.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    GatewayHelpLinkPipe,
  ]
})
export class ModbusDataKeysPanelComponent implements OnInit {

  @Input() isMaster = false;
  @Input() panelTitle: string;
  @Input() addKeyTitle: string;
  @Input() deleteKeyTitle: string;
  @Input() noKeysText: string;
  @Input() register: ModbusRegisterType;
  @Input() keysType: ModbusValueKey;
  @Input() values: ModbusValue[];
  @Input() popover: TbPopoverComponent<ModbusDataKeysPanelComponent>;

  @Output() keysDataApplied = new EventEmitter<Array<ModbusValue>>();

  keysListFormArray: UntypedFormArray;
  errorText = '';
  modbusDataTypes = Object.values(ModbusDataType);
  withFunctionCode = true;
  functionCodesMap = new Map();
  defaultFunctionCodes = [];

  readonly editableDataTypes = [ModbusDataType.BYTES, ModbusDataType.BITS, ModbusDataType.STRING];
  readonly ModbusFunctionCodeTranslationsMap = ModbusFunctionCodeTranslationsMap;
  readonly defaultReadFunctionCodes = [3, 4];
  readonly defaultWriteFunctionCodes = [5, 6, 15, 16];
  readonly stringAttrUpdatesWriteFunctionCodes = [6, 16];

  constructor(private fb: UntypedFormBuilder) {}

  ngOnInit(): void {
    this.keysListFormArray = this.prepareKeysFormArray(this.values);
    this.withFunctionCode = !this.isMaster || (this.keysType !== ModbusValueKey.ATTRIBUTES && this.keysType !== ModbusValueKey.TIMESERIES);
    this.defaultFunctionCodes = this.getDefaultFunctionCodes();
  }

  trackByKey(_: number, keyControl: AbstractControl): AbstractControl {
    return keyControl;
  }

  addKey(): void {
    const dataKeyFormGroup = this.fb.group({
      tag: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      value: [{value: '', disabled: !this.isMaster}, [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      type: [ModbusDataType.BYTES, [Validators.required]],
      address: [0, [Validators.required]],
      objectsCount: [1, [Validators.required]],
      functionCode: [this.getDefaultFunctionCodes()[0]],
      id: [{value: generateSecret(5), disabled: true}],
    });
    this.observeKeyDataType(dataKeyFormGroup);

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
    this.keysDataApplied.emit(this.keysListFormArray.value);
  }

  private prepareKeysFormArray(values: ModbusValue[]): UntypedFormArray {
    const keysControlGroups: Array<AbstractControl> = [];
    if (values) {
      values.forEach(keyData => {
        const { tag, value, type, address, objectsCount, functionCode } = keyData;
        const dataKeyFormGroup = this.fb.group({
          tag: [tag, [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
          value: [{value, disabled: !this.isMaster}, [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
          type: [type, [Validators.required]],
          address: [address, [Validators.required]],
          objectsCount: [objectsCount, [Validators.required]],
          functionCode: [functionCode, []],
          id: [{value: generateSecret(5), disabled: true}],
        });
        this.observeKeyDataType(dataKeyFormGroup);
        this.functionCodesMap.set(dataKeyFormGroup.get('id').value, this.getFunctionCodes(type));

        keysControlGroups.push(dataKeyFormGroup);
      });
    }
    return this.fb.array(keysControlGroups);
  }

  private observeKeyDataType(keyFormGroup: FormGroup): void {
    keyFormGroup.get('type').valueChanges.subscribe(dataType => {
      const objectsCountControl = keyFormGroup.get('objectsCount');
      if (!this.editableDataTypes.includes(dataType)) {
        objectsCountControl.patchValue(ModbusObjectCountByDataType[dataType]);
      }
      this.functionCodesMap.set(keyFormGroup.get('id').value, this.getFunctionCodes(dataType));
    });
  }

  private getFunctionCodes(dataType: ModbusDataType): number[] {
    if (this.keysType === ModbusValueKey.ATTRIBUTES_UPDATES) {
      if (dataType === ModbusDataType.STRING) {
        return this.stringAttrUpdatesWriteFunctionCodes;
      }
      return this.defaultWriteFunctionCodes;
    }
    const functionCodes = [...this.defaultReadFunctionCodes];
    if (dataType === ModbusDataType.BITS) {
      const bitsFunctionCodes = [1, 2];
      bitsFunctionCodes.forEach(code => functionCodes.push(code));
      functionCodes.sort();
    }
    if (this.keysType === ModbusValueKey.RPC_REQUESTS) {
      this.defaultWriteFunctionCodes.forEach(code => functionCodes.push(code));
    }
    return functionCodes;
  }

  private getDefaultFunctionCodes(): number[] {
    if (this.keysType === ModbusValueKey.ATTRIBUTES_UPDATES) {
      return this.defaultWriteFunctionCodes;
    }
    if (this.keysType === ModbusValueKey.RPC_REQUESTS) {
      return [...this.defaultReadFunctionCodes, ...this.defaultWriteFunctionCodes];
    }
    return this.defaultReadFunctionCodes;
  }
}

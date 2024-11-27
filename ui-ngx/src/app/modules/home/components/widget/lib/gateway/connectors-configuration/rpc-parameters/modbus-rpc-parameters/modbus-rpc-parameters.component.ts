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
  ChangeDetectionStrategy,
  Component,
  forwardRef,
  OnDestroy,
} from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import {
  ModbusDataType,
  ModbusEditableDataTypes,
  ModbusFunctionCodeTranslationsMap,
  ModbusObjectCountByDataType,
  noLeadTrailSpacesRegex,
  RPCTemplateConfigModbus,
} from '@home/components/widget/lib/gateway/gateway-widget.models';

@Component({
  selector: 'tb-modbus-rpc-parameters',
  templateUrl: './modbus-rpc-parameters.component.html',
  styleUrls: ['./modbus-rpc-parameters.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ModbusRpcParametersComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ModbusRpcParametersComponent),
      multi: true
    }
  ],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
  ],
})
export class ModbusRpcParametersComponent implements ControlValueAccessor, Validator, OnDestroy {

  rpcParametersFormGroup: UntypedFormGroup;
  functionCodes: Array<number>;

  readonly ModbusEditableDataTypes = ModbusEditableDataTypes;
  readonly ModbusFunctionCodeTranslationsMap = ModbusFunctionCodeTranslationsMap;

  readonly modbusDataTypes = Object.values(ModbusDataType) as ModbusDataType[];
  readonly writeFunctionCodes = [5, 6, 15, 16];

  private readonly defaultFunctionCodes = [3, 4, 6, 16];
  private readonly readFunctionCodes = [1, 2, 3, 4];
  private readonly bitsFunctionCodes = [...this.readFunctionCodes, ...this.writeFunctionCodes];

  private onChange: (value: RPCTemplateConfigModbus) => void;
  private onTouched: () => void;

  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder) {
    this.rpcParametersFormGroup = this.fb.group({
      type: [ModbusDataType.BYTES, [Validators.required]],
      functionCode: [this.defaultFunctionCodes[0], [Validators.required]],
      value: [{value: '', disabled: true}, [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      address: [null, [Validators.required]],
      objectsCount: [1, [Validators.required]],
    });

    this.updateFunctionCodes(this.rpcParametersFormGroup.get('type').value);
    this.observeValueChanges();
    this.observeKeyDataType();
    this.observeFunctionCode();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: (value: RPCTemplateConfigModbus) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  validate(): ValidationErrors | null {
    return this.rpcParametersFormGroup.valid ? null : {
      rpcParametersFormGroup: { valid: false }
    };
  }

  writeValue(value: RPCTemplateConfigModbus): void {
    this.rpcParametersFormGroup.patchValue(value, {emitEvent: false});
  }

  private observeValueChanges(): void {
    this.rpcParametersFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.onChange(value);
      this.onTouched();
    });
  }

  private observeKeyDataType(): void {
    this.rpcParametersFormGroup.get('type').valueChanges.pipe(takeUntil(this.destroy$)).subscribe(dataType => {
      if (!this.ModbusEditableDataTypes.includes(dataType)) {
        this.rpcParametersFormGroup.get('objectsCount').patchValue(ModbusObjectCountByDataType[dataType], {emitEvent: false});
      }
      this.updateFunctionCodes(dataType);
    });
  }

  private observeFunctionCode(): void {
    this.rpcParametersFormGroup.get('functionCode').valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(code => this.updateValueEnabling(code));
  }

  private updateValueEnabling(code: number): void {
    if (this.writeFunctionCodes.includes(code)) {
      this.rpcParametersFormGroup.get('value').enable({emitEvent: false});
    } else {
      this.rpcParametersFormGroup.get('value').setValue(null);
      this.rpcParametersFormGroup.get('value').disable({emitEvent: false});
    }
  }

  private updateFunctionCodes(dataType: ModbusDataType): void {
    this.functionCodes = dataType === ModbusDataType.BITS ? this.bitsFunctionCodes : this.defaultFunctionCodes;
    if (!this.functionCodes.includes(this.rpcParametersFormGroup.get('functionCode').value)) {
      this.rpcParametersFormGroup.get('functionCode').patchValue(this.functionCodes[0], {emitEvent: false});
    }
  }
}

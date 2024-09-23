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
  ChangeDetectorRef,
  Component,
  forwardRef,
  OnDestroy,
} from '@angular/core';
import {
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormGroup,
  ValidationErrors,
  Validator, Validators,
} from '@angular/forms';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import {
  integerRegex,
  MappingValueType,
  mappingValueTypesMap,
  noLeadTrailSpacesRegex,
  OPCTypeValue,
  RPCTemplateConfigOPC
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { isDefinedAndNotNull, isEqual } from '@core/utils';

@Component({
  selector: 'tb-opc-rpc-parameters',
  templateUrl: './opc-rpc-parameters.component.html',
  styleUrls: ['./opc-rpc-parameters.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => OpcRpcParametersComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => OpcRpcParametersComponent),
      multi: true
    }
  ],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
  ],
})
export class OpcRpcParametersComponent implements ControlValueAccessor, Validator, OnDestroy {

  rpcParametersFormGroup: UntypedFormGroup;

  readonly valueTypeKeys: MappingValueType[] = Object.values(MappingValueType);
  readonly MappingValueType = MappingValueType;
  readonly valueTypes = mappingValueTypesMap;

  private onChange: (value: RPCTemplateConfigOPC) => void = (_) => {} ;
  private onTouched: () => void = () => {};

  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder, private cdr: ChangeDetectorRef) {
    this.rpcParametersFormGroup = this.fb.group({
      method: [null, [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      arguments: this.fb.array([]),
    });

    this.observeValueChanges();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: (value: RPCTemplateConfigOPC) => void): void {
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

  writeValue(params: RPCTemplateConfigOPC): void {
    this.clearArguments();
    params.arguments?.map(({type, value}) => ({type, [type]: value }))
      .forEach(argument => this.addArgument(argument as OPCTypeValue));
    this.cdr.markForCheck();
    this.rpcParametersFormGroup.get('method').patchValue(params.method);
  }

  private observeValueChanges(): void {
    this.rpcParametersFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(params => {
      const updatedArguments = params.arguments.map(({type, ...config}) => ({type, value: config[type]}));
      this.onChange({method: params.method, arguments: updatedArguments});
      this.onTouched();
    });
  }

  removeArgument(index: number): void {
    (this.rpcParametersFormGroup.get('arguments') as FormArray).removeAt(index);
  }

  addArgument(value: OPCTypeValue = {} as OPCTypeValue): void {
    const fromGroup = this.fb.group({
      type: [value.type ?? MappingValueType.STRING],
      string: [
        value.string ?? { value: '', disabled: !(isEqual(value, {}) || value.string)},
        [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]
      ],
      integer: [
        {value: value.integer ?? 0, disabled: !isDefinedAndNotNull(value.integer)},
        [Validators.required, Validators.pattern(integerRegex)]
      ],
      double: [{value: value.double ?? 0, disabled: !isDefinedAndNotNull(value.double)}, [Validators.required]],
      boolean: [{value: value.boolean ?? false, disabled: !isDefinedAndNotNull(value.boolean)}, [Validators.required]],
    });
    this.observeTypeChange(fromGroup);
    (this.rpcParametersFormGroup.get('arguments') as FormArray).push(fromGroup, {emitEvent: false});
  }

  clearArguments(): void {
    const formArray = this.rpcParametersFormGroup.get('arguments') as FormArray;
    while (formArray.length !== 0) {
      formArray.removeAt(0);
    }
  }

  private observeTypeChange(dataKeyFormGroup: FormGroup): void {
    dataKeyFormGroup.get('type').valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(type => {
        dataKeyFormGroup.disable({emitEvent: false});
        dataKeyFormGroup.get('type').enable({emitEvent: false});
        dataKeyFormGroup.get(type).enable({emitEvent: false});
      });
  }
}

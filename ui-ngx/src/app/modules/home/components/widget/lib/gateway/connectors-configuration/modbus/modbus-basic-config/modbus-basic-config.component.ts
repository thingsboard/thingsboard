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

import { ChangeDetectionStrategy, Component, forwardRef, Input, OnDestroy, TemplateRef } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormControl,
  ValidationErrors,
  Validator,
} from '@angular/forms';
import { ModbusBasicConfig } from '@home/components/widget/lib/gateway/gateway-widget.models';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

import { EllipsisChipListDirective } from '@shared/directives/ellipsis-chip-list.directive';
import { ModbusSlaveConfigComponent } from '../modbus-slave-config/modbus-slave-config.component';
import { ModbusMasterTableComponent } from '../modbus-master-table/modbus-master-table.component';
import { isEqual } from '@core/utils';

@Component({
  selector: 'tb-modbus-basic-config',
  templateUrl: './modbus-basic-config.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ModbusBasicConfigComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ModbusBasicConfigComponent),
      multi: true
    }
  ],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    ModbusSlaveConfigComponent,
    ModbusMasterTableComponent,
    EllipsisChipListDirective,
  ],
  styleUrls: ['./modbus-basic-config.component.scss'],
})

export class ModbusBasicConfigComponent implements ControlValueAccessor, Validator, OnDestroy {

  @Input() generalTabContent: TemplateRef<any>;

  basicFormGroup: FormGroup;
  enableSlaveControl: FormControl<boolean>;

  onChange: (value: ModbusBasicConfig) => void;
  onTouched: () => void;

  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder) {
    this.basicFormGroup = this.fb.group({
      master: [],
      slave: [],
    });
    this.enableSlaveControl = new FormControl(false);

    this.basicFormGroup.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(value => {
        this.onChange(value);
        this.onTouched();
      });

    this.enableSlaveControl.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(enable => {
        this.updateSlaveEnabling(enable);
        this.basicFormGroup.get('slave').updateValueAndValidity();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: (value: ModbusBasicConfig) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  writeValue(basicConfig: ModbusBasicConfig): void {
    const editedBase = {
      slave: basicConfig.slave ?? {},
      master: basicConfig.master ?? {},
    };

    this.basicFormGroup.setValue(editedBase, {emitEvent: false});
    this.enableSlaveControl.setValue(!!basicConfig.slave && !isEqual(basicConfig.slave, {}));
  }

  validate(basicConfigControl: UntypedFormControl): ValidationErrors | null {
    const masterHasSlaves = !!basicConfigControl.value.master?.slaves?.length;
    const slaveEnabled = this.enableSlaveControl.value;
    const slaveIsValid = this.basicFormGroup.get('slave').valid;

    if ((slaveEnabled && slaveIsValid) || (masterHasSlaves && !slaveEnabled)) {
      return null;
    }

    return { basicFormGroup: { valid: false } };
  }

  private updateSlaveEnabling(isEnabled: boolean): void {
    if (isEnabled) {
      this.basicFormGroup.get('slave').enable({emitEvent: false});
    } else {
      this.basicFormGroup.get('slave').disable({emitEvent: false});
    }
  }
}

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
  Input,
  OnChanges,
  OnDestroy,
  Renderer2,
  ViewContainerRef
} from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
} from '@angular/forms';
import {
  ModbusKeysAddKeyTranslationsMap,
  ModbusKeysDeleteKeyTranslationsMap,
  ModbusKeysNoKeysTextTranslationsMap,
  ModbusKeysPanelTitleTranslationsMap,
  ModbusRegisterTranslationsMap,
  ModbusRegisterType,
  ModbusRegisterValues,
  ModbusValueKey,
  ModbusValues,
  ModbusValuesState,
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { EllipsisChipListDirective } from '@shared/directives/ellipsis-chip-list.directive';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { ModbusDataKeysPanelComponent } from '../modbus-data-keys-panel/modbus-data-keys-panel.component';

@Component({
  selector: 'tb-modbus-values',
  templateUrl: './modbus-values.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ModbusValuesComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ModbusValuesComponent),
      multi: true
    }
  ],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    EllipsisChipListDirective,
  ],
  styles: [`
    :host {
      .mat-mdc-tab-body-wrapper {
        min-height: 320px;
      }
    }
  `]
})

export class ModbusValuesComponent implements ControlValueAccessor, Validator, OnChanges, OnDestroy {

  @Input() singleMode = false;
  @Input() disabled = false;

  modbusRegisterTypes: ModbusRegisterType[] = Object.values(ModbusRegisterType);
  modbusValueKeys = Object.values(ModbusValueKey);
  ModbusValuesTranslationsMap = ModbusRegisterTranslationsMap;
  ModbusValueKey = ModbusValueKey;
  valuesFormGroup: FormGroup;

  private onChange: (value: string) => void;
  private onTouched: () => void;

  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private cdr: ChangeDetectorRef,
  ) {
    this.valuesFormGroup = this.fb.group(this.modbusRegisterTypes.reduce((registersAcc, register) => {
      return {
        ...registersAcc,
        [register]: this.fb.group(this.modbusValueKeys.reduce((acc, key) => ({...acc, [key]: [[], []]}), {})),
      };
    }, {}));

    this.observeValuesChanges();
  }

  ngOnChanges(): void {
    if (this.singleMode) {
      this.valuesFormGroup = this.fb.group(this.modbusValueKeys.reduce((acc, key) => ({...acc, [key]: [[], []]}), {}));
      this.observeValuesChanges();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  writeValue(values: ModbusValuesState): void {
    if (this.singleMode) {
      this.valuesFormGroup.setValue(this.getSingleRegisterState(values as ModbusValues), {emitEvent: false});
    } else {
      const registers = values as ModbusRegisterValues;
      this.valuesFormGroup.setValue({
        holding_registers: this.getSingleRegisterState(registers.holding_registers),
        coils_initializer: this.getSingleRegisterState(registers.coils_initializer),
        input_registers: this.getSingleRegisterState(registers.input_registers),
        discrete_inputs: this.getSingleRegisterState(registers.discrete_inputs),
      }, {emitEvent: false});
    }
    this.cdr.markForCheck();
  }

  validate(): ValidationErrors | null {
    return this.valuesFormGroup.valid ? null : {
      valuesFormGroup: {valid: false}
    };
  }

  getValueGroup(valueKey: ModbusValueKey, register?: ModbusRegisterType) {
    return register ? this.valuesFormGroup.get(register).get(valueKey).value : this.valuesFormGroup.get(valueKey).value;
  }

  manageKeys($event: Event, matButton: MatButton, keysType: ModbusValueKey, register?: ModbusRegisterType): void {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const group = this.valuesFormGroup;

      const keysControl = register ? group.get(register).get(keysType) : group.get(keysType);
      const ctx = {
        values: keysControl.value,
        isMaster: !this.singleMode,
        keysType,
        panelTitle: ModbusKeysPanelTitleTranslationsMap.get(keysType),
        addKeyTitle: ModbusKeysAddKeyTranslationsMap.get(keysType),
        deleteKeyTitle: ModbusKeysDeleteKeyTranslationsMap.get(keysType),
        noKeysText: ModbusKeysNoKeysTextTranslationsMap.get(keysType)
      };
      const dataKeysPanelPopover = this.popoverService.displayPopover(
        trigger,
        this.renderer,
        this.viewContainerRef,
        ModbusDataKeysPanelComponent,
        'leftBottom',
        false,
        null,
        ctx,
        {},
        {},
        {},
        true
      );
      dataKeysPanelPopover.tbComponentRef.instance.popover = dataKeysPanelPopover;
      dataKeysPanelPopover.tbComponentRef.instance.keysDataApplied.pipe(takeUntil(this.destroy$)).subscribe((keysData) => {
        dataKeysPanelPopover.hide();
        keysControl.patchValue(keysData);
        keysControl.markAsDirty();
        this.cdr.markForCheck();
      });
    }
  }

  private observeValuesChanges(): void {
    this.valuesFormGroup.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(value => {
        this.onChange(value);
        this.onTouched();
      });
  }

  private getSingleRegisterState(values: ModbusValues): ModbusValues {
    return {
      attributes: values?.attributes ?? [],
      timeseries: values?.timeseries ?? [],
      attributeUpdates: values?.attributeUpdates ?? [],
      rpc: values?.rpc ?? [],
    };
  }
}

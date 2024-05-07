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

import { ChangeDetectorRef, Component, forwardRef, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  defaultScadaObjectSettings,
  parseScadaObjectMetadataFromContent,
  IotSvgBehaviorType,
  IotSvgMetadata,
  IotSvgObjectSettings
} from '@home/components/widget/lib/svg/iot-svg.models';
import { HttpClient } from '@angular/common/http';
import { ValueType } from '@shared/models/constants';
import { IAliasController } from '@core/api/widget-api.models';
import { TargetDevice, widgetType } from '@shared/models/widget.models';
import { isDefinedAndNotNull } from '@core/utils';
import {
  ScadaPropertyRow,
  toPropertyRows
} from '@home/components/widget/lib/settings/common/svg/iot-svg-object-settings.models';
import { merge, Observable, Subscription } from 'rxjs';
import { WidgetActionCallbacks } from '@home/components/widget/action/manage-widget-actions.component.models';

@Component({
  selector: 'tb-iot-svg-object-settings',
  templateUrl: './iot-svg-object-settings.component.html',
  styleUrls: ['./../../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => IotSvgObjectSettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => IotSvgObjectSettingsComponent),
      multi: true
    }
  ]
})
export class IotSvgObjectSettingsComponent implements OnInit, OnChanges, ControlValueAccessor, Validator {

  IotSvgBehaviorType = IotSvgBehaviorType;

  @Input()
  disabled: boolean;

  @Input()
  svgPath = 'drawing.svg';

  @Input()
  aliasController: IAliasController;

  @Input()
  targetDevice: TargetDevice;

  @Input()
  callbacks: WidgetActionCallbacks;

  @Input()
  widgetType: widgetType;

  private modelValue: IotSvgObjectSettings;

  private propagateChange = null;

  private validatorTriggers: string[];
  private validatorSubscription: Subscription;

  public scadaObjectSettingsFormGroup: UntypedFormGroup;

  metadata: IotSvgMetadata;
  propertyRows: ScadaPropertyRow[];

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private http: HttpClient,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.scadaObjectSettingsFormGroup = this.fb.group({});
    this.scadaObjectSettingsFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
    this.loadMetadata();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['svgPath'].includes(propName)) {
          this.loadMetadata();
        }
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.scadaObjectSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.scadaObjectSettingsFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: IotSvgObjectSettings): void {
    this.modelValue = value || {};
    this.setupValue();
  }

  validate(c: UntypedFormControl) {
    const valid = this.scadaObjectSettingsFormGroup.valid;
    return valid ? null : {
      scadaObject: {
        valid: false,
      },
    };
  }

  private loadMetadata() {
    if (this.validatorSubscription) {
      this.validatorSubscription.unsubscribe();
      this.validatorSubscription = null;
    }
    this.validatorTriggers = [];
    this.http.get(this.svgPath, {responseType: 'text'}).subscribe(
      (svgContent) => {
        this.metadata = parseScadaObjectMetadataFromContent(svgContent);
        this.propertyRows = toPropertyRows(this.metadata.properties);
        for (const control of Object.keys(this.scadaObjectSettingsFormGroup.controls)) {
          this.scadaObjectSettingsFormGroup.removeControl(control, {emitEvent: false});
        }
        for (const behaviour of this.metadata.behavior) {
          this.scadaObjectSettingsFormGroup.addControl(behaviour.id, this.fb.control(null, []), {emitEvent: false});
        }
        for (const property of this.metadata.properties) {
          if (property.disableOnProperty) {
            if (!this.validatorTriggers.includes(property.disableOnProperty)) {
              this.validatorTriggers.push(property.disableOnProperty);
            }
          }
          const validators: ValidatorFn[] = [];
          if (property.required) {
            validators.push(Validators.required);
          }
          if (property.type === 'number') {
            if (isDefinedAndNotNull(property.min)) {
              validators.push(Validators.min(property.min));
            }
            if (isDefinedAndNotNull(property.max)) {
              validators.push(Validators.max(property.max));
            }
          }
          this.scadaObjectSettingsFormGroup.addControl(property.id, this.fb.control(null, validators), {emitEvent: false});
        }
        if (this.validatorTriggers.length) {
          const observables: Observable<any>[] = [];
          for (const trigger of this.validatorTriggers) {
            observables.push(this.scadaObjectSettingsFormGroup.get(trigger).valueChanges);
          }
          this.validatorSubscription = merge(...observables).subscribe(() => {
            this.updateValidators();
          });
        }
        this.setupValue();
        this.cd.markForCheck();
      }
    );
  }

  private updateValidators() {
    for (const trigger of this.validatorTriggers) {
      const value: boolean = this.scadaObjectSettingsFormGroup.get(trigger).value;
      this.metadata.properties.filter(p => p.disableOnProperty === trigger).forEach(
        (p) => {
          const control = this.scadaObjectSettingsFormGroup.get(p.id);
          if (value) {
            control.enable({emitEvent: false});
          } else {
            control.disable({emitEvent: false});
          }
        }
      );
    }
  }

  private setupValue() {
    if (this.metadata) {
      const defaults = defaultScadaObjectSettings(this.metadata);
      this.modelValue = {...defaults, ...this.modelValue};
      this.scadaObjectSettingsFormGroup.patchValue(
        this.modelValue, {emitEvent: false}
      );
      this.setDisabledState(this.disabled);
    }
  }

  private updateModel() {
    this.modelValue = this.scadaObjectSettingsFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }

  protected readonly ValueType = ValueType;
}

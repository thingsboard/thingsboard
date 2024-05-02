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

import { Component, forwardRef, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  defaultScadaObjectSettings,
  parseScadaObjectMetadataFromContent,
  ScadaObjectBehaviorType,
  ScadaObjectMetadata,
  ScadaObjectSettings
} from '@home/components/widget/lib/scada/scada.models';
import { HttpClient } from '@angular/common/http';
import { ValueType } from '@shared/models/constants';
import { IAliasController } from '@core/api/widget-api.models';
import { TargetDevice, widgetType } from '@shared/models/widget.models';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-scada-object-settings',
  templateUrl: './scada-object-settings.component.html',
  styleUrls: ['./../../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ScadaObjectSettingsComponent),
      multi: true
    }
  ]
})
export class ScadaObjectSettingsComponent implements OnInit, OnChanges, ControlValueAccessor {

  ScadaObjectBehaviorType = ScadaObjectBehaviorType;

  @Input()
  disabled: boolean;

  @Input()
  svgPath = '/assets/widget/scada/drawing.svg';

  @Input()
  aliasController: IAliasController;

  @Input()
  targetDevice: TargetDevice;

  @Input()
  widgetType: widgetType;

  private modelValue: ScadaObjectSettings;

  private propagateChange = null;

  public scadaObjectSettingsFormGroup: UntypedFormGroup;

  metadata: ScadaObjectMetadata;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private http: HttpClient) {
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
    }
  }

  writeValue(value: ScadaObjectSettings): void {
    this.modelValue = value || {};
    this.setupValue();
  }

  private loadMetadata() {
    this.http.get(this.svgPath, {responseType: 'text'}).subscribe(
      (svgContent) => {
        this.metadata = parseScadaObjectMetadataFromContent(svgContent);
        for (const control of Object.keys(this.scadaObjectSettingsFormGroup.controls)) {
          this.scadaObjectSettingsFormGroup.removeControl(control, {emitEvent: false});
        }
        for (const behaviour of this.metadata.behavior) {
          this.scadaObjectSettingsFormGroup.addControl(behaviour.id, this.fb.control(null, []), {emitEvent: false});
        }
        for (const property of this.metadata.properties) {
          const validators: ValidatorFn[] = [];
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
        this.setupValue();
      }
    );
  }

  private setupValue() {
    if (this.metadata) {
      const defaults = defaultScadaObjectSettings(this.metadata);
      this.modelValue = {...defaults, ...this.modelValue};
      this.scadaObjectSettingsFormGroup.patchValue(
        this.modelValue, {emitEvent: false}
      );
    }
  }

  private updateModel() {
    this.modelValue = this.scadaObjectSettingsFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }

  protected readonly ValueType = ValueType;
}

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

import { Component, Input, OnInit, forwardRef } from "@angular/core";
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator
} from "@angular/forms";
import { AppState } from "@app/core/core.state";
import { WidgetConfigComponentData } from "@app/modules/home/models/widget-component.models";
import { PageComponent } from "@app/shared/public-api";
import { Store } from "@ngrx/store";
import { TranslateService } from "@ngx-translate/core";

export enum DisplacementCalculationType {
  CUMULATIVE = 'cumulative',
  INCREMENTAL = 'incremental'
}
export enum DisplacementCalculationDirection {
  START = 'start',
  END = 'end'
}

export const displacementDefaultSettings = (dataKeys: string[]) => {
  return {
    grid: true,
    xaxis: {
      min: null,
      max: null,
    },
    yaxis: {
      unit: null,
    },
    calculation: {
      type: DisplacementCalculationType.INCREMENTAL,
      direction: DisplacementCalculationDirection.START,
    },
    baseline: {
      baseline_date: null,
      baseline_time: null,
      enterManually: false,
      baseline: dataKeys.map((key) => ({ key: key, value: null }))
    },
    position: dataKeys.map((key) => ({ key: key, depth: null })),
    layers: [],
    thresholds: []
  }
}

@Component({
  selector: "tb-displacement-chart-key-settings",
  templateUrl: "./displacement-chart-key-settings.component.html",
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DisplacementChartKeySettingsComponent),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DisplacementChartKeySettingsComponent),
      multi: true,
    },
  ],
})
export class DisplacementChartKeySettingsComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator
{
  @Input()
  widgetConfig: WidgetConfigComponentData;
  
  private modelValue: any;

  private propagateChange = null;
  
  public displacementSettingsFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    console.log('ngOnInit dataKeys()', this.dataKeys());

    this.displacementSettingsFormGroup = this.fb.group({
      grid: [true, []],
      xaxis: this.fb.group({
        min: [null, []],
        max: [null, []]
      }),
      yaxis: this.fb.group({
        unit: [null, []],
      }),
      calculation: this.fb.group({
        type: [DisplacementCalculationType.INCREMENTAL, []],
        direction: [DisplacementCalculationDirection.START, []]
      }),
      baseline: this.fb.group({
        baseline_date: [null, []],
        baseline_time: [null, []],
        enterManually: [false, []],
        baseline: this.fb.array(
          this.dataKeys().map((key) => this.fb.group({ key: [key, []], value: [null, []] }))
        ),
      }),
      position: this.fb.array(
        this.dataKeys().map((key) => this.fb.group({ key: [key, []], depth: [null, []] }))
      ),
      layers: this.fb.array([]),
      thresholds: this.fb.array([]),
    });

    this.displacementSettingsFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });

    this.updateValidators(false);
  }

  dataKeys(): string[] {
    return this.widgetConfig.config.datasources[0]?.dataKeys.map(
      (dataKeys) => dataKeys.name
    ) || [];
  }

  get baselineArray(): UntypedFormArray {
    return this.displacementSettingsFormGroup.get('baseline.baseline') as UntypedFormArray;
  }
  
  get positionArray(): UntypedFormArray {
    return this.displacementSettingsFormGroup.get('position') as UntypedFormArray;
  }
  
  get layersArray(): UntypedFormArray {
    return this.displacementSettingsFormGroup.get('layers') as UntypedFormArray;
  }
  
  get thresholdsArray(): UntypedFormArray {
    return this.displacementSettingsFormGroup.get('thresholds') as UntypedFormArray;
  }

  addLayer() {
    const layersArray = this.layersArray;
    const layerGroup = this.fb.group({
      from: [null, []],
      to: [null, []],
      title: [null, []],
      color: [null, []],
    });
    layersArray.push(layerGroup);
    this.displacementSettingsFormGroup.updateValueAndValidity();
  }

  addThreshold() {
    const thresholdsArray = this.thresholdsArray;
    const thresholdControl = this.fb.group({
      x_pos: [null, []],
      title: [null, []],
      color: [null, []],
    });
    thresholdsArray.push(thresholdControl);
    this.displacementSettingsFormGroup.updateValueAndValidity();
  }

  removeLayer(index: number) {
    const layersArray = this.displacementSettingsFormGroup.get('layers') as UntypedFormArray;
    layersArray.removeAt(index);
  }
  
  removeThreshold(index: number) {
    const thresholdsArray = this.displacementSettingsFormGroup.get('thresholds') as UntypedFormArray;
    thresholdsArray.removeAt(index);
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {}

  writeValue(value: any): void {
    console.log('writeValue value', value);
    this.modelValue = value;
    this.displacementSettingsFormGroup.patchValue(value, {emitEvent: false});

    const layers = value.layers;
    const thresholds = value.thresholds;
    if (layers && layers.length) {
      layers.forEach((layer) => this.layersArray.push(this.fb.group({
        from: [layer.from, []],
        to: [layer.to, []],
        title: [layer.title, []],
        color: [layer.color, []],
      }), {emitEvent: false}));
    }
    if (thresholds && thresholds.length) {
      thresholds.forEach((threshold) => this.thresholdsArray.push(this.fb.group({
        x_pos: [threshold.x_pos, []],
        title: [threshold.title, []],
        color: [threshold.color, []],
      }), {emitEvent: false}))
    }

    this.updateValidators(false);
  }

  validate(c: UntypedFormControl) {
    return (this.displacementSettingsFormGroup.valid) ? null : {
      displacementSettings: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value: any = this.displacementSettingsFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }

  private updateValidators(emitEvent?: boolean): void {
    this.displacementSettingsFormGroup.get('grid').updateValueAndValidity({emitEvent: false});
    this.displacementSettingsFormGroup.get('xaxis.min').updateValueAndValidity({emitEvent: false});
    this.displacementSettingsFormGroup.get('xaxis.max').updateValueAndValidity({emitEvent: false});
    this.displacementSettingsFormGroup.get('yaxis.unit').updateValueAndValidity({emitEvent: false});
    this.displacementSettingsFormGroup.get('calculation.type').updateValueAndValidity({emitEvent: false});
    this.displacementSettingsFormGroup.get('calculation.direction').updateValueAndValidity({emitEvent: false});
    this.displacementSettingsFormGroup.get('baseline.baseline_date').updateValueAndValidity({emitEvent: false});
    this.displacementSettingsFormGroup.get('baseline.baseline_time').updateValueAndValidity({emitEvent: false});
    this.displacementSettingsFormGroup.get('baseline.enterManually').updateValueAndValidity({emitEvent: false});
    this.displacementSettingsFormGroup.get('baseline.baseline').updateValueAndValidity({emitEvent: false});
    this.displacementSettingsFormGroup.get('position').updateValueAndValidity({emitEvent: false});
    this.displacementSettingsFormGroup.get('layers').updateValueAndValidity({emitEvent: false});
    this.displacementSettingsFormGroup.get('thresholds').updateValueAndValidity({emitEvent: false});
  }
}

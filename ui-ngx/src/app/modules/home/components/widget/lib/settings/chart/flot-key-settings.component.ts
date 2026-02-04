///
/// Copyright © 2016-2026 The Thingsboard Authors
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

import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { ChartType, TbFlotKeySettings, TbFlotKeyThreshold } from '@home/components/widget/lib/flot-widget.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { WidgetService } from '@core/http/widget.service';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { IAliasController } from 'src/app/core/api/widget-api.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export function flotDataKeyDefaultSettings(chartType: ChartType): TbFlotKeySettings {
  const settings: TbFlotKeySettings = {
    // Common settings
    hideDataByDefault: false,
    disableDataHiding: false,
    removeFromLegend: false,
    excludeFromStacking: false,

    // Line settings
    showLines: chartType === 'graph',
    lineWidth: 1,
    fillLines: false,
    fillLinesOpacity: 0.4,

    // Points settings
    showPoints: false,
    showPointsLineWidth: 5,
    showPointsRadius: 3,
    showPointShape: 'circle',
    pointShapeFormatter: 'var size = radius * Math.sqrt(Math.PI) / 2;\n' +
      'ctx.moveTo(x - size, y - size);\n' +
      'ctx.lineTo(x + size, y + size);\n' +
      'ctx.moveTo(x - size, y + size);\n' +
      'ctx.lineTo(x + size, y - size);',

    // Tooltip settings
    tooltipValueFormatter: '',

    // Y axis settings
    showSeparateAxis: false,
    axisTitle: '',
    axisMin: null,
    axisMax: null,
    axisPosition: 'left',

    // --> Y axis tick labels settings
    axisTickSize: null,
    axisTickDecimals: null,
    axisTicksFormatter: '',

    // Thresholds
    thresholds: [],

    // Comparison settings
    comparisonSettings: {
      showValuesForComparison: true,
      comparisonValuesLabel: '',
      color: ''
    }
  };
  return settings;
}

@Component({
    selector: 'tb-flot-key-settings',
    templateUrl: './flot-key-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => FlotKeySettingsComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => FlotKeySettingsComponent),
            multi: true,
        }
    ],
    standalone: false
})
export class FlotKeySettingsComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  disabled: boolean;

  @Input()
  chartType: ChartType;

  @Input()
  aliasController: IAliasController;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  private modelValue: TbFlotKeySettings;

  private propagateChange = null;

  public flotKeySettingsFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private widgetService: WidgetService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.flotKeySettingsFormGroup = this.fb.group({

      // Common settings

      hideDataByDefault: [false, []],
      disableDataHiding: [false, []],
      removeFromLegend: [false, []],
      excludeFromStacking: [false, []],

      // Line settings

      showLines: [this.chartType === 'graph', []],
      lineWidth: [1, [Validators.min(0)]],
      fillLines: [false, []],
      fillLinesOpacity: [0.4, [Validators.min(0), Validators.max(1)]],

      // Points settings

      showPoints: [false, []],
      showPointsLineWidth: [5, [Validators.min(0)]],
      showPointsRadius: [3, [Validators.min(0)]],
      showPointShape: ['circle', []],
      pointShapeFormatter: ['', []],

      // Tooltip settings

      tooltipValueFormatter: ['', []],

      // Y axis settings

      showSeparateAxis: [false, []],
      axisTitle: [null, []],
      axisMin: [null, []],
      axisMax: [null, []],
      axisPosition: ['left', []],

      // --> Y axis tick labels settings

      axisTickSize: [null, [Validators.min(0)]],
      axisTickDecimals: [null, [Validators.min(0)]],
      axisTicksFormatter: ['', []],

      // Thresholds

      thresholds: this.fb.array([]),

      // Comparison settings

      comparisonSettings: this.fb.group({
        showValuesForComparison: [true, []],
        comparisonValuesLabel: ['', []],
        color: ['', []]
      })

    });

    this.flotKeySettingsFormGroup.get('showLines').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(false);
    });

    this.flotKeySettingsFormGroup.get('fillLines').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(false);
    });

    this.flotKeySettingsFormGroup.get('showPoints').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(false);
    });

    this.flotKeySettingsFormGroup.get('comparisonSettings.showValuesForComparison').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(false);
    });

    this.flotKeySettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });

    this.updateValidators(false);
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.flotKeySettingsFormGroup.disable({emitEvent: false});
    } else {
      this.flotKeySettingsFormGroup.enable({emitEvent: false});
      this.updateValidators(false);
    }
  }

  writeValue(value: TbFlotKeySettings): void {
    const thresholds = value?.thresholds;
    this.modelValue = value;
    this.flotKeySettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    const thresholdsControls: Array<AbstractControl> = [];
    if (thresholds && thresholds.length) {
      thresholds.forEach((threshold) => {
        thresholdsControls.push(this.fb.control(threshold, []));
      });
    }
    this.flotKeySettingsFormGroup.setControl('thresholds', this.fb.array(thresholdsControls), {emitEvent: false});
    this.updateValidators(false);
  }

  validate(c: UntypedFormControl) {
    return (this.flotKeySettingsFormGroup.valid) ? null : {
      flotKeySettings: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value: TbFlotKeySettings = this.flotKeySettingsFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }

  private updateValidators(emitEvent?: boolean): void {
    const showLines: boolean = this.flotKeySettingsFormGroup.get('showLines').value;
    const fillLines: boolean = this.flotKeySettingsFormGroup.get('fillLines').value;
    const showPoints: boolean = this.flotKeySettingsFormGroup.get('showPoints').value;
    const showValuesForComparison: boolean = this.flotKeySettingsFormGroup.get('comparisonSettings.showValuesForComparison').value;

    if (showLines) {
      this.flotKeySettingsFormGroup.get('lineWidth').enable({emitEvent});
      this.flotKeySettingsFormGroup.get('fillLines').enable({emitEvent});
      if (fillLines) {
        this.flotKeySettingsFormGroup.get('fillLinesOpacity').enable({emitEvent});
      } else {
        this.flotKeySettingsFormGroup.get('fillLinesOpacity').disable({emitEvent});
      }
    } else {
      this.flotKeySettingsFormGroup.get('lineWidth').disable({emitEvent});
      this.flotKeySettingsFormGroup.get('fillLines').disable({emitEvent});
      this.flotKeySettingsFormGroup.get('fillLinesOpacity').disable({emitEvent});
    }

    if (showPoints) {
      this.flotKeySettingsFormGroup.get('showPointsLineWidth').enable({emitEvent});
      this.flotKeySettingsFormGroup.get('showPointsRadius').enable({emitEvent});
      this.flotKeySettingsFormGroup.get('showPointShape').enable({emitEvent});
      this.flotKeySettingsFormGroup.get('pointShapeFormatter').enable({emitEvent});
    } else {
      this.flotKeySettingsFormGroup.get('showPointsLineWidth').disable({emitEvent});
      this.flotKeySettingsFormGroup.get('showPointsRadius').disable({emitEvent});
      this.flotKeySettingsFormGroup.get('showPointShape').disable({emitEvent});
      this.flotKeySettingsFormGroup.get('pointShapeFormatter').disable({emitEvent});
    }

    if (showValuesForComparison) {
      this.flotKeySettingsFormGroup.get('comparisonSettings.comparisonValuesLabel').enable({emitEvent});
      this.flotKeySettingsFormGroup.get('comparisonSettings.color').enable({emitEvent});
    } else {
      this.flotKeySettingsFormGroup.get('comparisonSettings.comparisonValuesLabel').disable({emitEvent});
      this.flotKeySettingsFormGroup.get('comparisonSettings.color').disable({emitEvent});
    }

    this.flotKeySettingsFormGroup.get('lineWidth').updateValueAndValidity({emitEvent: false});
    this.flotKeySettingsFormGroup.get('fillLines').updateValueAndValidity({emitEvent: false});
    this.flotKeySettingsFormGroup.get('fillLinesOpacity').updateValueAndValidity({emitEvent: false});
    this.flotKeySettingsFormGroup.get('showPointsLineWidth').updateValueAndValidity({emitEvent: false});
    this.flotKeySettingsFormGroup.get('showPointsRadius').updateValueAndValidity({emitEvent: false});
    this.flotKeySettingsFormGroup.get('showPointShape').updateValueAndValidity({emitEvent: false});
    this.flotKeySettingsFormGroup.get('pointShapeFormatter').updateValueAndValidity({emitEvent: false});
    this.flotKeySettingsFormGroup.get('comparisonSettings.comparisonValuesLabel').updateValueAndValidity({emitEvent: false});
    this.flotKeySettingsFormGroup.get('comparisonSettings.color').updateValueAndValidity({emitEvent: false});
  }

  thresholdsFormArray(): UntypedFormArray {
    return this.flotKeySettingsFormGroup.get('thresholds') as UntypedFormArray;
  }

  public trackByThreshold(index: number, thresholdControl: AbstractControl): any {
    return thresholdControl;
  }

  public removeThreshold(index: number) {
    (this.flotKeySettingsFormGroup.get('thresholds') as UntypedFormArray).removeAt(index);
  }

  public addThreshold() {
    const threshold: TbFlotKeyThreshold = {
      thresholdValueSource: 'predefinedValue',
      thresholdEntityAlias: null,
      thresholdAttribute: null,
      thresholdValue: null,
      lineWidth: null,
      color: null
    };
    const thresholdsArray = this.flotKeySettingsFormGroup.get('thresholds') as UntypedFormArray;
    const thresholdControl = this.fb.control(threshold, []);
    (thresholdControl as any).new = true;
    thresholdsArray.push(thresholdControl);
    this.flotKeySettingsFormGroup.updateValueAndValidity();
  }

  thresholdDrop(event: CdkDragDrop<string[]>) {
    const thresholdsArray = this.flotKeySettingsFormGroup.get('thresholds') as UntypedFormArray;
    const threshold = thresholdsArray.at(event.previousIndex);
    thresholdsArray.removeAt(event.previousIndex);
    thresholdsArray.insert(event.currentIndex, threshold);
  }

}

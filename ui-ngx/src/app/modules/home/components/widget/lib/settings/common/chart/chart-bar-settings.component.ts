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

import { Component, DestroyRef, forwardRef, Input, OnInit, Optional } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { merge } from 'rxjs';
import { formatValue, isDefinedAndNotNull } from '@core/utils';
import { DataKeyConfigComponent } from '@home/components/widget/lib/settings/common/key/data-key-config.component';
import {
  ChartBarSettings,
  ChartLabelPosition,
  chartLabelPositions,
  chartLabelPositionTranslations,
  PieChartLabelPosition,
  pieChartLabelPositions,
  pieChartLabelPositionTranslations
} from '@home/components/widget/lib/chart/chart.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { getSourceTbUnitSymbol, isNotEmptyTbUnits } from '@shared/models/unit.models';

@Component({
  selector: 'tb-chart-bar-settings',
  templateUrl: './chart-bar-settings.component.html',
  styleUrls: ['./../../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ChartBarSettingsComponent),
      multi: true
    }
  ]
})
export class ChartBarSettingsComponent implements OnInit, ControlValueAccessor {

  chartLabelPositions: (ChartLabelPosition | PieChartLabelPosition)[];

  chartLabelPositionTranslations: Map<ChartLabelPosition | PieChartLabelPosition, string>;

  labelPreviewFn = this._labelPreviewFn.bind(this);

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  series = true;

  @Input()
  @coerceBoolean()
  pieLabelPosition = false;

  private modelValue: ChartBarSettings;

  private propagateChange = null;

  public barSettingsFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              @Optional() private dataKeyConfigComponent: DataKeyConfigComponent,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    if (this.pieLabelPosition) {
      this.chartLabelPositions = pieChartLabelPositions;
      this.chartLabelPositionTranslations = pieChartLabelPositionTranslations;
    } else {
      this.chartLabelPositions = chartLabelPositions;
      this.chartLabelPositionTranslations = chartLabelPositionTranslations;
    }
    this.barSettingsFormGroup = this.fb.group({
      showBorder: [null, []],
      borderWidth: [null, [Validators.min(0)]],
      borderRadius: [null, [Validators.min(0)]],
      showLabel: [null, []],
      labelPosition: [null, []],
      labelFont: [null, []],
      labelColor: [null, []],
      enableLabelBackground: [null, []],
      labelBackground: [null, []],
      backgroundSettings: [null, []]
    });
    if (!this.series) {
      this.barSettingsFormGroup.addControl('barWidth', this.fb.control(null,
        [Validators.min(0), Validators.max(100)]));
    }
    this.barSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    merge(this.barSettingsFormGroup.get('showBorder').valueChanges,
      this.barSettingsFormGroup.get('showLabel').valueChanges,
      this.barSettingsFormGroup.get('enableLabelBackground').valueChanges
    ).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.barSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.barSettingsFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: ChartBarSettings): void {
    this.modelValue = value;
    this.barSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators();
  }

  private updateValidators() {
    const showBorder: boolean = this.barSettingsFormGroup.get('showBorder').value;
    const showLabel: boolean = this.barSettingsFormGroup.get('showLabel').value;
    const enableLabelBackground: boolean = this.barSettingsFormGroup.get('enableLabelBackground').value;
    if (showBorder) {
      this.barSettingsFormGroup.get('borderWidth').enable({emitEvent: false});
    } else {
      this.barSettingsFormGroup.get('borderWidth').disable({emitEvent: false});
    }
    if (showLabel) {
      this.barSettingsFormGroup.get('labelPosition').enable({emitEvent: false});
      this.barSettingsFormGroup.get('labelFont').enable({emitEvent: false});
      this.barSettingsFormGroup.get('labelColor').enable({emitEvent: false});
      this.barSettingsFormGroup.get('enableLabelBackground').enable({emitEvent: false});
      if (enableLabelBackground) {
        this.barSettingsFormGroup.get('labelBackground').enable({emitEvent: false});
      } else {
        this.barSettingsFormGroup.get('labelBackground').disable({emitEvent: false});
      }
    } else {
      this.barSettingsFormGroup.get('labelPosition').disable({emitEvent: false});
      this.barSettingsFormGroup.get('labelFont').disable({emitEvent: false});
      this.barSettingsFormGroup.get('labelColor').disable({emitEvent: false});
      this.barSettingsFormGroup.get('enableLabelBackground').disable({emitEvent: false});
      this.barSettingsFormGroup.get('labelBackground').disable({emitEvent: false});
    }
  }

  private updateModel() {
    this.modelValue = this.barSettingsFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }

  private _labelPreviewFn(): string {
    if (this.series) {
      const dataKey = this.dataKeyConfigComponent.modelValue;
      const widgetConfig = this.dataKeyConfigComponent.widgetConfig;
      const units = isNotEmptyTbUnits(dataKey.units) ? dataKey.units : widgetConfig.config.units;
      const decimals = isDefinedAndNotNull(dataKey.decimals) ? dataKey.decimals :
        (isDefinedAndNotNull(widgetConfig.config.decimals) ? widgetConfig.config.decimals : 2);
      return formatValue(22, decimals, getSourceTbUnitSymbol(units), false);
    } else {
      return 'Wind';
    }
  }
}

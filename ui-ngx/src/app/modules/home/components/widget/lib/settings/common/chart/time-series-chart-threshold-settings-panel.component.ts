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

import { Component, DestroyRef, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import {
  TimeSeriesChartThreshold,
  TimeSeriesChartYAxisId,
  timeSeriesThresholdLabelPositions,
  timeSeriesThresholdLabelPositionTranslations
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { merge } from 'rxjs';
import { WidgetConfig } from '@shared/models/widget.models';
import { formatValue, isDefinedAndNotNull } from '@core/utils';
import { coerceBoolean } from '@shared/decorators/coercion';
import {
  chartLineTypes,
  chartLineTypeTranslations,
  ChartShape,
  chartShapes,
  chartShapeTranslations
} from '@home/components/widget/lib/chart/chart.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { getSourceTbUnitSymbol, isNotEmptyTbUnits, TbUnit } from '@shared/models/unit.models';

@Component({
  selector: 'tb-time-series-chart-threshold-settings-panel',
  templateUrl: './time-series-chart-threshold-settings-panel.component.html',
  providers: [],
  styleUrls: ['./time-series-chart-threshold-settings-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class TimeSeriesChartThresholdSettingsPanelComponent implements OnInit {

  chartLineTypes = chartLineTypes;

  chartLineTypeTranslations = chartLineTypeTranslations;

  chartShapes = chartShapes;

  chartShapeTranslations = chartShapeTranslations;

  timeSeriesThresholdLabelPositions = timeSeriesThresholdLabelPositions;

  timeSeriesThresholdLabelPositionTranslations = timeSeriesThresholdLabelPositionTranslations;

  labelPreviewFn = this._labelPreviewFn.bind(this);

  @Input()
  thresholdSettings: Partial<TimeSeriesChartThreshold>;

  @Input()
  widgetConfig: WidgetConfig;

  @Input()
  yAxisIds: TimeSeriesChartYAxisId[];

  @Input()
  popover: TbPopoverComponent<TimeSeriesChartThresholdSettingsPanelComponent>;

  @Input()
  @coerceBoolean()
  hideYAxis = false;

  @Input()
  panelTitle = 'widgets.time-series-chart.threshold.threshold-settings';

  @Output()
  thresholdSettingsApplied = new EventEmitter<Partial<TimeSeriesChartThreshold>>();

  thresholdSettingsFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.thresholdSettingsFormGroup = this.fb.group(
      {
        yAxisId: [this.thresholdSettings.yAxisId, [Validators.required]],
        units: [this.thresholdSettings.units, []],
        decimals: [this.thresholdSettings.decimals, [Validators.min(0)]],
        lineColor: [this.thresholdSettings.lineColor, []],
        lineType: [this.thresholdSettings.lineType, []],
        lineWidth: [this.thresholdSettings.lineWidth, [Validators.min(0)]],
        startSymbol: [this.thresholdSettings.startSymbol, []],
        startSymbolSize: [this.thresholdSettings.startSymbolSize, [Validators.min(0)]],
        endSymbol: [this.thresholdSettings.endSymbol, []],
        endSymbolSize: [this.thresholdSettings.endSymbolSize, [Validators.min(0)]],
        showLabel: [this.thresholdSettings.showLabel, []],
        labelPosition: [this.thresholdSettings.labelPosition, []],
        labelFont: [this.thresholdSettings.labelFont, []],
        labelColor: [this.thresholdSettings.labelColor, []],
        enableLabelBackground: [this.thresholdSettings.enableLabelBackground, []],
        labelBackground: [this.thresholdSettings.labelBackground, []]
      }
    );
    merge(this.thresholdSettingsFormGroup.get('showLabel').valueChanges,
          this.thresholdSettingsFormGroup.get('enableLabelBackground').valueChanges,
          this.thresholdSettingsFormGroup.get('startSymbol').valueChanges,
          this.thresholdSettingsFormGroup.get('endSymbol').valueChanges
    ).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
    });
    this.updateValidators();
  }

  cancel() {
    this.popover?.hide();
  }

  applyThresholdSettings() {
    const thresholdSettings = this.thresholdSettingsFormGroup.getRawValue();
    this.thresholdSettingsApplied.emit(thresholdSettings);
  }

  private updateValidators() {
    const showLabel: boolean = this.thresholdSettingsFormGroup.get('showLabel').value;
    const enableLabelBackground: boolean = this.thresholdSettingsFormGroup.get('enableLabelBackground').value;
    const startSymbol: ChartShape = this.thresholdSettingsFormGroup.get('startSymbol').value;
    const endSymbol: ChartShape = this.thresholdSettingsFormGroup.get('endSymbol').value;
    if (showLabel) {
      this.thresholdSettingsFormGroup.get('labelPosition').enable({emitEvent: false});
      this.thresholdSettingsFormGroup.get('labelFont').enable({emitEvent: false});
      this.thresholdSettingsFormGroup.get('labelColor').enable({emitEvent: false});
      this.thresholdSettingsFormGroup.get('enableLabelBackground').enable({emitEvent: false});
      if (enableLabelBackground) {
        this.thresholdSettingsFormGroup.get('labelBackground').enable({emitEvent: false});
      } else {
        this.thresholdSettingsFormGroup.get('labelBackground').disable({emitEvent: false});
      }
    } else {
      this.thresholdSettingsFormGroup.get('labelPosition').disable({emitEvent: false});
      this.thresholdSettingsFormGroup.get('labelFont').disable({emitEvent: false});
      this.thresholdSettingsFormGroup.get('labelColor').disable({emitEvent: false});
      this.thresholdSettingsFormGroup.get('enableLabelBackground').disable({emitEvent: false});
      this.thresholdSettingsFormGroup.get('labelBackground').disable({emitEvent: false});
    }
    if (startSymbol === ChartShape.none) {
      this.thresholdSettingsFormGroup.get('startSymbolSize').disable({emitEvent: false});
    } else {
      this.thresholdSettingsFormGroup.get('startSymbolSize').enable({emitEvent: false});
    }
    if (endSymbol === ChartShape.none) {
      this.thresholdSettingsFormGroup.get('endSymbolSize').disable({emitEvent: false});
    } else {
      this.thresholdSettingsFormGroup.get('endSymbolSize').enable({emitEvent: false});
    }
  }

  private _labelPreviewFn(): string {
    let units: TbUnit = this.thresholdSettingsFormGroup.get('units').value;
    units = isNotEmptyTbUnits(units) ? units : this.widgetConfig.units;
    let decimals: number = this.thresholdSettingsFormGroup.get('decimals').value;
    decimals = isDefinedAndNotNull(decimals) ? decimals :
      (isDefinedAndNotNull(this.widgetConfig.decimals) ? this.widgetConfig.decimals : 2);
    return formatValue(22, decimals, getSourceTbUnitSymbol(units), false);
  }
}

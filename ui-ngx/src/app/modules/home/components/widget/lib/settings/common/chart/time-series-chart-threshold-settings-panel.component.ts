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

import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import {
  TimeSeriesChartShape,
  timeSeriesChartShapes,
  timeSeriesChartShapeTranslations,
  TimeSeriesChartThreshold, TimeSeriesChartYAxisId,
  timeSeriesLineTypes,
  timeSeriesLineTypeTranslations,
  timeSeriesThresholdLabelPositions,
  timeSeriesThresholdLabelPositionTranslations
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { merge } from 'rxjs';
import { WidgetConfig } from '@shared/models/widget.models';
import { formatValue, isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-time-series-chart-threshold-settings-panel',
  templateUrl: './time-series-chart-threshold-settings-panel.component.html',
  providers: [],
  styleUrls: ['./time-series-chart-threshold-settings-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class TimeSeriesChartThresholdSettingsPanelComponent implements OnInit {

  timeSeriesLineTypes = timeSeriesLineTypes;

  timeSeriesLineTypeTranslations = timeSeriesLineTypeTranslations;

  timeSeriesChartShapes = timeSeriesChartShapes;

  timeSeriesChartShapeTranslations = timeSeriesChartShapeTranslations;

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

  @Output()
  thresholdSettingsApplied = new EventEmitter<Partial<TimeSeriesChartThreshold>>();

  thresholdSettingsFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
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
        labelColor: [this.thresholdSettings.labelColor, []]
      }
    );
    merge(this.thresholdSettingsFormGroup.get('showLabel').valueChanges,
          this.thresholdSettingsFormGroup.get('startSymbol').valueChanges,
          this.thresholdSettingsFormGroup.get('endSymbol').valueChanges).subscribe(() => {
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
    const startSymbol: TimeSeriesChartShape = this.thresholdSettingsFormGroup.get('startSymbol').value;
    const endSymbol: TimeSeriesChartShape = this.thresholdSettingsFormGroup.get('endSymbol').value;
    if (showLabel) {
      this.thresholdSettingsFormGroup.get('labelPosition').enable({emitEvent: false});
      this.thresholdSettingsFormGroup.get('labelFont').enable({emitEvent: false});
      this.thresholdSettingsFormGroup.get('labelColor').enable({emitEvent: false});
    } else {
      this.thresholdSettingsFormGroup.get('labelPosition').disable({emitEvent: false});
      this.thresholdSettingsFormGroup.get('labelFont').disable({emitEvent: false});
      this.thresholdSettingsFormGroup.get('labelColor').disable({emitEvent: false});
    }
    if (startSymbol === TimeSeriesChartShape.none) {
      this.thresholdSettingsFormGroup.get('startSymbolSize').disable({emitEvent: false});
    } else {
      this.thresholdSettingsFormGroup.get('startSymbolSize').enable({emitEvent: false});
    }
    if (endSymbol === TimeSeriesChartShape.none) {
      this.thresholdSettingsFormGroup.get('endSymbolSize').disable({emitEvent: false});
    } else {
      this.thresholdSettingsFormGroup.get('endSymbolSize').enable({emitEvent: false});
    }
  }

  private _labelPreviewFn(): string {
    let units: string = this.thresholdSettingsFormGroup.get('units').value;
    units = units && units.length ? units : this.widgetConfig.units;
    let decimals: number = this.thresholdSettingsFormGroup.get('decimals').value;
    decimals = isDefinedAndNotNull(decimals) ? decimals :
      (isDefinedAndNotNull(this.widgetConfig.decimals) ? this.widgetConfig.decimals : 2);
    return formatValue(22, decimals, units, false);
  }
}

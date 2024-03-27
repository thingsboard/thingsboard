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
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { TimeSeriesChartYAxisSettings } from '@home/components/widget/lib/chart/time-series-chart.models';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
  selector: 'tb-time-series-chart-y-axis-settings-panel',
  templateUrl: './time-series-chart-y-axis-settings-panel.component.html',
  providers: [],
  styleUrls: ['./time-series-chart-y-axis-settings-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class TimeSeriesChartYAxisSettingsPanelComponent implements OnInit {

  @Input()
  yAxisSettings: TimeSeriesChartYAxisSettings;

  @Input()
  @coerceBoolean()
  advanced = false;

  @Input()
  popover: TbPopoverComponent<TimeSeriesChartYAxisSettingsPanelComponent>;

  @Output()
  yAxisSettingsApplied = new EventEmitter<TimeSeriesChartYAxisSettings>();

  yAxisSettingsFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
  }

  ngOnInit(): void {
    this.yAxisSettingsFormGroup = this.fb.group(
      {
        yAxis: [this.yAxisSettings, []]
      }
    );
  }

  cancel() {
    this.popover?.hide();
  }

  applyYAxisSettings() {
    const yAxisSettings = this.yAxisSettingsFormGroup.get('yAxis').getRawValue();
    this.yAxisSettingsApplied.emit(yAxisSettings);
  }
}

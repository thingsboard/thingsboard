///
/// Copyright © 2016-2025 The Thingsboard Authors
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
import {
  TimeSeriesChartAxisSettings,
  TimeSeriesChartYAxisSettings
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { IAliasController } from '@core/api/widget-api.models';
import { DataKeysCallbacks } from '@home/components/widget/lib/settings/common/key/data-keys.component.models';
import { Datasource } from '@shared/models/widget.models';

@Component({
  selector: 'tb-time-series-chart-axis-settings-panel',
  templateUrl: './time-series-chart-axis-settings-panel.component.html',
  providers: [],
  styleUrls: ['./time-series-chart-axis-settings-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class TimeSeriesChartAxisSettingsPanelComponent implements OnInit {

  @Input()
  aliasController: IAliasController;

  @Input()
  dataKeyCallbacks: DataKeysCallbacks;

  @Input()
  datasource: Datasource;

  @Input()
  axisType: 'xAxis' | 'yAxis' = 'xAxis';

  @Input()
  panelTitle: string;

  @Input()
  axisSettings: TimeSeriesChartAxisSettings;

  @Input()
  @coerceBoolean()
  advanced = false;

  @Input()
  popover: TbPopoverComponent<TimeSeriesChartAxisSettingsPanelComponent>;

  @Output()
  axisSettingsApplied = new EventEmitter<TimeSeriesChartAxisSettings>();

  axisSettingsFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
  }

  ngOnInit(): void {
    this.axisSettingsFormGroup = this.fb.group(
      {
        axis: [this.axisSettings, []]
      }
    );
  }

  cancel() {
    this.popover?.hide();
  }

  applyAxisSettings() {
    const axisSettings = this.axisSettingsFormGroup.get('axis').getRawValue();
    this.axisSettingsApplied.emit(axisSettings);
  }
}

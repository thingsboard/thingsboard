// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    encapsulation: ViewEncapsulation.None,
    standalone: false
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

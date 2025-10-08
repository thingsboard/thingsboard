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

import { Component, TemplateRef, ViewChild } from '@angular/core';
import { WidgetSettings } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  doughnutDefaultSettings,
  DoughnutLayout,
  DoughnutWidgetSettings
} from '@home/components/widget/lib/chart/doughnut-widget.models';
import {
  LatestChartWidgetSettingsComponent
} from '@home/components/widget/lib/settings/chart/latest-chart-widget-settings.component';

@Component({
  selector: 'tb-doughnut-widget-settings',
  templateUrl: './latest-chart-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class DoughnutWidgetSettingsComponent extends LatestChartWidgetSettingsComponent<DoughnutWidgetSettings> {

  @ViewChild('doughnutChart')
  doughnutChartConfigTemplate: TemplateRef<any>;

  constructor(protected store: Store<AppState>,
              protected fb: UntypedFormBuilder) {
    super(store, fb);
  }

  protected defaultLatestChartSettings() {
    return doughnutDefaultSettings(this.doughnutHorizontal);
  }

  public latestChartConfigTemplate(): TemplateRef<any> {
    return this.doughnutChartConfigTemplate;
  }

  protected setupLatestChartControls(latestChartWidgetSettingsForm: UntypedFormGroup, settings: WidgetSettings) {
    latestChartWidgetSettingsForm.addControl('layout', this.fb.control(settings.layout, []));
    latestChartWidgetSettingsForm.addControl('autoScale', this.fb.control(settings.autoScale, []));
    latestChartWidgetSettingsForm.addControl('clockwise', this.fb.control(settings.clockwise, []));
    latestChartWidgetSettingsForm.addControl('totalValueFont', this.fb.control(settings.totalValueFont, []));
    latestChartWidgetSettingsForm.addControl('totalValueColor', this.fb.control(settings.totalValueColor, []));
  }

  protected latestChartValidatorTriggers(): string[] {
    return ['layout'];
  }

  protected updateLatestChartValidators(latestChartWidgetSettingsForm: UntypedFormGroup, emitEvent: boolean, trigger?: string) {
    const layout: DoughnutLayout = latestChartWidgetSettingsForm.get('layout').value;
    const totalEnabled = layout === DoughnutLayout.with_total;
    if (totalEnabled) {
      latestChartWidgetSettingsForm.get('totalValueFont').enable();
      latestChartWidgetSettingsForm.get('totalValueColor').enable();
      latestChartWidgetSettingsForm.get('legendShowTotal').disable();
    } else {
      latestChartWidgetSettingsForm.get('totalValueFont').disable();
      latestChartWidgetSettingsForm.get('totalValueColor').disable();
      latestChartWidgetSettingsForm.get('legendShowTotal').enable();
    }
  }
}

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

import { Component, TemplateRef, ViewChild } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { DataKey, widgetTitleAutocompleteValues } from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import {
  doughnutDefaultSettings,
  DoughnutLayout,
  DoughnutWidgetSettings
} from '@home/components/widget/lib/chart/doughnut-widget.models';
import {
  LatestChartBasicConfigComponent
} from '@home/components/widget/config/basic/chart/latest-chart-basic-config.component';

@Component({
    selector: 'tb-doughnut-basic-config',
    templateUrl: './latest-chart-basic-config.component.html',
    styleUrls: ['../basic-config.scss'],
    standalone: false
})
export class DoughnutBasicConfigComponent extends LatestChartBasicConfigComponent<DoughnutWidgetSettings> {

  @ViewChild('doughnutChart')
  doughnutChartConfigTemplate: TemplateRef<any>;

  predefinedValues = widgetTitleAutocompleteValues;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              protected fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent, fb);
  }

  protected defaultDataKeys(configData: WidgetConfigComponentData): DataKey[] {
    return [{ name: 'windPower', label: 'Wind power', type: DataKeyType.timeseries, units: '', decimals: 0, color: '#08872B' },
            { name: 'solarPower', label: 'Solar power', type: DataKeyType.timeseries, units: '', decimals: 0, color: '#FF4D5A' }];
  }

  protected defaultSettings() {
    return doughnutDefaultSettings(this.doughnutHorizontal);
  }

  public latestChartConfigTemplate(): TemplateRef<any> {
    return this.doughnutChartConfigTemplate;
  }

  protected setupLatestChartControls(latestChartWidgetConfigForm: UntypedFormGroup, settings: DoughnutWidgetSettings) {
    latestChartWidgetConfigForm.addControl('layout', this.fb.control(settings.layout, []));
    latestChartWidgetConfigForm.addControl('autoScale', this.fb.control(settings.autoScale, []));
    latestChartWidgetConfigForm.addControl('clockwise', this.fb.control(settings.clockwise, []));
    latestChartWidgetConfigForm.addControl('totalValueFont', this.fb.control(settings.totalValueFont, []));
    latestChartWidgetConfigForm.addControl('totalValueColor', this.fb.control(settings.totalValueColor, []));
  }

  protected prepareOutputLatestChartConfig(config: any) {
    this.widgetConfig.config.settings.layout = config.layout;
    this.widgetConfig.config.settings.autoScale = config.autoScale;
    this.widgetConfig.config.settings.clockwise = config.clockwise;
    this.widgetConfig.config.settings.totalValueFont = config.totalValueFont;
    this.widgetConfig.config.settings.totalValueColor = config.totalValueColor;
  }

  protected latestChartValidatorTriggers(): string[] {
    return ['layout'];
  }

  protected updateLatestChartValidators(latestChartWidgetConfigForm: UntypedFormGroup, emitEvent: boolean, trigger?: string) {
    const layout: DoughnutLayout = latestChartWidgetConfigForm.get('layout').value;
    const totalEnabled = layout === DoughnutLayout.with_total;
    if (totalEnabled) {
      latestChartWidgetConfigForm.get('totalValueFont').enable();
      latestChartWidgetConfigForm.get('totalValueColor').enable();
      latestChartWidgetConfigForm.get('legendShowTotal').disable();
    } else {
      latestChartWidgetConfigForm.get('totalValueFont').disable();
      latestChartWidgetConfigForm.get('totalValueColor').disable();
      latestChartWidgetConfigForm.get('legendShowTotal').enable();
    }
  }
}

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
  polarAreaChartWidgetDefaultSettings,
  PolarAreaChartWidgetSettings
} from '@home/components/widget/lib/chart/polar-area-widget.models';
import {
  LatestChartBasicConfigComponent
} from '@home/components/widget/config/basic/chart/latest-chart-basic-config.component';

@Component({
  selector: 'tb-polar-area-chart-basic-config',
  templateUrl: './latest-chart-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class PolarAreaChartBasicConfigComponent extends LatestChartBasicConfigComponent<PolarAreaChartWidgetSettings> {

  @ViewChild('polarAreaChart')
  polarAreaChartConfigTemplate: TemplateRef<any>;

  predefinedValues = widgetTitleAutocompleteValues;
  
  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              protected fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent, fb);
  }

  protected defaultDataKeys(configData: WidgetConfigComponentData): DataKey[] {
    return [{ name: 'windPower', label: 'Wind', type: DataKeyType.timeseries, units: '', decimals: 0, color: '#08872B' },
      { name: 'solarPower', label: 'Solar', type: DataKeyType.timeseries, units: '', decimals: 0, color: '#FF4D5A' },
      { name: 'hydroelectricPower', label: 'Hydroelectric', type: DataKeyType.timeseries, units: '', decimals: 0, color: '#FFDE30' }];
  }

  protected defaultSettings() {
    return polarAreaChartWidgetDefaultSettings;
  }

  public latestChartConfigTemplate(): TemplateRef<any> {
    return this.polarAreaChartConfigTemplate;
  }

  protected setupLatestChartControls(latestChartWidgetConfigForm: UntypedFormGroup, settings: PolarAreaChartWidgetSettings) {
    latestChartWidgetConfigForm.addControl('barSettings', this.fb.control(settings.barSettings, []));

    latestChartWidgetConfigForm.addControl('axisMin', this.fb.control(settings.axisMin, []));
    latestChartWidgetConfigForm.addControl('axisMax', this.fb.control(settings.axisMax, []));
    latestChartWidgetConfigForm.addControl('axisTickLabelFont', this.fb.control(settings.axisTickLabelFont, []));
    latestChartWidgetConfigForm.addControl('axisTickLabelColor', this.fb.control(settings.axisTickLabelColor, []));
    latestChartWidgetConfigForm.addControl('angleAxisStartAngle', this.fb.control(settings.angleAxisStartAngle, []));
  }

  protected prepareOutputLatestChartConfig(config: any) {
    this.widgetConfig.config.settings.barSettings = config.barSettings;

    this.widgetConfig.config.settings.axisMin = config.axisMin;
    this.widgetConfig.config.settings.axisMax = config.axisMax;
    this.widgetConfig.config.settings.axisTickLabelFont = config.axisTickLabelFont;
    this.widgetConfig.config.settings.axisTickLabelColor = config.axisTickLabelColor;
    this.widgetConfig.config.settings.angleAxisStartAngle = config.angleAxisStartAngle;
  }
}

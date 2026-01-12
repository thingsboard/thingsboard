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
import { DataKey } from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import {
  pieChartWidgetDefaultSettings,
  PieChartWidgetSettings
} from '@home/components/widget/lib/chart/pie-chart-widget.models';
import {
  LatestChartBasicConfigComponent
} from '@home/components/widget/config/basic/chart/latest-chart-basic-config.component';

@Component({
  selector: 'tb-pie-chart-basic-config',
  templateUrl: './latest-chart-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class PieChartBasicConfigComponent extends LatestChartBasicConfigComponent<PieChartWidgetSettings> {

  @ViewChild('pieChart')
  pieChartConfigTemplate: TemplateRef<any>;

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
    return pieChartWidgetDefaultSettings;
  }

  public latestChartConfigTemplate(): TemplateRef<any> {
    return this.pieChartConfigTemplate;
  }

  protected setupLatestChartControls(latestChartWidgetConfigForm: UntypedFormGroup, settings: PieChartWidgetSettings) {
    latestChartWidgetConfigForm.addControl('showLabel', this.fb.control(settings.showLabel, []));
    latestChartWidgetConfigForm.addControl('labelPosition', this.fb.control(settings.labelPosition, []));
    latestChartWidgetConfigForm.addControl('labelFont', this.fb.control(settings.labelFont, []));
    latestChartWidgetConfigForm.addControl('labelColor', this.fb.control(settings.labelColor, []));

    latestChartWidgetConfigForm.addControl('borderWidth', this.fb.control(settings.borderWidth, []));
    latestChartWidgetConfigForm.addControl('borderColor', this.fb.control(settings.borderColor, []));

    latestChartWidgetConfigForm.addControl('radius', this.fb.control(settings.radius, []));
    latestChartWidgetConfigForm.addControl('clockwise', this.fb.control(settings.clockwise, []));
  }

  protected prepareOutputLatestChartConfig(config: any) {
    this.widgetConfig.config.settings.showLabel = config.showLabel;
    this.widgetConfig.config.settings.labelPosition = config.labelPosition;
    this.widgetConfig.config.settings.labelFont = config.labelFont;
    this.widgetConfig.config.settings.labelColor = config.labelColor;

    this.widgetConfig.config.settings.borderWidth = config.borderWidth;
    this.widgetConfig.config.settings.borderColor = config.borderColor;

    this.widgetConfig.config.settings.radius = config.radius;
    this.widgetConfig.config.settings.clockwise = config.clockwise;
  }

  protected latestChartValidatorTriggers(): string[] {
    return ['showLabel'];
  }

  protected updateLatestChartValidators(latestChartWidgetConfigForm: UntypedFormGroup, emitEvent: boolean, trigger?: string) {
    const showLabel: boolean = latestChartWidgetConfigForm.get('showLabel').value;

    if (showLabel) {
      latestChartWidgetConfigForm.get('labelPosition').enable();
      latestChartWidgetConfigForm.get('labelFont').enable();
      latestChartWidgetConfigForm.get('labelColor').enable();
    } else {
      latestChartWidgetConfigForm.get('labelPosition').disable();
      latestChartWidgetConfigForm.get('labelFont').disable();
      latestChartWidgetConfigForm.get('labelColor').disable();
    }
  }
}

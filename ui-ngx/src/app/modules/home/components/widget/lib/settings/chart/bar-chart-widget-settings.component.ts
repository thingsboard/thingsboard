// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, TemplateRef, ViewChild } from '@angular/core';
import { WidgetSettings } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  barChartWidgetDefaultSettings,
  BarChartWidgetSettings
} from '@home/components/widget/lib/chart/bar-chart-widget.models';
import {
  LatestChartWidgetSettingsComponent
} from '@home/components/widget/lib/settings/chart/latest-chart-widget-settings.component';

@Component({
    selector: 'tb-bar-chart-widget-settings',
    templateUrl: './latest-chart-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class BarChartWidgetSettingsComponent extends LatestChartWidgetSettingsComponent<BarChartWidgetSettings> {

  @ViewChild('barChart')
  barChartConfigTemplate: TemplateRef<any>;

  constructor(protected store: Store<AppState>,
              protected fb: UntypedFormBuilder) {
    super(store, fb);
  }

  protected defaultLatestChartSettings() {
    return barChartWidgetDefaultSettings;
  }

  public latestChartConfigTemplate(): TemplateRef<any> {
    return this.barChartConfigTemplate;
  }

  protected setupLatestChartControls(latestChartWidgetSettingsForm: UntypedFormGroup, settings: WidgetSettings) {
    latestChartWidgetSettingsForm.addControl('barSettings', this.fb.control(settings.barSettings, []));
    latestChartWidgetSettingsForm.addControl('axisMin', this.fb.control(settings.axisMin, []));
    latestChartWidgetSettingsForm.addControl('axisMax', this.fb.control(settings.axisMax, []));
    latestChartWidgetSettingsForm.addControl('axisTickLabelFont', this.fb.control(settings.axisTickLabelFont, []));
    latestChartWidgetSettingsForm.addControl('axisTickLabelColor', this.fb.control(settings.axisTickLabelColor, []));
  }
}

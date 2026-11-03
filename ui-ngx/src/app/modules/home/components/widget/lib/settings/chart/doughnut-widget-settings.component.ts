// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
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

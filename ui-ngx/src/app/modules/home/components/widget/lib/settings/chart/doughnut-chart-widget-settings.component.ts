// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
    selector: 'tb-doughnut-chart-widget-settings',
    templateUrl: './doughnut-chart-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class DoughnutChartWidgetSettingsComponent extends WidgetSettingsComponent {

  doughnutChartWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.doughnutChartWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      showTooltip: true,
      borderWidth: 5,
      borderColor: '#fff',
      legend: {
        display: true,
        labelsFontColor: '#666'
      }
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.doughnutChartWidgetSettingsForm = this.fb.group({

      // Common settings

      showTooltip: [settings.showTooltip, []],

      // Border settings

      borderWidth: [settings.borderWidth, [Validators.min(0)]],
      borderColor: [settings.borderColor, []],

      // Legend settings

      legend: this.fb.group({
        display: [settings.legend?.display, []],
        labelsFontColor: [settings.legend?.labelsFontColor, []]
      })
    });
  }

  protected validatorTriggers(): string[] {
    return ['legend.display'];
  }

  protected updateValidators(emitEvent: boolean) {
    const displayLegend: boolean = this.doughnutChartWidgetSettingsForm.get('legend.display').value;
    if (displayLegend) {
      this.doughnutChartWidgetSettingsForm.get('legend.labelsFontColor').enable();
    } else {
      this.doughnutChartWidgetSettingsForm.get('legend.labelsFontColor').disable();
    }
    this.doughnutChartWidgetSettingsForm.get('legend.labelsFontColor').updateValueAndValidity({emitEvent});
  }
}

// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
    selector: 'tb-chart-widget-settings',
    templateUrl: './chart-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class ChartWidgetSettingsComponent extends WidgetSettingsComponent {

  chartWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.chartWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      showTooltip: true
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.chartWidgetSettingsForm = this.fb.group({
      showTooltip: [settings.showTooltip, []]
    });
  }
}

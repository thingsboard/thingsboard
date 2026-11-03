// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { flotDefaultSettings } from '@home/components/widget/lib/settings/chart/flot-widget-settings.component';
import { deepClone } from '@core/utils';

@Component({
    selector: 'tb-flot-line-widget-settings',
    templateUrl: './flot-line-widget-settings.component.html',
    styleUrls: [],
    standalone: false
})
export class FlotLineWidgetSettingsComponent extends WidgetSettingsComponent {

  flotLineWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.flotLineWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return flotDefaultSettings('graph');
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.flotLineWidgetSettingsForm = this.fb.group({
      flotSettings: [settings.flotSettings, []]
    });
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    return {
      flotSettings: settings
    };
  }

  protected prepareOutputSettings(settings: any): WidgetSettings {
    return settings.flotSettings;
  }
}

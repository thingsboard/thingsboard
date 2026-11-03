// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { flotDataKeyDefaultSettings } from '@home/components/widget/lib/settings/chart/flot-key-settings.component';

@Component({
    selector: 'tb-flot-line-key-settings',
    templateUrl: './flot-line-key-settings.component.html',
    styleUrls: [],
    standalone: false
})
export class FlotLineKeySettingsComponent extends WidgetSettingsComponent {

  flotLineKeySettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.flotLineKeySettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return flotDataKeyDefaultSettings('graph');
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.flotLineKeySettingsForm = this.fb.group({
      flotKeySettings: [settings.flotKeySettings, []]
    });
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    return {
      flotKeySettings: settings
    };
  }

  protected prepareOutputSettings(settings: any): WidgetSettings {
    return settings.flotKeySettings;
  }
}

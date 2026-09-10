// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { countDefaultSettings } from '@home/components/widget/lib/count/count-widget.models';

@Component({
    selector: 'tb-alarm-count-widget-settings',
    templateUrl: './alarm-count-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class AlarmCountWidgetSettingsComponent extends WidgetSettingsComponent {

  alarmCountWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.alarmCountWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return countDefaultSettings(true);
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.alarmCountWidgetSettingsForm = this.fb.group({
      alarmCountSettings: [settings.alarmCountSettings, []],
    });
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    return {
      alarmCountSettings: settings
    };
  }

  protected prepareOutputSettings(settings: any): WidgetSettings {
    return settings.alarmCountSettings;
  }

}

// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { TargetDevice, WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { switchRpcDefaultSettings } from '@home/components/widget/lib/settings/control/switch-rpc-settings.component';
import { deepClone } from '@core/utils';

@Component({
    selector: 'tb-switch-control-widget-settings',
    templateUrl: './switch-control-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class SwitchControlWidgetSettingsComponent extends WidgetSettingsComponent {

  switchControlWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  get targetDevice(): TargetDevice {
    return this.widgetConfig?.config?.targetDevice;
  }

  protected settingsForm(): UntypedFormGroup {
    return this.switchControlWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      title: '',
      showOnOffLabels: true,
      ...switchRpcDefaultSettings()
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.switchControlWidgetSettingsForm = this.fb.group({
      title: [settings.title, []],
      showOnOffLabels: [settings.showOnOffLabels, []],
      switchRpcSettings: [settings.switchRpcSettings, []]
    });
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    const switchRpcSettings = deepClone(settings, ['title', 'showOnOffLabels']);
    return {
      title: settings.title,
      showOnOffLabels: settings.showOnOffLabels,
      switchRpcSettings
    };
  }

  protected prepareOutputSettings(settings: any): WidgetSettings {
    return {
      ...settings.switchRpcSettings,
      title: settings.title,
      showOnOffLabels: settings.showOnOffLabels
    };
  }
}

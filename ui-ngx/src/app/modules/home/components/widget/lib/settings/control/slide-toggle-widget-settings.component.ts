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
    selector: 'tb-slide-toggle-widget-settings',
    templateUrl: './slide-toggle-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class SlideToggleWidgetSettingsComponent extends WidgetSettingsComponent {

  slideToggleWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  get targetDevice(): TargetDevice {
    return this.widgetConfig?.config?.targetDevice;
  }

  protected settingsForm(): UntypedFormGroup {
    return this.slideToggleWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      title: '',
      labelPosition: 'after',
      sliderColor: 'accent',
      ...switchRpcDefaultSettings()
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.slideToggleWidgetSettingsForm = this.fb.group({
      title: [settings.title, []],
      labelPosition: [settings.labelPosition, []],
      sliderColor: [settings.sliderColor, []],
      switchRpcSettings: [settings.switchRpcSettings, []]
    });
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    const switchRpcSettings = deepClone(settings, ['title', 'labelPosition', 'sliderColor']);
    return {
      title: settings.title,
      labelPosition: settings.labelPosition,
      sliderColor: settings.sliderColor,
      switchRpcSettings
    };
  }

  protected prepareOutputSettings(settings: any): WidgetSettings {
    return {
      ...settings.switchRpcSettings,
      title: settings.title,
      labelPosition: settings.labelPosition,
      sliderColor: settings.sliderColor
    };
  }
}

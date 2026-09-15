// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { TargetDevice, WidgetSettings, WidgetSettingsComponent, widgetType } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { scadaSymbolWidgetDefaultSettings } from '@home/components/widget/lib/scada/scada-symbol-widget.models';

@Component({
    selector: 'tb-scada-symbol-widget-settings',
    templateUrl: './scada-symbol-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class ScadaSymbolWidgetSettingsComponent extends WidgetSettingsComponent {

  get targetDevice(): TargetDevice {
    return this.widgetConfig?.config?.targetDevice;
  }

  get widgetType(): widgetType {
    return this.widgetConfig?.widgetType;
  }

  scadaSymbolWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.scadaSymbolWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return scadaSymbolWidgetDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.scadaSymbolWidgetSettingsForm = this.fb.group({
      scadaSymbolUrl: [settings.scadaSymbolUrl, []],
      scadaSymbolObjectSettings: [settings.scadaSymbolObjectSettings, []],
      background: [settings.background, []],
      padding: [settings.padding, []]
    });
  }
}

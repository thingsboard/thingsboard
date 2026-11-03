// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { countDefaultSettings } from '@home/components/widget/lib/count/count-widget.models';

@Component({
    selector: 'tb-entity-count-widget-settings',
    templateUrl: './entity-count-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class EntityCountWidgetSettingsComponent extends WidgetSettingsComponent {

  entityCountWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.entityCountWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return countDefaultSettings(false);
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.entityCountWidgetSettingsForm = this.fb.group({
      entityCountSettings: [settings.entityCountSettings, []],
    });
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    return {
      entityCountSettings: settings
    };
  }

  protected prepareOutputSettings(settings: any): WidgetSettings {
    return settings.entityCountSettings;
  }

}

// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { defaultMapSettings } from 'src/app/modules/home/components/widget/lib/maps-legacy/map-models';

@Component({
    selector: 'tb-route-map-widget-settings',
    templateUrl: './route-map-widget-settings.component.html',
    styleUrls: ['./../../widget-settings.scss'],
    standalone: false
})
export class RouteMapWidgetSettingsComponent extends WidgetSettingsComponent {

  routeMapWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.routeMapWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return defaultMapSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.routeMapWidgetSettingsForm = this.fb.group({
      mapSettings: [settings.mapSettings, []]
    });
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    return {
      mapSettings: settings
    };
  }

  protected prepareOutputSettings(settings: any): WidgetSettings {
    return settings.mapSettings;
  }
}

///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { isDefinedAndNotNull } from '@core/utils';
import { mapWidgetDefaultSettings } from '@home/components/widget/lib/maps/map-widget.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';

@Component({
    selector: 'tb-map-widget-settings',
    templateUrl: './map-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class MapWidgetSettingsComponent extends WidgetSettingsComponent {

  mapWidgetSettingsForm: UntypedFormGroup;

  trip = false;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.mapWidgetSettingsForm;
  }

  protected onWidgetConfigSet(widgetConfig: WidgetConfigComponentData) {
    const params = widgetConfig.typeParameters as any;
    if (isDefinedAndNotNull(params.trip)) {
      this.trip = params.trip === true;
    } else {
      this.trip = false;
    }
  }

  protected defaultSettings(): WidgetSettings {
    return mapWidgetDefaultSettings;
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    return {
      mapSettings: settings,
      background: settings.background,
      padding: settings.padding
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.mapWidgetSettingsForm = this.fb.group({
      mapSettings: [settings.mapSettings, []],
      background: [settings.background, []],
      padding: [settings.padding, []]
    });
  }

  protected prepareOutputSettings(settings: any): WidgetSettings {
    return {...settings.mapSettings, background: settings.background, padding: settings.padding};
  }
}

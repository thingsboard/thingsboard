///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { Component } from "@angular/core";
import { UntypedFormBuilder, UntypedFormGroup } from "@angular/forms";
import { AppState } from "@app/core/core.state";
import { Store } from "@ngrx/store";
import {
  WidgetSettings,
  WidgetSettingsComponent,
} from "@shared/models/widget.models";
import { displacementDefaultSettings } from "./displacement-chart-key-settings.component";

@Component({
  selector: "tb-displacement-chart-widget-settings",
  templateUrl: "./displacement-chart-widget-settings.component.html",
})
export class DisplacementChartWidgetSettingsComponent extends WidgetSettingsComponent {
  
  displacementChartWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.displacementChartWidgetSettingsForm;
  }

  protected defaultSettings() {
    return displacementDefaultSettings(
      this.widgetConfig.config.datasources[0]?.dataKeys.map(
        (dataKey) => dataKey.name
      ) || []
    );
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.displacementChartWidgetSettingsForm = this.fb.group({
      displacementSettings: [settings.displacementSettings, []]
    });
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    return {
      displacementSettings: settings
    };
  }

  protected prepareOutputSettings(settings: any): WidgetSettings {
    return settings.displacementSettings;
  }
}

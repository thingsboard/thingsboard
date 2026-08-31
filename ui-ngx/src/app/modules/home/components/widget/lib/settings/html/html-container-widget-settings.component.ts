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

import { Component, HostBinding } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { htmlContainerDefaultSettings } from '@home/components/widget/lib/html/html-container-widget.models';

@Component({
    selector: 'tb-html-container-widget-settings',
    templateUrl: './html-container-widget-settings.component.html',
    styleUrls: [],
    standalone: false
})
export class HtmlContainerWidgetSettingsComponent extends WidgetSettingsComponent {

  @HostBinding('height')
  hostHeight = '100%';

  htmlContainerWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.htmlContainerWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return htmlContainerDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.htmlContainerWidgetSettingsForm = this.fb.group({
      htmlContainerSettings: [settings.htmlContainerSettings, []]
    });
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    return {
      htmlContainerSettings: settings
    };
  }

  protected prepareOutputSettings(settings: any): WidgetSettings {
    return settings.htmlContainerSettings;
  }
}

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
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import {
  htmlContainerDefaultSettings,
  HtmlContainerWidgetSettings
} from '@home/components/widget/lib/html/html-container-widget.models';

@Component({
  selector: 'tb-html-container-basic-config',
  templateUrl: './html-container-basic-config.component.html',
  styleUrls: ['../basic-config.scss'],
  standalone: false
})
export class HtmlContainerBasicConfigComponent extends BasicWidgetConfigComponent {

  @HostBinding('style.height') height = '100%';

  htmlContainerWidgetConfigForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.htmlContainerWidgetConfigForm;
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: HtmlContainerWidgetSettings = {...htmlContainerDefaultSettings, ...(configData.config.settings || {})};
    this.htmlContainerWidgetConfigForm = this.fb.group({
      settings: [settings, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    this.widgetConfig.config.settings = {...(this.widgetConfig.config.settings || {}), ...config.settings};
    return this.widgetConfig;
  }
}

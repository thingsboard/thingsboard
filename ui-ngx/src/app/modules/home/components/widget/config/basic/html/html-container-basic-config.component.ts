// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component, HostBinding } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { htmlContainerDefaultSettings, HtmlContainerWidgetSettings } from '@shared/models/html-container.models';

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
      settings: [settings, []],
      actions: [configData.config.actions || {}, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    this.widgetConfig.config.settings = {...(this.widgetConfig.config.settings || {}), ...config.settings};
    this.widgetConfig.config.actions = config.actions;
    return this.widgetConfig;
  }
}

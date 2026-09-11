// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

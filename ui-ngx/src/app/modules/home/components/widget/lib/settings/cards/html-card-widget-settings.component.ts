// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
    selector: 'tb-html-card-widget-settings',
    templateUrl: './html-card-widget-settings.component.html',
    styleUrls: [],
    standalone: false
})
export class HtmlCardWidgetSettingsComponent extends WidgetSettingsComponent {

  htmlCardWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.htmlCardWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      cardHtml: '<div class=\'card\'>HTML code here</div>',
      cardCss: '.card {\n font-weight: bold; \n}'
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.htmlCardWidgetSettingsForm = this.fb.group({
      cardHtml: [settings.cardHtml, [Validators.required]],
      cardCss: [settings.cardCss, []]
    });
  }
}

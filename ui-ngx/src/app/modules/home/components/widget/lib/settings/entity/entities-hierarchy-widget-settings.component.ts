// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
    selector: 'tb-entities-hierarchy-widget-settings',
    templateUrl: './entities-hierarchy-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class EntitiesHierarchyWidgetSettingsComponent extends WidgetSettingsComponent {

  entitiesHierarchyWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.entitiesHierarchyWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      nodeRelationQueryFunction: '',
      nodeHasChildrenFunction: '',
      nodeOpenedFunction: '',
      nodeDisabledFunction: '',
      nodeIconFunction: '',
      nodeTextFunction: '',
      nodesSortFunction: '',
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.entitiesHierarchyWidgetSettingsForm = this.fb.group({
      nodeRelationQueryFunction: [settings.nodeRelationQueryFunction, []],
      nodeHasChildrenFunction: [settings.nodeHasChildrenFunction, []],
      nodeOpenedFunction: [settings.nodeOpenedFunction, []],
      nodeDisabledFunction: [settings.nodeDisabledFunction, []],
      nodeIconFunction: [settings.nodeIconFunction, []],
      nodeTextFunction: [settings.nodeTextFunction, []],
      nodesSortFunction: [settings.nodesSortFunction, []]
    });
  }
}

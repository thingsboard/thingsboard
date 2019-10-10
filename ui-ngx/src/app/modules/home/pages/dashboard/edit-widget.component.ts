///
/// Copyright © 2016-2019 The Thingsboard Authors
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

import { Component, OnInit, Input, OnChanges, SimpleChanges } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActivatedRoute, Router } from '@angular/router';
import { WidgetService } from '@core/http/widget.service';
import { DialogService } from '@core/services/dialog.service';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { Dashboard, WidgetLayout } from '@shared/models/dashboard.models';
import { IAliasController } from '@core/api/widget-api.models';
import { Widget, WidgetActionSource, WidgetTypeParameters } from '@shared/models/widget.models';
import { WidgetComponentService } from '@home/components/widget/widget-component.service';
import { WidgetConfigComponentData } from '../../models/widget-component.models';
import { isDefined, isString } from '@core/utils';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'tb-edit-widget',
  templateUrl: './edit-widget.component.html',
  styleUrls: []
})
export class EditWidgetComponent extends PageComponent implements OnInit, OnChanges {

  @Input()
  dashboard: Dashboard;

  @Input()
  aliasController: IAliasController;

  @Input()
  widgetEditMode: boolean;

  @Input()
  widget: Widget;

  @Input()
  widgetLayout: WidgetLayout;

  @Input()
  widgetFormGroup: FormGroup;

  widgetConfig: WidgetConfigComponentData;
  typeParameters: WidgetTypeParameters;
  actionSources: {[key: string]: WidgetActionSource};
  isDataEnabled: boolean;
  settingsSchema: any;
  dataKeySettingsSchema: any;
  functionsOnly: boolean;

  constructor(protected store: Store<AppState>,
              private widgetComponentService: WidgetComponentService) {
    super(store);
  }

  ngOnInit(): void {
    this.loadWidgetConfig();
  }

  ngOnChanges(changes: SimpleChanges): void {
    let reloadConfig = false;
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['widget', 'widgetLayout'].includes(propName)) {
          reloadConfig = true;
        }
      }
    }
    if (reloadConfig) {
      this.loadWidgetConfig();
    }
  }

  private loadWidgetConfig() {
    const widgetInfo = this.widgetComponentService.getInstantWidgetInfo(this.widget);
    this.widgetConfig = {
      config: this.widget.config,
      layout: this.widgetLayout,
      widgetType: this.widget.type
    };
    const settingsSchema = widgetInfo.typeSettingsSchema || widgetInfo.settingsSchema;
    const dataKeySettingsSchema = widgetInfo.typeDataKeySettingsSchema || widgetInfo.dataKeySettingsSchema;
    this.typeParameters = widgetInfo.typeParameters;
    this.actionSources = widgetInfo.actionSources;
    this.isDataEnabled = isDefined(widgetInfo.typeParameters) ? !widgetInfo.typeParameters.useCustomDatasources : true;
    if (!settingsSchema || settingsSchema === '') {
      this.settingsSchema = {};
    } else {
      this.settingsSchema = isString(settingsSchema) ? JSON.parse(settingsSchema) : settingsSchema;
    }
    if (!dataKeySettingsSchema || dataKeySettingsSchema === '') {
      this.dataKeySettingsSchema = {};
    } else {
      this.dataKeySettingsSchema = isString(dataKeySettingsSchema) ? JSON.parse(dataKeySettingsSchema) : dataKeySettingsSchema;
    }
    this.functionsOnly = this.dashboard ? false : true;
    this.widgetFormGroup.reset({widgetConfig: this.widgetConfig});
  }
}

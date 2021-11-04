///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { Widget, widgetTypesData } from '@shared/models/widget.models';
import { Dashboard } from '@app/shared/models/dashboard.models';
import { IAliasController } from '@core/api/widget-api.models';
import { WidgetConfigComponentData, WidgetInfo } from '@home/models/widget-component.models';
import { isDefined, isString } from '@core/utils';

export interface AddWidgetDialogData {
  dashboard: Dashboard;
  aliasController: IAliasController;
  widget: Widget;
  widgetInfo: WidgetInfo;
}

@Component({
  selector: 'tb-add-widget-dialog',
  templateUrl: './add-widget-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: AddWidgetDialogComponent}],
  styleUrls: []
})
export class AddWidgetDialogComponent extends DialogComponent<AddWidgetDialogComponent, Widget>
  implements OnInit, ErrorStateMatcher {

  widgetFormGroup: FormGroup;

  dashboard: Dashboard;
  aliasController: IAliasController;
  widget: Widget;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddWidgetDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<AddWidgetDialogComponent, Widget>,
              private fb: FormBuilder) {
    super(store, router, dialogRef);

    this.dashboard = this.data.dashboard;
    this.aliasController = this.data.aliasController;
    this.widget = this.data.widget;

    const widgetInfo = this.data.widgetInfo;

    const rawSettingsSchema = widgetInfo.typeSettingsSchema || widgetInfo.settingsSchema;
    const rawDataKeySettingsSchema = widgetInfo.typeDataKeySettingsSchema || widgetInfo.dataKeySettingsSchema;
    const typeParameters = widgetInfo.typeParameters;
    const actionSources = widgetInfo.actionSources;
    const isDataEnabled = isDefined(widgetInfo.typeParameters) ? !widgetInfo.typeParameters.useCustomDatasources : true;
    let settingsSchema;
    if (!rawSettingsSchema || rawSettingsSchema === '') {
      settingsSchema = {};
    } else {
      settingsSchema = isString(rawSettingsSchema) ? JSON.parse(rawSettingsSchema) : rawSettingsSchema;
    }
    let dataKeySettingsSchema;
    if (!rawDataKeySettingsSchema || rawDataKeySettingsSchema === '') {
      dataKeySettingsSchema = {};
    } else {
      dataKeySettingsSchema = isString(rawDataKeySettingsSchema) ? JSON.parse(rawDataKeySettingsSchema) : rawDataKeySettingsSchema;
    }
    const widgetConfig: WidgetConfigComponentData = {
      config: this.widget.config,
      layout: {},
      widgetType: this.widget.type,
      typeParameters,
      actionSources,
      isDataEnabled,
      settingsSchema,
      dataKeySettingsSchema
    };

    this.widgetFormGroup = this.fb.group({
        widgetConfig: [widgetConfig, []]
      }
    );
  }

  ngOnInit(): void {
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  helpLinkIdForWidgetType(): string {
    let link = 'widgetsConfig';
    if (this.widget && this.widget.type) {
      link = widgetTypesData.get(this.widget.type).configHelpLinkId;
    }
    return link;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    this.submitted = true;
    const widgetConfig: WidgetConfigComponentData = this.widgetFormGroup.get('widgetConfig').value;
    this.widget.config = widgetConfig.config;
    this.widget.config.mobileOrder = widgetConfig.layout.mobileOrder;
    this.widget.config.mobileHeight = widgetConfig.layout.mobileHeight;
    this.widget.config.mobileHide = widgetConfig.layout.mobileHide;
    this.dialogRef.close(this.widget);
  }
}

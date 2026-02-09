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

import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { MatDialog } from '@angular/material/dialog';
import { Dashboard, WidgetLayout } from '@shared/models/dashboard.models';
import { IAliasController, IStateController } from '@core/api/widget-api.models';
import { Widget, WidgetConfigMode } from '@shared/models/widget.models';
import { WidgetComponentService } from '@home/components/widget/widget-component.service';
import { WidgetConfigComponentData } from '../../models/widget-component.models';
import { isDefined, isDefinedAndNotNull } from '@core/utils';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKeySettingsFunction } from '@home/components/widget/lib/settings/common/key/data-keys.component.models';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
    selector: 'tb-edit-widget',
    templateUrl: './edit-widget.component.html',
    styleUrls: ['./edit-widget.component.scss'],
    standalone: false
})
export class EditWidgetComponent extends PageComponent implements OnInit, OnChanges {

  @ViewChild('widgetConfigComponent')
  widgetConfigComponent: WidgetConfigComponent;

  @Input()
  dashboard: Dashboard;

  @Input()
  aliasController: IAliasController;

  @Input()
  stateController: IStateController;

  @Input()
  widgetEditMode: boolean;

  @Input()
  widget: Widget;

  @Input()
  widgetLayout: WidgetLayout;

  @Input()
  @coerceBoolean()
  showLayoutConfig = true;

  @Input()
  @coerceBoolean()
  isDefaultBreakpoint= true;

  @Output()
  applyWidgetConfig = new EventEmitter<void>();

  @Output()
  revertWidgetConfig = new EventEmitter<void>();

  widgetFormGroup: UntypedFormGroup;

  widgetConfig: WidgetConfigComponentData;

  previewMode = false;

  get widgetConfigMode(): WidgetConfigMode {
    return this.widgetConfigComponent?.widgetConfigMode;
  }

  set widgetConfigMode(widgetConfigMode: WidgetConfigMode) {
    this.widgetConfigComponent.setWidgetConfigMode(widgetConfigMode);
  }

  private currentWidgetConfigChanged = false;

  constructor(protected store: Store<AppState>,
              private dialog: MatDialog,
              private fb: UntypedFormBuilder,
              private widgetComponentService: WidgetComponentService) {
    super(store);
    this.widgetFormGroup = this.fb.group({
      widgetConfig: [null]
    });
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
      if (this.currentWidgetConfigChanged) {
        this.currentWidgetConfigChanged = false;
      } else {
        this.previewMode = false;
      }
      this.loadWidgetConfig();
    }
  }

  onApplyWidgetConfig() {
    if (this.widgetFormGroup.valid) {
      this.currentWidgetConfigChanged = true;
      this.applyWidgetConfig.emit();
    }
  }

  onRevertWidgetConfig() {
    this.currentWidgetConfigChanged = true;
    this.revertWidgetConfig.emit();
  }

  private loadWidgetConfig() {
    if (!this.widget) {
      return;
    }
    const widgetInfo = this.widgetComponentService.getInstantWidgetInfo(this.widget);

    const settingsForm = widgetInfo.typeSettingsForm?.length ?
      widgetInfo.typeSettingsForm : (widgetInfo.settingsForm || []);
    const dataKeySettingsForm = widgetInfo.typeDataKeySettingsForm?.length ?
      widgetInfo.typeDataKeySettingsForm : (widgetInfo.dataKeySettingsForm || []);
    const latestDataKeySettingsForm = widgetInfo.typeLatestDataKeySettingsForm?.length ?
      widgetInfo.typeLatestDataKeySettingsForm : (widgetInfo.latestDataKeySettingsForm || []);
    const typeParameters = widgetInfo.typeParameters;
    const dataKeySettingsFunction: DataKeySettingsFunction = typeParameters?.dataKeySettingsFunction;
    const actionSources = widgetInfo.actionSources;
    const isDataEnabled = isDefined(widgetInfo.typeParameters) ? !widgetInfo.typeParameters.useCustomDatasources : true;

    this.widgetConfig = {
      widgetName: widgetInfo.widgetName,
      config: this.widget.config,
      layout: this.widgetLayout,
      widgetType: this.widget.type,
      typeParameters,
      actionSources,
      isDataEnabled,
      settingsForm,
      dataKeySettingsForm,
      latestDataKeySettingsForm,
      dataKeySettingsFunction,
      settingsDirective: widgetInfo.settingsDirective,
      dataKeySettingsDirective: widgetInfo.dataKeySettingsDirective,
      latestDataKeySettingsDirective: widgetInfo.latestDataKeySettingsDirective,
      hasBasicMode: isDefinedAndNotNull(widgetInfo.hasBasicMode) ? widgetInfo.hasBasicMode : false,
      basicModeDirective: widgetInfo.basicModeDirective
    };
    this.widgetFormGroup.reset({widgetConfig: this.widgetConfig});
  }
}

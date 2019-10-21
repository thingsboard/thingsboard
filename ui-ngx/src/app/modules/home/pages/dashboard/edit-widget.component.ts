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

import { Component, OnInit, Input, OnChanges, SimpleChanges, ViewChild } from '@angular/core';
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
import { deepClone, isDefined, isString } from '@core/utils';
import { FormBuilder, FormGroup, NgForm, Validators } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import { Observable, of } from 'rxjs';
import { EntityAlias, EntityAliases } from '@shared/models/alias.models';
import { WidgetConfigCallbacks } from '@home/components/widget/widget-config.component.models';
import {
  EntityAliasesDialogComponent,
  EntityAliasesDialogData
} from '@home/components/alias/entity-aliases-dialog.component';
import {
  EntityAliasDialogComponent,
  EntityAliasDialogData
} from '@home/components/alias/entity-alias-dialog.component';
import { tap } from 'rxjs/operators';

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

  @ViewChild('widgetForm', {static: true}) widgetForm: NgForm;

  widgetFormGroup: FormGroup;

  widgetConfig: WidgetConfigComponentData;

  constructor(protected store: Store<AppState>,
              private dialog: MatDialog,
              private fb: FormBuilder,
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
      this.loadWidgetConfig();
    }
  }

  private loadWidgetConfig() {
    const widgetInfo = this.widgetComponentService.getInstantWidgetInfo(this.widget);
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
    this.widgetConfig = {
      config: this.widget.config,
      layout: this.widgetLayout,
      widgetType: this.widget.type,
      typeParameters,
      actionSources,
      isDataEnabled,
      settingsSchema,
      dataKeySettingsSchema
    };
    this.widgetFormGroup.reset({widgetConfig: this.widgetConfig});
  }

  private createEntityAlias(alias: string, allowedEntityTypes: Array<EntityType>): Observable<EntityAlias> {
    const singleEntityAlias: EntityAlias = {id: null, alias, filter: {resolveMultiple: false}};
    return this.dialog.open<EntityAliasDialogComponent, EntityAliasDialogData,
      EntityAlias>(EntityAliasDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd: true,
        allowedEntityTypes,
        entityAliases: this.dashboard.configuration.entityAliases,
        alias: singleEntityAlias
      }
    }).afterClosed().pipe(
      tap((entityAlias) => {
        if (entityAlias) {
          this.dashboard.configuration.entityAliases[entityAlias.id] = entityAlias;
          this.aliasController.updateEntityAliases(this.dashboard.configuration.entityAliases);
        }
      })
    );
  }
}

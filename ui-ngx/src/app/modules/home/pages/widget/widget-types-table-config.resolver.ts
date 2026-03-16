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

import { Injectable } from '@angular/core';

import { Router } from '@angular/router';
import {
  checkBoxCell,
  DateEntityTableColumn,
  EntityChipsEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { WidgetService } from '@app/core/http/widget.service';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@app/core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { Direction } from '@shared/models/page/sort-order';
import {
  BaseWidgetType,
  widgetType as WidgetDataType,
  WidgetTypeDetails,
  WidgetTypeInfo,
  widgetTypesData
} from '@shared/models/widget.models';
import { WidgetTypeComponent } from '@home/pages/widget/widget-type.component';
import { WidgetTypeTabsComponent } from '@home/pages/widget/widget-type-tabs.component';
import { SelectWidgetTypeDialogComponent } from '@home/pages/widget/select-widget-type-dialog.component';
import { MatDialog } from '@angular/material/dialog';

@Injectable()
export class WidgetTypesTableConfigResolver  {

  private readonly config: EntityTableConfig<WidgetTypeInfo | WidgetTypeDetails> =
    new EntityTableConfig<WidgetTypeInfo | WidgetTypeDetails>();

  constructor(private store: Store<AppState>,
              private dialog: MatDialog,
              private widgetsService: WidgetService,
              private translate: TranslateService,
              private importExport: ImportExportService,
              private datePipe: DatePipe,
              private router: Router) {

    this.config.entityType = EntityType.WIDGETS_BUNDLE;
    this.config.entityComponent = WidgetTypeComponent;
    this.config.entityTabsComponent = WidgetTypeTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.WIDGET_TYPE);
    this.config.entityResources = entityTypeResources.get(EntityType.WIDGET_TYPE);
    this.config.defaultSortOrder = {property: 'name', direction: Direction.ASC};

    this.config.rowPointer = true;

    this.config.entityTitle = (widgetType) => widgetType ?
      widgetType.name : '';

    this.config.columns.push(
      new DateEntityTableColumn<WidgetTypeInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<WidgetTypeInfo>('name', 'widget.title', '60%'),
      new EntityChipsEntityTableColumn<WidgetTypeInfo>( 'bundles', 'entity.type-widgets-bundles', '40%',
          () => '/resources/widgets-library/widgets-bundles'),
      new EntityTableColumn<WidgetTypeInfo>('widgetType', 'widget.type', '150px', entity =>
        entity?.widgetType ? this.translate.instant(widgetTypesData.get(entity.widgetType).name) : '', undefined, false),
      new EntityTableColumn<WidgetTypeInfo>('tenantId', 'widget.system', '60px',
        entity => checkBoxCell(entity.tenantId.id === NULL_UUID)),
      new EntityTableColumn<WidgetTypeInfo>('deprecated', 'widget.deprecated', '60px',
        entity => checkBoxCell(entity.deprecated))
    );

    this.config.addActionDescriptors.push(
      {
        name: this.translate.instant('dashboard.create-new-widget'),
        icon: 'insert_drive_file',
        isEnabled: () => true,
        onAction: ($event) => this.addWidgetType($event)
      },
      {
        name: this.translate.instant('widget.import'),
        icon: 'file_upload',
        isEnabled: () => true,
        onAction: ($event) => this.importWidgetType($event)
      }
    );

    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('widget.export'),
        icon: 'file_download',
        isEnabled: () => true,
        onAction: ($event, entity) => this.exportWidgetType($event, entity)
      },
      {
        name: this.translate.instant('widget.widget-details'),
        icon: 'edit',
        isEnabled: () => true,
        onAction: ($event, entity) => this.config.toggleEntityDetails($event, entity)
      }
    );

    this.config.groupActionDescriptors.push(
      {
        name: this.translate.instant('widget.export-widgets'),
        icon: 'file_download',
        isEnabled: true,
        onAction: ($event, entities) => this.exportWidgetTypes($event, entities)
      }
    );

    this.config.deleteEntityTitle = widgetType => this.translate.instant('widget.delete-widget-title',
      { widgetName: widgetType.name });
    this.config.deleteEntityContent = () => this.translate.instant('widget.delete-widget-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('widget.delete-widgets-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('widget.delete-widgets-text');


    this.config.loadEntity = id => this.widgetsService.getWidgetTypeById(id.id);
    this.config.saveEntity = widgetType => this.widgetsService.saveWidgetType(widgetType as WidgetTypeDetails);
    this.config.deleteEntity = id => this.widgetsService.deleteWidgetType(id.id);
    this.config.onEntityAction = action => this.onWidgetTypeAction(action, this.config);

    this.config.handleRowClick = ($event, widgetType) => {
      if (this.config.isDetailsOpen()) {
        this.config.toggleEntityDetails($event, widgetType);
      } else {
        this.openWidgetEditor($event, widgetType);
      }
      return true;
    };
  }

  resolve(): EntityTableConfig<WidgetTypeInfo | WidgetTypeDetails> {
    this.config.tableTitle = this.translate.instant('widget.widgets');
    const authUser = getCurrentAuthUser(this.store);
    this.config.deleteEnabled = (widgetType) => this.isWidgetTypeEditable(widgetType, authUser.authority);
    this.config.entitySelectionEnabled = (widgetType) => this.isWidgetTypeEditable(widgetType, authUser.authority);
    this.config.detailsReadonly = (widgetType) => !this.isWidgetTypeEditable(widgetType, authUser.authority);
    this.config.entitiesFetchFunction = pageLink => this.widgetsService.getWidgetTypes(pageLink);
    return this.config;
  }

  isWidgetTypeEditable(widgetType: BaseWidgetType, authority: Authority): boolean {
    if (authority === Authority.TENANT_ADMIN) {
      return widgetType && widgetType.tenantId && widgetType.tenantId.id !== NULL_UUID;
    } else {
      return authority === Authority.SYS_ADMIN;
    }
  }

  addWidgetType($event: Event): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<SelectWidgetTypeDialogComponent, any,
      WidgetDataType>(SelectWidgetTypeDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog']
    }).afterClosed().subscribe(
      (type) => {
        if (type) {
          this.router.navigateByUrl(`resources/widgets-library/widget-types/${type}`);
        }
      }
    );
  }

  importWidgetType($event: Event) {
    this.importExport.importWidgetType().subscribe(
      (widgetType) => {
        if (widgetType) {
          this.config.updateData();
        }
      }
    );
  }

  openWidgetEditor($event: Event, widgetType: BaseWidgetType) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`resources/widgets-library/widget-types/${widgetType.id.id}`);
  }

  private openWidgetTypeDetails($event: Event, widgetType: BaseWidgetType, config: EntityTableConfig<BaseWidgetType>) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree(['details', widgetType.id.id], {relativeTo: config.getActivatedRoute()});
    this.router.navigateByUrl(url);
  }

  exportWidgetType($event: Event, widgetType: BaseWidgetType) {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExport.exportWidgetType(widgetType.id.id);
  }

  exportWidgetTypes($event: Event, widgetTypes: BaseWidgetType[]) {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExport.exportWidgetTypes(widgetTypes.map(w => w.id.id)).subscribe(
        () => {
          this.config.getTable().clearSelection();
        }
    );
  }

  onWidgetTypeAction(action: EntityAction<BaseWidgetType>, config: EntityTableConfig<BaseWidgetType>): boolean {
    switch (action.action) {
      case 'edit':
        this.openWidgetEditor(action.event, action.entity);
        return true;
      case 'openDetails':
        this.openWidgetTypeDetails(action.event, action.entity, config);
        return true;
      case 'export':
        this.exportWidgetType(action.event, action.entity);
        return true;
    }
    return false;
  }

}

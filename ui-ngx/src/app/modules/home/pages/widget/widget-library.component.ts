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

import { Component, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { AuthUser } from '@shared/models/user.model';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { ActivatedRoute } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { Observable } from 'rxjs';
import { Widget, widgetType } from '@app/shared/models/widget.models';
import { WidgetService } from '@core/http/widget.service';
import { map, share } from 'rxjs/operators';
import { DialogService } from '@core/services/dialog.service';
import { FooterFabButtons } from '@app/shared/components/footer-fab-buttons.component';
import { DashboardCallbacks, WidgetsData } from '@home/models/dashboard-component.models';
import { IAliasController } from '@app/core/api/widget-api.models';
import { toWidgetInfo } from '@home/models/widget-component.models';

@Component({
  selector: 'tb-widget-library',
  templateUrl: './widget-library.component.html',
  styleUrls: ['./widget-library.component.scss']
})
export class WidgetLibraryComponent extends PageComponent implements OnInit {

  authUser: AuthUser;

  isReadOnly: boolean;

  widgetsBundle: WidgetsBundle;

  widgetTypes$: Observable<Array<Widget>>;

  footerFabButtons: FooterFabButtons = {
    fabTogglerName: 'widget.add-widget-type',
    fabTogglerIcon: 'add',
    buttons: [
      {
        name: 'widget-type.create-new-widget-type',
        icon: 'insert_drive_file',
        onAction: ($event) => {
          this.addWidgetType($event);
        }
      },
      {
        name: 'widget-type.import',
        icon: 'file_upload',
        onAction: ($event) => {
          this.importWidgetType($event);
        }
      }
    ]
  };

  dashboardCallbacks: DashboardCallbacks = {
    onEditWidget: this.openWidgetType.bind(this),
    onExportWidget: this.exportWidgetType.bind(this),
    onRemoveWidget: this.removeWidgetType.bind(this)
  };

  widgetsData: Observable<WidgetsData>;

  aliasController: IAliasController = {};

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private widgetService: WidgetService,
              private dialogService: DialogService) {
    super(store);

    this.authUser = getCurrentAuthUser(store);
    this.widgetsBundle = this.route.snapshot.data.widgetsBundle;
    if (this.authUser.authority === Authority.TENANT_ADMIN) {
      this.isReadOnly = !this.widgetsBundle || this.widgetsBundle.tenantId.id === NULL_UUID;
    } else {
      this.isReadOnly = this.authUser.authority !== Authority.SYS_ADMIN;
    }
    this.loadWidgetTypes();
    this.widgetsData = this.widgetTypes$.pipe(
      map(widgets => ({ widgets })));
  }

  loadWidgetTypes() {
    const bundleAlias = this.widgetsBundle.alias;
    const isSystem = this.widgetsBundle.tenantId.id === NULL_UUID;
    this.widgetTypes$ = this.widgetService.getBundleWidgetTypes(bundleAlias,
      isSystem).pipe(
      map((types) => {
          types = types.sort((a, b) => {
            let result = widgetType[b.descriptor.type].localeCompare(widgetType[a.descriptor.type]);
            if (result === 0) {
              result = b.createdTime - a.createdTime;
            }
            return result;
          });
          const widgetTypes = new Array<Widget>(types.length);
          let top = 0;
          const lastTop = [0, 0, 0];
          let col = 0;
          let column = 0;
          types.forEach((type) => {
            const widgetTypeInfo = toWidgetInfo(type);
            const sizeX = 8;
            const sizeY = Math.floor(widgetTypeInfo.sizeY);
            const widget: Widget = {
              typeId: type.id,
              isSystemType: isSystem,
              bundleAlias,
              typeAlias: widgetTypeInfo.alias,
              type: widgetTypeInfo.type,
              title: widgetTypeInfo.widgetName,
              sizeX,
              sizeY,
              row: top,
              col,
              config: JSON.parse(widgetTypeInfo.defaultConfig)
            };

            widget.config.title = widgetTypeInfo.widgetName;

            widgetTypes.push(widget);
            top += sizeY;
            if (top > lastTop[column] + 10) {
              lastTop[column] = top;
              column++;
              if (column > 2) {
                column = 0;
              }
              top = lastTop[column];
              col = column * 8;
            }
          });
          return widgetTypes;
        }
      ),
      share());
  }

  ngOnInit(): void {
  }

  addWidgetType($event: Event): void {
    this.openWidgetType($event);
  }

  importWidgetType($event: Event): void {
    if (event) {
      event.stopPropagation();
    }
    this.dialogService.todo();
  }

  openWidgetType($event: Event, widget?: Widget): void {
    if (event) {
      event.stopPropagation();
    }
    if (widget) {
      this.dialogService.todo();
    } else {
      this.dialogService.todo();
    }
  }

  exportWidgetType($event: Event, widget: Widget): void {
    if (event) {
      event.stopPropagation();
    }
    this.dialogService.todo();
  }

  removeWidgetType($event: Event, widget: Widget): void {
    if (event) {
      event.stopPropagation();
    }
    this.dialogService.todo();
  }

}

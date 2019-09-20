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
import { ActivatedRoute, Router } from '@angular/router';
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
import { DummyAliasController } from '@core/api/alias-controller';
import {
  DeviceCredentialsDialogComponent,
  DeviceCredentialsDialogData
} from '@home/pages/device/device-credentials-dialog.component';
import { DeviceCredentials } from '@shared/models/device.models';
import { MatDialog } from '@angular/material/dialog';
import { SelectWidgetTypeDialogComponent } from '@home/pages/widget/select-widget-type-dialog.component';

@Component({
  selector: 'tb-widget-library',
  templateUrl: './widget-library.component.html',
  styleUrls: ['./widget-library.component.scss']
})
export class WidgetLibraryComponent extends PageComponent implements OnInit {

  authUser: AuthUser;

  isReadOnly: boolean;

  widgetsBundle: WidgetsBundle;

  widgetsData: WidgetsData;

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

  aliasController: IAliasController = new DummyAliasController();

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private router: Router,
              private widgetService: WidgetService,
              private dialogService: DialogService,
              private dialog: MatDialog) {
    super(store);

    this.authUser = getCurrentAuthUser(store);
    this.widgetsBundle = this.route.snapshot.data.widgetsBundle;
    this.widgetsData = this.route.snapshot.data.widgetsData;
    if (this.authUser.authority === Authority.TENANT_ADMIN) {
      this.isReadOnly = !this.widgetsBundle || this.widgetsBundle.tenantId.id === NULL_UUID;
    } else {
      this.isReadOnly = this.authUser.authority !== Authority.SYS_ADMIN;
    }
  }

  ngOnInit(): void {
  }

  addWidgetType($event: Event): void {
    this.openWidgetType($event);
  }

  importWidgetType($event: Event): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.todo();
  }

  openWidgetType($event: Event, widget?: Widget): void {
    if ($event) {
      $event.stopPropagation();
    }
    if (widget) {
      this.router.navigate([widget.typeId.id], {relativeTo: this.route});
    } else {
      this.dialog.open<SelectWidgetTypeDialogComponent, any,
        widgetType>(SelectWidgetTypeDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog']
      }).afterClosed().subscribe(
        (type) => {
          if (type) {
            this.router.navigate(['add', type], {relativeTo: this.route});
          }
        }
      );
    }
  }

  exportWidgetType($event: Event, widget: Widget): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.todo();
  }

  removeWidgetType($event: Event, widget: Widget): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.todo();
  }

}

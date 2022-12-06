///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import {
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import {EntityType, EntityTypeResource} from '@shared/models/entity-type.models';
import {TranslateService} from '@ngx-translate/core';
import {DatePipe} from '@angular/common';
import {Direction} from '@shared/models/page/sort-order';
import {MatDialog} from '@angular/material/dialog';
import {TimePageLink} from '@shared/models/page/page-link';
import {Observable} from 'rxjs';
import {emptyPageData, PageData} from '@shared/models/page/page-data';
import {UtilsService} from '@core/services/utils.service';
import {DeviceService} from "@core/http/device.service";
import {AttributeService} from "@core/http/attribute.service";
import {Device} from "@shared/models/device.models";
import {
  GatewayCommandDialogComponent,
  GatewayCommandDialogData
} from "@home/components/gateway/gateway-command-dialog.component";
import {ActionNotificationShow} from "@core/notification/notification.actions";
import {Store} from "@ngrx/store";
import {AppState} from "@core/core.state";
import {map} from "rxjs/operators";
import {EntityService} from "@core/http/entity.service";

export class GatewayListTableConfig extends EntityTableConfig<Device, TimePageLink> {

  constructor(protected store: Store<AppState>,
              private deviceService: DeviceService,
              private attributeService: AttributeService,
              private entityService: EntityService,
              private datePipe: DatePipe,
              private translate: TranslateService,
              private utils: UtilsService,
              private dialog: MatDialog,
              updateOnInit = true) {
    super();
    this.loadDataOnInit = updateOnInit;
    this.tableTitle = 'Gateway list';
    this.useTimePageLink = false;
    this.pageMode = false;
    this.displayPagination = false;
    this.detailsPanelEnabled = false;
    this.selectionEnabled = false;
    this.searchEnabled = false;
    this.addEnabled = false;
    this.entitiesDeleteEnabled = false;
    this.actionsColumnTitle = '';
    this.entityTranslations = {
      noEntities: 'gateway.no-gateway-found',
      search: 'gateway.gateway-search'
    };
    this.entityResources = {} as EntityTypeResource<Device>;

    this.entitiesFetchFunction = () => this.fetchGateways();

    this.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.columns.push(
      new DateEntityTableColumn<Device>('createdTime', 'gateway.created-time', this.datePipe, '150px'));

    this.columns.push(
      new EntityTableColumn<Device>('entityName', 'gateway.gateway-name', '20%',
        (entity => this.utils.customTranslation(entity.name, entity.name))
      )
    );

    this.cellActionDescriptors.push(
      {
        name: this.translate.instant('gateway.command'),
        icon: 'vpn_key',
        isEnabled: () => true,
        onAction: ($event, entity) => this.showGatewayDockerCommand(entity)
      }
    );
  }

  showGatewayDockerCommand(entity: Device) {
    this.deviceService.getDeviceCredentials(entity.id.id).subscribe(credentials => {
      this.dialog.open<GatewayCommandDialogComponent, GatewayCommandDialogData, boolean>
      (GatewayCommandDialogComponent,
        {
          disableClose: true,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
          data: {
            credentials: credentials,
            device: entity
          }
        }).afterClosed().subscribe(
        (res) => {
          if (res) {
            this.updateData();
          }
        }
      );
    }, error => {
      const messageToShow = `<div>${error}</div>`;
      this.store.dispatch(new ActionNotificationShow({message: messageToShow, type: 'error'}));
    })
  }

  fetchGateways(): Observable<PageData<Device>> {
    return this.entityService.getEntitiesByNameFilter(EntityType.DEVICE, "", -1).pipe(
      map((array: Array<Device>) => {
        const pageData = emptyPageData<Device>();
        pageData.data = array.filter(device => device.additionalInfo?.gateway);
        pageData.totalPages = 1;
        pageData.totalElements = array.length;
        return pageData;
      }))
  }
}

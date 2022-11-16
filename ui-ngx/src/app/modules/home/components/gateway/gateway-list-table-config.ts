///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
///
/// NOTICE: All information contained herein is, and remains
/// the property of ThingsBoard, Inc. and its suppliers,
/// if any.  The intellectual and technical concepts contained
/// herein are proprietary to ThingsBoard, Inc.
/// and its suppliers and may be covered by U.S. and Foreign Patents,
/// patents in process, and are protected by trade secret or copyright law.
///
/// Dissemination of this information or reproduction of this material is strictly forbidden
/// unless prior written permission is obtained from COMPANY.
///
/// Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
/// managers or contractors who have executed Confidentiality and Non-disclosure agreements
/// explicitly covering such access.
///
/// The copyright notice above does not evidence any actual or intended publication
/// or disclosure  of  this source code, which includes
/// information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
/// ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
/// OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
/// THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
/// AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
/// THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
/// DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
/// OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
///

import {
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import {EntityTypeResource} from '@shared/models/entity-type.models';
import {TranslateService} from '@ngx-translate/core';
import {DatePipe} from '@angular/common';
import {Direction} from '@shared/models/page/sort-order';
import {MatDialog} from '@angular/material/dialog';
import {TimePageLink} from '@shared/models/page/page-link';
import {Observable} from 'rxjs';
import {PageData} from '@shared/models/page/page-data';
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
import {getCurrentAuthUser} from "@core/auth/auth.selectors";
import {Authority} from "@shared/models/authority.enum";

export class GatewayListTableConfig extends EntityTableConfig<Device, TimePageLink> {
  private authUser = getCurrentAuthUser(this.store);

  constructor(protected store: Store<AppState>,
              private deviceService: DeviceService,
              private attributeService: AttributeService,
              private datePipe: DatePipe,
              private translate: TranslateService,
              private utils: UtilsService,
              private dialog: MatDialog,
              updateOnInit = true,
              pageMode = false) {
    super();
    this.loadDataOnInit = updateOnInit;
    this.tableTitle = '';
    this.useTimePageLink = false;
    this.pageMode = false;
    this.displayPagination = false;
    this.detailsPanelEnabled = false;
    this.selectionEnabled = false;
    this.searchEnabled = true;
    this.addEnabled = false;
    this.entitiesDeleteEnabled = false;
    this.actionsColumnTitle = '';
    this.entityTranslations = {
      noEntities: 'gateway.no-gateway-found',
      search: 'gateway.gateway-search'
    };
    this.entityResources = {} as EntityTypeResource<Device>;

    this.entitiesFetchFunction = pageLink => this.fetchGateways(pageLink);

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

  fetchGateways(pageLink: TimePageLink): Observable<PageData<Device>> {
    let request = this.deviceService.getTenantDevices(pageLink);
    if (this.authUser.authority === Authority.CUSTOMER_USER) {
      request = this.deviceService.getCustomerDeviceInfos(this.authUser.customerId, pageLink);
    }
    return request.pipe(
      map(pageData => {
        pageData.data = pageData.data.filter(device => device.additionalInfo?.gateway);
        pageData.totalElements = pageData.data.length;
        return pageData;
      })
    );
  }
}

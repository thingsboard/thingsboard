// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';

import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { UtilsService } from '@core/services/utils.service';
import { EntityGroupParams, entityGroupsTitle, resolveGroupParams } from '@shared/models/entity-group.models';
import { EntityGroupService } from '@core/http/entity-group.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { CustomerService } from '@core/http/customer.service';
import { EntityGroupsTableConfig } from './entity-groups-table-config';
import { MatDialog } from '@angular/material/dialog';
import { EdgeService } from '@core/http/edge.service';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityType } from '@shared/models/entity-type.models';
import { Operation, Resource } from '@shared/models/security.models';
import { deviceAiAssistantConfig } from '@shared/models/device.models';
import { AiAssistantViewType } from '@shared/models/ai-chat.models';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';

@Injectable()
export class EntityGroupsTableConfigResolver  {

  constructor(private entityGroupService: EntityGroupService,
              private customerService: CustomerService,
              private edgeService: EdgeService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private utils: UtilsService,
              private router: Router,
              private dialog: MatDialog,
              private homeDialogs: HomeDialogsService,
              private customTranslate: CustomTranslatePipe,
              private store: Store<AppState>) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityGroupsTableConfig> | EntityGroupsTableConfig {
    return this.resolveEntityGroupTableConfig(resolveGroupParams(route));
  }

  resolveEntityGroupTableConfig(params: EntityGroupParams, resolveCustomer = true, customerTitle?: string):
    Observable<EntityGroupsTableConfig> | EntityGroupsTableConfig {

    const config = new EntityGroupsTableConfig(
      this.entityGroupService,
      this.userPermissionsService,
      this.translate,
      this.datePipe,
      this.utils,
      this.router,
      this.dialog,
      this.homeDialogs,
      this.customTranslate,
      getCurrentAuthUser(this.store),
      params
    );

    if (config.groupType === EntityType.DEVICE) {
      config.aiAssistantConfig = deviceAiAssistantConfig(this.store, this.userPermissionsService, this.translate,
        this.userPermissionsService.hasGenericPermission(Resource.DEVICE, Operation.WRITE),
        AiAssistantViewType.DEVICE_GROUP, AiAssistantViewType.DEVICE_GROUP_LIST);
    }

    if (config.customerId && resolveCustomer) {
      if (config.edgeId) {
        return this.resolveEdgeInfo(config);
      } else {
        return this.customerService.getShortCustomerInfo(config.customerId).pipe(
          map((info) => {
            config.tableTitle = info.title + ': ' + this.translate.instant(entityGroupsTitle(config.groupType, params.shared));
            return config;
          })
        );
      }
    } else if (config.customerId && customerTitle){
      config.tableTitle = customerTitle + ': ' + this.translate.instant(entityGroupsTitle(config.groupType, params.shared));
      return config;
    } else if (config.edgeId && resolveCustomer) {
      return this.resolveEdgeInfo(config);
    } else {
      return config;
    }
  }

  private resolveEdgeInfo(config: EntityGroupsTableConfig): Observable<EntityGroupsTableConfig> {
    return this.edgeService.getEdge(config.edgeId).pipe(
      map((info) => {
        config.tableTitle = info.name + ': ' + this.translate.instant(entityGroupsTitle(config.groupType));
        return config;
      })
    );
  }

}

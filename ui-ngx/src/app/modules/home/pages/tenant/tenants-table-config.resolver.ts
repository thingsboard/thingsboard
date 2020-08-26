///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { Resolve, Router } from '@angular/router';

import { TenantInfo } from '@shared/models/tenant.model';
import {
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { TenantService } from '@core/http/tenant.service';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { TenantComponent } from '@modules/home/pages/tenant/tenant.component';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { TenantTabsComponent } from '@home/pages/tenant/tenant-tabs.component';
import { mergeMap } from 'rxjs/operators';

@Injectable()
export class TenantsTableConfigResolver implements Resolve<EntityTableConfig<TenantInfo>> {

  private readonly config: EntityTableConfig<TenantInfo> = new EntityTableConfig<TenantInfo>();

  constructor(private tenantService: TenantService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router) {

    this.config.entityType = EntityType.TENANT;
    this.config.entityComponent = TenantComponent;
    this.config.entityTabsComponent = TenantTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.TENANT);
    this.config.entityResources = entityTypeResources.get(EntityType.TENANT);

    this.config.columns.push(
      new DateEntityTableColumn<TenantInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<TenantInfo>('title', 'tenant-profile.tenant-profile', '20%'),
      new EntityTableColumn<TenantInfo>('tenantProfileName', 'tenant.title', '20%'),
      new EntityTableColumn<TenantInfo>('email', 'contact.email', '20%'),
      new EntityTableColumn<TenantInfo>('country', 'contact.country', '20%'),
      new EntityTableColumn<TenantInfo>('city', 'contact.city', '20%')
    );

    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('tenant.manage-tenant-admins'),
        icon: 'account_circle',
        isEnabled: () => true,
        onAction: ($event, entity) => this.manageTenantAdmins($event, entity)
      }
    );

    this.config.deleteEntityTitle = tenant => this.translate.instant('tenant.delete-tenant-title', { tenantTitle: tenant.title });
    this.config.deleteEntityContent = () => this.translate.instant('tenant.delete-tenant-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('tenant.delete-tenants-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('tenant.delete-tenants-text');

    this.config.entitiesFetchFunction = pageLink => this.tenantService.getTenantInfos(pageLink);
    this.config.loadEntity = id => this.tenantService.getTenantInfo(id.id);
    this.config.saveEntity = tenant => this.tenantService.saveTenant(tenant).pipe(
      mergeMap((savedTenant) => this.tenantService.getTenantInfo(savedTenant.id.id))
    );
    this.config.deleteEntity = id => this.tenantService.deleteTenant(id.id);
    this.config.onEntityAction = action => this.onTenantAction(action);
  }

  resolve(): EntityTableConfig<TenantInfo> {
    this.config.tableTitle = this.translate.instant('tenant.tenants');

    return this.config;
  }

  manageTenantAdmins($event: Event, tenant: TenantInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`tenants/${tenant.id.id}/users`);
  }

  onTenantAction(action: EntityAction<TenantInfo>): boolean {
    switch (action.action) {
      case 'manageTenantAdmins':
        this.manageTenantAdmins(action.event, action.entity);
        return true;
    }
    return false;
  }

}

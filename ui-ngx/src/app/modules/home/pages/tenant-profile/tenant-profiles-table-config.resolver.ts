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

import { Injectable } from '@angular/core';
import { Resolve } from '@angular/router';
import { TenantProfile } from '@shared/models/tenant.model';
import {
  checkBoxCell,
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig,
  HeaderActionDescriptor
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { TenantProfileService } from '@core/http/tenant-profile.service';
import { TenantProfileComponent } from '../../components/profile/tenant-profile.component';
import { TenantProfileTabsComponent } from './tenant-profile-tabs.component';
import { DialogService } from '@core/services/dialog.service';
import { ImportExportService } from '@home/components/import-export/import-export.service';

@Injectable()
export class TenantProfilesTableConfigResolver implements Resolve<EntityTableConfig<TenantProfile>> {

  private readonly config: EntityTableConfig<TenantProfile> = new EntityTableConfig<TenantProfile>();

  constructor(private tenantProfileService: TenantProfileService,
              private importExport: ImportExportService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private dialogService: DialogService) {

    this.config.entityType = EntityType.TENANT_PROFILE;
    this.config.entityComponent = TenantProfileComponent;
    this.config.entityTabsComponent = TenantProfileTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.TENANT_PROFILE);
    this.config.entityResources = entityTypeResources.get(EntityType.TENANT_PROFILE);

    this.config.columns.push(
      new DateEntityTableColumn<TenantProfile>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<TenantProfile>('name', 'tenant-profile.name', '40%'),
      new EntityTableColumn<TenantProfile>('description', 'tenant-profile.description', '60%'),
      new EntityTableColumn<TenantProfile>('isDefault', 'tenant-profile.default', '60px',
        entity => {
          return checkBoxCell(entity.default);
        })
    );

    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('tenant-profile.export'),
        icon: 'file_download',
        isEnabled: () => true,
        onAction: ($event, entity) => this.exportTenantProfile($event, entity)
      },
      {
        name: this.translate.instant('tenant-profile.set-default'),
        icon: 'flag',
        isEnabled: (tenantProfile) => !tenantProfile.default,
        onAction: ($event, entity) => this.setDefaultTenantProfile($event, entity)
      }
    );

    this.config.deleteEntityTitle = tenantProfile => this.translate.instant('tenant-profile.delete-tenant-profile-title',
      { tenantProfileName: tenantProfile.name });
    this.config.deleteEntityContent = () => this.translate.instant('tenant-profile.delete-tenant-profile-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('tenant-profile.delete-tenant-profiles-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('tenant-profile.delete-tenant-profiles-text');

    this.config.entitiesFetchFunction = pageLink => this.tenantProfileService.getTenantProfiles(pageLink);
    this.config.loadEntity = id => this.tenantProfileService.getTenantProfile(id.id);
    this.config.saveEntity = tenantProfile => this.tenantProfileService.saveTenantProfile(tenantProfile);
    this.config.deleteEntity = id => this.tenantProfileService.deleteTenantProfile(id.id);
    this.config.onEntityAction = action => this.onTenantProfileAction(action);
    this.config.deleteEnabled = (tenantProfile) => tenantProfile && !tenantProfile.default;
    this.config.entitySelectionEnabled = (tenantProfile) => tenantProfile && !tenantProfile.default;
    this.config.addActionDescriptors = this.configureAddActions();
  }

  resolve(): EntityTableConfig<TenantProfile> {
    this.config.tableTitle = this.translate.instant('tenant-profile.tenant-profiles');

    return this.config;
  }

  configureAddActions(): Array<HeaderActionDescriptor> {
    const actions: Array<HeaderActionDescriptor> = [];
    actions.push(
      {
        name: this.translate.instant('tenant-profile.create-tenant-profile'),
        icon: 'insert_drive_file',
        isEnabled: () => true,
        onAction: ($event) => this.config.table.addEntity($event)
      },
      {
        name: this.translate.instant('tenant-profile.import'),
        icon: 'file_upload',
        isEnabled: () => true,
        onAction: ($event) => this.importTenantProfile($event)
      }
    );
    return actions;
  }

  setDefaultTenantProfile($event: Event, tenantProfile: TenantProfile) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('tenant-profile.set-default-tenant-profile-title', {tenantProfileName: tenantProfile.name}),
      this.translate.instant('tenant-profile.set-default-tenant-profile-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.tenantProfileService.setDefaultTenantProfile(tenantProfile.id.id).subscribe(
            () => {
              this.config.table.updateData();
            }
          );
        }
      }
    );
  }

  importTenantProfile($event: Event) {
    this.importExport.importTenantProfile().subscribe(
      (deviceProfile) => {
        if (deviceProfile) {
          this.config.table.updateData();
        }
      }
    );
  }

  exportTenantProfile($event: Event, tenantProfile: TenantProfile) {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExport.exportTenantProfile(tenantProfile.id.id);
  }

  onTenantProfileAction(action: EntityAction<TenantProfile>): boolean {
    switch (action.action) {
      case 'setDefault':
        this.setDefaultTenantProfile(action.event, action.entity);
        return true;
    }
    return false;
  }

}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';

import { Router } from '@angular/router';
import {
  DateEntityTableColumn, defaultEntityTablePermissions,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { Role } from '@shared/models/role.models';
import { RoleService } from '@core/http/role.service';
import { RoleComponent } from '@home/pages/role/role.component';
import { RoleTabsComponent } from '@home/pages/role/role-tabs.component';
import { roleTypeTranslationMap } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';

@Injectable()
export class RolesTableConfigResolver  {

  private readonly config: EntityTableConfig<Role> = new EntityTableConfig<Role>();

  constructor(private roleService: RoleService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private router: Router,
              private datePipe: DatePipe,
              private customTranslate: CustomTranslatePipe) {

    this.config.entityType = EntityType.ROLE;
    this.config.entityComponent = RoleComponent;
    this.config.entityTabsComponent = RoleTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.ROLE);
    this.config.entityResources = entityTypeResources.get(EntityType.ROLE);

    this.config.addDialogStyle = {width: '1000px'};

    this.config.entityTitle = (role) => role ? role.name : '';

    this.config.columns.push(
      new DateEntityTableColumn<Role>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<Role>('name', 'role.name', '25%', this.config.entityTitle),
      new EntityTableColumn<Role>('type', 'role.role-type', '25%', (role) => {
        return this.translate.instant(roleTypeTranslationMap.get(role.type));
      }),
      new EntityTableColumn<Role>('description', 'role.description', '40%',
        (role) => this.customTranslate.transform(role?.additionalInfo?.description || ''),
        () => ({}), false)
    );

    this.config.deleteEntityTitle = role =>
      this.translate.instant('role.delete-role-title', { roleName: role.name });
    this.config.deleteEntityContent = () => this.translate.instant('role.delete-role-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('role.delete-roles-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('role.delete-roles-text');
    this.config.entitiesFetchFunction = pageLink => this.roleService.getRoles(pageLink);
    this.config.loadEntity = id => this.roleService.getRole(id.id);
    this.config.saveEntity = role => this.roleService.saveRole(role);
    this.config.deleteEntity = id => this.roleService.deleteRole(id.id);

    this.config.onEntityAction = action => this.onRoleAction(action);
  }

  resolve(): EntityTableConfig<Role> {
    this.config.tableTitle = this.translate.instant('role.roles');
    defaultEntityTablePermissions(this.userPermissionsService, this.config);
    return this.config;
  }

  openRoles($event: Event, role: Role) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree(['roles', role.id.id]);
    this.router.navigateByUrl(url);
  }

  onRoleAction(action: EntityAction<Role>): boolean {
    switch (action.action) {
      case 'open':
        this.openRoles(action.event, action.entity);
        return true;
    }
    return false;
  }

}

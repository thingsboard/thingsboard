// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectorRef, Directive, Input } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '../../components/entity/entity.component';
import { UntypedFormBuilder } from '@angular/forms';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { PageLink } from '@shared/models/page/page-link';
import { ShortEntityView } from '@shared/models/entity-group.models';
import { BaseData, HasId } from '@shared/models/base-data';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation } from '@shared/models/security.models';
import { WhiteLabelingService } from '@core/http/white-labeling.service';

// @dynamic
@Directive()
// eslint-disable-next-line @angular-eslint/directive-class-suffix
export abstract class GroupEntityComponent<T extends BaseData<HasId>>
  extends EntityComponent<T, PageLink, T | ShortEntityView, EntityTableConfig<T> | GroupEntityTableConfig<T>> {

  get groupEntitiesTableConfig(): GroupEntityTableConfig<T> {
    return this.entitiesTableConfigValue as GroupEntityTableConfig<T>;
  }

  entityGroup = this.groupEntitiesTableConfig?.entityGroup;

  constructor(protected store: Store<AppState>,
              protected fb: UntypedFormBuilder,
              protected entityValue: T,
              protected entitiesTableConfigValue: EntityTableConfig<T> | GroupEntityTableConfig<T>,
              protected cd: ChangeDetectorRef,
              protected userPermissionsService: UserPermissionsService) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  protected setEntitiesTableConfig(entitiesTableConfig: EntityTableConfig<T> | GroupEntityTableConfig<T>) {
    super.setEntitiesTableConfig(entitiesTableConfig);
    if (entitiesTableConfig) {
      this.entityGroup = (entitiesTableConfig as GroupEntityTableConfig<T>).entityGroup;
    }
  }

  protected isGroupMode(): boolean {
    return this.entitiesTableConfig && this.entitiesTableConfig instanceof GroupEntityTableConfig;
  }

  canManageOwnerAndGroups(): boolean {
    return (!this.isGroupMode() || this.userPermissionsService.isOwnedGroup(this.entityGroup)) &&
      (this.userPermissionsService.hasGenericPermissionByEntityGroupType(Operation.CHANGE_OWNER, this.entitiesTableConfig.entityType)
    || (this.userPermissionsService.hasGenericEntityGroupTypePermission(Operation.ADD_TO_GROUP, this.entitiesTableConfig.entityType) &&
    this.userPermissionsService.hasGenericEntityGroupTypePermission(Operation.REMOVE_FROM_GROUP, this.entitiesTableConfig.entityType)));
  }
}

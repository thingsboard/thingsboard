// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTabsComponent } from '../../components/entity/entity-tabs.component';
import { BaseData, HasId } from '@shared/models/base-data';
import { PageLink } from '@shared/models/page/page-link';
import { EntityGroupInfo, ShortEntityView } from '@shared/models/entity-group.models';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { EntityType } from '@shared/models/entity-type.models';
import { Operation, Resource, resourceByEntityType } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { exportableEntityTypes } from '@shared/models/vc.models';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';

@Component({
    selector: 'tb-group-entity-tabs',
    templateUrl: './group-entity-tabs.component.html',
    styleUrls: [],
    standalone: false
})
export class GroupEntityTabsComponent<T extends BaseData<HasId>>
  extends EntityTabsComponent<T, PageLink, T | ShortEntityView, EntityTableConfig<T> | GroupEntityTableConfig<T>> {

  entityGroup: EntityGroupInfo;
  entityType: EntityType;
  entityResource: Resource;

  constructor(private userPermissionsService: UserPermissionsService,
              protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
  }

  resolveTabIndex(tab: string): number {
    if (tab === 'cf') {
      return 3;
    } else {
      return super.resolveTabIndex(tab);
    }
  }

  hasVersionControl(): boolean {
    if (this.authUser.authority === this.authorities.TENANT_ADMIN && this.entityType &&
      exportableEntityTypes.includes(this.entityType) && EntityType.USER !== this.entityType) {
      return this.userPermissionsService.hasResourcesGenericPermission([Resource.VERSION_CONTROL, this.entityResource], Operation.READ);
    } else {
      return false;
    }
  }

  hasEventsTab(): boolean {
    return this.entityType === EntityType.DEVICE || this.entityType === EntityType.EDGE;
  }

  protected setEntitiesTableConfig(entitiesTableConfig: EntityTableConfig<T> | GroupEntityTableConfig<T>) {
    super.setEntitiesTableConfig(entitiesTableConfig);
    if (entitiesTableConfig) {
      this.entityGroup = (entitiesTableConfig as GroupEntityTableConfig<T>).entityGroup;
      if (this.entityGroup) {
        this.entityType = this.entityGroup.type;
      } else {
        this.entityType = entitiesTableConfig.entityType;
      }
      this.entityResource = resourceByEntityType.get(this.entityType);
    }
  }

  protected isGroupMode(): boolean {
    return this.entitiesTableConfig && this.entitiesTableConfig instanceof GroupEntityTableConfig;
  }

}

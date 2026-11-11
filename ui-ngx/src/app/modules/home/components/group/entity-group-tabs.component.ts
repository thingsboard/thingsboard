// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTabsComponent } from '@home/components/entity/entity-tabs.component';
import { entityGroupActionSources, entityGroupActionTypes, EntityGroupInfo } from '@shared/models/entity-group.models';
import { PageLink } from '@shared/models/page/page-link';
import { EntityGroupsTableConfig } from '@home/components/group/entity-groups-table-config';
import { exportableEntityTypes } from '@shared/models/vc.models';
import { groupResourceByGroupType, Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';

@Component({
    selector: 'tb-entity-group-tabs',
    templateUrl: './entity-group-tabs.component.html',
    styleUrls: [],
    standalone: false
})
export class EntityGroupTabsComponent extends EntityTabsComponent<EntityGroupInfo, PageLink, EntityGroupInfo, EntityGroupsTableConfig> {

  entityGroupActionTypesList = entityGroupActionTypes;

  entityGroupActionSourcesList = entityGroupActionSources;

  constructor(private userPermissionsService: UserPermissionsService,
              protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
  }

  validateAndMark() {
    this.validate();
    this.detailsForm.markAsDirty();
  }

  onPermissionsChanged() {
    this.entitiesTableConfig.onGroupUpdated();
  }

  hasVersionControl(): boolean {
    if (!this.sharedGroup() &&
      this.authUser.authority === this.authorities.TENANT_ADMIN && this.entity && exportableEntityTypes.includes(this.entity.type)) {
      const entityResource = groupResourceByGroupType.get(this.entity.type);
      return this.userPermissionsService.hasResourcesGenericPermission([Resource.VERSION_CONTROL, entityResource], Operation.READ);
    } else {
      return false;
    }
  }

  sharedGroup(): boolean {
    if (this.entitiesTableConfig) {
      return this.entitiesTableConfig.componentsData.shared === true;
    } else {
      return false;
    }
  }

  private validate() {
    const columnsValid = this.entity.configuration.columns !== null;
    const settingsValid = this.entity.configuration.settings !== null;
    if (!columnsValid || !settingsValid) {
      const errors: any = {};
      if (!columnsValid) {
        errors.columns = true;
      }
      if (!settingsValid) {
        errors.settings = true;
      }
      this.detailsForm.setErrors(errors);
    } else {
      this.detailsForm.setErrors(null);
    }
  }

  protected setEntity(entity: EntityGroupInfo) {
    super.setEntity(entity);
  }
}

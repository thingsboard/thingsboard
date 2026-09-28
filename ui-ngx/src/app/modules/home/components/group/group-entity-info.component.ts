// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Input } from '@angular/core';
import { EntityType, groupUrlPrefixByEntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { GroupEntityInfo } from '@shared/models/base-data';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { MatFormFieldAppearance } from '@angular/material/form-field';

@Component({
    selector: 'tb-group-entity-info',
    templateUrl: './group-entity-info.component.html',
    styleUrls: ['./group-entity-info.component.scss'],
    standalone: false
})
export class GroupEntityInfoComponent {

  groupEntityValue?: GroupEntityInfo<EntityId>;

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  set groupEntity(value: GroupEntityInfo<EntityId>) {
    this.groupEntityValue = value;
    this.update();
  }

  get groupEntity(): GroupEntityInfo<EntityId> {
    return this.groupEntityValue;
  }

  authUser = getCurrentAuthUser(this.store);

  displayOwner: boolean;
  ownerLabel: string;
  groupPrefixUrl: string;

  constructor(private store: Store<AppState>,
              private userPermissionsService: UserPermissionsService) {
  }

  update(): void {
    if (this.groupEntity && this.groupEntity.id) {
      this.groupPrefixUrl = groupUrlPrefixByEntityType.get(this.groupEntity.id.entityType as EntityType);
      this.displayOwner = !this.userPermissionsService.isDirectOwner(this.groupEntity.ownerId);
      this.ownerLabel = this.authUser.authority === Authority.CUSTOMER_USER
        ? 'entity.sub-customer-name' : 'entity.customer-name';
      if (this.groupEntity.ownerId && !this.userPermissionsService.isDirectOwner(this.groupEntity.ownerId)) {
        this.groupPrefixUrl = `/customers/all/${this.groupEntity.ownerId.id}${this.groupPrefixUrl}`;
      }
    }
  }

}

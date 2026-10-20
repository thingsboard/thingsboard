// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { BaseData, GroupEntityInfo } from '@shared/models/base-data';
import { EntityId } from '@shared/models/id/entity-id';
import { baseDetailsPageByEntityType, EntityType, groupUrlPrefixByEntityType } from '@app/shared/public-api';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { isEqual, isNotEmptyStr, isObject } from '@core/utils';

@Component({
    selector: 'tb-entity-chips',
    templateUrl: './entity-chips.component.html',
    styleUrls: ['./entity-chips.component.scss'],
    standalone: false
})
export class EntityChipsComponent implements OnChanges {

  @Input()
  entity: BaseData<EntityId> | GroupEntityInfo<EntityId>;

  @Input()
  key: string;

  @Input()
  detailsPagePrefixUrl: string;

  entityDetailsPrefixUrl: string;

  subEntities: Array<BaseData<EntityId>> = [];

  constructor(private userPermissionsService: UserPermissionsService) {
  }

  ngOnChanges(changes: SimpleChanges) {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (propName === 'entity' && change.currentValue !== change.previousValue) {
        this.update();
      }
    }
  }

  private update(): void {
    if (this.entity && this.entity.id && this.key) {
      let entitiesList = this.entity?.[this.key];
      const entityType = this.entity.id.entityType as EntityType;
      if (isObject(entitiesList) && !Array.isArray(entitiesList)) {
        entitiesList = [entitiesList];
      }
      if (isNotEmptyStr(this.detailsPagePrefixUrl)) {
        this.entityDetailsPrefixUrl = this.detailsPagePrefixUrl;
      } else if (this.key === 'groups' && groupUrlPrefixByEntityType.has(entityType)) {
        this.entityDetailsPrefixUrl = groupUrlPrefixByEntityType.get(entityType);
        if (this.entity.ownerId && !this.userPermissionsService.isDirectOwner(this.entity.ownerId)) {
          this.entityDetailsPrefixUrl = `/customers/all/${this.entity.ownerId.id}${this.entityDetailsPrefixUrl}`;
        }
      } else if (Array.isArray(entitiesList)) {
        if (entitiesList.length) {
          this.entityDetailsPrefixUrl = baseDetailsPageByEntityType.get(entitiesList[0].id.entityType as EntityType);
        }
      } else {
        entitiesList = [];
      }
      if (!isEqual(entitiesList, this.subEntities)) {
        this.subEntities = entitiesList;
      }
    }
  }

}

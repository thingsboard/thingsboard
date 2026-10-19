// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { BaseData } from '@shared/models/base-data';
import { EntityId } from '@shared/models/id/entity-id';
import { baseDetailsPageByEntityType, EntityType } from '@app/shared/public-api';
import { isEqual, isNotEmptyStr, isObject } from '@core/utils';

@Component({
    selector: 'tb-entity-chips',
    templateUrl: './entity-chips.component.html',
    styleUrls: ['./entity-chips.component.scss'],
    standalone: false
})
export class EntityChipsComponent implements OnChanges {

  @Input()
  entity: BaseData<EntityId>;

  @Input()
  key: string;

  @Input()
  detailsPagePrefixUrl: string;

  entityDetailsPrefixUrl: string;

  subEntities: Array<BaseData<EntityId>> = [];

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
      if (isObject(entitiesList) && !Array.isArray(entitiesList)) {
        entitiesList = [entitiesList];
      }
      if (isNotEmptyStr(this.detailsPagePrefixUrl)) {
        this.entityDetailsPrefixUrl = this.detailsPagePrefixUrl;
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

///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { Component, Input } from '@angular/core';
import { BaseData } from '@shared/models/base-data';
import { EntityId } from '@shared/models/id/entity-id';
import { baseDetailsPageByEntityType, EntityType } from '@app/shared/public-api';

const entityTypeEntitiesPropertyKeyMap = new Map<EntityType, string>([
  [EntityType.DOMAIN, 'oauth2ClientInfos'],
  [EntityType.MOBILE_APP, 'oauth2ClientInfos']
]);

@Component({
  selector: 'tb-entity-chips',
  templateUrl: './entity-chips.component.html',
  styleUrls: ['./entity-chips.component.scss']
})
export class EntityChipsComponent {

  @Input()
  set entity(value: BaseData<EntityId>) {
    this.entityValue = value;
    this.update();
  }

  get entity(): BaseData<EntityId> {
    return this.entityValue;
  }

  entityDetailsPrefixUrl: string;

  subEntities: Array<BaseData<EntityId>>;

  private entityValue?: BaseData<EntityId>;

  private subEntitiesKey: string;

  update(): void {
    if (this.entity && this.entity.id) {
      const entityType = this.entity.id.entityType as EntityType;
      this.subEntitiesKey = entityTypeEntitiesPropertyKeyMap.get(entityType);
      this.subEntities = this.entity?.[this.subEntitiesKey];
      if (this.subEntities.length) {
        this.entityDetailsPrefixUrl = baseDetailsPageByEntityType.get(this.subEntities[0].id.entityType as EntityType);
      }
    }
  }

}

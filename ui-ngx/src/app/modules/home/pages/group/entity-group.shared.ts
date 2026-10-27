// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot } from '@angular/router';
import { EntityGroupStateInfo } from '@home/models/group/group-entities-table-config.models';
import { EntityGroupConfigResolver } from '@home/components/group/entity-group-config.resolver';
import { Observable } from 'rxjs';
import { resolveGroupParams } from '@shared/models/entity-group.models';
import { BreadCrumbLabelFunction } from '@shared/components/breadcrumb';
import { GroupEntitiesTableComponent } from '@home/components/group/group-entities-table.component';

@Injectable()
export class EntityGroupResolver<T>  {

  constructor(private entityGroupConfigResolver: EntityGroupConfigResolver) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityGroupStateInfo<T>> | EntityGroupStateInfo<T> {
    const entityGroupParams = resolveGroupParams(route);
    return this.entityGroupConfigResolver.constructGroupConfigByStateParams(entityGroupParams);
  }
}

export const groupEntitiesLabelFunction: BreadCrumbLabelFunction<GroupEntitiesTableComponent> =
  (route, translate, component, data, utils) => {
    return utils ? utils.customTranslation(component.entityGroup.name, component.entityGroup.name) : component.entityGroup.name;
  };

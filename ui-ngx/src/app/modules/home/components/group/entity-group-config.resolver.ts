// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable, Injector } from '@angular/core';
import { EntityGroupService } from '@core/http/entity-group.service';
import { CustomerService } from '@core/http/customer.service';
import {
  edgeEntitiesTitle,
  EntityGroupInfo,
  EntityGroupParams,
  entityGroupsTitle
} from '@shared/models/entity-group.models';
import { forkJoin, Observable, of } from 'rxjs';
import { map, mergeMap } from 'rxjs/operators';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import {
  EntityGroupStateConfigFactory,
  EntityGroupStateInfo,
  groupConfigFactoryTokenMap
} from '@home/models/group/group-entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { EdgeService } from '@core/http/edge.service';
import { deepClone } from '@app/core/utils';

@Injectable()
export class EntityGroupConfigResolver {

  constructor(private entityGroupService: EntityGroupService,
              private customerService: CustomerService,
              private edgeService: EdgeService,
              private translate: TranslateService,
              private injector: Injector) {
  }

  public constructGroupConfigByStateParams<T>(params: EntityGroupParams): Observable<EntityGroupStateInfo<T>> {
    const entityGroupId: string = params.edgeEntitiesGroupId || params.childEntityGroupId || params.entityGroupId;
    if (entityGroupId) {
      return this.entityGroupService.getEntityGroup(entityGroupId).pipe(
        mergeMap((entityGroup) => {
            return this.constructGroupConfig<T>(params, entityGroup);
          }
        ));
    } else {
      return of(null);
    }
  }

  public constructGroupConfig<T>(params: EntityGroupParams,
                                 entityGroup: EntityGroupInfo): Observable<EntityGroupStateInfo<T>> {
    const entityGroupStateInfo: EntityGroupStateInfo<T> = deepClone(entityGroup);
    // entityGroupStateInfo.origEntityGroup = deepClone(entityGroup);
    return this.resolveParentGroupInfo(params, entityGroupStateInfo).pipe(
      mergeMap((resolvedEntityGroup) => {
          const token = groupConfigFactoryTokenMap.get(resolvedEntityGroup.type);
          const factory = this.injector.get(token) as EntityGroupStateConfigFactory<T>;
          return factory.createConfig(params, resolvedEntityGroup).pipe(
            map(entityGroupConfig => {
              resolvedEntityGroup.entityGroupConfig = entityGroupConfig;
              return resolvedEntityGroup;
            })
          );
        }
      ));
  }

  private resolveParentGroupInfo<T>(params: EntityGroupParams, entityGroup: EntityGroupStateInfo<T>): Observable<EntityGroupStateInfo<T>> {
    if (params.customerId) {
      const groupType: EntityType = params.childGroupType || params.groupType;
      return this.customerService.getShortCustomerInfo(params.customerId).pipe(
        mergeMap((info) => {
            entityGroup.customerTitle = info.title;
            entityGroup.customerGroupsTitle = info.title + ': ' + this.translate.instant(entityTypeTranslations.get(groupType).typePlural);
            const tasks = [];
            if (params.childEntityGroupId) {
              tasks.push(this.entityGroupService.getEntityGroup(params.entityGroupId).pipe(
                map(parentEntityGroup => {
                  entityGroup.parentEntityGroup = parentEntityGroup;
                  return entityGroup;
                })
              ));
            } else {
              tasks.push(of(entityGroup));
            }
            if (params.childGroupType === EntityType.EDGE && params.groupType === EntityType.CUSTOMER && params.edgeId) {
              tasks.push(this.edgeService.getEdge(params.edgeId).pipe(
                map(edge =>
                  entityGroup.edgeEntitiesTitle = edge.name + ': ' + this.translate.instant(edgeEntitiesTitle(params.edgeEntitiesType)))
              ));
              tasks.push(this.entityGroupService.getEntityGroup(params.childEntityGroupId || params.entityGroupId).pipe(
                map(edgeGroup => entityGroup.edgeGroupName = edgeGroup.name)
              ));
            }
            return forkJoin(tasks).pipe(
              mergeMap(() => of(entityGroup))
            );
          }
        ));
    } else if (params.edgeId) {
      const groupType: EntityType = params.edgeEntitiesType || params.childGroupType || params.groupType;
      const tasks = [];
      tasks.push(this.edgeService.getEdge(params.edgeId).pipe(
          map(
          edge => entityGroup.edgeEntitiesTitle = edge.name + ': ' + this.translate.instant(edgeEntitiesTitle(groupType))
          )
        )
      );
      tasks.push(this.entityGroupService.getEntityGroup(params.entityGroupId).pipe(
          map(parentEntityGroup => entityGroup.parentEntityGroup = parentEntityGroup)
        )
      );
      return forkJoin(tasks).pipe(
        mergeMap(() => of(entityGroup))
      );
    } else {
      return of(entityGroup);
    }
  }

}

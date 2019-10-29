///
/// Copyright © 2016-2019 The Thingsboard Authors
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

import { Injectable } from '@angular/core';
import { EMPTY, forkJoin, Observable, of, throwError } from 'rxjs/index';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { AliasEntityType, EntityType } from '@shared/models/entity-type.models';
import { BaseData } from '@shared/models/base-data';
import { EntityId } from '@shared/models/id/entity-id';
import { DeviceService } from '@core/http/device.service';
import { TenantService } from '@core/http/tenant.service';
import { CustomerService } from '@core/http/customer.service';
import { UserService } from './user.service';
import { DashboardService } from '@core/http/dashboard.service';
import { Direction } from '@shared/models/page/sort-order';
import { PageData } from '@shared/models/page/page-data';
import { getCurrentAuthUser } from '../auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Authority } from '@shared/models/authority.enum';
import { Tenant } from '@shared/models/tenant.model';
import { catchError, concatMap, expand, map, mergeMap, toArray } from 'rxjs/operators';
import { Customer } from '@app/shared/models/customer.model';
import { AssetService } from '@core/http/asset.service';
import { EntityViewService } from '@core/http/entity-view.service';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { defaultHttpOptions } from '@core/http/http-utils';
import { RuleChainService } from '@core/http/rule-chain.service';
import { AliasInfo, StateParams, SubscriptionInfo } from '@core/api/widget-api.models';
import { Datasource, DatasourceType, KeyInfo } from '@app/shared/models/widget.models';
import { UtilsService } from '@core/services/utils.service';
import { AliasFilterType, EntityAlias, EntityAliasFilter, EntityAliasFilterResult } from '@shared/models/alias.models';
import { EntityInfo } from '@shared/models/entity.models';
import {
  EntityRelationInfo,
  EntityRelationsQuery,
  EntitySearchDirection,
  EntitySearchQuery
} from '@shared/models/relation.models';
import { EntityRelationService } from '@core/http/entity-relation.service';
import { isDefined } from '../utils';
import { AssetSearchQuery } from '@shared/models/asset.models';
import { DeviceSearchQuery } from '@shared/models/device.models';
import { EntityViewSearchQuery } from '@shared/models/entity-view.models';

@Injectable({
  providedIn: 'root'
})
export class EntityService {

  constructor(
    private http: HttpClient,
    private store: Store<AppState>,
    private deviceService: DeviceService,
    private assetService: AssetService,
    private entityViewService: EntityViewService,
    private tenantService: TenantService,
    private customerService: CustomerService,
    private userService: UserService,
    private ruleChainService: RuleChainService,
    private dashboardService: DashboardService,
    private entityRelationService: EntityRelationService,
    private utils: UtilsService
  ) { }

  private getEntityObservable(entityType: EntityType, entityId: string,
                              ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<BaseData<EntityId>> {

    let observable: Observable<BaseData<EntityId>>;
    switch (entityType) {
      case EntityType.DEVICE:
        observable = this.deviceService.getDevice(entityId, ignoreErrors, ignoreLoading);
        break;
      case EntityType.ASSET:
        observable = this.assetService.getAsset(entityId, ignoreErrors, ignoreLoading);
        break;
      case EntityType.ENTITY_VIEW:
        observable = this.entityViewService.getEntityView(entityId, ignoreErrors, ignoreLoading);
        break;
      case EntityType.TENANT:
        observable = this.tenantService.getTenant(entityId, ignoreErrors, ignoreLoading);
        break;
      case EntityType.CUSTOMER:
        observable = this.customerService.getCustomer(entityId, ignoreErrors, ignoreLoading);
        break;
      case EntityType.DASHBOARD:
        observable = this.dashboardService.getDashboardInfo(entityId, ignoreErrors, ignoreLoading);
        break;
      case EntityType.USER:
        observable = this.userService.getUser(entityId, ignoreErrors, ignoreLoading);
        break;
      case EntityType.RULE_CHAIN:
        observable = this.ruleChainService.getRuleChain(entityId, ignoreErrors, ignoreLoading);
        break;
      case EntityType.ALARM:
        console.error('Get Alarm Entity is not implemented!');
        break;
    }
    return observable;
  }
  public getEntity(entityType: EntityType, entityId: string,
                   ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<BaseData<EntityId>> {
    const entityObservable = this.getEntityObservable(entityType, entityId, ignoreErrors, ignoreLoading);
    if (entityObservable) {
      return entityObservable;
    } else {
      return throwError(null);
    }
  }

  private getEntitiesByIdsObservable(fetchEntityFunction: (entityId: string) => Observable<BaseData<EntityId>>,
                                     entityIds: Array<string>): Observable<Array<BaseData<EntityId>>> {
    const tasks: Observable<BaseData<EntityId>>[] = [];
    entityIds.forEach((entityId) => {
      tasks.push(fetchEntityFunction(entityId));
    });
    return forkJoin(tasks).pipe(
      map((entities) => {
        if (entities) {
          entities.sort((entity1, entity2) => {
            const index1 = entityIds.indexOf(entity1.id.id);
            const index2 = entityIds.indexOf(entity2.id.id);
            return index1 - index2;
          });
          return entities;
        } else {
          return [];
        }
      })
    );
  }


  private getEntitiesObservable(entityType: EntityType, entityIds: Array<string>,
                                ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Array<BaseData<EntityId>>> {
    let observable: Observable<Array<BaseData<EntityId>>>;
    switch (entityType) {
      case EntityType.DEVICE:
        observable = this.deviceService.getDevices(entityIds, ignoreErrors, ignoreLoading);
        break;
      case EntityType.ASSET:
        observable = this.assetService.getAssets(entityIds, ignoreErrors, ignoreLoading);
        break;
      case EntityType.ENTITY_VIEW:
        observable = this.getEntitiesByIdsObservable(
          (id) => this.entityViewService.getEntityView(id, ignoreErrors, ignoreLoading),
          entityIds);
        break;
      case EntityType.TENANT:
        observable = this.getEntitiesByIdsObservable(
          (id) => this.tenantService.getTenant(id, ignoreErrors, ignoreLoading),
          entityIds);
        break;
      case EntityType.CUSTOMER:
        observable = this.getEntitiesByIdsObservable(
          (id) => this.customerService.getCustomer(id, ignoreErrors, ignoreLoading),
          entityIds);
        break;
      case EntityType.DASHBOARD:
        observable = this.getEntitiesByIdsObservable(
          (id) => this.dashboardService.getDashboardInfo(id, ignoreErrors, ignoreLoading),
          entityIds);
        break;
      case EntityType.USER:
        observable = this.getEntitiesByIdsObservable(
          (id) => this.userService.getUser(id, ignoreErrors, ignoreLoading),
          entityIds);
        break;
      case EntityType.ALARM:
        console.error('Get Alarm Entity is not implemented!');
        break;
    }
    return observable;
  }

  public getEntities(entityType: EntityType, entityIds: Array<string>,
                     ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Array<BaseData<EntityId>>> {
    const entitiesObservable = this.getEntitiesObservable(entityType, entityIds, ignoreErrors, ignoreLoading);
    if (entitiesObservable) {
      return entitiesObservable;
    } else {
      return throwError(null);
    }
  }

  private getSingleTenantByPageLinkObservable(pageLink: PageLink,
                                              ignoreErrors: boolean = false,
                                              ignoreLoading: boolean = false): Observable<PageData<Tenant>> {
    const authUser = getCurrentAuthUser(this.store);
    const tenantId = authUser.tenantId;
    return this.tenantService.getTenant(tenantId, ignoreErrors, ignoreLoading).pipe(
      map((tenant) => {
        const result = {
          data: [],
          totalPages: 0,
          totalElements: 0,
          hasNext: false
        } as PageData<Tenant>;
        if (tenant.title.toLowerCase().startsWith(pageLink.textSearch.toLowerCase())) {
          result.data.push(tenant);
          result.totalPages = 1;
          result.totalElements = 1;
        }
        return result;
      })
    );
  }

  private getSingleCustomerByPageLinkObservable(pageLink: PageLink,
                                                ignoreErrors: boolean = false,
                                                ignoreLoading: boolean = false): Observable<PageData<Customer>> {
    const authUser = getCurrentAuthUser(this.store);
    const customerId = authUser.customerId;
    return this.customerService.getCustomer(customerId, ignoreErrors, ignoreLoading).pipe(
      map((customer) => {
        const result = {
          data: [],
          totalPages: 0,
          totalElements: 0,
          hasNext: false
        } as PageData<Customer>;
        if (customer.title.toLowerCase().startsWith(pageLink.textSearch.toLowerCase())) {
          result.data.push(customer);
          result.totalPages = 1;
          result.totalElements = 1;
        }
        return result;
      })
    );
  }

  private getEntitiesByPageLinkObservable(entityType: EntityType, pageLink: PageLink, subType: string = '',
                                          ignoreErrors: boolean = false,
                                          ignoreLoading: boolean = false): Observable<PageData<BaseData<EntityId>>> {
    let entitiesObservable: Observable<PageData<BaseData<EntityId>>>;
    const authUser = getCurrentAuthUser(this.store);
    const customerId = authUser.customerId;
    switch (entityType) {
      case EntityType.DEVICE:
        pageLink.sortOrder.property = 'name';
        if (authUser.authority === Authority.CUSTOMER_USER) {
          entitiesObservable = this.deviceService.getCustomerDeviceInfos(customerId, pageLink, subType, ignoreErrors, ignoreLoading);
        } else {
          entitiesObservable = this.deviceService.getTenantDeviceInfos(pageLink, subType, ignoreErrors, ignoreLoading);
        }
        break;
      case EntityType.ASSET:
        pageLink.sortOrder.property = 'name';
        if (authUser.authority === Authority.CUSTOMER_USER) {
          entitiesObservable = this.assetService.getCustomerAssetInfos(customerId, pageLink, subType, ignoreErrors, ignoreLoading);
        } else {
          entitiesObservable = this.assetService.getTenantAssetInfos(pageLink, subType, ignoreErrors, ignoreLoading);
        }
        break;
      case EntityType.ENTITY_VIEW:
        pageLink.sortOrder.property = 'name';
        if (authUser.authority === Authority.CUSTOMER_USER) {
          entitiesObservable = this.entityViewService.getCustomerEntityViewInfos(customerId, pageLink,
            subType, ignoreErrors, ignoreLoading);
        } else {
          entitiesObservable = this.entityViewService.getTenantEntityViewInfos(pageLink, subType, ignoreErrors, ignoreLoading);
        }
        break;
      case EntityType.TENANT:
        pageLink.sortOrder.property = 'title';
        if (authUser.authority === Authority.TENANT_ADMIN) {
          entitiesObservable = this.getSingleTenantByPageLinkObservable(pageLink, ignoreErrors, ignoreLoading);
        } else {
          entitiesObservable = this.tenantService.getTenants(pageLink, ignoreErrors, ignoreLoading);
        }
        break;
      case EntityType.CUSTOMER:
        pageLink.sortOrder.property = 'title';
        if (authUser.authority === Authority.CUSTOMER_USER) {
          entitiesObservable = this.getSingleCustomerByPageLinkObservable(pageLink, ignoreErrors, ignoreLoading);
        } else {
          entitiesObservable = this.customerService.getCustomers(pageLink, ignoreErrors, ignoreLoading);
        }
        break;
      case EntityType.RULE_CHAIN:
        pageLink.sortOrder.property = 'name';
        entitiesObservable = this.ruleChainService.getRuleChains(pageLink, ignoreErrors, ignoreLoading);
        break;
      case EntityType.DASHBOARD:
        pageLink.sortOrder.property = 'title';
        if (authUser.authority === Authority.CUSTOMER_USER) {
          entitiesObservable = this.dashboardService.getCustomerDashboards(customerId, pageLink, ignoreErrors, ignoreLoading);
        } else {
          entitiesObservable = this.dashboardService.getTenantDashboards(pageLink, ignoreErrors, ignoreLoading);
        }
        break;
      case EntityType.USER:
        console.error('Get User Entities is not implemented!');
        break;
      case EntityType.ALARM:
        console.error('Get Alarm Entities is not implemented!');
        break;
    }
    return entitiesObservable;
  }

  private getEntitiesByPageLink(entityType: EntityType, pageLink: PageLink, subType: string = '',
                                ignoreErrors: boolean = false,
                                ignoreLoading: boolean = false): Observable<Array<BaseData<EntityId>>> {
    const entitiesObservable: Observable<PageData<BaseData<EntityId>>> =
      this.getEntitiesByPageLinkObservable(entityType, pageLink, subType, ignoreErrors, ignoreLoading);
    if (entitiesObservable) {
      return entitiesObservable.pipe(
        expand((data) => {
          if (data.hasNext) {
            pageLink.page += 1;
            return this.getEntitiesByPageLinkObservable(entityType, pageLink, subType, ignoreErrors, ignoreLoading);
          } else {
            return EMPTY;
          }
        }),
        map((data) => data.data),
        concatMap((data) => data),
        toArray()
      );
    } else {
      return of(null);
    }
  }

  public getEntitiesByNameFilter(entityType: EntityType, entityNameFilter: string,
                                 pageSize: number, subType: string = '',
                                 ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Array<BaseData<EntityId>>> {
    const pageLink = new PageLink(pageSize, 0, entityNameFilter, {
      property: 'name',
      direction: Direction.ASC
    });
    if (pageSize === -1) { // all
      pageLink.pageSize = 100;
      return this.getEntitiesByPageLink(entityType, pageLink, subType, ignoreErrors, ignoreLoading).pipe(
        map((data) => data && data.length ? data : null)
      );
    } else {
      const entitiesObservable: Observable<PageData<BaseData<EntityId>>> =
        this.getEntitiesByPageLinkObservable(entityType, pageLink, subType, ignoreErrors, ignoreLoading);
      if (entitiesObservable) {
        return entitiesObservable.pipe(
          map((data) => data && data.data.length ? data.data : null)
        );
      } else {
        return of(null);
      }
    }
  }

  public getAliasFilterTypesByEntityTypes(entityTypes: Array<EntityType | AliasEntityType>): Array<AliasFilterType> {
    const allAliasFilterTypes: Array<AliasFilterType> = Object.keys(AliasFilterType).map((key) => AliasFilterType[key]);
    if (!entityTypes || !entityTypes.length) {
      return allAliasFilterTypes;
    }
    const result = [];
    for (const aliasFilterType of allAliasFilterTypes) {
      if (this.filterAliasFilterTypeByEntityTypes(aliasFilterType, entityTypes)) {
        result.push(aliasFilterType);
      }
    }
    return result;
  }

  public filterAliasByEntityTypes(entityAlias: EntityAlias, entityTypes: Array<EntityType | AliasEntityType>): boolean {
    const filter = entityAlias.filter;
    if (this.filterAliasFilterTypeByEntityTypes(filter.type, entityTypes)) {
      switch (filter.type) {
        case AliasFilterType.singleEntity:
          return entityTypes.indexOf(filter.singleEntity.entityType) > -1 ? true : false;
        case AliasFilterType.entityList:
          return entityTypes.indexOf(filter.entityType) > -1 ? true : false;
        case AliasFilterType.entityName:
          return entityTypes.indexOf(filter.entityType) > -1 ? true : false;
        case AliasFilterType.stateEntity:
          return true;
        case AliasFilterType.assetType:
          return entityTypes.indexOf(EntityType.ASSET)  > -1 ? true : false;
        case AliasFilterType.deviceType:
          return entityTypes.indexOf(EntityType.DEVICE)  > -1 ? true : false;
        case AliasFilterType.entityViewType:
          return entityTypes.indexOf(EntityType.ENTITY_VIEW)  > -1 ? true : false;
        case AliasFilterType.relationsQuery:
          if (filter.filters && filter.filters.length) {
            let match = false;
            for (const relationFilter of filter.filters) {
              if (relationFilter.entityTypes && relationFilter.entityTypes.length) {
                for (const relationFilterEntityType of relationFilter.entityTypes) {
                  if (entityTypes.indexOf(relationFilterEntityType) > -1) {
                    match = true;
                    break;
                  }
                }
              } else {
                match = true;
                break;
              }
            }
            return match;
          } else {
            return true;
          }
        case AliasFilterType.assetSearchQuery:
          return entityTypes.indexOf(EntityType.ASSET)  > -1 ? true : false;
        case AliasFilterType.deviceSearchQuery:
          return entityTypes.indexOf(EntityType.DEVICE)  > -1 ? true : false;
        case AliasFilterType.entityViewSearchQuery:
          return entityTypes.indexOf(EntityType.ENTITY_VIEW)  > -1 ? true : false;
      }
    }
    return false;
  }

  private filterAliasFilterTypeByEntityTypes(aliasFilterType: AliasFilterType,
                                             entityTypes: Array<EntityType | AliasEntityType>): boolean {
    if (!entityTypes || !entityTypes.length) {
      return true;
    }
    let valid = false;
    entityTypes.forEach((entityType) => {
      valid = valid || this.filterAliasFilterTypeByEntityType(aliasFilterType, entityType);
    });
    return valid;
  }

  private filterAliasFilterTypeByEntityType(aliasFilterType: AliasFilterType, entityType: EntityType | AliasEntityType): boolean {
    switch (aliasFilterType) {
      case AliasFilterType.singleEntity:
        return true;
      case AliasFilterType.entityList:
        return true;
      case AliasFilterType.entityName:
        return true;
      case AliasFilterType.stateEntity:
        return true;
      case AliasFilterType.assetType:
        return entityType === EntityType.ASSET;
      case AliasFilterType.deviceType:
        return entityType === EntityType.DEVICE;
      case AliasFilterType.entityViewType:
        return entityType === EntityType.ENTITY_VIEW;
      case AliasFilterType.relationsQuery:
        return true;
      case AliasFilterType.assetSearchQuery:
        return entityType === EntityType.ASSET;
      case AliasFilterType.deviceSearchQuery:
        return entityType === EntityType.DEVICE;
      case AliasFilterType.entityViewSearchQuery:
        return entityType === EntityType.ENTITY_VIEW;
    }
    return false;
  }

  public prepareAllowedEntityTypesList(allowedEntityTypes: Array<EntityType | AliasEntityType>,
                                       useAliasEntityTypes?: boolean): Array<EntityType | AliasEntityType> {
    const authUser = getCurrentAuthUser(this.store);
    const entityTypes: Array<EntityType | AliasEntityType> = [];
    switch (authUser.authority) {
      case Authority.SYS_ADMIN:
        entityTypes.push(EntityType.TENANT);
        break;
      case Authority.TENANT_ADMIN:
        entityTypes.push(EntityType.DEVICE);
        entityTypes.push(EntityType.ASSET);
        entityTypes.push(EntityType.ENTITY_VIEW);
        entityTypes.push(EntityType.TENANT);
        entityTypes.push(EntityType.CUSTOMER);
        entityTypes.push(EntityType.DASHBOARD);
        if (useAliasEntityTypes) {
          entityTypes.push(AliasEntityType.CURRENT_CUSTOMER);
        }
        break;
      case Authority.CUSTOMER_USER:
        entityTypes.push(EntityType.DEVICE);
        entityTypes.push(EntityType.ASSET);
        entityTypes.push(EntityType.ENTITY_VIEW);
        entityTypes.push(EntityType.CUSTOMER);
        entityTypes.push(EntityType.DASHBOARD);
        if (useAliasEntityTypes) {
          entityTypes.push(AliasEntityType.CURRENT_CUSTOMER);
        }
        break;
    }
    if (allowedEntityTypes && allowedEntityTypes.length) {
      for (let index = entityTypes.length - 1; index >= 0; index--) {
        if (allowedEntityTypes.indexOf(entityTypes[index]) === -1) {
          entityTypes.splice(index, 1);
        }
      }
    }
    return entityTypes;
  }

  public getEntityKeys(entityId: EntityId, query: string, type: DataKeyType,
                       ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Array<string>> {
    let url = `/api/plugins/telemetry/${entityId.entityType}/${entityId.id}/keys/`;
    if (type === DataKeyType.timeseries) {
      url += 'timeseries';
    } else if (type === DataKeyType.attribute) {
      url += 'attributes';
    }
    return this.http.get<Array<string>>(url,
      defaultHttpOptions(ignoreLoading, ignoreErrors))
      .pipe(
        map(
          (dataKeys) => {
            if (query) {
              const lowercaseQuery = query.toLowerCase();
              return dataKeys.filter((dataKey) => dataKey.toLowerCase().indexOf(lowercaseQuery) === 0);
            } else {
              return dataKeys;
            }
           }
        )
    );
  }

  public createDatasourcesFromSubscriptionsInfo(subscriptionsInfo: Array<SubscriptionInfo>): Observable<Array<Datasource>> {
    const observables = new Array<Observable<Array<Datasource>>>();
    subscriptionsInfo.forEach((subscriptionInfo) => {
      observables.push(this.createDatasourcesFromSubscriptionInfo(subscriptionInfo));
    });
    return forkJoin(observables).pipe(
      map((arrayOfDatasources) => {
        const result = new Array<Datasource>();
        arrayOfDatasources.forEach((datasources) => {
          result.push(...datasources);
        });
        this.utils.generateColors(result);
        return result;
      })
    );
  }

  public createAlarmSourceFromSubscriptionInfo(subscriptionInfo: SubscriptionInfo): Observable<Datasource> {
    if (subscriptionInfo.entityId && subscriptionInfo.entityType) {
      return this.getEntity(subscriptionInfo.entityType, subscriptionInfo.entityId,
        true, true).pipe(
        map((entity) => {
          const alarmSource = this.createDatasourceFromSubscription(subscriptionInfo, entity);
          this.utils.generateColors([alarmSource]);
          return alarmSource;
        })
      );
    } else {
      return throwError(null);
    }
  }

  public resolveAlias(entityAlias: EntityAlias, stateParams: StateParams): Observable<AliasInfo> {
    const filter = entityAlias.filter;
    return this.resolveAliasFilter(filter, stateParams, -1, false).pipe(
      map((result) => {
        const aliasInfo: AliasInfo = {
          alias: entityAlias.alias,
          stateEntity: result.stateEntity,
          entityParamName: result.entityParamName,
          resolveMultiple: filter.resolveMultiple
        };
        aliasInfo.resolvedEntities = result.entities;
        aliasInfo.currentEntity = null;
        if (aliasInfo.resolvedEntities.length) {
          aliasInfo.currentEntity = aliasInfo.resolvedEntities[0];
        }
        return aliasInfo;
      })
    );
  }

  public resolveAliasFilter(filter: EntityAliasFilter, stateParams: StateParams,
                            maxItems: number, failOnEmpty: boolean): Observable<EntityAliasFilterResult> {
    const result: EntityAliasFilterResult = {
      entities: [],
      stateEntity: false
    };
    if (filter.stateEntityParamName && filter.stateEntityParamName.length) {
      result.entityParamName = filter.stateEntityParamName;
    }
    const stateEntityId = this.getStateEntityId(filter, stateParams);
    switch (filter.type) {
      case AliasFilterType.singleEntity:
        const aliasEntityId = this.resolveAliasEntityId(filter.singleEntity.entityType, filter.singleEntity.id);
        return this.getEntity(aliasEntityId.entityType as EntityType, aliasEntityId.id, true, true).pipe(
          map((entity) => {
            result.entities = this.entitiesToEntitiesInfo([entity]);
            return result;
          }
        ));
        break;
      case AliasFilterType.entityList:
        return this.getEntities(filter.entityType, filter.entityList, true, true).pipe(
          map((entities) => {
              if (entities && entities.length || !failOnEmpty) {
                result.entities = this.entitiesToEntitiesInfo(entities);
                return result;
              } else {
                throw new Error();
              }
            }
          ));
        break;
      case AliasFilterType.entityName:
        return this.getEntitiesByNameFilter(filter.entityType, filter.entityNameFilter, maxItems,
          '', true, true).pipe(
            map((entities) => {
              if (entities && entities.length || !failOnEmpty) {
                result.entities = this.entitiesToEntitiesInfo(entities);
                return result;
              } else {
                throw new Error();
              }
            }
          )
        );
        break;
      case AliasFilterType.stateEntity:
        result.stateEntity = true;
        if (stateEntityId) {
          return this.getEntity(stateEntityId.entityType as EntityType, stateEntityId.id, true, true).pipe(
            map((entity) => {
                result.entities = this.entitiesToEntitiesInfo([entity]);
                return result;
              }
            ));
        } else {
          return of(result);
        }
        break;
      case AliasFilterType.assetType:
        return this.getEntitiesByNameFilter(EntityType.ASSET, filter.assetNameFilter, maxItems,
          filter.assetType, true, true).pipe(
          map((entities) => {
              if (entities && entities.length || !failOnEmpty) {
                result.entities = this.entitiesToEntitiesInfo(entities);
                return result;
              } else {
                throw new Error();
              }
            }
          )
        );
        break;
      case AliasFilterType.deviceType:
        return this.getEntitiesByNameFilter(EntityType.DEVICE, filter.deviceNameFilter, maxItems,
          filter.deviceType, true, true).pipe(
          map((entities) => {
              if (entities && entities.length || !failOnEmpty) {
                result.entities = this.entitiesToEntitiesInfo(entities);
                return result;
              } else {
                throw new Error();
              }
            }
          )
        );
        break;
      case AliasFilterType.entityViewType:
        return this.getEntitiesByNameFilter(EntityType.ENTITY_VIEW, filter.entityViewNameFilter, maxItems,
          filter.entityViewType, true, true).pipe(
          map((entities) => {
              if (entities && entities.length || !failOnEmpty) {
                result.entities = this.entitiesToEntitiesInfo(entities);
                return result;
              } else {
                throw new Error();
              }
            }
          )
        );
        break;
      case AliasFilterType.relationsQuery:
        result.stateEntity = filter.rootStateEntity;
        let rootEntityType;
        let rootEntityId;
        if (result.stateEntity && stateEntityId) {
          rootEntityType = stateEntityId.entityType;
          rootEntityId = stateEntityId.id;
        } else if (!result.stateEntity) {
          rootEntityType = filter.rootEntity.entityType;
          rootEntityId = filter.rootEntity.id;
        }
        if (rootEntityType && rootEntityId) {
          const relationQueryRootEntityId = this.resolveAliasEntityId(rootEntityType, rootEntityId);
          const searchQuery: EntityRelationsQuery = {
            parameters: {
              rootId: relationQueryRootEntityId.id,
              rootType: relationQueryRootEntityId.entityType as EntityType,
              direction: filter.direction
            },
            filters: filter.filters
          };
          searchQuery.parameters.maxLevel = filter.maxLevel && filter.maxLevel > 0 ? filter.maxLevel : -1;
          return this.entityRelationService.findInfoByQuery(searchQuery, true, true).pipe(
            mergeMap((allRelations) => {
              if (allRelations && allRelations.length || !failOnEmpty) {
                if (isDefined(maxItems) && maxItems > 0 && allRelations) {
                  const limit = Math.min(allRelations.length, maxItems);
                  allRelations.length = limit;
                }
                return this.entityRelationInfosToEntitiesInfo(allRelations, filter.direction).pipe(
                  map((entities) => {
                    result.entities = entities;
                    return result;
                  })
                );
              } else {
                return throwError(null);
              }
            })
          );
        } else {
          return of(result);
        }
        break;
      case AliasFilterType.assetSearchQuery:
      case AliasFilterType.deviceSearchQuery:
      case AliasFilterType.entityViewSearchQuery:
        result.stateEntity = filter.rootStateEntity;
        if (result.stateEntity && stateEntityId) {
          rootEntityType = stateEntityId.entityType;
          rootEntityId = stateEntityId.id;
        } else if (!result.stateEntity) {
          rootEntityType = filter.rootEntity.entityType;
          rootEntityId = filter.rootEntity.id;
        }
        if (rootEntityType && rootEntityId) {
          const searchQueryRootEntityId = this.resolveAliasEntityId(rootEntityType, rootEntityId);
          const searchQuery: EntitySearchQuery = {
            parameters: {
              rootId: searchQueryRootEntityId.id,
              rootType: searchQueryRootEntityId.entityType as EntityType,
              direction: filter.direction
            },
            relationType: filter.relationType
          };
          let findByQueryObservable: Observable<Array<BaseData<EntityId>>>;
          if (filter.type === AliasFilterType.assetSearchQuery) {
            const assetSearchQuery = searchQuery as AssetSearchQuery;
            assetSearchQuery.assetTypes = filter.assetTypes;
            findByQueryObservable = this.assetService.findByQuery(assetSearchQuery, true, true);
          } else if (filter.type === AliasFilterType.deviceSearchQuery) {
            const deviceSearchQuery = searchQuery as DeviceSearchQuery;
            deviceSearchQuery.deviceTypes = filter.deviceTypes;
            findByQueryObservable = this.deviceService.findByQuery(deviceSearchQuery, true, true);
          } else if (filter.type === AliasFilterType.entityViewSearchQuery) {
            const entityViewSearchQuery = searchQuery as EntityViewSearchQuery;
            entityViewSearchQuery.entityViewTypes = filter.entityViewTypes;
            findByQueryObservable = this.entityViewService.findByQuery(entityViewSearchQuery, true, true);
          }
          return findByQueryObservable.pipe(
            map((entities) => {
              if (entities && entities.length || !failOnEmpty) {
                if (isDefined(maxItems) && maxItems > 0 && entities) {
                  const limit = Math.min(entities.length, maxItems);
                  entities.length = limit;
                }
                result.entities = this.entitiesToEntitiesInfo(entities);
                return result;
              } else {
                throw Error();
              }
            })
          );
        } else {
          return of(result);
        }
        break;
    }
  }

  private entitiesToEntitiesInfo(entities: Array<BaseData<EntityId>>): Array<EntityInfo> {
    const entitiesInfo = [];
    if (entities) {
      entities.forEach((entity) => {
        entitiesInfo.push(this.entityToEntityInfo(entity));
      });
    }
    return entitiesInfo;
  }

  private entityToEntityInfo(entity: BaseData<EntityId>): EntityInfo {
    return {
      origEntity: entity,
      name: entity.name,
      label: (entity as any).label ? (entity as any).label : '',
      entityType: entity.id.entityType as EntityType,
      id: entity.id.id,
      entityDescription: (entity as any).additionalInfo ? (entity as any).additionalInfo.description : ''
    };
  }

  private entityRelationInfosToEntitiesInfo(entityRelations: Array<EntityRelationInfo>,
                                            direction: EntitySearchDirection): Observable<Array<EntityInfo>> {
    if (entityRelations) {
      const tasks: Observable<EntityInfo>[] = [];
      entityRelations.forEach((entityRelation) => {
        tasks.push(this.entityRelationInfoToEntityInfo(entityRelation, direction));
      });
      return forkJoin(tasks);
    } else {
      return of([]);
    }
  }

  private entityRelationInfoToEntityInfo(entityRelationInfo: EntityRelationInfo, direction: EntitySearchDirection): Observable<EntityInfo> {
    const entityId = direction === EntitySearchDirection.FROM ? entityRelationInfo.to : entityRelationInfo.from;
    return this.getEntity(entityId.entityType as EntityType, entityId.id, true, true).pipe(
      map((entity) => {
        return this.entityToEntityInfo(entity);
      })
    );
  }

  private getStateEntityId(filter: EntityAliasFilter, stateParams: StateParams): EntityId {
    let entityId = null;
    if (stateParams) {
      if (filter.stateEntityParamName && filter.stateEntityParamName.length) {
        if (stateParams[filter.stateEntityParamName]) {
          entityId = stateParams[filter.stateEntityParamName].entityId;
        }
      } else {
        entityId = stateParams.entityId;
      }
    }
    if (!entityId) {
      entityId = filter.defaultStateEntity;
    }
    if (entityId) {
      entityId = this.resolveAliasEntityId(entityId.entityType, entityId.id);
    }
    return entityId;
  }

  private resolveAliasEntityId(entityType: EntityType | AliasEntityType, id: string): EntityId {
    const entityId: EntityId = {
      entityType,
      id
    };
    if (entityType === AliasEntityType.CURRENT_CUSTOMER) {
      const authUser = getCurrentAuthUser(this.store);
      entityId.entityType = EntityType.CUSTOMER;
      if (authUser.authority === Authority.CUSTOMER_USER) {
        entityId.id = authUser.customerId;
      }
    }
    return entityId;
  }

  private createDatasourcesFromSubscriptionInfo(subscriptionInfo: SubscriptionInfo): Observable<Array<Datasource>> {
    subscriptionInfo = this.validateSubscriptionInfo(subscriptionInfo);
    if (subscriptionInfo.type === DatasourceType.entity) {
      return this.resolveEntitiesFromSubscriptionInfo(subscriptionInfo).pipe(
        map((entities) => {
          const datasources = new Array<Datasource>();
          entities.forEach((entity) => {
            datasources.push(this.createDatasourceFromSubscription(subscriptionInfo, entity));
          });
          return datasources;
        })
      );
    } else if (subscriptionInfo.type === DatasourceType.function) {
      return of([this.createDatasourceFromSubscription(subscriptionInfo)]);
    } else {
      return of([]);
    }
  }

  private resolveEntitiesFromSubscriptionInfo(subscriptionInfo: SubscriptionInfo): Observable<Array<BaseData<EntityId>>> {
    if (subscriptionInfo.entityId) {
      if (subscriptionInfo.entityName) {
        const entity: BaseData<EntityId> = {
          id: {id: subscriptionInfo.entityId, entityType: subscriptionInfo.entityType},
          name: subscriptionInfo.entityName
        };
        return of([entity]);
      } else {
        return this.getEntity(subscriptionInfo.entityType, subscriptionInfo.entityId,
          true, true).pipe(
          map((entity) => [entity]),
          catchError(e => of([]))
        );
      }
    } else if (subscriptionInfo.entityName || subscriptionInfo.entityNamePrefix || subscriptionInfo.entityIds) {
      let entitiesObservable: Observable<Array<BaseData<EntityId>>>;
      if (subscriptionInfo.entityName) {
        entitiesObservable = this.getEntitiesByNameFilter(subscriptionInfo.entityType, subscriptionInfo.entityName,
          1, null, true, true);
      } else if (subscriptionInfo.entityNamePrefix) {
        entitiesObservable = this.getEntitiesByNameFilter(subscriptionInfo.entityType, subscriptionInfo.entityNamePrefix,
          100, null, true, true);
      } else if (subscriptionInfo.entityIds) {
        entitiesObservable = this.getEntities(subscriptionInfo.entityType, subscriptionInfo.entityIds, true, true);
      }
      return entitiesObservable.pipe(
        catchError(e => of([]))
      );
    } else {
      return of([]);
    }
  }

  private validateSubscriptionInfo(subscriptionInfo: SubscriptionInfo): SubscriptionInfo {
    // @ts-ignore
    if (subscriptionInfo.type === 'device') {
      subscriptionInfo.type = DatasourceType.entity;
      subscriptionInfo.entityType = EntityType.DEVICE;
      if (subscriptionInfo.deviceId) {
        subscriptionInfo.entityId = subscriptionInfo.deviceId;
      } else if (subscriptionInfo.deviceName) {
        subscriptionInfo.entityName = subscriptionInfo.deviceName;
      } else if (subscriptionInfo.deviceNamePrefix) {
        subscriptionInfo.entityNamePrefix = subscriptionInfo.deviceNamePrefix;
      } else if (subscriptionInfo.deviceIds) {
        subscriptionInfo.entityIds = subscriptionInfo.deviceIds;
      }
    }
    return subscriptionInfo;
  }

  private createDatasourceFromSubscription(subscriptionInfo: SubscriptionInfo, entity?: BaseData<EntityId>): Datasource {
    let datasource: Datasource;
    if (subscriptionInfo.type === DatasourceType.entity) {
      datasource = {
        type: subscriptionInfo.type,
        entityName: entity.name,
        name: entity.name,
        entityType: subscriptionInfo.entityType,
        entityId: entity.id.id,
        dataKeys: []
      };
    } else if (subscriptionInfo.type === DatasourceType.function) {
      datasource = {
        type: subscriptionInfo.type,
        name: subscriptionInfo.name || DatasourceType.function,
        dataKeys: []
      };
    }
    if (subscriptionInfo.timeseries) {
      this.createDatasourceKeys(subscriptionInfo.timeseries, DataKeyType.timeseries, datasource);
    }
    if (subscriptionInfo.attributes) {
      this.createDatasourceKeys(subscriptionInfo.attributes, DataKeyType.attribute, datasource);
    }
    if (subscriptionInfo.functions) {
      this.createDatasourceKeys(subscriptionInfo.functions, DataKeyType.function, datasource);
    }
    if (subscriptionInfo.alarmFields) {
      this.createDatasourceKeys(subscriptionInfo.alarmFields, DataKeyType.alarm, datasource);
    }
    return datasource;
  }

  private createDatasourceKeys(keyInfos: Array<KeyInfo>, type: DataKeyType, datasource: Datasource) {
    keyInfos.forEach((keyInfo) => {
      const dataKey = this.utils.createKey(keyInfo, type);
      datasource.dataKeys.push(dataKey);
    });
  }
}

///
/// Copyright © 2016-2021 The Thingsboard Authors
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
import { EMPTY, forkJoin, Observable, of, throwError } from 'rxjs';
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
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Authority } from '@shared/models/authority.enum';
import { Tenant } from '@shared/models/tenant.model';
import { catchError, concatMap, expand, map, mergeMap, toArray } from 'rxjs/operators';
import { Customer } from '@app/shared/models/customer.model';
import { AssetService } from '@core/http/asset.service';
import { EntityViewService } from '@core/http/entity-view.service';
import { AttributeScope, DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { RuleChainService } from '@core/http/rule-chain.service';
import { AliasInfo, StateParams, SubscriptionInfo } from '@core/api/widget-api.models';
import { DataKey, Datasource, DatasourceType, KeyInfo } from '@app/shared/models/widget.models';
import { UtilsService } from '@core/services/utils.service';
import { AliasFilterType, EntityAlias, EntityAliasFilter, EntityAliasFilterResult } from '@shared/models/alias.models';
import {
  EntitiesKeysByQuery,
  entityFields,
  EntityInfo,
  ImportEntitiesResultInfo,
  ImportEntityData
} from '@shared/models/entity.models';
import { EntityRelationService } from '@core/http/entity-relation.service';
import { deepClone, isDefined, isDefinedAndNotNull } from '@core/utils';
import { Asset } from '@shared/models/asset.models';
import { Device, DeviceCredentialsType } from '@shared/models/device.models';
import { AttributeService } from '@core/http/attribute.service';
import {
  AlarmData,
  AlarmDataQuery,
  createDefaultEntityDataPageLink,
  defaultEntityDataPageLink,
  EntityData,
  EntityDataQuery,
  entityDataToEntityInfo,
  EntityFilter,
  entityInfoFields,
  EntityKey,
  EntityKeyType,
  EntityKeyValueType,
  FilterPredicateType,
  singleEntityDataPageLink,
  StringOperation
} from '@shared/models/query/query.models';
import { alarmFields } from '@shared/models/alarm.models';

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
    private attributeService: AttributeService,
    private utils: UtilsService
  ) { }

  private getEntityObservable(entityType: EntityType, entityId: string,
                              config?: RequestConfig): Observable<BaseData<EntityId>> {

    let observable: Observable<BaseData<EntityId>>;
    switch (entityType) {
      case EntityType.DEVICE:
        observable = this.deviceService.getDevice(entityId, config);
        break;
      case EntityType.ASSET:
        observable = this.assetService.getAsset(entityId, config);
        break;
      case EntityType.ENTITY_VIEW:
        observable = this.entityViewService.getEntityView(entityId, config);
        break;
      case EntityType.TENANT:
        observable = this.tenantService.getTenant(entityId, config);
        break;
      case EntityType.CUSTOMER:
        observable = this.customerService.getCustomer(entityId, config);
        break;
      case EntityType.DASHBOARD:
        observable = this.dashboardService.getDashboardInfo(entityId, config);
        break;
      case EntityType.USER:
        observable = this.userService.getUser(entityId, config);
        break;
      case EntityType.RULE_CHAIN:
        observable = this.ruleChainService.getRuleChain(entityId, config);
        break;
      case EntityType.ALARM:
        console.error('Get Alarm Entity is not implemented!');
        break;
    }
    return observable;
  }
  public getEntity(entityType: EntityType, entityId: string,
                   config?: RequestConfig): Observable<BaseData<EntityId>> {
    const entityObservable = this.getEntityObservable(entityType, entityId, config);
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
                                config?: RequestConfig): Observable<Array<BaseData<EntityId>>> {
    let observable: Observable<Array<BaseData<EntityId>>>;
    switch (entityType) {
      case EntityType.DEVICE:
        observable = this.deviceService.getDevices(entityIds, config);
        break;
      case EntityType.ASSET:
        observable = this.assetService.getAssets(entityIds, config);
        break;
      case EntityType.ENTITY_VIEW:
        observable = this.getEntitiesByIdsObservable(
          (id) => this.entityViewService.getEntityView(id, config),
          entityIds);
        break;
      case EntityType.TENANT:
        observable = this.getEntitiesByIdsObservable(
          (id) => this.tenantService.getTenant(id, config),
          entityIds);
        break;
      case EntityType.CUSTOMER:
        observable = this.getEntitiesByIdsObservable(
          (id) => this.customerService.getCustomer(id, config),
          entityIds);
        break;
      case EntityType.DASHBOARD:
        observable = this.getEntitiesByIdsObservable(
          (id) => this.dashboardService.getDashboardInfo(id, config),
          entityIds);
        break;
      case EntityType.USER:
        observable = this.getEntitiesByIdsObservable(
          (id) => this.userService.getUser(id, config),
          entityIds);
        break;
      case EntityType.ALARM:
        console.error('Get Alarm Entity is not implemented!');
        break;
    }
    return observable;
  }

  public getEntities(entityType: EntityType, entityIds: Array<string>,
                     config?: RequestConfig): Observable<Array<BaseData<EntityId>>> {
    const entitiesObservable = this.getEntitiesObservable(entityType, entityIds, config);
    if (entitiesObservable) {
      return entitiesObservable;
    } else {
      return throwError(null);
    }
  }

  private getSingleTenantByPageLinkObservable(pageLink: PageLink,
                                              config?: RequestConfig): Observable<PageData<Tenant>> {
    const authUser = getCurrentAuthUser(this.store);
    const tenantId = authUser.tenantId;
    return this.tenantService.getTenant(tenantId, config).pipe(
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
                                                config?: RequestConfig): Observable<PageData<Customer>> {
    const authUser = getCurrentAuthUser(this.store);
    const customerId = authUser.customerId;
    return this.customerService.getCustomer(customerId, config).pipe(
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
                                          config?: RequestConfig): Observable<PageData<BaseData<EntityId>>> {
    let entitiesObservable: Observable<PageData<BaseData<EntityId>>>;
    const authUser = getCurrentAuthUser(this.store);
    const customerId = authUser.customerId;
    switch (entityType) {
      case EntityType.DEVICE:
        pageLink.sortOrder.property = 'name';
        if (authUser.authority === Authority.CUSTOMER_USER) {
          entitiesObservable = this.deviceService.getCustomerDeviceInfos(customerId, pageLink, subType, config);
        } else {
          entitiesObservable = this.deviceService.getTenantDeviceInfos(pageLink, subType, config);
        }
        break;
      case EntityType.ASSET:
        pageLink.sortOrder.property = 'name';
        if (authUser.authority === Authority.CUSTOMER_USER) {
          entitiesObservable = this.assetService.getCustomerAssetInfos(customerId, pageLink, subType, config);
        } else {
          entitiesObservable = this.assetService.getTenantAssetInfos(pageLink, subType, config);
        }
        break;
      case EntityType.ENTITY_VIEW:
        pageLink.sortOrder.property = 'name';
        if (authUser.authority === Authority.CUSTOMER_USER) {
          entitiesObservable = this.entityViewService.getCustomerEntityViewInfos(customerId, pageLink,
            subType, config);
        } else {
          entitiesObservable = this.entityViewService.getTenantEntityViewInfos(pageLink, subType, config);
        }
        break;
      case EntityType.TENANT:
        pageLink.sortOrder.property = 'title';
        if (authUser.authority === Authority.TENANT_ADMIN) {
          entitiesObservable = this.getSingleTenantByPageLinkObservable(pageLink, config);
        } else {
          entitiesObservable = this.tenantService.getTenants(pageLink, config);
        }
        break;
      case EntityType.CUSTOMER:
        pageLink.sortOrder.property = 'title';
        if (authUser.authority === Authority.CUSTOMER_USER) {
          entitiesObservable = this.getSingleCustomerByPageLinkObservable(pageLink, config);
        } else {
          entitiesObservable = this.customerService.getCustomers(pageLink, config);
        }
        break;
      case EntityType.RULE_CHAIN:
        pageLink.sortOrder.property = 'name';
        entitiesObservable = this.ruleChainService.getRuleChains(pageLink, config);
        break;
      case EntityType.DASHBOARD:
        pageLink.sortOrder.property = 'title';
        if (authUser.authority === Authority.CUSTOMER_USER) {
          entitiesObservable = this.dashboardService.getCustomerDashboards(customerId, pageLink, config);
        } else {
          entitiesObservable = this.dashboardService.getTenantDashboards(pageLink, config);
        }
        break;
      case EntityType.USER:
        pageLink.sortOrder.property = 'email';
        entitiesObservable = this.userService.getUsers(pageLink);
        break;
      case EntityType.ALARM:
        console.error('Get Alarm Entities is not implemented!');
        break;
    }
    return entitiesObservable;
  }

  private getEntitiesByPageLink(entityType: EntityType, pageLink: PageLink, subType: string = '',
                                config?: RequestConfig): Observable<Array<BaseData<EntityId>>> {
    const entitiesObservable: Observable<PageData<BaseData<EntityId>>> =
      this.getEntitiesByPageLinkObservable(entityType, pageLink, subType, config);
    if (entitiesObservable) {
      return entitiesObservable.pipe(
        expand((data) => {
          if (data.hasNext) {
            pageLink.page += 1;
            return this.getEntitiesByPageLinkObservable(entityType, pageLink, subType, config);
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
                                 config?: RequestConfig): Observable<Array<BaseData<EntityId>>> {
    const pageLink = new PageLink(pageSize, 0, entityNameFilter, {
      property: 'name',
      direction: Direction.ASC
    });
    if (pageSize === -1) { // all
      pageLink.pageSize = 100;
      return this.getEntitiesByPageLink(entityType, pageLink, subType, config).pipe(
        map((data) => data && data.length ? data : null)
      );
    } else {
      const entitiesObservable: Observable<PageData<BaseData<EntityId>>> =
        this.getEntitiesByPageLinkObservable(entityType, pageLink, subType, config);
      if (entitiesObservable) {
        return entitiesObservable.pipe(
          map((data) => data && data.data.length ? data.data : null)
        );
      } else {
        return of(null);
      }
    }
  }

  public findEntityDataByQuery(query: EntityDataQuery, config?: RequestConfig): Observable<PageData<EntityData>> {
    return this.http.post<PageData<EntityData>>('/api/entitiesQuery/find', query, defaultHttpOptionsFromConfig(config));
  }

  public findEntityKeysByQuery(query: EntityDataQuery, attributes = true, timeseries = true,
                               config?: RequestConfig): Observable<EntitiesKeysByQuery> {
    return this.http.post<EntitiesKeysByQuery>(
      `/api/entitiesQuery/find/keys?attributes=${attributes}&timeseries=${timeseries}`,
      query, defaultHttpOptionsFromConfig(config));
  }

  public findAlarmDataByQuery(query: AlarmDataQuery, config?: RequestConfig): Observable<PageData<AlarmData>> {
    return this.http.post<PageData<AlarmData>>('/api/alarmsQuery/find', query, defaultHttpOptionsFromConfig(config));
  }

  public findEntityInfosByFilterAndName(filter: EntityFilter,
                                        searchText: string, config?: RequestConfig): Observable<PageData<EntityInfo>> {
    const nameField: EntityKey = {
      type: EntityKeyType.ENTITY_FIELD,
      key: 'name'
    };
    const query: EntityDataQuery = {
      entityFilter: filter,
      pageLink: {
        pageSize: 10,
        page: 0,
        sortOrder: {
          key: nameField,
          direction: Direction.ASC
        }
      },
      entityFields: entityInfoFields,
      keyFilters: searchText && searchText.length ? [
        {
          key: nameField,
          valueType: EntityKeyValueType.STRING,
          value: null,
          predicate: {
            type: FilterPredicateType.STRING,
            operation: StringOperation.STARTS_WITH,
            ignoreCase: true,
            value: {
              defaultValue: searchText
            }
          }
        }
      ] : null
    };
    return this.findEntityDataByQuery(query, config).pipe(
      map((data) => {
        const entityInfos = data.data.map(entityData => entityDataToEntityInfo(entityData));
        return {
          data: entityInfos,
          hasNext: data.hasNext,
          totalElements: data.totalElements,
          totalPages: data.totalPages
        };
      })
    );
  }

  public findSingleEntityInfoByEntityFilter(filter: EntityFilter, config?: RequestConfig): Observable<EntityInfo> {
    const query: EntityDataQuery = {
      entityFilter: filter,
      pageLink: createDefaultEntityDataPageLink(1),
      entityFields: entityInfoFields
    };
    return this.findEntityDataByQuery(query, config).pipe(
      map((data) => {
        if (data.data.length) {
          const entityData = data.data[0];
          return entityDataToEntityInfo(entityData);
        } else {
          return null;
        }
      })
    );
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
        entityTypes.push(EntityType.USER);
        entityTypes.push(EntityType.DASHBOARD);
        if (useAliasEntityTypes) {
          entityTypes.push(AliasEntityType.CURRENT_CUSTOMER);
          entityTypes.push(AliasEntityType.CURRENT_TENANT);
        }
        break;
      case Authority.CUSTOMER_USER:
        entityTypes.push(EntityType.DEVICE);
        entityTypes.push(EntityType.ASSET);
        entityTypes.push(EntityType.ENTITY_VIEW);
        entityTypes.push(EntityType.CUSTOMER);
        entityTypes.push(EntityType.USER);
        entityTypes.push(EntityType.DASHBOARD);
        if (useAliasEntityTypes) {
          entityTypes.push(AliasEntityType.CURRENT_CUSTOMER);
        }
        break;
    }
    if (useAliasEntityTypes) {
      entityTypes.push(AliasEntityType.CURRENT_USER);
      if (authUser.authority !== Authority.SYS_ADMIN) {
        entityTypes.push(AliasEntityType.CURRENT_USER_OWNER);
      }
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

  private getEntityFieldKeys(entityType: EntityType, searchText: string = ''): Array<string> {
    const entityFieldKeys: string[] = [entityFields.createdTime.keyName];
    const query = searchText.toLowerCase();
    switch (entityType) {
      case EntityType.USER:
        entityFieldKeys.push(entityFields.name.keyName);
        entityFieldKeys.push(entityFields.email.keyName);
        entityFieldKeys.push(entityFields.firstName.keyName);
        entityFieldKeys.push(entityFields.lastName.keyName);
        break;
      case EntityType.TENANT:
      case EntityType.CUSTOMER:
        entityFieldKeys.push(entityFields.title.keyName);
        entityFieldKeys.push(entityFields.email.keyName);
        entityFieldKeys.push(entityFields.country.keyName);
        entityFieldKeys.push(entityFields.state.keyName);
        entityFieldKeys.push(entityFields.city.keyName);
        entityFieldKeys.push(entityFields.address.keyName);
        entityFieldKeys.push(entityFields.address2.keyName);
        entityFieldKeys.push(entityFields.zip.keyName);
        entityFieldKeys.push(entityFields.phone.keyName);
        break;
      case EntityType.ENTITY_VIEW:
        entityFieldKeys.push(entityFields.name.keyName);
        entityFieldKeys.push(entityFields.type.keyName);
        break;
      case EntityType.DEVICE:
      case EntityType.ASSET:
        entityFieldKeys.push(entityFields.name.keyName);
        entityFieldKeys.push(entityFields.type.keyName);
        entityFieldKeys.push(entityFields.label.keyName);
        break;
      case EntityType.DASHBOARD:
        entityFieldKeys.push(entityFields.title.keyName);
        break;
      case EntityType.API_USAGE_STATE:
        entityFieldKeys.push(entityFields.name.keyName);
        break;
    }
    return query ? entityFieldKeys.filter((entityField) => entityField.toLowerCase().indexOf(query) === 0) : entityFieldKeys;
  }

  private getAlarmKeys(searchText: string = ''): Array<string> {
    const alarmKeys: string[] = Object.keys(alarmFields);
    const query = searchText.toLowerCase();
    return query ? alarmKeys.filter((alarmField) => alarmField.toLowerCase().indexOf(query) === 0) : alarmKeys;
  }

  public getEntityKeys(entityId: EntityId, query: string, type: DataKeyType,
                       config?: RequestConfig): Observable<Array<string>> {
    if (type === DataKeyType.entityField) {
      return of(this.getEntityFieldKeys(entityId.entityType as EntityType, query));
    } else if (type === DataKeyType.alarm) {
      return of(this.getAlarmKeys(query));
    }
    let url = `/api/plugins/telemetry/${entityId.entityType}/${entityId.id}/keys/`;
    if (type === DataKeyType.timeseries) {
      url += 'timeseries';
    } else if (type === DataKeyType.attribute) {
      url += 'attributes';
    }
    return this.http.get<Array<string>>(url,
      defaultHttpOptionsFromConfig(config))
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

  public getEntityKeysByEntityFilter(filter: EntityFilter, types: DataKeyType[], config?: RequestConfig): Observable<Array<DataKey>> {
    if (!types.length) {
      return of([]);
    }
    let entitiesKeysByQuery$: Observable<EntitiesKeysByQuery>;
    if (filter !== null && types.some(type => [DataKeyType.timeseries, DataKeyType.attribute].includes(type))) {
      const dataQuery = {
        entityFilter: filter,
        pageLink: createDefaultEntityDataPageLink(100),
      };
      entitiesKeysByQuery$ = this.findEntityKeysByQuery(dataQuery, types.includes(DataKeyType.attribute),
        types.includes(DataKeyType.timeseries), config);
    } else {
      entitiesKeysByQuery$ = of({
        attribute: [],
        timeseries: [],
        entityTypes: [],
      });
    }
    return entitiesKeysByQuery$.pipe(
      map((entitiesKeys) => {
        const dataKeys: Array<DataKey> = [];
        types.forEach(type => {
          let keys: Array<string>;
          switch (type) {
            case DataKeyType.entityField:
              if (entitiesKeys.entityTypes.length) {
                const entitiesFields = [];
                entitiesKeys.entityTypes.forEach(entityType => entitiesFields.push(...this.getEntityFieldKeys(entityType)));
                keys = Array.from(new Set(entitiesFields));
              }
              break;
            case DataKeyType.alarm:
              keys = this.getAlarmKeys();
              break;
            case DataKeyType.attribute:
            case DataKeyType.timeseries:
              if (entitiesKeys[type].length) {
                keys = entitiesKeys[type];
              }
              break;
          }
          if (keys) {
            dataKeys.push(...keys.map(key => {
              return {name: key, type};
            }));
          }
        });
        return dataKeys;
      })
    );
  }

  public createDatasourcesFromSubscriptionsInfo(subscriptionsInfo: Array<SubscriptionInfo>): Array<Datasource> {
    const datasources = subscriptionsInfo.map(subscriptionInfo => this.createDatasourceFromSubscriptionInfo(subscriptionInfo));
    this.utils.generateColors(datasources);
    return datasources;
  }

  public createAlarmSourceFromSubscriptionInfo(subscriptionInfo: SubscriptionInfo): Datasource {
    if (subscriptionInfo.entityId && subscriptionInfo.entityType) {
      const alarmSource = this.createDatasourceFromSubscriptionInfo(subscriptionInfo);
      this.utils.generateColors([alarmSource]);
      return alarmSource;
    } else {
      throw new Error('Can\'t crate alarm source without entityId information!');
    }
  }

  public resolveAlias(entityAlias: EntityAlias, stateParams: StateParams): Observable<AliasInfo> {
    const filter = entityAlias.filter;
    return this.resolveAliasFilter(filter, stateParams).pipe(
      mergeMap((result) => {
        const aliasInfo: AliasInfo = {
          alias: entityAlias.alias,
          entityFilter: result.entityFilter,
          stateEntity: result.stateEntity,
          entityParamName: result.entityParamName,
          resolveMultiple: filter.resolveMultiple
        };
        aliasInfo.currentEntity = null;
        if (!aliasInfo.resolveMultiple && aliasInfo.entityFilter) {
          return this.findSingleEntityInfoByEntityFilter(aliasInfo.entityFilter,
            {ignoreLoading: true, ignoreErrors: true}).pipe(
            map((entity) => {
              aliasInfo.currentEntity = entity;
              return aliasInfo;
            })
          );
        }
        return of(aliasInfo);
      })
    );
  }

  public resolveAliasFilter(filter: EntityAliasFilter, stateParams: StateParams): Observable<EntityAliasFilterResult> {
    const result: EntityAliasFilterResult = {
      entityFilter: null,
      stateEntity: false
    };
    if (filter.stateEntityParamName && filter.stateEntityParamName.length) {
      result.entityParamName = filter.stateEntityParamName;
    }
    const stateEntityInfo = this.getStateEntityInfo(filter, stateParams);
    const stateEntityId = stateEntityInfo.entityId;
    switch (filter.type) {
      case AliasFilterType.singleEntity:
        const aliasEntityId = this.resolveAliasEntityId(filter.singleEntity.entityType, filter.singleEntity.id);
        result.entityFilter = {
          type: AliasFilterType.singleEntity,
          singleEntity: aliasEntityId
        };
        return of(result);
      case AliasFilterType.entityList:
        result.entityFilter = deepClone(filter);
        return of(result);
      case AliasFilterType.entityName:
        result.entityFilter = deepClone(filter);
        return of(result);
      case AliasFilterType.stateEntity:
        result.stateEntity = true;
        if (stateEntityId) {
          result.entityFilter = {
            type: AliasFilterType.singleEntity,
            singleEntity: stateEntityId
          };
        }
        return of(result);
      case AliasFilterType.assetType:
        result.entityFilter = deepClone(filter);
        return of(result);
      case AliasFilterType.deviceType:
        result.entityFilter = deepClone(filter);
        return of(result);
      case AliasFilterType.entityViewType:
        result.entityFilter = deepClone(filter);
        return of(result);
      case AliasFilterType.apiUsageState:
        result.entityFilter = deepClone(filter);
        return of(result);
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
          result.entityFilter = deepClone(filter);
          result.entityFilter.rootEntity = relationQueryRootEntityId;
          return of(result);
        } else {
          return of(result);
        }
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
          result.entityFilter = deepClone(filter);
          result.entityFilter.rootEntity = searchQueryRootEntityId;
          return of(result);
        } else {
          return of(result);
        }
    }
  }

  public checkEntityAlias(entityAlias: EntityAlias): Observable<boolean> {
    return this.resolveAliasFilter(entityAlias.filter, null).pipe(
      map((result) => {
        if (result.stateEntity) {
          return true;
        } else {
          return isDefinedAndNotNull(result.entityFilter);
        }
      }),
      catchError(err => of(false))
    );
  }

  public saveEntityParameters(entityType: EntityType, entityData: ImportEntityData, update: boolean,
                              config?: RequestConfig): Observable<ImportEntitiesResultInfo> {
    let saveEntityObservable: Observable<BaseData<EntityId>>;
    switch (entityType) {
      case EntityType.DEVICE:
        const device: Device = {
          name: entityData.name,
          type: entityData.type,
          label: entityData.label,
          additionalInfo: {
            description: entityData.description
          }
        };
        if (entityData.gateway !== null) {
          device.additionalInfo = {
            ...device.additionalInfo,
            gateway: entityData.gateway
          };
        }
        saveEntityObservable = this.deviceService.saveDevice(device, config);
        break;
      case EntityType.ASSET:
        const asset: Asset = {
          name: entityData.name,
          type: entityData.type,
          label: entityData.label,
          additionalInfo: {
            description: entityData.description
          }
        };
        saveEntityObservable = this.assetService.saveAsset(asset, config);
        break;
    }
    return saveEntityObservable.pipe(
      mergeMap((entity) => {
        return this.saveEntityData(entity.id, entityData, config).pipe(
          map(() => {
            return { create: { entity: 1 } } as ImportEntitiesResultInfo;
          }),
          catchError(err => of({ error: { entity: 1 } } as ImportEntitiesResultInfo))
        );
      }),
      catchError(err => {
        if (update) {
          let findEntityObservable: Observable<BaseData<EntityId>>;
          switch (entityType) {
            case EntityType.DEVICE:
              findEntityObservable = this.deviceService.findByName(entityData.name, config);
              break;
            case EntityType.ASSET:
              findEntityObservable = this.assetService.findByName(entityData.name, config);
              break;
          }
          return findEntityObservable.pipe(
            mergeMap((entity) => {
              const tasks: Observable<any>[] = [];
              const result: Device & Asset = entity as (Device | Asset);
              const additionalInfo = result.additionalInfo || {};
              if (result.label !== entityData.label ||
                 result.type !== entityData.type ||
                 additionalInfo.description !== entityData.description ||
                 (result.id.entityType === EntityType.DEVICE && (additionalInfo.gateway !== entityData.gateway)) ) {
                result.label = entityData.label;
                result.type = entityData.type;
                result.additionalInfo = additionalInfo;
                result.additionalInfo.description = entityData.description;
                if (result.id.entityType === EntityType.DEVICE) {
                  result.additionalInfo.gateway = entityData.gateway;
                }
                if (result.id.entityType === EntityType.DEVICE && result.deviceProfileId) {
                  delete result.deviceProfileId;
                }
                switch (result.id.entityType) {
                  case EntityType.DEVICE:
                    tasks.push(this.deviceService.saveDevice(result, config));
                    break;
                  case EntityType.ASSET:
                    tasks.push(this.assetService.saveAsset(result, config));
                    break;
                }
              }
              tasks.push(this.saveEntityData(entity.id, entityData, config));
              return forkJoin(tasks).pipe(
                map(() => {
                  return { update: { entity: 1 } } as ImportEntitiesResultInfo;
                }),
                catchError(updateError => of({ error: { entity: 1 } } as ImportEntitiesResultInfo))
              );
            }),
            catchError(findErr => of({ error: { entity: 1 } } as ImportEntitiesResultInfo))
          );
        } else {
          return of({ error: { entity: 1 } } as ImportEntitiesResultInfo);
        }
      })
    );
  }

  public saveEntityData(entityId: EntityId, entityData: ImportEntityData, config?: RequestConfig): Observable<any> {
    const observables: Observable<string>[] = [];
    let observable: Observable<string>;
    if (entityData.accessToken && entityData.accessToken !== '') {
      observable = this.deviceService.getDeviceCredentials(entityId.id, false, config).pipe(
        mergeMap((credentials) => {
          credentials.credentialsId = entityData.accessToken;
          credentials.credentialsType = DeviceCredentialsType.ACCESS_TOKEN;
          credentials.credentialsValue = null;
          return this.deviceService.saveDeviceCredentials(credentials, config).pipe(
            map(() => 'ok'),
            catchError(err => of('error'))
          );
        })
      );
      observables.push(observable);
    }
    if (entityData.attributes.shared && entityData.attributes.shared.length) {
      observable = this.attributeService.saveEntityAttributes(entityId, AttributeScope.SHARED_SCOPE,
        entityData.attributes.shared, config).pipe(
        map(() => 'ok'),
        catchError(err => of('error'))
      );
      observables.push(observable);
    }
    if (entityData.attributes.server && entityData.attributes.server.length) {
      observable = this.attributeService.saveEntityAttributes(entityId, AttributeScope.SERVER_SCOPE,
        entityData.attributes.server, config).pipe(
        map(() => 'ok'),
        catchError(err => of('error'))
      );
      observables.push(observable);
    }
    if (entityData.timeseries && entityData.timeseries.length) {
      observable = this.attributeService.saveEntityTimeseries(entityId, 'time', entityData.timeseries, config).pipe(
        map(() => 'ok'),
        catchError(err => of('error'))
      );
      observables.push(observable);
    }
    if (observables.length) {
      return forkJoin(observables).pipe(
        map((response) => {
          const hasError = response.filter((status) => status === 'error').length > 0;
          if (hasError) {
            throw Error();
          } else {
            return response;
          }
        })
      );
    } else {
      return of(null);
    }
  }

  private getStateEntityInfo(filter: EntityAliasFilter, stateParams: StateParams): {entityId: EntityId} {
    let entityId: EntityId = null;
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
    return {entityId};
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
    } else if (entityType === AliasEntityType.CURRENT_TENANT){
      const authUser =  getCurrentAuthUser(this.store);
      entityId.entityType = EntityType.TENANT;
      entityId.id = authUser.tenantId;
    } else if (entityType === AliasEntityType.CURRENT_USER){
      const authUser =  getCurrentAuthUser(this.store);
      entityId.entityType = EntityType.USER;
      entityId.id = authUser.userId;
    } else if (entityType === AliasEntityType.CURRENT_USER_OWNER){
      const authUser =  getCurrentAuthUser(this.store);
      if (authUser.authority === Authority.TENANT_ADMIN) {
        entityId.entityType = EntityType.TENANT;
        entityId.id = authUser.tenantId;
      } else if (authUser.authority === Authority.CUSTOMER_USER) {
        entityId.entityType = EntityType.CUSTOMER;
        entityId.id = authUser.customerId;
      }
    }
    return entityId;
  }

  private createDatasourceFromSubscriptionInfo(subscriptionInfo: SubscriptionInfo): Datasource {
    subscriptionInfo = this.validateSubscriptionInfo(subscriptionInfo);
    let datasource: Datasource = null;
    if (subscriptionInfo.type === DatasourceType.entity) {
      datasource = {
        type: subscriptionInfo.type,
        entityName: subscriptionInfo.entityName,
        name: subscriptionInfo.entityName,
        entityType: subscriptionInfo.entityType,
        entityId: subscriptionInfo.entityId,
        dataKeys: []
      };
      this.prepareEntityFilterFromSubscriptionInfo(datasource, subscriptionInfo);
    } else if (subscriptionInfo.type === DatasourceType.function) {
      datasource = {
        type: subscriptionInfo.type,
        name: subscriptionInfo.name || DatasourceType.function,
        dataKeys: []
      };
    }
    if (datasource !== null) {
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
    }
    return datasource;
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

  private prepareEntityFilterFromSubscriptionInfo(datasource: Datasource, subscriptionInfo: SubscriptionInfo) {
    if (subscriptionInfo.entityId) {
      datasource.entityFilter = {
        type: AliasFilterType.singleEntity,
        singleEntity: {
          entityType: subscriptionInfo.entityType,
          id: subscriptionInfo.entityId
        }
      };
      datasource.pageLink = singleEntityDataPageLink;
    } else if (subscriptionInfo.entityName || subscriptionInfo.entityNamePrefix) {
      let nameFilter;
      let pageLink;
      if (isDefined(subscriptionInfo.entityName) && subscriptionInfo.entityName.length) {
        nameFilter = subscriptionInfo.entityName;
        pageLink = deepClone(singleEntityDataPageLink);
      } else {
        nameFilter = subscriptionInfo.entityNamePrefix;
        pageLink = deepClone(defaultEntityDataPageLink);
      }
      datasource.entityFilter = {
        type: AliasFilterType.entityName,
        entityType: subscriptionInfo.entityType,
        entityNameFilter: nameFilter
      };
      datasource.pageLink = pageLink;
    } else if (subscriptionInfo.entityIds) {
      datasource.entityFilter = {
        type: AliasFilterType.entityList,
        entityType: subscriptionInfo.entityType,
        entityList: subscriptionInfo.entityIds
      };
      datasource.pageLink = deepClone(defaultEntityDataPageLink);
    }
  }

  private createDatasourceKeys(keyInfos: Array<KeyInfo>, type: DataKeyType, datasource: Datasource) {
    keyInfos.forEach((keyInfo) => {
      const dataKey = this.utils.createKey(keyInfo, type);
      datasource.dataKeys.push(dataKey);
    });
  }
}

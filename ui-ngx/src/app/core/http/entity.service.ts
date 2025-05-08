///
/// Copyright © 2016-2025 The Thingsboard Authors
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
import { BaseData, HasId } from '@shared/models/base-data';
import { EntityId } from '@shared/models/id/entity-id';
import { DeviceService } from '@core/http/device.service';
import { TenantService } from '@core/http/tenant.service';
import { CustomerService } from '@core/http/customer.service';
import { UserService } from './user.service';
import { DashboardService } from '@core/http/dashboard.service';
import { Direction } from '@shared/models/page/sort-order';
import { PageData } from '@shared/models/page/page-data';
import { getCurrentAuthState, getCurrentAuthUser } from '@core/auth/auth.selectors';
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
import { DataKey, Datasource, DatasourceType, DeprecatedFilter, KeyInfo } from '@app/shared/models/widget.models';
import { UtilsService } from '@core/services/utils.service';
import {
  AliasFilterType,
  edgeAliasFilterTypes,
  EntityAlias,
  EntityAliasFilter,
  EntityAliasFilterResult
} from '@shared/models/alias.models';
import {
  EdgeImportEntityData,
  EntitiesKeysByQuery,
  entityFields,
  EntityInfo,
  ImportEntitiesResultInfo,
  ImportEntityData
} from '@shared/models/entity.models';
import { EntityRelationService } from '@core/http/entity-relation.service';
import { deepClone, generateSecret, guid, isDefined, isDefinedAndNotNull, isNotEmptyStr } from '@core/utils';
import { Asset } from '@shared/models/asset.models';
import { Device, DeviceCredentialsType } from '@shared/models/device.models';
import { AttributeService } from '@core/http/attribute.service';
import {
  AlarmData,
  AlarmDataQuery,
  AlarmFilter,
  AlarmFilterConfig,
  createDefaultEntityDataPageLink,
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
import { OtaPackageService } from '@core/http/ota-package.service';
import { EdgeService } from '@core/http/edge.service';
import { bodyContentEdgeEventActionTypes, Edge, EdgeEvent, EdgeEventType } from '@shared/models/edge.models';
import { RuleChainMetaData, RuleChainType } from '@shared/models/rule-chain.models';
import { WidgetService } from '@core/http/widget.service';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { QueueService } from '@core/http/queue.service';
import { AssetProfileService } from '@core/http/asset-profile.service';
import { NotificationService } from '@core/http/notification.service';
import { TenantProfileService } from '@core/http/tenant-profile.service';
import { NotificationType } from '@shared/models/notification.models';
import { UserId } from '@shared/models/id/user-id';
import { AlarmService } from '@core/http/alarm.service';
import { ResourceService } from '@core/http/resource.service';
import { OAuth2Service } from '@core/http/oauth2.service';
import { MobileAppService } from '@core/http/mobile-app.service';
import { PlatformType } from '@shared/models/oauth2.models';

@Injectable({
  providedIn: 'root'
})
export class EntityService {

  constructor(
    private http: HttpClient,
    private store: Store<AppState>,
    private deviceService: DeviceService,
    private edgeService: EdgeService,
    private assetService: AssetService,
    private entityViewService: EntityViewService,
    private tenantService: TenantService,
    private customerService: CustomerService,
    private userService: UserService,
    private ruleChainService: RuleChainService,
    private dashboardService: DashboardService,
    private entityRelationService: EntityRelationService,
    private attributeService: AttributeService,
    private otaPackageService: OtaPackageService,
    private widgetService: WidgetService,
    private deviceProfileService: DeviceProfileService,
    private tenantProfileService: TenantProfileService,
    private assetProfileService: AssetProfileService,
    private utils: UtilsService,
    private queueService: QueueService,
    private notificationService: NotificationService,
    private alarmService: AlarmService,
    private resourceService: ResourceService,
    private oauth2Service: OAuth2Service,
    private mobileAppService: MobileAppService,
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
      case EntityType.EDGE:
        observable = this.edgeService.getEdge(entityId, config);
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
        observable = this.alarmService.getAlarm(entityId, config);
        break;
      case EntityType.OTA_PACKAGE:
        observable = this.otaPackageService.getOtaPackageInfo(entityId, config);
        break;
      case EntityType.QUEUE:
        observable = this.queueService.getQueueById(entityId, config);
        break;
      case EntityType.QUEUE_STATS:
        observable = this.queueService.getQueueStatisticsById(entityId, config);
        break;
      case EntityType.MOBILE_APP:
        observable = this.mobileAppService.getMobileAppInfoById(entityId, config);
        break;
      case EntityType.MOBILE_APP_BUNDLE:
        observable = this.mobileAppService.getMobileAppBundleInfoById(entityId, config);
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
      case EntityType.EDGE:
        observable = this.edgeService.getEdges(entityIds, config);
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
      case EntityType.DEVICE_PROFILE:
        observable = this.getEntitiesByIdsObservable(
          (id) => this.deviceProfileService.getDeviceProfileInfo(id, config),
          entityIds);
        break;
      case EntityType.TENANT_PROFILE:
        observable = this.tenantProfileService.getTenantProfilesByIds(entityIds, config);
        break;
      case EntityType.ASSET_PROFILE:
        observable = this.getEntitiesByIdsObservable(
          (id) => this.assetProfileService.getAssetProfileInfo(id, config),
          entityIds);
        break;
      case EntityType.WIDGETS_BUNDLE:
        observable = this.getEntitiesByIdsObservable(
          (id) => this.widgetService.getWidgetsBundle(id, config),
          entityIds);
        break;
      case EntityType.NOTIFICATION_TARGET:
        observable = this.notificationService.getNotificationTargetsByIds(entityIds, config);
        break;
      case EntityType.QUEUE_STATS:
        observable = this.queueService.getQueueStatisticsByIds(entityIds, config);
        break;
      case EntityType.OAUTH2_CLIENT:
        observable = this.oauth2Service.findTenantOAuth2ClientInfosByIds(entityIds, config);
        break;
      case EntityType.RULE_CHAIN:
        observable = this.getEntitiesByIdsObservable(
          (id) => this.ruleChainService.getRuleChain(id, config),
          entityIds);
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
      case EntityType.EDGE:
        pageLink.sortOrder.property = 'name';
        if (authUser.authority === Authority.CUSTOMER_USER) {
          entitiesObservable = this.edgeService.getCustomerEdgeInfos(customerId, pageLink, subType, config);
        } else {
          entitiesObservable = this.edgeService.getTenantEdgeInfos(pageLink, subType, config);
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
        if (RuleChainType[subType]) {
          entitiesObservable = this.ruleChainService.getRuleChains(pageLink, subType as RuleChainType, config);
        } else {
          // safe fallback to default core type
          entitiesObservable = this.ruleChainService.getRuleChains(pageLink, RuleChainType.CORE, config);
        }
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
      case EntityType.OTA_PACKAGE:
        pageLink.sortOrder.property = 'title';
        entitiesObservable = this.otaPackageService.getOtaPackages(pageLink, config);
        break;
      case EntityType.DEVICE_PROFILE:
        pageLink.sortOrder.property = 'name';
        entitiesObservable = this.deviceProfileService.getDeviceProfileInfos(pageLink, null, config);
        break;
      case EntityType.TENANT_PROFILE:
        pageLink.sortOrder.property = 'name';
        entitiesObservable = this.tenantProfileService.getTenantProfiles(pageLink, config);
        break;
      case EntityType.ASSET_PROFILE:
        pageLink.sortOrder.property = 'name';
        entitiesObservable = this.assetProfileService.getAssetProfileInfos(pageLink, config);
        break;
      case EntityType.WIDGETS_BUNDLE:
        pageLink.sortOrder.property = 'title';
        entitiesObservable = this.widgetService.getWidgetBundles(pageLink, false, true, false, config);
        break;
      case EntityType.WIDGET_TYPE:
        pageLink.sortOrder.property = 'name';
        entitiesObservable = this.widgetService.getWidgetTypes(pageLink, true, false, false, DeprecatedFilter.ALL, null, config);
        break;
      case EntityType.NOTIFICATION_TARGET:
        pageLink.sortOrder.property = 'name';
        entitiesObservable = this.notificationService.getNotificationTargets(pageLink, subType as NotificationType, config);
        break;
      case EntityType.NOTIFICATION_TEMPLATE:
        pageLink.sortOrder.property = 'name';
        entitiesObservable = this.notificationService.getNotificationTemplates(pageLink, subType as NotificationType, config);
        break;
      case EntityType.NOTIFICATION_RULE:
        pageLink.sortOrder.property = 'name';
        entitiesObservable = this.notificationService.getNotificationRules(pageLink, config);
        break;
      case EntityType.TB_RESOURCE:
        pageLink.sortOrder.property = 'title';
        entitiesObservable = this.resourceService.getTenantResources(pageLink, config);
        break;
      case EntityType.QUEUE_STATS:
        pageLink.sortOrder.property = 'createdTime';
        entitiesObservable = this.queueService.getQueueStatistics(pageLink, config);
        break;
      case EntityType.OAUTH2_CLIENT:
        pageLink.sortOrder.property = 'title';
        entitiesObservable = this.oauth2Service.findTenantOAuth2ClientInfos(pageLink, config);
        break;
      case EntityType.MOBILE_APP:
        pageLink.sortOrder.property = 'pkgName';
        entitiesObservable = this.mobileAppService.getTenantMobileAppInfos(pageLink, subType as PlatformType, config);
        break;
      case EntityType.MOBILE_APP_BUNDLE:
        pageLink.sortOrder.property = 'title';
        entitiesObservable = this.mobileAppService.getTenantMobileAppBundleInfos(pageLink, config);
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
                               scope?: AttributeScope, config?: RequestConfig): Observable<EntitiesKeysByQuery> {
    let url = `/api/entitiesQuery/find/keys?attributes=${attributes}&timeseries=${timeseries}`;
    if (scope) {
      url += `&scope=${scope}`;
    }
    return this.http.post<EntitiesKeysByQuery>(
      url,
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
    const authState = getCurrentAuthState(this.store);
    let allAliasFilterTypes: Array<AliasFilterType> = Object.values(AliasFilterType);
    if (!authState.edgesSupportEnabled) {
      allAliasFilterTypes = allAliasFilterTypes.filter(aliasFilterType => !edgeAliasFilterTypes.includes(aliasFilterType));
    }
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
        case AliasFilterType.entityType:
          return entityTypes.indexOf(filter.entityType) > -1 ? true : false;
        case AliasFilterType.stateEntity:
          return true;
        case AliasFilterType.assetType:
          return entityTypes.indexOf(EntityType.ASSET)  > -1 ? true : false;
        case AliasFilterType.deviceType:
          return entityTypes.indexOf(EntityType.DEVICE)  > -1 ? true : false;
        case AliasFilterType.edgeType:
          return entityTypes.indexOf(EntityType.EDGE) > -1 ? true : false;
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
        case AliasFilterType.edgeSearchQuery:
          return entityTypes.indexOf(EntityType.EDGE) > -1 ? true : false;
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
      case AliasFilterType.entityType:
        return true;
      case AliasFilterType.stateEntity:
        return true;
      case AliasFilterType.assetType:
        return entityType === EntityType.ASSET;
      case AliasFilterType.deviceType:
        return entityType === EntityType.DEVICE;
      case AliasFilterType.edgeType:
        return entityType === EntityType.EDGE;
      case AliasFilterType.entityViewType:
        return entityType === EntityType.ENTITY_VIEW;
      case AliasFilterType.relationsQuery:
        return true;
      case AliasFilterType.apiUsageState:
        return true;
      case AliasFilterType.assetSearchQuery:
        return entityType === EntityType.ASSET;
      case AliasFilterType.deviceSearchQuery:
        return entityType === EntityType.DEVICE;
      case AliasFilterType.edgeSearchQuery:
        return entityType === EntityType.EDGE;
      case AliasFilterType.entityViewSearchQuery:
        return entityType === EntityType.ENTITY_VIEW;
    }
    return false;
  }

  public prepareAllowedEntityTypesList(allowedEntityTypes: Array<EntityType | AliasEntityType>,
                                       useAliasEntityTypes?: boolean): Array<EntityType | AliasEntityType> {
    const authState = getCurrentAuthState(this.store);
    const entityTypes: Array<EntityType | AliasEntityType> = [];
    switch (authState.authUser.authority) {
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
        if (authState.edgesSupportEnabled) {
          entityTypes.push(EntityType.EDGE);
        }
        if (useAliasEntityTypes) {
          entityTypes.push(EntityType.QUEUE_STATS);

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
        if (authState.edgesSupportEnabled) {
          entityTypes.push(EntityType.EDGE);
        }
        if (useAliasEntityTypes) {
          entityTypes.push(AliasEntityType.CURRENT_CUSTOMER);
        }
        break;
    }
    if (useAliasEntityTypes) {
      entityTypes.push(AliasEntityType.CURRENT_USER);
      if (authState.authUser.authority !== Authority.SYS_ADMIN) {
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
        entityFieldKeys.push(entityFields.phone.keyName);
        entityFieldKeys.push(entityFields.ownerName.keyName);
        entityFieldKeys.push(entityFields.ownerType.keyName);
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
        entityFieldKeys.push(entityFields.ownerName.keyName);
        entityFieldKeys.push(entityFields.ownerType.keyName);
        break;
      case EntityType.DEVICE:
      case EntityType.EDGE:
      case EntityType.ASSET:
        entityFieldKeys.push(entityFields.name.keyName);
        entityFieldKeys.push(entityFields.type.keyName);
        entityFieldKeys.push(entityFields.label.keyName);
        entityFieldKeys.push(entityFields.ownerName.keyName);
        entityFieldKeys.push(entityFields.ownerType.keyName);
        break;
      case EntityType.DASHBOARD:
        entityFieldKeys.push(entityFields.title.keyName);
        entityFieldKeys.push(entityFields.ownerName.keyName);
        entityFieldKeys.push(entityFields.ownerType.keyName);
        break;
      case EntityType.API_USAGE_STATE:
        entityFieldKeys.push(entityFields.name.keyName);
        break;
      case EntityType.QUEUE_STATS:
        entityFieldKeys.push(entityFields.queueName.keyName);
        entityFieldKeys.push(entityFields.serviceId.keyName);
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

  public getEntityKeysByEntityFilter(filter: EntityFilter, types: DataKeyType[],
                                     entityTypes?: EntityType[],
                                     config?: RequestConfig): Observable<Array<DataKey>> {
    return this.getEntityKeysByEntityFilterAndScope(filter, types, entityTypes, null, config);
  }

  public getEntityKeysByEntityFilterAndScope(filter: EntityFilter, types: DataKeyType[],
                                             entityTypes?: EntityType[], scope?: AttributeScope,
                                             config?: RequestConfig): Observable<Array<DataKey>> {
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
        types.includes(DataKeyType.timeseries), scope, config);
    } else {
      entitiesKeysByQuery$ = of({
        attribute: [],
        timeseries: [],
        entityTypes: entityTypes || [],
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
          let currentEntity: EntityInfo = null;
          if (result.stateEntity && aliasInfo.entityFilter.type === AliasFilterType.singleEntity) {
            if (stateParams) {
              let targetParams = stateParams;
              if (result.entityParamName && result.entityParamName.length) {
                targetParams = stateParams[result.entityParamName];
              }
              if (targetParams && targetParams.entityId && targetParams.entityName) {
                currentEntity = {
                  id: targetParams.entityId.id,
                  entityType: targetParams.entityId.entityType as EntityType,
                  name: targetParams.entityName,
                  label: targetParams.entityLabel
                };
              }
            }
          }
          const entityInfoObservable = currentEntity ? of(currentEntity) : this.findSingleEntityInfoByEntityFilter(aliasInfo.entityFilter,
            {ignoreLoading: true, ignoreErrors: true});
          return entityInfoObservable.pipe(
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
      case AliasFilterType.entityType:
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
      case AliasFilterType.edgeType:
        result.entityFilter = deepClone(filter);
        return of(result);
      case AliasFilterType.relationsQuery:
      case AliasFilterType.assetSearchQuery:
      case AliasFilterType.deviceSearchQuery:
      case AliasFilterType.edgeSearchQuery:
      case AliasFilterType.entityViewSearchQuery:
        let rootEntityType;
        let rootEntityId;
        result.stateEntity = filter.rootStateEntity;
        if (result.stateEntity && stateEntityId) {
          rootEntityType = stateEntityId.entityType;
          rootEntityId = stateEntityId.id;
        } else if (!result.stateEntity) {
          rootEntityType = filter.rootEntity.entityType;
          rootEntityId = filter.rootEntity.id;
        }
        if (rootEntityType && rootEntityId) {
          const queryRootEntityId = this.resolveAliasEntityId(rootEntityType, rootEntityId);
          result.entityFilter = deepClone(filter);
          result.entityFilter.rootEntity = queryRootEntityId;
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

  public resolveAlarmFilter(alarmFilterConfig?: AlarmFilterConfig, searchPropagatedByDefault = true): AlarmFilter {
    const alarmFilter: AlarmFilter = {};
    if (alarmFilterConfig) {
      alarmFilter.typeList = alarmFilterConfig.typeList;
      alarmFilter.severityList = alarmFilterConfig.severityList;
      alarmFilter.statusList = alarmFilterConfig.statusList;
      alarmFilter.searchPropagatedAlarms = isDefined(alarmFilterConfig.searchPropagatedAlarms) ?
        alarmFilterConfig.searchPropagatedAlarms : searchPropagatedByDefault;
      if (alarmFilterConfig.assignedToCurrentUser) {
        const authUser = getCurrentAuthUser(this.store);
        alarmFilter.assigneeId = new UserId(authUser.userId);
      } else {
        alarmFilter.assigneeId = alarmFilterConfig.assigneeId;
      }
    }
    return alarmFilter;
  }

  public saveEntityParameters(entityType: EntityType, entityData: ImportEntityData, update: boolean,
                              config?: RequestConfig): Observable<ImportEntitiesResultInfo> {
    const saveEntityObservable: Observable<BaseData<EntityId>> = this.getSaveEntityObservable(entityType, entityData, config);
    return saveEntityObservable.pipe(
      mergeMap((entity) => {
        return this.saveEntityData(entity.id, entityData, config).pipe(
          map(() => {
            return { create: { entity: 1 } } as ImportEntitiesResultInfo;
          }),
          catchError(err => of({
            error: {
              entity: 1,
              errors: err.message
            }
          } as ImportEntitiesResultInfo))
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
            case EntityType.EDGE:
              findEntityObservable = this.edgeService.findByName(entityData.name, config);
              break;
          }
          return findEntityObservable.pipe(
            mergeMap((entity) => {
              const updateEntityTasks: Observable<any>[] = this.getUpdateEntityTasks(entityType, entityData, entity, config);
              return forkJoin(updateEntityTasks).pipe(
                map(() => {
                  return { update: { entity: 1 } } as ImportEntitiesResultInfo;
                }),
                catchError(updateError => of({
                  error: {
                    entity: 1,
                    errors: updateError.message
                  }
                } as ImportEntitiesResultInfo))
              );
            }),
            catchError(findErr => of({
              error: {
                entity: 1,
                errors: `Line: ${entityData.lineNumber}; Error: ${findErr.error.message}`
              }
            } as ImportEntitiesResultInfo))
          );
        } else {
          return of({
            error: {
              entity: 1,
              errors: `Line: ${entityData.lineNumber}; Error: ${err.error.message}`
            }
          } as ImportEntitiesResultInfo);
        }
      })
    );
  }

  private getSaveEntityObservable(entityType: EntityType, entityData: ImportEntityData,
                                  config?: RequestConfig): Observable<BaseData<EntityId>> {
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
      case EntityType.EDGE:
        const edgeEntityData: EdgeImportEntityData = entityData as EdgeImportEntityData;
        const edge: Edge = {
          name: edgeEntityData.name,
          type: edgeEntityData.type,
          label: edgeEntityData.label,
          additionalInfo: {
            description: edgeEntityData.description
          },
          routingKey: edgeEntityData.routingKey !== '' ? edgeEntityData.routingKey : guid(),
          secret: edgeEntityData.secret !== '' ? edgeEntityData.secret : generateSecret(20)
        };
        saveEntityObservable = this.edgeService.saveEdge(edge, config);
        break;
    }
    return saveEntityObservable;
  }

  private getUpdateEntityTasks(entityType: EntityType,  entityData: ImportEntityData | EdgeImportEntityData,
                               entity: BaseData<EntityId>, config?: RequestConfig): Observable<any>[] {
    const tasks: Observable<any>[] = [];
    let result;
    let additionalInfo;
    switch (entityType) {
      case EntityType.ASSET:
      case EntityType.DEVICE:
        result = entity as (Device | Asset);
        additionalInfo = result.additionalInfo || {};
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
        break;
      case EntityType.EDGE:
        result = entity as Edge;
        additionalInfo = result.additionalInfo || {};
        const edgeEntityData: EdgeImportEntityData = entityData as EdgeImportEntityData;
        if (result.label !== edgeEntityData.label ||
          result.type !== edgeEntityData.type ||
          (edgeEntityData.routingKey !== '' && result.routingKey !== edgeEntityData.routingKey) ||
          (edgeEntityData.secret !== '' && result.secret !== edgeEntityData.secret) ||
          additionalInfo.description !== edgeEntityData.description) {
          result.label = edgeEntityData.label;
          result.type = edgeEntityData.type;
          result.additionalInfo = additionalInfo;
          result.additionalInfo.description = edgeEntityData.description;
          if (edgeEntityData.routingKey !== '') {
            result.routingKey = edgeEntityData.routingKey;
          }
          if (edgeEntityData.secret !== '') {
            result.secret = edgeEntityData.secret;
          }
          tasks.push(this.edgeService.saveEdge(result, config));
        }
        tasks.push(this.saveEntityData(entity.id, edgeEntityData, config));
        break;
    }
    return tasks;
  }

  public saveEntityData(entityId: EntityId, entityData: ImportEntityData, config?: RequestConfig): Observable<any> {
    const observables: Observable<string>[] = [];
    let observable: Observable<string>;
    if (Object.keys(entityData.credential).length) {
      let credentialsType: DeviceCredentialsType;
      let credentialsId: string = null;
      let credentialsValue: string = null;
      if (isDefinedAndNotNull(entityData.credential.mqtt)) {
        credentialsType = DeviceCredentialsType.MQTT_BASIC;
        credentialsValue = JSON.stringify(entityData.credential.mqtt);
      } else if (isDefinedAndNotNull(entityData.credential.lwm2m)) {
        credentialsType = DeviceCredentialsType.LWM2M_CREDENTIALS;
        credentialsValue = JSON.stringify(entityData.credential.lwm2m);
      } else if (isNotEmptyStr(entityData.credential.x509)) {
        credentialsType = DeviceCredentialsType.X509_CERTIFICATE;
        credentialsValue = entityData.credential.x509;
      } else {
        credentialsType = DeviceCredentialsType.ACCESS_TOKEN;
        credentialsId = entityData.credential.accessToken;
      }
      observable = this.deviceService.getDeviceCredentials(entityId.id, false, config).pipe(
        mergeMap((credentials) => {
          credentials.credentialsId = credentialsId;
          credentials.credentialsType = credentialsType;
          credentials.credentialsValue = credentialsValue;
          return this.deviceService.saveDeviceCredentials(credentials, config).pipe(
            map(() => 'ok'),
            catchError(err => of(`Line: ${entityData.lineNumber}; Error: ${err.error.message}`))
          );
        })
      );
      observables.push(observable);
    }
    if (entityData.attributes.shared && entityData.attributes.shared.length) {
      observable = this.attributeService.saveEntityAttributes(entityId, AttributeScope.SHARED_SCOPE,
        entityData.attributes.shared, config).pipe(
        map(() => 'ok'),
        catchError(err => of(`Line: ${entityData.lineNumber}; Error: ${err.error.message}`))
      );
      observables.push(observable);
    }
    if (entityData.attributes.server && entityData.attributes.server.length) {
      observable = this.attributeService.saveEntityAttributes(entityId, AttributeScope.SERVER_SCOPE,
        entityData.attributes.server, config).pipe(
        map(() => 'ok'),
        catchError(err => of(`Line: ${entityData.lineNumber}; Error: ${err.error.message}`))
      );
      observables.push(observable);
    }
    if (entityData.timeseries && entityData.timeseries.length) {
      observable = this.attributeService.saveEntityTimeseries(entityId, 'time', entityData.timeseries, config).pipe(
        map(() => 'ok'),
        catchError(err => of(`Line: ${entityData.lineNumber}; Error: ${err.error.message}`))
      );
      observables.push(observable);
    }
    if (observables.length) {
      return forkJoin(observables).pipe(
        map((response) => {
          const hasError = response.filter((status) => status !== 'ok');
          if (hasError.length > 0) {
            throw Error(hasError.join('\n'));
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
    } else if (subscriptionInfo.type === DatasourceType.function || subscriptionInfo.type === DatasourceType.entityCount ||
      subscriptionInfo.type === DatasourceType.alarmCount) {
      datasource = {
        type: subscriptionInfo.type,
        name: subscriptionInfo.name || subscriptionInfo.type,
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
      if (subscriptionInfo.type === DatasourceType.entityCount || subscriptionInfo.type === DatasourceType.alarmCount) {
        const dataKey = this.utils.createKey({ name: 'count'}, DataKeyType.count);
        datasource.dataKeys.push(dataKey);
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
        const pageSize = isDefinedAndNotNull(subscriptionInfo.pageSize) && subscriptionInfo.pageSize > 0 ? subscriptionInfo.pageSize : 1024;
        pageLink = createDefaultEntityDataPageLink(pageSize);
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
      const pageSize = isDefinedAndNotNull(subscriptionInfo.pageSize) && subscriptionInfo.pageSize > 0 ? subscriptionInfo.pageSize : 1024;
      datasource.pageLink = createDefaultEntityDataPageLink(pageSize);
    }
  }

  private createDatasourceKeys(keyInfos: Array<KeyInfo>, type: DataKeyType, datasource: Datasource) {
    keyInfos.forEach((keyInfo) => {
      const dataKey = this.utils.createKey(keyInfo, type);
      datasource.dataKeys.push(dataKey);
    });
  }

  public getAssignedToEdgeEntitiesByType(edgeId: string, entityType: EntityType, pageLink: PageLink): Observable<PageData<any>> {
    let entitiesObservable: Observable<PageData<any>>;
    switch (entityType) {
      case (EntityType.ASSET):
        entitiesObservable = this.assetService.getEdgeAssets(edgeId, pageLink);
        break;
      case (EntityType.DEVICE):
        entitiesObservable = this.deviceService.getEdgeDevices(edgeId, pageLink);
        break;
      case (EntityType.ENTITY_VIEW):
        entitiesObservable = this.entityViewService.getEdgeEntityViews(edgeId, pageLink);
        break;
      case (EntityType.DASHBOARD):
        entitiesObservable = this.dashboardService.getEdgeDashboards(edgeId, pageLink);
        break;
      case (EntityType.RULE_CHAIN):
        entitiesObservable = this.ruleChainService.getEdgeRuleChains(edgeId, pageLink);
        break;
    }
    return entitiesObservable;
  }

  public getEdgeEventContent(entity: EdgeEvent): Observable<BaseData<HasId> | RuleChainMetaData | string> {
    let entityObservable: Observable<BaseData<HasId> | RuleChainMetaData | string>;
    const entityId: string = entity.entityId;
    const entityType: any = entity.type;
    switch (entityType) {
      case EdgeEventType.DASHBOARD:
      case EdgeEventType.ALARM:
      case EdgeEventType.RULE_CHAIN:
      case EdgeEventType.EDGE:
      case EdgeEventType.USER:
      case EdgeEventType.CUSTOMER:
      case EdgeEventType.TENANT:
      case EdgeEventType.ASSET:
      case EdgeEventType.DEVICE:
      case EdgeEventType.ENTITY_VIEW:
        if (bodyContentEdgeEventActionTypes.includes(entity.action)) {
          entityObservable = of(entity.body);
        } else {
          entityObservable = this.getEntity(entityType, entityId, { ignoreLoading: true, ignoreErrors: true });
        }
        break;
      case EdgeEventType.RULE_CHAIN_METADATA:
        entityObservable = this.ruleChainService.getRuleChainMetadata(entityId);
        break;
      case EdgeEventType.WIDGET_TYPE:
        entityObservable = this.widgetService.getWidgetTypeById(entityId);
        break;
      case EdgeEventType.WIDGETS_BUNDLE:
        entityObservable = this.widgetService.getWidgetsBundle(entityId);
        break;
      case EdgeEventType.DEVICE_PROFILE:
        entityObservable = this.deviceProfileService.getDeviceProfile(entityId);
        break;
      case EdgeEventType.ASSET_PROFILE:
        entityObservable = this.assetProfileService.getAssetProfile(entityId);
        break;
      case EdgeEventType.RELATION:
        entityObservable = of(entity.body);
        break;
    }
    return entityObservable;
  }

  public getEntitySubtypesObservable(entityType: EntityType): Observable<Array<string>> {
    let observable: Observable<Array<string>>;
    switch (entityType) {
      case EntityType.ASSET:
        observable = this.assetProfileService.getAssetProfileNames(false, {ignoreLoading: true}).pipe(
          map(subTypes => subTypes.map(subType => subType.name))
        );
        break;
      case EntityType.DEVICE:
        observable = this.deviceProfileService.getDeviceProfileNames(false,{ignoreLoading: true}).pipe(
          map(subTypes => subTypes.map(subType => subType.name))
        );
        break;
      case EntityType.EDGE:
        observable = this.edgeService.getEdgeTypes({ignoreLoading: true}).pipe(
          map(subTypes => subTypes.map(subType => subType.type))
        );
        break;
      case EntityType.ENTITY_VIEW:
        observable = this.entityViewService.getEntityViewTypes({ignoreLoading: true}).pipe(
          map(subTypes => subTypes.map(subType => subType.type))
        );
        break;
    }
    return observable;
  }
}

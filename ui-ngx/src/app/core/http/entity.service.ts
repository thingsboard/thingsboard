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

import {Injectable} from '@angular/core';
import {EMPTY, forkJoin, Observable, of, throwError} from 'rxjs/index';
import {HttpClient} from '@angular/common/http';
import {PageLink} from '@shared/models/page/page-link';
import {AliasEntityType, EntityType} from '@shared/models/entity-type.models';
import {BaseData} from '@shared/models/base-data';
import {EntityId} from '@shared/models/id/entity-id';
import {DeviceService} from '@core/http/device.service';
import {TenantService} from '@core/http/tenant.service';
import {CustomerService} from '@core/http/customer.service';
import {UserService} from './user.service';
import {DashboardService} from '@core/http/dashboard.service';
import {Direction} from '@shared/models/page/sort-order';
import {PageData} from '@shared/models/page/page-data';
import {getCurrentAuthUser} from '../auth/auth.selectors';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {Authority} from '@shared/models/authority.enum';
import {Tenant} from '@shared/models/tenant.model';
import {concatMap, expand, map, toArray} from 'rxjs/operators';
import {Customer} from '@app/shared/models/customer.model';
import {AssetService} from '@core/http/asset.service';
import {EntityViewService} from '@core/http/entity-view.service';
import {DataKeyType} from '@shared/models/telemetry/telemetry.models';
import {DeviceInfo} from '@shared/models/device.models';
import {defaultHttpOptions} from '@core/http/http-utils';
import {RuleChainService} from '@core/http/rule-chain.service';

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
    private dashboardService: DashboardService
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

  public prepareAllowedEntityTypesList(allowedEntityTypes: Array<EntityType | AliasEntityType>,
                                       useAliasEntityTypes: boolean): Array<EntityType | AliasEntityType> {
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
}

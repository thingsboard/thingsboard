// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { EMPTY, forkJoin, Observable, of, throwError } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { ContactBased } from '@shared/models/contact-based.model';
import { EntityId } from '@shared/models/id/entity-id';
import {
  DeviceEntityGroupInfo,
  EntityGroup,
  EntityGroupInfo,
  prepareEntityGroupConfiguration,
  ShareGroupRequest,
  ShortEntityView
} from '@shared/models/entity-group.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroupId } from '@shared/models/id/entity-group-id';
import { concatMap, expand, map, mergeMap, toArray } from 'rxjs/operators';
import { BaseData, HasId, sortEntitiesByIds } from '@shared/models/base-data';
import { deepClone, isDefinedAndNotNull } from '@core/utils';
import { OtaPackageService } from '@core/http/ota-package.service';
import { DeviceGroupOtaPackage, OtaUpdateType } from '@shared/models/ota-package.models';
import { OtaPackageId } from '@shared/models/id/ota-package-id';
import { Direction } from '@shared/models/page/sort-order';
import { EntityInfo, EntityInfoData } from '@shared/models/entity.models';

@Injectable({
  providedIn: 'root'
})
export class EntityGroupService {

  constructor(
    private http: HttpClient,
    private otaPackageService: OtaPackageService
  ) {}

  public getOwners(pageLink: PageLink, config?: RequestConfig): Observable<PageData<ContactBased<EntityId>>> {
    return this.http.get<PageData<ContactBased<EntityId>>>(`/api/owners${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getOwnerInfos(pageLink: PageLink, config?: RequestConfig): Observable<PageData<EntityInfoData>> {
    return this.http.get<PageData<EntityInfoData>>(`/api/ownerInfos${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getOwnerInfo(ownerId: EntityId, config?: RequestConfig): Observable<EntityInfoData> {
    return this.http.get<EntityInfoData>(`/api/ownerInfo/${ownerId.entityType}/${ownerId.id}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEntityGroup(entityGroupId: string, config?: RequestConfig): Observable<EntityGroupInfo> {
    return this.http.get<EntityGroupInfo>(`/api/entityGroup/${entityGroupId}`,
      defaultHttpOptionsFromConfig(config)).pipe(
      map(group => {
        group.configuration = prepareEntityGroupConfiguration(group.type, group.configuration);
        return group;
      })
    );
  }

  public getDeviceEntityGroup(entityGroupId: string, config?: RequestConfig): Observable<DeviceEntityGroupInfo> {
    return this.http.get<EntityGroupInfo>(`/api/entityGroup/${entityGroupId}`,
      defaultHttpOptionsFromConfig(config)).pipe(
      map(group => {
        group.configuration = prepareEntityGroupConfiguration(group.type, group.configuration);
        return group;
      }),
      mergeMap(group => this.fetchOtaPackageGroupInfo(group, config))
    );
  }

  public fetchOtaPackageGroupInfo(group: EntityGroupInfo, config?: RequestConfig): Observable<DeviceEntityGroupInfo> {
    if (isDefinedAndNotNull(group) && group.type === EntityType.DEVICE && !group.groupAll) {
      const tasks = [];
      tasks.push(this.otaPackageService.getOtaPackageInfoByDeviceGroupId(group.id.id, OtaUpdateType.FIRMWARE, config));
      tasks.push(this.otaPackageService.getOtaPackageInfoByDeviceGroupId(group.id.id, OtaUpdateType.SOFTWARE, config));
      return forkJoin(tasks).pipe(
        map(([firmware, software]: DeviceGroupOtaPackage[]) => {
          const deviceGroup = group as DeviceEntityGroupInfo;
          if (isDefinedAndNotNull(firmware)) {
            deviceGroup.firmwareGroup = firmware;
            deviceGroup.firmwareId = deepClone(firmware.otaPackageId);
          }
          if (isDefinedAndNotNull(software)) {
            deviceGroup.softwareGroup = software;
            deviceGroup.softwareId = deepClone(software.otaPackageId);
          }
          return deviceGroup;
        })
      );
    }
    return of(group);
  }

  public updateDeviceGroupOtaPackage(deviceGroupOtaPackage: DeviceGroupOtaPackage, otaPackageId: OtaPackageId | null,
                                     groupId: EntityGroupId, otaPackageType: OtaUpdateType,
                                     config?: RequestConfig): Observable<DeviceGroupOtaPackage> {
    if (isDefinedAndNotNull(deviceGroupOtaPackage)) {
      if (otaPackageId === null) {
        return this.deleteDeviceGroupOtaPackage(deviceGroupOtaPackage.id, config);
      } else if (otaPackageId.id !== deviceGroupOtaPackage.otaPackageId.id) {
        deviceGroupOtaPackage.otaPackageId = otaPackageId;
        return this.saveDeviceGroupOtaPackage(deviceGroupOtaPackage, config);
      } else {
        return of(deviceGroupOtaPackage);
      }
    } else if (isDefinedAndNotNull(otaPackageId)) {
      const groupOtaPackage: DeviceGroupOtaPackage = {
        otaPackageId,
        otaPackageType,
        groupId
      };
      return this.saveDeviceGroupOtaPackage(groupOtaPackage, config);
    }
    return of(null);
  }

  public saveDeviceGroupOtaPackage(deviceGroupOtaPackage: DeviceGroupOtaPackage,
                                   config?: RequestConfig ): Observable<DeviceGroupOtaPackage> {
    return this.http.post<DeviceGroupOtaPackage>('/api/deviceGroupOtaPackage', deviceGroupOtaPackage, defaultHttpOptionsFromConfig(config));
  }

  public saveDeviceEntityGroup(entityGroup: DeviceEntityGroupInfo, originalEntityGroup?: DeviceEntityGroupInfo,
                               config?: RequestConfig): Observable<DeviceEntityGroupInfo> {
    if (isDefinedAndNotNull(entityGroup.id)) {
      return this.otaPackageService.confirmDialogUpdatePackage(entityGroup, originalEntityGroup).pipe(
        mergeMap((update) => {
          if (update) {
            const firmwareId = entityGroup.firmwareId;
            const firmwareGroup = entityGroup.firmwareGroup;
            const softwareId = entityGroup.softwareId;
            const softwareGroup = entityGroup.softwareGroup;
            delete entityGroup.firmwareId;
            delete entityGroup.firmwareGroup;
            delete entityGroup.softwareId;
            delete entityGroup.softwareGroup;
            const tasks = [this.updateDeviceGroupOtaPackage(firmwareGroup, firmwareId, entityGroup.id, OtaUpdateType.FIRMWARE, config),
                           this.updateDeviceGroupOtaPackage(softwareGroup, softwareId, entityGroup.id, OtaUpdateType.SOFTWARE, config),
                           this.saveEntityGroup(entityGroup, config)];
            return forkJoin(tasks).pipe(
              map(([firmware, software, savedEntityGroup]: [DeviceGroupOtaPackage, DeviceGroupOtaPackage, EntityGroupInfo]) =>
                Object.assign(savedEntityGroup, {
                    firmwareId: deepClone(firmware?.otaPackageId),
                    firmwareGroup: firmware,
                    softwareId: deepClone(software?.otaPackageId),
                    softwareGroup: software
                  })
              ));
          }
          return throwError(() => 'Canceled saving device group');
        })
      );
    }
    return this.saveEntityGroup(entityGroup, config);
  }

  public deleteDeviceGroupOtaPackage(otaPackageId: string, config?: RequestConfig ) {
    return this.http.delete<null>(`/api/deviceGroupOtaPackage/${otaPackageId}`, defaultHttpOptionsFromConfig(config));
  }

  public getEntityGroupEntityInfo(entityGroupId: string, config?: RequestConfig): Observable<EntityInfoData> {
    return this.http.get<EntityInfoData>(`/api/entityGroupInfo/${entityGroupId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public saveEntityGroup(entityGroup: EntityGroup, config?: RequestConfig): Observable<EntityGroupInfo> {
    return this.http.post<EntityGroupInfo>('/api/entityGroup', entityGroup, defaultHttpOptionsFromConfig(config));
  }

  public deleteEntityGroup(entityGroupId: string, config?: RequestConfig) {
    return this.http.delete(`/api/entityGroup/${entityGroupId}`, defaultHttpOptionsFromConfig(config));
  }

  public makeEntityGroupPublic(entityGroupId: string, config?: RequestConfig): Observable<any> {
    return this.http.post(`/api/entityGroup/${entityGroupId}/makePublic`, null, defaultHttpOptionsFromConfig(config));
  }

  public makeEntityGroupPrivate(entityGroupId: string, config?: RequestConfig): Observable<any> {
    return this.http.post(`/api/entityGroup/${entityGroupId}/makePrivate`, null, defaultHttpOptionsFromConfig(config));
  }

  public shareEntityGroup(entityGroupId: string, shareGroupRequest: ShareGroupRequest, config?: RequestConfig): Observable<any> {
    return this.http.post(`/api/entityGroup/${entityGroupId}/share`, shareGroupRequest, defaultHttpOptionsFromConfig(config));
  }

  public shareEntityGroupV2(entityGroupId: string, shareGroupRequest: ShareGroupRequest, config?: RequestConfig): Observable<any> {
    return this.http.post(`/api/v2/entityGroup/${entityGroupId}/share`, shareGroupRequest, defaultHttpOptionsFromConfig(config));
  }

  public getEntityGroups(pageLink: PageLink, groupType: EntityType,
                         includeShared = true, config?: RequestConfig): Observable<PageData<EntityGroupInfo>> {
    return this.http.get<PageData<EntityGroupInfo>>(`/api/entityGroups/${groupType}${pageLink.toQuery()}&includeShared=${includeShared}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEntityGroupEntityInfos(pageLink: PageLink, groupType: EntityType,
                                   includeShared = true, config?: RequestConfig): Observable<PageData<EntityInfoData>> {
    return this.http.get<PageData<EntityInfoData>>(`/api/entityGroupInfos/${groupType}${pageLink.toQuery()}&includeShared=${includeShared}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAllEntityGroups(pageSize: number, groupType: EntityType, includeShared = true,
                            config?: RequestConfig): Observable<Array<EntityGroupInfo>> {
    const pageLink = new PageLink(pageSize, 0, null, {
      property: 'name',
      direction: Direction.ASC
    });
    if (pageSize === -1) { // all
      pageLink.pageSize = groupType === EntityType.CUSTOMER ? 1024 : 100;
      return this.getAllEntityGroupsByPageLink(pageLink, groupType, includeShared, config).pipe(
        map((data) => data && data.length ? data : null)
      );
    } else {
      const entityGroupsObservable: Observable<PageData<EntityGroupInfo>> =
        this.getEntityGroups(pageLink, groupType, includeShared, config);
      if (entityGroupsObservable) {
        return entityGroupsObservable.pipe(
          map((data) => data && data.data.length ? data.data : null)
        );
      } else {
        return of(null);
      }
    }
  }

  private getAllEntityGroupsByPageLink(pageLink: PageLink, groupType: EntityType, includeShared = true,
                                       config?: RequestConfig): Observable<Array<EntityGroupInfo>> {
    const entityGroupsObservable: Observable<PageData<EntityGroupInfo>> =
      this.getEntityGroups(pageLink, groupType, includeShared, config);
    if (entityGroupsObservable) {
      return entityGroupsObservable.pipe(
        expand((data) => {
          if (data.hasNext) {
            pageLink.page += 1;
            return this.getEntityGroups(pageLink, groupType, includeShared, config);
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

  public getSharedEntityGroups(pageLink: PageLink, groupType: EntityType, config?: RequestConfig): Observable<PageData<EntityGroupInfo>> {
    return this.http.get<PageData<EntityGroupInfo>>(`/api/entityGroups/${groupType}/shared${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getSharedEntityGroupEntityInfos(pageLink: PageLink, groupType: EntityType,
                                         config?: RequestConfig): Observable<PageData<EntityInfoData>> {
    return this.http.get<PageData<EntityInfoData>>(`/api/entityGroupInfos/${groupType}/shared${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEntityGroupIdsForEntityId(entityType: EntityType, entityId: string, config?: RequestConfig): Observable<Array<EntityGroupId>> {
    return this.http.get<Array<EntityGroupId>>(`/api/entityGroups/${entityType}/${entityId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEntityGroupsByIds(entityGroupIds: Array<string>, config?: RequestConfig): Observable<Array<EntityGroupInfo>> {
    return this.http.get<Array<EntityGroupInfo>>(`/api/entityGroups?entityGroupIds=${entityGroupIds.join(',')}`,
      defaultHttpOptionsFromConfig(config)).pipe(
      map((entityGroups) => sortEntitiesByIds(entityGroups, entityGroupIds))
    );
  }

  public getEntityGroupEntityInfosByIds(entityGroupIds: Array<string>, config?: RequestConfig): Observable<Array<EntityInfoData>> {
    return this.http.get<Array<EntityInfoData>>(`/api/entityGroupInfos?entityGroupIds=${entityGroupIds.join(',')}`,
      defaultHttpOptionsFromConfig(config)).pipe(
      map((entityGroups) => sortEntitiesByIds(entityGroups, entityGroupIds))
    );
  }

  public getEntityGroupsHierarchyByOwnerId(pageLink: PageLink, ownerType: EntityType, ownerId: string, groupType: EntityType,
                                           config?: RequestConfig): Observable<PageData<EntityGroupInfo>> {
    return this.http.get<PageData<EntityGroupInfo>>(`/api/entityGroupsHierarchy/${ownerType}/${ownerId}/${groupType}${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEntityGroupEntityInfosHierarchyByOwnerId(pageLink: PageLink, ownerType: EntityType, ownerId: string, groupType: EntityType,
                                                     config?: RequestConfig): Observable<PageData<EntityInfoData>> {
    return this.http.get<PageData<EntityInfoData>>(
      `/api/entityGroupInfosHierarchy/${ownerType}/${ownerId}/${groupType}${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEntityGroupsByOwnerIdAndPageLink(ownerType: EntityType, ownerId: string, groupType: EntityType, pageLink: PageLink,
                                             config?: RequestConfig): Observable<PageData<EntityGroupInfo>> {
    return this.http.get<PageData<EntityGroupInfo>>(`/api/entityGroups/${ownerType}/${ownerId}/${groupType}${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEntityGroupsByOwnerId(ownerType: EntityType, ownerId: string, groupType: EntityType,
                                  config?: RequestConfig): Observable<Array<EntityGroupInfo>> {
    return this.http.get<Array<EntityGroupInfo>>(`/api/entityGroups/${ownerType}/${ownerId}/${groupType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEntityGroupEntityInfosByOwnerId(pageLink: PageLink, ownerType: EntityType, ownerId: string, groupType: EntityType,
                                  config?: RequestConfig): Observable<PageData<EntityInfoData>> {
    return this.http.get<PageData<EntityInfoData>>(`/api/entityGroupInfos/${ownerType}/${ownerId}/${groupType}${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAllEntityGroupsByOwnerId(pageSize: number, ownerType: EntityType, ownerId: string, groupType: EntityType,
                                     config?: RequestConfig): Observable<Array<EntityGroupInfo>> {
    const pageLink = new PageLink(pageSize, 0, null, {
      property: 'name',
      direction: Direction.ASC
    });
    if (pageSize === -1) { // all
      pageLink.pageSize = groupType === EntityType.CUSTOMER ? 1024 : 100;
      return this.getAllEntityGroupsByOwnerIdAndPageLink(pageLink, ownerType, ownerId, groupType, config).pipe(
        map((data) => data && data.length ? data : null)
      );
    } else {
      const entityGroupsObservable: Observable<PageData<EntityGroupInfo>> =
        this.getEntityGroupsByOwnerIdAndPageLink(ownerType, ownerId, groupType, pageLink, config);
      if (entityGroupsObservable) {
        return entityGroupsObservable.pipe(
          map((data) => data && data.data.length ? data.data : null)
        );
      } else {
        return of(null);
      }
    }
  }

  private getAllEntityGroupsByOwnerIdAndPageLink(pageLink: PageLink, ownerType: EntityType, ownerId: string, groupType: EntityType,
                                                 config?: RequestConfig): Observable<Array<EntityGroupInfo>> {
    const entityGroupsObservable: Observable<PageData<EntityGroupInfo>> =
      this.getEntityGroupsByOwnerIdAndPageLink(ownerType, ownerId, groupType, pageLink, config);
    if (entityGroupsObservable) {
      return entityGroupsObservable.pipe(
        expand((data) => {
          if (data.hasNext) {
            pageLink.page += 1;
            return this.getEntityGroupsByOwnerIdAndPageLink(ownerType, ownerId, groupType, pageLink, config);
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

  public getEntityGroupsByPageLink(pageLink: PageLink, groupType: EntityType,
                                   config?: RequestConfig): Observable<PageData<EntityGroupInfo>> {
    return this.getEntityGroups(pageLink, groupType, true, config);
  }

  public getEntityGroupAllByOwnerId(ownerType: EntityType, ownerId: string, groupType: EntityType,
                                    config?: RequestConfig): Observable<EntityGroupInfo> {
    return this.http.get<EntityGroupInfo>(`/api/entityGroup/all/${ownerType}/${ownerId}/${groupType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public addEntityToEntityGroup(entityGroupId: string, entityId: string, config?: RequestConfig): Observable<any> {
    return this.http.post(`/api/entityGroup/${entityGroupId}/addEntities`, [entityId], defaultHttpOptionsFromConfig(config));
  }

  public addEntitiesToEntityGroup(entityGroupId: string, entityIds: string[], config?: RequestConfig): Observable<any> {
    return this.http.post(`/api/entityGroup/${entityGroupId}/addEntities`, entityIds, defaultHttpOptionsFromConfig(config));
  }

  public changeEntityOwner(ownerId: EntityId, entityId: EntityId, entityGroupIds?: string[], config?: RequestConfig): Observable<any> {
    return this.http.post(`/api/owner/${ownerId.entityType}/${ownerId.id}/${entityId.entityType}/${entityId.id}`,
      entityGroupIds, defaultHttpOptionsFromConfig(config));
  }

  public removeEntityFromEntityGroup(entityGroupId: string, entityId: string, config?: RequestConfig): Observable<any> {
    return this.http.post(`/api/entityGroup/${entityGroupId}/deleteEntities`, [entityId], defaultHttpOptionsFromConfig(config));
  }

  public removeEntitiesFromEntityGroup(entityGroupId: string, entityIds: string[], config?: RequestConfig): Observable<any> {
    return this.http.post(`/api/entityGroup/${entityGroupId}/deleteEntities`, entityIds, defaultHttpOptionsFromConfig(config));
  }

  public getEntityGroupEntity(entityGroupId: string, entityId: string, config?: RequestConfig): Observable<ShortEntityView> {
    return this.http.get<ShortEntityView>(`/api/entityGroup/${entityGroupId}/${entityId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEntityGroupEntities<T extends BaseData<HasId> | ShortEntityView>(entityGroupId: string,
                                                                             pageLink: PageLink,
                                                                             entityType?: EntityType,
                                                                             config?: RequestConfig): Observable<PageData<T>> {
    let url = `/api/entityGroup/${entityGroupId}/`;
    if (entityType) {
      switch (entityType) {
        case EntityType.ASSET:
          url += 'assets';
          break;
        case EntityType.CUSTOMER:
          url += 'customers';
          break;
        case EntityType.DEVICE:
          url += 'devices';
          break;
        case EntityType.USER:
          url += 'users';
          break;
        case EntityType.ENTITY_VIEW:
          url += 'entityViews';
          break;
        case EntityType.DASHBOARD:
          url += 'dashboards';
          break;
        case EntityType.EDGE:
          url += 'edges';
          break;
        default:
          url += 'entities';
      }
    } else {
      url += 'entities';
    }
    url += pageLink.toQuery();
    return this.http.get<PageData<T>>(url, defaultHttpOptionsFromConfig(config));
  }

  public getEdgeEntityGroups(pageLink: PageLink, edgeId: string, groupType: EntityType,
                             config?: RequestConfig): Observable<PageData<EntityGroupInfo>> {
    return this.http.get<PageData<EntityGroupInfo>>(`/api/entityGroups/edge/${edgeId}/${groupType}${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAllEdgeEntityGroups(pageSize: number, edgeId: string, groupType: EntityType,
                                config?: RequestConfig): Observable<Array<EntityGroupInfo>> {
    const pageLink = new PageLink(pageSize, 0, null, {
      property: 'name',
      direction: Direction.ASC
    });
    if (pageSize === -1) { // all
      pageLink.pageSize = groupType === EntityType.CUSTOMER ? 1024 : 100;
      return this.getAllEdgeEntityGroupsByPageLink(pageLink, edgeId, groupType, config).pipe(
        map((data) => data && data.length ? data : null)
      );
    } else {
      const entityGroupsObservable: Observable<PageData<EntityGroupInfo>> =
        this.getEdgeEntityGroups(pageLink, edgeId, groupType, config);
      if (entityGroupsObservable) {
        return entityGroupsObservable.pipe(
          map((data) => data && data.data.length ? data.data : null)
        );
      } else {
        return of(null);
      }
    }
  }

  private getAllEdgeEntityGroupsByPageLink(pageLink: PageLink, edgeId: string, groupType: EntityType,
                                           config?: RequestConfig): Observable<Array<EntityGroupInfo>> {
    const entityGroupsObservable: Observable<PageData<EntityGroupInfo>> =
      this.getEdgeEntityGroups(pageLink, edgeId, groupType, config);
    if (entityGroupsObservable) {
      return entityGroupsObservable.pipe(
        expand((data) => {
          if (data.hasNext) {
            pageLink.page += 1;
            return this.getEdgeEntityGroups(pageLink, edgeId, groupType, config);
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

  public assignEntityGroupToEdge(edgeId: string, entityGroupId: string,
                                 groupType: string, config?: RequestConfig): Observable<EntityGroup> {
    return this.http.post<EntityGroup>(`/api/edge/${edgeId}/entityGroup/${entityGroupId}/${groupType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public unassignEntityGroupFromEdge(edgeId: string, entityGroupId: string,
                                     groupType: string, config?: RequestConfig): Observable<EntityGroup> {
    return this.http.delete<EntityGroup>(`/api/edge/${edgeId}/entityGroup/${entityGroupId}/${groupType}`,
      defaultHttpOptionsFromConfig(config));
  }
}

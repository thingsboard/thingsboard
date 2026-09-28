// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import {
  AllowedPermissionsInfo,
  groupResourceByGroupType,
  MergedUserPermissions,
  Operation,
  Resource,
  resourceByEntityType
} from '@shared/models/security.models';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroup, EntityGroupInfo } from '@shared/models/entity-group.models';
import { isArray } from '@core/utils';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthState } from '@core/auth/auth.selectors';

@Injectable({
  providedIn: 'root'
})
export class UserPermissionsService {

  private operationsByResource: {[resource: string]: Operation[]};
  private allowedGroupRoleOperations: Operation[];
  private allowedGroupOwnerOnlyOperations: Operation[];
  private allowedGroupOwnerOnlyGroupOperations: Operation[];
  private allowedResources: Resource[];
  private userPermissions: MergedUserPermissions;
  private userOwnerId: EntityId;

  constructor(
    private http: HttpClient,
    private store: Store<AppState>
  ) {
  }

  public loadPermissionsInfo(): Observable<AllowedPermissionsInfo> {
    return this.http.get<AllowedPermissionsInfo>('/api/permissions/allowedPermissions').pipe(
      tap((allowedPermissionsInfo) => {
        this.operationsByResource = allowedPermissionsInfo.operationsByResource;
        this.allowedGroupRoleOperations = allowedPermissionsInfo.allowedForGroupRoleOperations;
        this.allowedGroupOwnerOnlyOperations = allowedPermissionsInfo.allowedForGroupOwnerOnlyOperations;
        this.allowedGroupOwnerOnlyGroupOperations = allowedPermissionsInfo.allowedForGroupOwnerOnlyGroupOperations;
        this.allowedResources = allowedPermissionsInfo.allowedResources;
        this.userPermissions = allowedPermissionsInfo.userPermissions;
        this.userOwnerId = allowedPermissionsInfo.userOwnerId;
      })
    );
  }

  public hasEntityPermission(entityId: EntityId, operation: Operation, config?: RequestConfig): Observable<boolean> {
    return this.http.get<boolean>(
      `/api/permission/${entityId.entityType}/${entityId.id}/${operation}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getOperationsByResource(resource: Resource): Operation[] {
    if (resource && this.operationsByResource && this.operationsByResource[resource]) {
      return this.operationsByResource[resource];
    } else {
      return [];
    }
  }

  public getAllowedGroupRoleOperations(): Operation[] {
    return this.allowedGroupRoleOperations ? this.allowedGroupRoleOperations : [];
  }

  public getAllowedResources(): Resource[] {
    return this.allowedResources ? this.allowedResources : [];
  }

  public hasReadGroupsPermission(entityType: EntityType): boolean {
    if (this.userPermissions) {
      const readGroupPermissions = this.userPermissions.readGroupPermissions;
      const groupTypePermissionInfo = readGroupPermissions[entityType];
      return groupTypePermissionInfo.hasGenericRead || groupTypePermissionInfo.entityGroupIds.length > 0;
    } else {
      return false;
    }
  }

  public hasGenericReadGroupsPermission(entityType: EntityType): boolean {
    if (this.userPermissions) {
      const readGroupPermissions = this.userPermissions.readGroupPermissions;
      const groupTypePermissionInfo = readGroupPermissions[entityType];
      return groupTypePermissionInfo.hasGenericRead;
    } else {
      return false;
    }
  }

  public hasSharedReadGroupsPermission(entityType: EntityType): boolean {
    if (this.userPermissions) {
      const readGroupPermissions = this.userPermissions.readGroupPermissions;
      const groupTypePermissionInfo = readGroupPermissions[entityType];
      return groupTypePermissionInfo.entityGroupIds.length > 0;
    } else {
      return false;
    }
  }

  public hasReadGenericPermission(resource: Resource): boolean {
    if (this.userPermissions) {
      return this.hasGenericPermission(resource, Operation.READ);
    } else {
      return false;
    }
  }

  public hasGenericPermission(resource: Resource, operation: Operation): boolean {
    if (this.userPermissions) {
      return this.hasGenericResourcePermission(resource, operation) || this.hasGenericAllPermission(resource, operation);
    } else {
      return false;
    }
  }

  public isAiEnabled(): boolean {
    return getCurrentAuthState(this.store).aiEnabled;
  }

  private isResourceAllowed(resource: Resource): boolean {
    return !this.allowedResources || this.allowedResources.indexOf(resource) > -1;
  }


  public hasGenericPermissionByEntityGroupType(operation: Operation, groupType: EntityType): boolean {
    if (!groupType) {
      return false;
    }
    const resource = resourceByEntityType.get(groupType);
    return this.hasGenericPermission(resource, operation);
  }

  public hasGenericEntityGroupTypePermission(operation: Operation, groupType: EntityType): boolean {
    if (!groupType) {
      return false;
    }
    const resource = groupResourceByGroupType.get(groupType);
    return this.hasGenericPermission(resource, operation);
  }

  public hasGenericEntityGroupPermission(operation: Operation, entityGroup: EntityGroup): boolean {
    if (!entityGroup) {
      return false;
    }
    return this.hasGenericEntityGroupTypePermission(operation, entityGroup.type);
  }

  public hasEntityGroupPermission(operation: Operation, entityGroup: EntityGroupInfo): boolean {
    return this.checkEntityGroupPermission(operation, entityGroup, true);
  }

  public hasGroupEntityPermission(operation: Operation, entityGroup: EntityGroupInfo): boolean {
    return this.checkEntityGroupPermission(operation, entityGroup, false);
  }

  public isDirectlyOwnedGroup(entityGroup: EntityGroupInfo) {
    if (this.userOwnerId && entityGroup && entityGroup.ownerId) {
      return this.idsEqual(this.userOwnerId, entityGroup.ownerId);
    } else {
      return false;
    }
  }

  public isOwnedGroup(entityGroup: EntityGroupInfo): boolean {
    if (!entityGroup) {
      return false;
    }
    return this.isCurrentUserOwner(entityGroup);
  }

  public getUserOwnerId(): EntityId {
    return this.userOwnerId;
  }

  public isDirectOwner(ownerId: EntityId): boolean {
    if (this.userOwnerId && ownerId) {
      return this.idsEqual(this.userOwnerId, ownerId);
    } else {
      return false;
    }
  }

  public hasEntityTypeOperationPermission(entityType: EntityType, operation: Operation, entityGroup?: EntityGroupInfo): boolean {
    const resource = resourceByEntityType.get(entityType);
    if (this.hasGenericPermission(resource, operation)) {
      return true;
    }
    if (!this.userPermissions || !this.allowedGroupRoleOperations ||
      this.allowedGroupRoleOperations.indexOf(operation) === -1) {
      return false;
    }
    if (entityGroup) {
      return this.hasGroupPermissions(entityGroup, operation, false);
    }
    const groupTypePermissionInfo = this.userPermissions.readGroupPermissions?.[entityType];
    return groupTypePermissionInfo?.entityGroupIds.some(groupId => {
      const permissionInfo = this.userPermissions.groupPermissions[groupId.id];
      return permissionInfo && this.checkOperation(permissionInfo.operations, operation);
    }) ?? false;
  }

  public hasResourcesGenericPermission(resource: Resource | Resource[], operation: Operation | Operation[]): boolean {
    if (isArray(resource)) {
      return this.hasGenericResourcesPermission(resource as Resource[], operation);
    } else if (isArray(operation)) {
      return this.hasGenericOperationsPermission(resource, operation as Operation[]);
    } else {
      return this.hasGenericPermission(resource as Resource, operation as Operation);
    }
  }

  private hasGenericResourcesPermission(resources: Resource[], operation: Operation | Operation[]): boolean {
    for (const resource of resources) {
      if (!this.hasResourcesGenericPermission(resource, operation)) {
        return false;
      }
    }
    return true;
  }

  private hasGenericOperationsPermission(resource: Resource | Resource[], operations: Operation[]): boolean {
    for (const operation of operations) {
      if (!this.hasResourcesGenericPermission(resource, operation)) {
        return false;
      }
    }
    return true;
  }

  private hasGenericAllPermission(resource: Resource, operation: Operation): boolean {
    if (!this.isResourceAllowed(resource)) {
      return false;
    }
    const operations = this.userPermissions.genericPermissions[Resource.ALL];
    if (operations) {
      return this.checkOperation(operations, operation);
    } else {
      return false;
    }
  }

  private hasGenericResourcePermission(resource: Resource, operation: Operation): boolean {
    const operations = this.userPermissions.genericPermissions[resource];
    if (operations) {
      return this.checkOperation(operations, operation);
    } else {
      return false;
    }
  }

  private checkOperation(operations: Operation[], operation: Operation): boolean {
    return operations.indexOf(Operation.ALL) > -1 || operations.indexOf(operation) > -1;
  }

  private checkEntityGroupPermission(operation: Operation, entityGroup: EntityGroupInfo, isGroup: boolean): boolean {
    if (!entityGroup) {
      return false;
    }
    let resource: Resource;
    if (isGroup) {
      resource = groupResourceByGroupType.get(entityGroup.type);
    } else {
      resource = resourceByEntityType.get(entityGroup.type);
    }
    if (this.isCurrentUserOwner(entityGroup)) {
      if (this.hasGenericPermission(resource, operation)) {
        return true;
      }
    }
    return this.hasGroupPermissions(entityGroup, operation, isGroup);
  }

  private hasGroupPermissions(entityGroup: EntityGroupInfo, operation: Operation, isGroup: boolean): boolean {
    if (!this.allowedGroupRoleOperations || this.allowedGroupRoleOperations.indexOf(operation) === -1) {
      return false;
    }
    if (isGroup) {
      if (this.allowedGroupOwnerOnlyGroupOperations && this.allowedGroupOwnerOnlyGroupOperations.indexOf(operation) > -1) {
        return false;
      }
    } else {
      if (this.allowedGroupOwnerOnlyOperations && this.allowedGroupOwnerOnlyOperations.indexOf(operation) > -1) {
        if (!this.isCurrentUserOwner(entityGroup)) {
          return false;
        }
      }
    }
    if (this.userPermissions && this.userPermissions.groupPermissions) {
      const permissionInfo = this.userPermissions.groupPermissions[entityGroup.id.id];
      return permissionInfo && this.checkOperation(permissionInfo.operations, operation);
    }
    return false;
  }

  private isCurrentUserOwner(entityGroup: EntityGroupInfo): boolean {
    const groupOwnerIds = entityGroup.ownerIds;
    if (this.userOwnerId && groupOwnerIds) {
      return this.containsId(groupOwnerIds, this.userOwnerId);
    } else {
      return false;
    }
  }

  private containsId(idsArray: EntityId[], id: EntityId): boolean {
    for (const arrayId of idsArray) {
      if (this.idsEqual(arrayId, id)) {
        return true;
      }
    }
    return false;
  }

  private idsEqual(id1: EntityId, id2: EntityId): boolean {
    return id1.id === id2.id && id1.entityType === id2.entityType;
  }

}

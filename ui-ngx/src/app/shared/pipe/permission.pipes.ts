// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Pipe, PipeTransform } from '@angular/core';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { EntityGroupInfo } from '@shared/models/entity-group.models';

@Pipe({
    name: 'hasGenericPermission',
    standalone: false
})
export class HasGenericPermissionPipe implements PipeTransform {

  constructor(private userPermissionsService: UserPermissionsService) {}

  transform(resource: Resource | Resource[], operation: Operation | Operation[]): boolean {
    return this.userPermissionsService.hasResourcesGenericPermission(resource, operation);
  }
}

@Pipe({
    name: 'hasEntityGroupPermission',
    standalone: false
})
export class HasEntityGroupPermissionPipe implements PipeTransform {

  constructor(private userPermissionsService: UserPermissionsService) {}

  transform(entityGroup: EntityGroupInfo, operation: Operation): boolean {
    return this.userPermissionsService.hasEntityGroupPermission(operation, entityGroup);
  }
}

@Pipe({
    name: 'hasGroupEntityPermission',
    standalone: false
})
export class HasGroupEntityPermissionPipe implements PipeTransform {

  constructor(private userPermissionsService: UserPermissionsService) {}

  transform(entityGroup: EntityGroupInfo, operation: Operation): boolean {
    return this.userPermissionsService.hasGroupEntityPermission(operation, entityGroup);
  }
}

@Pipe({
    name: 'hasGroupEntityOrGenericPermission',
    standalone: false
})
export class HasGroupEntityOrGenericPermissionPipe implements PipeTransform {

  constructor(private userPermissionsService: UserPermissionsService) {}

  transform(entityGroup: EntityGroupInfo, resource: Resource, operation: Operation): boolean {
    if (entityGroup) {
      return this.userPermissionsService.hasGroupEntityPermission(operation, entityGroup);
    } else {
      return this.userPermissionsService.hasResourcesGenericPermission(resource, operation);
    }
  }
}


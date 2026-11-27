// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { inject, Injectable } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Authority } from '@shared/models/authority.enum';
import { EntityType } from '@shared/models/entity-type.models';
import { Operation, Resource, resourceByEntityType } from '@shared/models/security.models';
import { alarmRuleEntityTypeList } from '@shared/models/alarm-rule.models';
import { getEntityDetailsPageURL } from '@core/utils';
import { HomeCardId } from '@shared/models/user-settings.models';

const ENTITY_LIST_PATHS: Partial<Record<EntityType, string>> = {
  [EntityType.DEVICE]: '/entities/devices',
  [EntityType.ASSET]: '/entities/assets',
  [EntityType.CUSTOMER]: '/customers',
  [EntityType.DASHBOARD]: '/dashboards'
};

@Injectable({
  providedIn: 'root'
})
export class HomePermissionsService {

  private userPermissions = inject(UserPermissionsService);
  private store = inject(Store<AppState>);

  get tenantAdmin(): boolean {
    return getCurrentAuthUser(this.store)?.authority === Authority.TENANT_ADMIN;
  }

  canReadEntities(entityType: EntityType): boolean {
    return this.userPermissions.hasReadGenericPermission(resourceByEntityType.get(entityType)) ||
      this.userPermissions.hasSharedReadGroupsPermission(entityType);
  }

  canReadAllEntities(entityType: EntityType): boolean {
    return this.userPermissions.hasReadGenericPermission(resourceByEntityType.get(entityType));
  }

  canCreateEntities(entityType: EntityType): boolean {
    return this.userPermissions.hasGenericPermission(resourceByEntityType.get(entityType), Operation.CREATE);
  }

  entitiesLink(entityType: EntityType): string | null {
    const base = ENTITY_LIST_PATHS[entityType];
    if (!base) {
      return null;
    }
    if (this.canReadAllEntities(entityType)) {
      return `${base}/all`;
    }
    return this.userPermissions.hasSharedReadGroupsPermission(entityType) ? `${base}/shared` : null;
  }

  entityDetailsLink(entityType: EntityType, id: string): string | null {
    if (!this.canReadAllEntities(entityType)) {
      return null;
    }
    return getEntityDetailsPageURL(id, entityType) || null;
  }

  canReadAlarms(): boolean {
    return this.userPermissions.hasReadGenericPermission(Resource.ALARM);
  }

  canWriteAlarms(): boolean {
    return this.userPermissions.hasGenericPermission(Resource.ALARM, Operation.WRITE);
  }

  canReadAlarmRules(): boolean {
    return this.tenantAdmin && alarmRuleEntityTypeList.some(entityType =>
      this.userPermissions.hasGenericPermissionByEntityGroupType(Operation.READ_CALCULATED_FIELD, entityType));
  }

  canAddAlarmRules(): boolean {
    return this.tenantAdmin && alarmRuleEntityTypeList.some(entityType =>
      this.userPermissions.hasGenericPermissionByEntityGroupType(Operation.WRITE_CALCULATED_FIELD, entityType));
  }

  canUseIotHub(): boolean {
    return this.tenantAdmin && this.userPermissions.hasGenericPermission(Resource.ALL, Operation.ALL);
  }

  canReadApiUsage(): boolean {
    return this.tenantAdmin && this.userPermissions.hasReadGenericPermission(Resource.API_USAGE_STATE) &&
      this.userPermissions.hasGenericPermission(Resource.API_USAGE_STATE, Operation.READ_TELEMETRY);
  }

  canReadUsageInfo(): boolean {
    return this.tenantAdmin && this.userPermissions.hasGenericPermission(Resource.ALL, Operation.READ);
  }

  canReadMobileAppSettings(): boolean {
    return this.userPermissions.hasReadGenericPermission(Resource.MOBILE_APP_SETTINGS);
  }

  canGetStarted(): boolean {
    return this.tenantAdmin && this.canAddAlarmRules() &&
      [EntityType.DEVICE, EntityType.DASHBOARD, EntityType.CUSTOMER].every(entityType =>
        this.canReadAllEntities(entityType) && this.canCreateEntities(entityType));
  }

  cardAvailable(id: HomeCardId): boolean {
    switch (id) {
      case HomeCardId.GET_STARTED:
        return this.canGetStarted();
      case HomeCardId.IOT_HUB:
        return this.canUseIotHub();
      case HomeCardId.ALARMS:
        return this.canReadAlarms();
      case HomeCardId.DASHBOARDS:
        return this.canReadEntities(EntityType.DASHBOARD);
      case HomeCardId.DEVICES:
        return this.canReadEntities(EntityType.DEVICE);
      case HomeCardId.ACTIVITY:
        return this.canReadApiUsage();
      case HomeCardId.USAGE:
        return this.canReadUsageInfo();
      case HomeCardId.MOBILE_APP:
        return this.canReadMobileAppSettings();
      default:
        return true;
    }
  }
}

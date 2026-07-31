///
/// Copyright © 2016-2026 The Thingsboard Authors
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
import { HttpErrorResponse } from '@angular/common/http';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { forkJoin, from, Observable, of, throwError } from 'rxjs';
import { catchError, map, mergeMap, switchMap, toArray } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { EntityService } from '@core/http/entity.service';
import {
  BrowserGeolocationError,
  browserGeolocationErrorMessageKey,
  BrowserGeolocationService
} from '@core/services/browser-geolocation.service';
import { AliasInfo } from '@core/api/widget-api.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { isDefinedAndNotNull, isNotEmptyStr, validateEntityId } from '@core/utils';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityInfo } from '@shared/models/entity.models';
import { PageData } from '@shared/models/page/page-data';
import { Direction } from '@shared/models/page/sort-order';
import { AttributeData, AttributeScope, LatestTelemetry } from '@shared/models/telemetry/telemetry.models';
import {
  entityDataToEntityInfo,
  EntityDataQuery,
  EntityFilter,
  entityInfoFields,
  EntityKeyType
} from '@shared/models/query/query.models';
import { WidgetMobileActionDescriptor } from '@shared/models/widget.models';
import {
  defaultLocationKeyMappings,
  LiveTrackingSaveInfo,
  LocationKey,
  LocationKeyMapping,
  locationKeyName,
  LocationKeyValueType,
  MobileActionAttributeSource,
  MobileActionLocationAccuracy,
  MobileActionTargetEntityConfig,
  MobileActionTargetEntityType,
  MobileLocationResult,
  SaveBrowserLocationDescriptor
} from '@shared/models/location.models';

export interface LocationTargetEntity {
  entityId: EntityId;
  name: string | null;
}

export interface LocationTargetResolution {
  targets: LocationTargetEntity[];
  totalTargets: number;
}

// Alias fan-out cap — savers report "saved X of Y" when the alias resolves more.
const aliasTargetsPageSize = 100;

// Bounds the request burst (up to aliasTargetsPageSize targets x 2 calls).
const saveConcurrency = 8;

/// Owns the location widget actions: resolving their target entities and
/// writing the coordinates as attributes/time series.
@Injectable({
  providedIn: 'root'
})
export class LocationService {

  constructor(private store: Store<AppState>,
              private translate: TranslateService,
              private entityService: EntityService,
              private browserGeolocationService: BrowserGeolocationService) {
  }

  /// Saves a position the mobile app reported back for a `getLocation` action.
  saveMobileActionLocation(ctx: WidgetContext, mobileAction: WidgetMobileActionDescriptor,
                           locationResult: MobileLocationResult,
                           currentEntityId?: EntityId): Observable<LiveTrackingSaveInfo> {
    const values: Partial<Record<LocationKey, any>> = {
      [LocationKey.latitude]: locationResult.latitude,
      [LocationKey.longitude]: locationResult.longitude,
      [LocationKey.accuracy]: locationResult.accuracy
    };
    return this.resolveTargets(ctx, mobileAction.targetEntity, currentEntityId).pipe(
      switchMap((resolution) => this.saveToTargets(ctx, resolution.targets, mobileAction.keys, values).pipe(
        map(({saved, firstError}) => {
          if (!saved.length) {
            throw firstError;
          }
          return {
            targetName: saved.map((target) => target.name).filter(Boolean).join(', ') || null,
            keys: this.savedKeyNames(mobileAction.keys, values)
          };
        })
      ))
    );
  }

  /// Runs the whole `saveBrowserLocation` action: reads the browser position,
  /// saves it and reports the outcome through the widget context toasts.
  saveBrowserLocation(ctx: WidgetContext, config: SaveBrowserLocationDescriptor, currentEntityId?: EntityId): void {
    if (!config) {
      return;
    }
    this.browserGeolocationService.getCurrentPosition().pipe(
      switchMap((position) => this.resolveTargets(ctx, config.targetEntity, currentEntityId, false).pipe(
        switchMap((resolution) => {
          const coords = position.coords;
          const values: Partial<Record<LocationKey, any>> = {
            [LocationKey.latitude]: coords.latitude,
            [LocationKey.longitude]: coords.longitude,
            [LocationKey.accuracy]: coords.accuracy,
            [LocationKey.altitude]: coords.altitude,
            [LocationKey.altitudeAccuracy]: coords.altitudeAccuracy,
            [LocationKey.speed]: coords.speed,
            [LocationKey.heading]: coords.heading
          };
          return this.saveToTargets(ctx, resolution.targets, config.keys, values).pipe(
            map(({saved, firstError}) => {
              if (!saved.length) {
                throw firstError;
              }
              return {
                savedKeys: this.savedKeyNames(config.keys, values),
                savedCount: saved.length,
                totalTargets: resolution.totalTargets
              };
            })
          );
        })
      ))
    ).subscribe({
      next: ({savedKeys, savedCount, totalTargets}) => {
        if (savedCount < totalTargets) {
          ctx.showWarnToast(
            this.translate.instant('widget-action.browser-location.location-saved-partial',
              {saved: savedCount, total: totalTargets, keys: savedKeys.join(', ')}));
        } else if (savedCount > 1) {
          ctx.showSuccessToast(
            this.translate.instant('widget-action.browser-location.location-saved-keys-multi',
              {count: savedCount, keys: savedKeys.join(', ')}));
        } else {
          ctx.showSuccessToast(savedKeys.length
            ? this.translate.instant('widget-action.browser-location.location-saved-keys', {keys: savedKeys.join(', ')})
            : this.translate.instant('widget-action.browser-location.location-saved'));
        }
      },
      error: (err) => {
        if (err instanceof BrowserGeolocationError) {
          ctx.showErrorToast(this.translate.instant(browserGeolocationErrorMessageKey(err)));
        } else {
          ctx.showErrorToast(
            this.translate.instant('widget-action.browser-location.location-save-failed',
              {error: this.saveErrorMessage(err)}));
        }
      }
    });
  }

  /// Builds the `startLiveLocation` bridge arguments. The app saves the samples
  /// itself, so the target entity and the key labels are resolved up front.
  liveTrackingArgs(ctx: WidgetContext, mobileAction: WidgetMobileActionDescriptor,
                   currentEntityId?: EntityId): Observable<any[]> {
    return this.resolveTargetEntity(ctx, mobileAction.targetEntity, currentEntityId).pipe(
      switchMap((targetEntityId) => this.resolveTargetEntityName(targetEntityId).pipe(
        map((targetName) => [{
          target: {
            entityType: targetEntityId.entityType,
            id: targetEntityId.id
          },
          targetName,
          dashboard: this.currentDashboardInfo(ctx),
          keys: this.keyMappings(mobileAction.keys).map((mapping) => ({
            key: mapping.key,
            label: locationKeyName(mapping),
            valueType: mapping.valueType
          })),
          accuracy: mobileAction.accuracy || MobileActionLocationAccuracy.balanced,
          distanceFilterMeters: mobileAction.distanceFilterMeters ?? null,
          intervalSeconds: mobileAction.intervalSeconds ?? null,
          maxDurationSeconds: mobileAction.maxDurationSeconds ?? null,
          trackedBy: getCurrentAuthUser(this.store)?.sub || null
        }])
      ))
    );
  }

  /// Mirrors the started session back to the action's launch result callback.
  liveTrackingInfo(config: any): LiveTrackingSaveInfo | null {
    if (!config) {
      return null;
    }
    return {
      targetName: config.targetName || null,
      keys: (config.keys || []).map((key: any) => key.label)
    };
  }

  saveErrorMessage(err: any): string {
    if (err instanceof HttpErrorResponse) {
      // ThingsBoard returns these errors as text/plain, so the body itself is the message.
      const body = err.error;
      if (isNotEmptyStr(body)) {
        return body;
      }
      if (isNotEmptyStr(body?.message)) {
        return body.message;
      }
      // Never fall back to err.message for HTTP errors — Angular's wording leaks the request URL.
      return this.translate.instant('widget-action.location.error-save-failed');
    }
    return isNotEmptyStr(err?.message) ? err.message
      : this.translate.instant('widget-action.location.error-save-failed');
  }

  private resolveTargets(ctx: WidgetContext, target: MobileActionTargetEntityConfig, currentEntityId?: EntityId,
                         resolveNames = true): Observable<LocationTargetResolution> {
    const type = target?.type || MobileActionTargetEntityType.currentEntity;
    if (type === MobileActionTargetEntityType.entityAlias) {
      return this.resolveEntityAliasTargets(ctx, target.aliasName);
    }
    return this.resolveTargetEntity(ctx, target, currentEntityId).pipe(
      switchMap((entityId) => (resolveNames ? this.resolveTargetEntityName(entityId) : of(null)).pipe(
        map((name) => ({targets: [{entityId, name}], totalTargets: 1}))
      ))
    );
  }

  private resolveEntityAliasTargets(ctx: WidgetContext, aliasName: string): Observable<LocationTargetResolution> {
    return this.aliasInfo(ctx, aliasName).pipe(
      mergeMap((aliasInfo) => this.findAliasEntities(aliasInfo, aliasTargetsPageSize)),
      map((page) => {
        const targets = (page.data || [])
          .filter((entity) => entity?.id && entity?.entityType)
          .map((entity) => ({
            entityId: {entityType: entity.entityType, id: entity.id} as EntityId,
            name: entity.name || null
          }));
        if (!targets.length) {
          throw new Error(
            this.translate.instant('widget-action.location.error-alias-not-resolved', {alias: aliasName}));
        }
        return {
          targets,
          totalTargets: Math.max(page.totalElements ?? 0, targets.length)
        };
      })
    );
  }

  private aliasInfo(ctx: WidgetContext, aliasName: string): Observable<AliasInfo> {
    const aliasId = ctx.aliasController.getEntityAliasId(aliasName);
    if (!aliasId) {
      return throwError(() => new Error(
        this.translate.instant('widget-action.location.error-alias-not-found', {alias: aliasName})));
    }
    return ctx.aliasController.getAliasInfo(aliasId);
  }

  /// Aliases resolving a single entity keep the one the dashboard already picked;
  /// the rest are queried by their filter, ordered by name so the page is stable.
  private findAliasEntities(aliasInfo: AliasInfo, pageSize: number): Observable<PageData<EntityInfo>> {
    if (!aliasInfo.resolveMultiple || !aliasInfo.entityFilter) {
      const currentEntity = aliasInfo.resolveMultiple ? null : aliasInfo.currentEntity;
      return of({
        data: currentEntity ? [currentEntity] : [],
        totalPages: 1,
        totalElements: currentEntity ? 1 : 0,
        hasNext: false
      });
    }
    return this.findEntityInfos(aliasInfo.entityFilter, pageSize);
  }

  private findEntityInfos(entityFilter: EntityFilter, pageSize: number): Observable<PageData<EntityInfo>> {
    const query: EntityDataQuery = {
      entityFilter,
      pageLink: {
        pageSize,
        page: 0,
        sortOrder: {
          key: {type: EntityKeyType.ENTITY_FIELD, key: 'name'},
          direction: Direction.ASC
        }
      },
      entityFields: entityInfoFields
    };
    return this.entityService.findEntityDataByQuery(query, {ignoreLoading: true, ignoreErrors: true}).pipe(
      map((data) => ({...data, data: data.data.map((entityData) => entityDataToEntityInfo(entityData))}))
    );
  }

  private resolveTargetEntity(ctx: WidgetContext, target: MobileActionTargetEntityConfig,
                              currentEntityId?: EntityId): Observable<EntityId> {
    const type = target?.type || MobileActionTargetEntityType.currentEntity;
    switch (type) {
      case MobileActionTargetEntityType.currentEntity:
        if (validateEntityId(currentEntityId)) {
          return of(currentEntityId);
        }
        return throwError(() => new Error(this.translate.instant('widget-action.location.error-no-current-entity')));
      case MobileActionTargetEntityType.currentUser:
        return of(this.currentUserEntityId());
      case MobileActionTargetEntityType.entityAlias:
        return this.resolveSingleEntityAlias(ctx, target.aliasName);
      case MobileActionTargetEntityType.fromAttribute:
        return this.resolveAttributeSourceEntity(ctx, target, currentEntityId).pipe(
          switchMap((sourceEntityId) => ctx.attributeService.getEntityAttributes(
            sourceEntityId, AttributeScope.SERVER_SCOPE, [target.attributeKey], {ignoreErrors: true})),
          map((attributes) => {
            const attribute = attributes.find(a => a.key === target.attributeKey);
            if (!attribute || !isDefinedAndNotNull(attribute.value)) {
              throw new Error(
                this.translate.instant('widget-action.location.error-attribute-not-found', {key: target.attributeKey}));
            }
            return this.parseTargetEntityAttributeValue(target.attributeKey, attribute.value);
          })
        );
    }
  }

  private resolveAttributeSourceEntity(ctx: WidgetContext, target: MobileActionTargetEntityConfig,
                                       currentEntityId?: EntityId): Observable<EntityId> {
    switch (target.attributeSource) {
      case MobileActionAttributeSource.currentEntity:
        return validateEntityId(currentEntityId) ? of(currentEntityId)
          : throwError(() => new Error(this.translate.instant('widget-action.location.error-no-current-entity')));
      case MobileActionAttributeSource.entityAlias:
        return this.resolveSingleEntityAlias(ctx, target.aliasName);
      default:
        return of(this.currentUserEntityId());
    }
  }

  private resolveSingleEntityAlias(ctx: WidgetContext, aliasName: string): Observable<EntityId> {
    return this.aliasInfo(ctx, aliasName).pipe(
      mergeMap((aliasInfo) => this.findAliasEntities(aliasInfo, 1)),
      map((page) => {
        const entity = page.data?.[0];
        if (!entity?.id || !entity?.entityType) {
          throw new Error(
            this.translate.instant('widget-action.location.error-alias-not-resolved', {alias: aliasName}));
        }
        return {entityType: entity.entityType, id: entity.id} as EntityId;
      })
    );
  }

  private resolveTargetEntityName(targetEntityId: EntityId): Observable<string | null> {
    return this.entityService.getEntity(targetEntityId.entityType as EntityType, targetEntityId.id,
      {ignoreLoading: true, ignoreErrors: true}).pipe(
      map((entity) => entity?.name || null),
      catchError(() => of(null))
    );
  }

  private currentUserEntityId(): EntityId {
    const authUser = getCurrentAuthUser(this.store);
    return {entityType: EntityType.USER, id: authUser.userId};
  }

  private currentDashboardInfo(ctx: WidgetContext): {id: string | null; title: string | null} {
    const dashboard = ctx.stateController?.dashboardCtrl?.dashboardCtx?.getDashboard();
    return {
      id: dashboard?.id?.id || null,
      title: dashboard?.title || null
    };
  }

  private parseTargetEntityAttributeValue(attributeKey: string, value: any): EntityId {
    let entityId = value;
    if (typeof entityId === 'string') {
      try {
        entityId = JSON.parse(entityId);
      } catch (e) {
        entityId = null;
      }
    }
    if (!entityId || typeof entityId !== 'object') {
      throw new Error(this.translate.instant('widget-action.location.error-attribute-not-object', {key: attributeKey}));
    }
    if (!entityId.entityType || !entityId.id) {
      throw new Error(this.translate.instant('widget-action.location.error-attribute-incomplete', {key: attributeKey}));
    }
    if (!Object.values(EntityType).includes(entityId.entityType)) {
      throw new Error(this.translate.instant('widget-action.location.error-attribute-unknown-entity-type',
        {key: attributeKey, entityType: entityId.entityType}));
    }
    return {entityType: entityId.entityType, id: entityId.id};
  }

  private keyMappings(keys?: LocationKeyMapping[]): LocationKeyMapping[] {
    return keys?.length ? keys : defaultLocationKeyMappings();
  }

  private savedKeyNames(keys: LocationKeyMapping[], values: Partial<Record<LocationKey, any>>): string[] {
    return this.keyMappings(keys)
      .filter((mapping) => {
        const value = values[mapping.key];
        return isDefinedAndNotNull(value) && !Number.isNaN(value);
      })
      .map((mapping) => locationKeyName(mapping));
  }

  private saveToTargets(ctx: WidgetContext, targets: LocationTargetEntity[], keys: LocationKeyMapping[],
                        values: Partial<Record<LocationKey, any>>):
      Observable<{saved: LocationTargetEntity[]; firstError: any}> {
    return from(targets).pipe(
      mergeMap((target) => this.saveKeys(ctx, target.entityId, keys, values).pipe(
        map(() => ({target, error: null})),
        catchError((error) => of({target, error}))
      ), saveConcurrency),
      toArray(),
      map((results) => ({
        saved: results.filter((result) => !result.error).map((result) => result.target),
        firstError: results.find((result) => result.error)?.error ?? null
      }))
    );
  }

  private saveKeys(ctx: WidgetContext, targetEntityId: EntityId, keys: LocationKeyMapping[],
                   values: Partial<Record<LocationKey, any>>): Observable<any> {
    const attributes: Array<AttributeData> = [];
    const timeseries: Array<AttributeData> = [];
    this.keyMappings(keys).forEach((mapping) => {
      const value = values[mapping.key];
      if (isDefinedAndNotNull(value) && !Number.isNaN(value)) {
        const data: AttributeData = {key: locationKeyName(mapping), value};
        (mapping.valueType === LocationKeyValueType.timeseries ? timeseries : attributes).push(data);
      }
    });
    const saveObservables: Array<Observable<any>> = [];
    if (attributes.length) {
      saveObservables.push(ctx.attributeService.saveEntityAttributes(
        targetEntityId, AttributeScope.SERVER_SCOPE, attributes, {ignoreErrors: true}));
    }
    if (timeseries.length) {
      saveObservables.push(ctx.attributeService.saveEntityTimeseries(
        targetEntityId, LatestTelemetry.LATEST_TELEMETRY, timeseries, {ignoreErrors: true}));
    }
    return saveObservables.length ? forkJoin(saveObservables) : of(null);
  }
}

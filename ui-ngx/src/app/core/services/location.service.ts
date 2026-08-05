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
import { DomSanitizer } from '@angular/platform-browser';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { forkJoin, Observable, of, throwError } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { EntityService } from '@core/http/entity.service';
import { WidgetContext } from '@home/models/widget-component.models';
import { isDefinedAndNotNull, isNotEmptyStr, parseHttpErrorMessage, validateEntityId } from '@core/utils';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { AttributeData, AttributeScope, LatestTelemetry } from '@shared/models/telemetry/telemetry.models';
import { WidgetMobileActionDescriptor } from '@shared/models/widget.models';
import {
  BrowserGeolocationErrorType,
  browserGeolocationErrorTranslationMap,
  defaultLocationKeyMappings,
  LiveTrackingConfig,
  LiveTrackingSaveInfo,
  LocationKey,
  LocationKeyMapping,
  locationKeyName,
  LocationKeyValueType,
  MobileActionAttributeSource,
  MobileActionLocationAccuracy,
  MobileActionTargetEntityConfig,
  MobileActionTargetIndirection,
  MobileLocationResult,
  SaveBrowserLocationDescriptor
} from '@shared/models/location.models';

@Injectable({
  providedIn: 'root'
})
export class LocationService {

  constructor(private store: Store<AppState>,
              private translate: TranslateService,
              private sanitizer: DomSanitizer,
              private entityService: EntityService) {
  }

  saveMobileActionLocation(ctx: WidgetContext, mobileAction: WidgetMobileActionDescriptor,
                           locationResult: MobileLocationResult,
                           currentEntityId?: EntityId): Observable<LiveTrackingSaveInfo> {
    const values = this.locationValues(locationResult.latitude, locationResult.longitude, locationResult.accuracy);
    return this.resolveTargetEntity(ctx, mobileAction.targetEntity, currentEntityId).pipe(
      switchMap((targetEntityId) => this.resolveTargetEntityName(targetEntityId).pipe(
        switchMap((targetName) => this.saveKeys(ctx, targetEntityId, mobileAction.keys, values).pipe(
          map(() => ({
            targetName,
            keys: this.savedKeyNames(mobileAction.keys, values)
          }))
        ))
      )),
      catchError((err) => throwError(() => new Error(this.saveErrorMessage(err))))
    );
  }

  saveBrowserLocation(ctx: WidgetContext, config: SaveBrowserLocationDescriptor, currentEntityId?: EntityId): void {
    if (!config) {
      return;
    }
    this.getCurrentPosition().pipe(
      switchMap((position) => this.resolveTargetEntity(ctx, config.targetEntity, currentEntityId).pipe(
        switchMap((targetEntityId) => {
          const coords = position.coords;
          const values = this.locationValues(coords.latitude, coords.longitude, coords.accuracy);
          return this.saveKeys(ctx, targetEntityId, config.keys, values);
        })
      ))
    ).subscribe({
      next: () => {
        ctx.showSuccessToast(this.translate.instant('widget-action.browser-location.location-saved'));
      },
      error: (err) => {
        const geolocationErrorKey = browserGeolocationErrorTranslationMap.get(err?.message as BrowserGeolocationErrorType);
        ctx.showErrorToast(geolocationErrorKey
          ? this.translate.instant(geolocationErrorKey)
          : this.translate.instant('widget-action.browser-location.location-save-failed',
            {error: this.saveErrorMessage(err)}));
      }
    });
  }

  liveTrackingArgs(ctx: WidgetContext, mobileAction: WidgetMobileActionDescriptor,
                   currentEntityId?: EntityId): Observable<[LiveTrackingConfig]> {
    return this.resolveTargetEntity(ctx, mobileAction.targetEntity, currentEntityId).pipe(
      switchMap((targetEntityId) => this.resolveTargetEntityName(targetEntityId).pipe(
        map((targetName): [LiveTrackingConfig] => [{
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
          accuracy: mobileAction.accuracy || MobileActionLocationAccuracy.BALANCED,
          distanceFilterMeters: mobileAction.distanceFilterMeters ?? null,
          intervalSeconds: mobileAction.intervalSeconds ?? null,
          maxDurationSeconds: mobileAction.maxDurationSeconds ?? null,
          trackedBy: getCurrentAuthUser(this.store)?.sub || null
        }])
      )),
      catchError((err) => throwError(() => new Error(this.saveErrorMessage(err))))
    );
  }

  liveTrackingInfo(config: LiveTrackingConfig): LiveTrackingSaveInfo | null {
    if (!config) {
      return null;
    }
    return {
      targetName: config.targetName || null,
      keys: (config.keys || []).map((key) => key.label)
    };
  }

  private getCurrentPosition(): Observable<GeolocationPosition> {
    return new Observable<GeolocationPosition>((subscriber) => {
      if (!window.isSecureContext) {
        subscriber.error(new Error(BrowserGeolocationErrorType.insecureContext));
        return;
      }
      if (!navigator.geolocation) {
        subscriber.error(new Error(BrowserGeolocationErrorType.unsupported));
        return;
      }
      navigator.geolocation.getCurrentPosition(
        (position) => {
          subscriber.next(position);
          subscriber.complete();
        },
        (error) => {
          switch (error.code) {
            case error.PERMISSION_DENIED:
              subscriber.error(new Error(BrowserGeolocationErrorType.permissionDenied));
              break;
            case error.TIMEOUT:
              subscriber.error(new Error(BrowserGeolocationErrorType.timeout));
              break;
            default:
              subscriber.error(new Error(BrowserGeolocationErrorType.positionUnavailable));
          }
        },
        { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
      );
    });
  }

  private saveErrorMessage(err: any): string {
    if (err instanceof HttpErrorResponse) {
      return parseHttpErrorMessage(err, this.translate, undefined, this.sanitizer).message;
    }
    return isNotEmptyStr(err?.message) ? err.message
      : this.translate.instant('widget-action.location.error-save-failed');
  }

  private resolveTargetEntity(ctx: WidgetContext, target: MobileActionTargetEntityConfig,
                              currentEntityId?: EntityId): Observable<EntityId> {
    const type = target?.type || MobileActionAttributeSource.CURRENT_ENTITY;
    switch (type) {
      case MobileActionAttributeSource.CURRENT_ENTITY:
        if (validateEntityId(currentEntityId)) {
          return of(currentEntityId);
        }
        return throwError(() => new Error(this.translate.instant('widget-action.location.error-no-current-entity')));
      case MobileActionAttributeSource.CURRENT_USER:
        return this.currentUserEntityId();
      case MobileActionAttributeSource.ENTITY_ALIAS:
        return this.resolveEntityAlias(ctx, target.aliasName);
      case MobileActionTargetIndirection.FROM_ATTRIBUTE:
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
      default:
        return throwError(() => new Error(
          this.translate.instant('widget-action.location.error-unknown-target-type', {type})));
    }
  }

  private resolveAttributeSourceEntity(ctx: WidgetContext, target: MobileActionTargetEntityConfig,
                                       currentEntityId?: EntityId): Observable<EntityId> {
    switch (target.attributeSource) {
      case MobileActionAttributeSource.CURRENT_ENTITY:
        return validateEntityId(currentEntityId) ? of(currentEntityId)
          : throwError(() => new Error(this.translate.instant('widget-action.location.error-no-current-entity')));
      case MobileActionAttributeSource.ENTITY_ALIAS:
        return this.resolveEntityAlias(ctx, target.aliasName);
      default:
        return this.currentUserEntityId();
    }
  }

  private resolveEntityAlias(ctx: WidgetContext, aliasName: string): Observable<EntityId> {
    const aliasId = ctx.aliasController.getEntityAliasId(aliasName);
    if (!aliasId) {
      return throwError(() => new Error(
        this.translate.instant('widget-action.location.error-alias-not-found', {alias: aliasName})));
    }
    return ctx.aliasController.resolveSingleEntityInfo(aliasId).pipe(
      map((entity) => {
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

  private currentUserEntityId(): Observable<EntityId> {
    const userId = getCurrentAuthUser(this.store)?.userId;
    if (!userId) {
      return throwError(() => new Error(this.translate.instant('widget-action.location.error-no-current-user')));
    }
    return of({entityType: EntityType.USER, id: userId});
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

  private hasLocationValue(values: Partial<Record<LocationKey, any>>, key: LocationKey): boolean {
    const value = values[key];
    return isDefinedAndNotNull(value) && !Number.isNaN(value);
  }

  private savedKeyNames(keys: LocationKeyMapping[], values: Partial<Record<LocationKey, any>>): string[] {
    return this.keyMappings(keys)
      .filter((mapping) => this.hasLocationValue(values, mapping.key))
      .map((mapping) => locationKeyName(mapping));
  }

  private locationValues(latitude: number, longitude: number, accuracy?: number): Partial<Record<LocationKey, any>> {
    return {
      [LocationKey.LATITUDE]: latitude,
      [LocationKey.LONGITUDE]: longitude,
      [LocationKey.ACCURACY]: accuracy
    };
  }

  private saveKeys(ctx: WidgetContext, targetEntityId: EntityId, keys: LocationKeyMapping[],
                   values: Partial<Record<LocationKey, any>>): Observable<any> {
    const attributes: Array<AttributeData> = [];
    const timeseries: Array<AttributeData> = [];
    this.keyMappings(keys).forEach((mapping) => {
      if (this.hasLocationValue(values, mapping.key)) {
        const data: AttributeData = {key: locationKeyName(mapping), value: values[mapping.key]};
        (mapping.valueType === LocationKeyValueType.TIMESERIES ? timeseries : attributes).push(data);
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

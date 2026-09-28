// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable, SecurityContext } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { DomSanitizer } from '@angular/platform-browser';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { forkJoin, Observable, of, throwError } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { EntityService } from '@core/http/entity.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { WidgetContext } from '@home/models/widget-component.models';
import { isDefinedAndNotNull, isNotEmptyStr, parseHttpErrorMessage, validateEntityId } from '@core/utils';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { Operation } from '@shared/models/security.models';
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
  LocationTargetEntityConfig,
  LocationTargetIndirection,
  LocationTargetSource,
  MobileActionLocationAccuracy,
  MobileLocationResult,
  SaveBrowserLocationDescriptor
} from '@shared/models/location.models';
import { MobileService } from '@core/services/mobile.service';

@Injectable({
  providedIn: 'root'
})
export class LocationService {

  constructor(private store: Store<AppState>,
              private translate: TranslateService,
              private sanitizer: DomSanitizer,
              private mobileService: MobileService,
              private entityService: EntityService,
              private userPermissionsService: UserPermissionsService) {
  }

  saveMobileActionLocation(ctx: WidgetContext, mobileAction: WidgetMobileActionDescriptor,
                           locationResult: MobileLocationResult,
                           currentEntityId?: EntityId): Observable<LiveTrackingSaveInfo> {
    const values = this.locationValues(locationResult.latitude, locationResult.longitude, locationResult.accuracy);
    return this.resolveTargetEntity(ctx, mobileAction.targetEntity, currentEntityId).pipe(
      switchMap((targetEntityId) => this.resolveTargetEntityName(targetEntityId).pipe(
        switchMap((targetName) => this.saveKeys(ctx, targetEntityId, mobileAction.keys, values).pipe(
          map(() => ({targetName}))
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
                   currentEntityId?: EntityId): Observable<[LiveTrackingConfig] | []> {
    // Outside the mobile app, skip config resolution: it may fail (e.g. an unresolvable alias)
    // and surface an error dialog before the dispatcher reaches its non-mobile fallback toast.
    if (!this.mobileService.isMobileApp()) {
      return of([]);
    }
    return this.resolveTargetEntity(ctx, mobileAction.targetEntity, currentEntityId).pipe(
      switchMap((targetEntityId) => this.checkWritePermissions(targetEntityId,
        this.requiredWriteOperations(this.keyMappings(mobileAction.keys))).pipe(
        switchMap(() => this.resolveTargetEntityName(targetEntityId)),
        map((targetName): [LiveTrackingConfig] => [{
          target: {
            entityType: targetEntityId.entityType,
            id: targetEntityId.id
          },
          targetName,
          dashboard: this.currentDashboardInfo(ctx),
          keys: this.keyMappings(mobileAction.keys).map((mapping) => ({
            key: mapping.argument,
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
      targetName: config.targetName || null
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
    return isNotEmptyStr(err?.message) ? this.sanitizer.sanitize(SecurityContext.HTML, err.message)
      : this.translate.instant('widget-action.location.error-save-failed');
  }

  private resolveTargetEntity(ctx: WidgetContext, target: LocationTargetEntityConfig,
                              currentEntityId?: EntityId): Observable<EntityId> {
    const type = target?.type || LocationTargetSource.CURRENT_ENTITY;
    switch (type) {
      case LocationTargetSource.CURRENT_ENTITY:
        if (validateEntityId(currentEntityId)) {
          return of(currentEntityId);
        }
        return throwError(() => new Error(this.translate.instant('widget-action.location.error-no-current-entity')));
      case LocationTargetSource.CURRENT_USER:
        return this.currentUserEntityId();
      case LocationTargetSource.ENTITY_ALIAS:
        return this.resolveEntityAlias(ctx, target.aliasName);
      case LocationTargetIndirection.FROM_ATTRIBUTE:
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

  private resolveAttributeSourceEntity(ctx: WidgetContext, target: LocationTargetEntityConfig,
                                       currentEntityId?: EntityId): Observable<EntityId> {
    switch (target.attributeSource) {
      case LocationTargetSource.CURRENT_ENTITY:
        return validateEntityId(currentEntityId) ? of(currentEntityId)
          : throwError(() => new Error(this.translate.instant('widget-action.location.error-no-current-entity')));
      case LocationTargetSource.ENTITY_ALIAS:
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
      if (this.hasLocationValue(values, mapping.argument)) {
        const data: AttributeData = {key: locationKeyName(mapping), value: values[mapping.argument]};
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

  private requiredWriteOperations(mappings: LocationKeyMapping[]): Operation[] {
    const operations: Operation[] = [];
    if (mappings.some((mapping) => mapping.valueType !== LocationKeyValueType.TIMESERIES)) {
      operations.push(Operation.WRITE_ATTRIBUTES);
    }
    if (mappings.some((mapping) => mapping.valueType === LocationKeyValueType.TIMESERIES)) {
      operations.push(Operation.WRITE_TELEMETRY);
    }
    return operations;
  }

  /// Group permissions may deny telemetry writes on the resolved target, so it is
  /// checked up front to fail with a clear message instead of a raw server error.
  /// A failed check itself is treated as permitted: the save attempt stays the
  /// authority on whether the write is allowed.
  private checkWritePermissions(targetEntityId: EntityId, operations: Operation[]): Observable<null> {
    if (!operations.length) {
      return of(null);
    }
    return forkJoin(operations.map((operation) =>
      this.userPermissionsService.hasEntityPermission(targetEntityId, operation,
        {ignoreLoading: true, ignoreErrors: true}).pipe(
        catchError(() => of(true))
      )
    )).pipe(
      map((results) => {
        if (results.some((allowed) => !allowed)) {
          throw new Error(this.translate.instant('widget-action.location.error-permission-denied'));
        }
        return null;
      })
    );
  }
}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { isNotEmptyStr } from '@core/utils';
import { EntityId } from '@shared/models/id/entity-id';
import { TbFunction } from '@shared/models/js-function.models';
import { ProcessLaunchResultDescriptor } from '@shared/models/widget.models';

export interface LiveTrackingSaveInfo {
  targetName: string | null;
}

export interface MobileLocationResult {
  latitude: number;
  longitude: number;
  accuracy?: number;
}

export enum LocationTargetSource {
  CURRENT_ENTITY = 'CURRENT_ENTITY',
  CURRENT_USER = 'CURRENT_USER',
  ENTITY_ALIAS = 'ENTITY_ALIAS'
}

export enum LocationTargetIndirection {
  FROM_ATTRIBUTE = 'FROM_ATTRIBUTE'
}

export type LocationTargetEntityType = LocationTargetSource | LocationTargetIndirection;

export const locationTargetSourceTranslationMap = new Map<LocationTargetSource, string>(
  [
    [ LocationTargetSource.CURRENT_ENTITY, 'widget-action.mobile.target-current-entity' ],
    [ LocationTargetSource.CURRENT_USER, 'widget-action.mobile.target-current-user' ],
    [ LocationTargetSource.ENTITY_ALIAS, 'widget-action.mobile.target-entity-alias' ]
  ]
);

export interface LocationTargetEntityConfig {
  type: LocationTargetEntityType;
  aliasName?: string;
  attributeSource?: LocationTargetSource;
  attributeKey?: string;
}

export enum LocationTargetEntityMode {
  ENTITY = 'ENTITY',
  FROM_ATTRIBUTE = 'FROM_ATTRIBUTE'
}

export enum LocationKey {
  LATITUDE = 'LATITUDE',
  LONGITUDE = 'LONGITUDE',
  ACCURACY = 'ACCURACY',
  ALTITUDE = 'ALTITUDE',
  SPEED = 'SPEED',
  HEADING = 'HEADING',
  GPS_ACTIVE = 'GPS_ACTIVE',
  GPS_TRACKED_BY = 'GPS_TRACKED_BY'
}

export const locationKeyTranslationMap = new Map<LocationKey, string>(
  [
    [ LocationKey.LATITUDE, 'widget-action.location.key-latitude' ],
    [ LocationKey.LONGITUDE, 'widget-action.location.key-longitude' ],
    [ LocationKey.ACCURACY, 'widget-action.location.key-accuracy' ],
    [ LocationKey.ALTITUDE, 'widget-action.location.key-altitude' ],
    [ LocationKey.SPEED, 'widget-action.location.key-speed' ],
    [ LocationKey.HEADING, 'widget-action.location.key-heading' ],
    [ LocationKey.GPS_ACTIVE, 'widget-action.location.key-gps-active' ],
    [ LocationKey.GPS_TRACKED_BY, 'widget-action.location.key-gps-tracked-by' ]
  ]
);

export enum LocationKeyValueType {
  ATTRIBUTE = 'ATTRIBUTE',
  TIMESERIES = 'TIMESERIES'
}

export const locationKeyValueTypeTranslationMap = new Map<LocationKeyValueType, string>(
  [
    [ LocationKeyValueType.ATTRIBUTE, 'widget-action.location.value-type-attribute' ],
    [ LocationKeyValueType.TIMESERIES, 'widget-action.location.value-type-timeseries' ]
  ]
);

export interface LocationKeyMapping {
  argument: LocationKey;
  keyName?: string;
  valueType: LocationKeyValueType;
}

export const locationKeyDefaultNameMap = new Map<LocationKey, string>(
  [
    [ LocationKey.LATITUDE, 'latitude' ],
    [ LocationKey.LONGITUDE, 'longitude' ],
    [ LocationKey.ACCURACY, 'gpsAccuracy' ],
    [ LocationKey.ALTITUDE, 'gpsAltitude' ],
    [ LocationKey.SPEED, 'gpsSpeed' ],
    [ LocationKey.HEADING, 'gpsHeading' ],
    [ LocationKey.GPS_ACTIVE, 'gpsActive' ],
    [ LocationKey.GPS_TRACKED_BY, 'gpsTrackedBy' ]
  ]
);

export const locationKeyDefaultValueTypeMap = new Map<LocationKey, LocationKeyValueType>(
  [
    [ LocationKey.LATITUDE, LocationKeyValueType.ATTRIBUTE ],
    [ LocationKey.LONGITUDE, LocationKeyValueType.ATTRIBUTE ],
    [ LocationKey.ACCURACY, LocationKeyValueType.TIMESERIES ],
    [ LocationKey.ALTITUDE, LocationKeyValueType.TIMESERIES ],
    [ LocationKey.SPEED, LocationKeyValueType.TIMESERIES ],
    [ LocationKey.HEADING, LocationKeyValueType.TIMESERIES ],
    [ LocationKey.GPS_ACTIVE, LocationKeyValueType.ATTRIBUTE ],
    [ LocationKey.GPS_TRACKED_BY, LocationKeyValueType.ATTRIBUTE ]
  ]
);

export const mandatoryLocationKeys: LocationKey[] = [LocationKey.LATITUDE, LocationKey.LONGITUDE];

export const getLocationKeys: LocationKey[] = [...mandatoryLocationKeys, LocationKey.ACCURACY];

export const liveLocationKeys: LocationKey[] = [...getLocationKeys, LocationKey.ALTITUDE,
  LocationKey.SPEED, LocationKey.HEADING, LocationKey.GPS_ACTIVE, LocationKey.GPS_TRACKED_BY];

export const locationKeyName = (mapping: LocationKeyMapping): string =>
  isNotEmptyStr(mapping?.keyName?.trim()) ? mapping.keyName.trim() : locationKeyDefaultNameMap.get(mapping?.argument);

export const locationKeyMapping = (argument: LocationKey): LocationKeyMapping =>
  ({argument, keyName: locationKeyDefaultNameMap.get(argument),
    valueType: locationKeyDefaultValueTypeMap.get(argument)});

export const defaultLocationKeyMappings = (): LocationKeyMapping[] =>
  mandatoryLocationKeys.map(argument => locationKeyMapping(argument));

export interface LocationTargetDescriptor {
  targetEntity?: LocationTargetEntityConfig;
  keys?: LocationKeyMapping[];
}

export interface SaveLocationDescriptor extends LocationTargetDescriptor {
  saveToEntity?: boolean;
}

export interface GetLocationDescriptor extends SaveLocationDescriptor {
  processLocationFunction: TbFunction;
}

export enum MobileActionLocationAccuracy {
  HIGH = 'HIGH',
  BALANCED = 'BALANCED',
  LOW = 'LOW'
}

export const mobileActionLocationAccuracyTranslationMap = new Map<MobileActionLocationAccuracy, string>(
  [
    [ MobileActionLocationAccuracy.HIGH, 'widget-action.mobile.accuracy-high' ],
    [ MobileActionLocationAccuracy.BALANCED, 'widget-action.mobile.accuracy-balanced' ],
    [ MobileActionLocationAccuracy.LOW, 'widget-action.mobile.accuracy-low' ]
  ]
);

export const mobileActionLocationAccuracyHintMap = new Map<MobileActionLocationAccuracy, string>(
  [
    [ MobileActionLocationAccuracy.HIGH, 'widget-action.mobile.accuracy-high-hint' ],
    [ MobileActionLocationAccuracy.BALANCED, 'widget-action.mobile.accuracy-balanced-hint' ],
    [ MobileActionLocationAccuracy.LOW, 'widget-action.mobile.accuracy-low-hint' ]
  ]
);

export interface StartLiveLocationDescriptor extends ProcessLaunchResultDescriptor, LocationTargetDescriptor {
  accuracy?: MobileActionLocationAccuracy;
  distanceFilterMeters?: number;
  intervalSeconds?: number;
  maxDurationSeconds?: number;
}

export interface LiveTrackingKey {
  key: LocationKey;
  label: string;
  valueType: LocationKeyValueType;
}

export interface LiveTrackingConfig {
  target: EntityId;
  targetName: string | null;
  dashboard: {id: string | null; title: string | null};
  keys: LiveTrackingKey[];
  accuracy: MobileActionLocationAccuracy;
  distanceFilterMeters: number | null;
  intervalSeconds: number | null;
  maxDurationSeconds: number | null;
  trackedBy: string | null;
}

export enum BrowserGeolocationErrorType {
  unsupported = 'unsupported',
  insecureContext = 'insecureContext',
  permissionDenied = 'permissionDenied',
  positionUnavailable = 'positionUnavailable',
  timeout = 'timeout'
}

export const browserGeolocationErrorTranslationMap = new Map<BrowserGeolocationErrorType, string>(
  [
    [ BrowserGeolocationErrorType.unsupported, 'widget-action.browser-location.error-unsupported' ],
    [ BrowserGeolocationErrorType.insecureContext, 'widget-action.browser-location.error-insecure-context' ],
    [ BrowserGeolocationErrorType.permissionDenied, 'widget-action.browser-location.error-permission-denied' ],
    [ BrowserGeolocationErrorType.positionUnavailable, 'widget-action.browser-location.error-position-unavailable' ],
    [ BrowserGeolocationErrorType.timeout, 'widget-action.browser-location.error-timeout' ]
  ]
);

export type SaveBrowserLocationDescriptor = LocationTargetDescriptor;

export const defaultSaveBrowserLocationDescriptor = (): SaveBrowserLocationDescriptor => ({
  targetEntity: {type: LocationTargetSource.CURRENT_ENTITY},
  keys: defaultLocationKeyMappings()
});

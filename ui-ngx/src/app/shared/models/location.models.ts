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

import { isNotEmptyStr } from '@core/utils';
import { AliasFilterType } from '@shared/models/alias.models';
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

export enum MobileActionAttributeSource {
  CURRENT_ENTITY = 'CURRENT_ENTITY',
  CURRENT_USER = 'CURRENT_USER',
  ENTITY_ALIAS = 'ENTITY_ALIAS'
}

export enum MobileActionTargetIndirection {
  FROM_ATTRIBUTE = 'FROM_ATTRIBUTE'
}

export type MobileActionTargetEntityType = MobileActionAttributeSource | MobileActionTargetIndirection;

export const mobileActionAttributeSourceTranslationMap = new Map<MobileActionAttributeSource, string>(
  [
    [ MobileActionAttributeSource.CURRENT_ENTITY, 'widget-action.mobile.target-current-entity' ],
    [ MobileActionAttributeSource.CURRENT_USER, 'widget-action.mobile.target-current-user' ],
    [ MobileActionAttributeSource.ENTITY_ALIAS, 'widget-action.mobile.target-entity-alias' ]
  ]
);

export interface MobileActionTargetEntityConfig {
  type: MobileActionTargetEntityType;
  aliasName?: string;
  attributeSource?: MobileActionAttributeSource;
  attributeKey?: string;
}

export const queryableAliasFilterTypes: AliasFilterType[] =
  Object.values(AliasFilterType).filter((type) => type !== AliasFilterType.stateEntity);

export const rootedAliasFilterTypes: AliasFilterType[] = [
  AliasFilterType.relationsQuery,
  AliasFilterType.assetSearchQuery,
  AliasFilterType.deviceSearchQuery,
  AliasFilterType.edgeSearchQuery,
  AliasFilterType.entityViewSearchQuery
];

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
  key: LocationKey;
  label?: string;
  valueType: LocationKeyValueType;
}

export const locationKeyDefaultLabelMap = new Map<LocationKey, string>(
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
  isNotEmptyStr(mapping?.label?.trim()) ? mapping.label.trim() : locationKeyDefaultLabelMap.get(mapping?.key);

export const locationKeyMapping = (key: LocationKey): LocationKeyMapping =>
  ({key, label: locationKeyDefaultLabelMap.get(key), valueType: locationKeyDefaultValueTypeMap.get(key)});

export const defaultLocationKeyMappings = (): LocationKeyMapping[] =>
  mandatoryLocationKeys.map(key => locationKeyMapping(key));

export interface LocationTargetDescriptor {
  targetEntity?: MobileActionTargetEntityConfig;
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
  targetEntity: {type: MobileActionAttributeSource.CURRENT_ENTITY},
  keys: defaultLocationKeyMappings()
});

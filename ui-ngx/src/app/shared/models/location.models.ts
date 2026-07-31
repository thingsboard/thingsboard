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
import { TbFunction } from '@shared/models/js-function.models';
import { ProcessLaunchResultDescriptor } from '@shared/models/widget.models';

export interface LiveTrackingSaveInfo {
  targetName: string;
  keys: string[];
}

export interface MobileLocationResult {
  latitude: number;
  longitude: number;
  accuracy?: number;
  ts?: number;
}

export enum MobileActionTargetEntityType {
  currentEntity = 'CURRENT_ENTITY',
  currentUser = 'CURRENT_USER',
  entityAlias = 'ENTITY_ALIAS',
  fromAttribute = 'FROM_ATTRIBUTE'
}

export enum MobileActionAttributeSource {
  currentEntity = 'CURRENT_ENTITY',
  currentUser = 'CURRENT_USER',
  entityAlias = 'ENTITY_ALIAS'
}

export const mobileActionAttributeSourceTranslationMap = new Map<MobileActionAttributeSource, string>(
  [
    [ MobileActionAttributeSource.currentEntity, 'widget-action.mobile.target-current-entity' ],
    [ MobileActionAttributeSource.currentUser, 'widget-action.mobile.target-current-user' ],
    [ MobileActionAttributeSource.entityAlias, 'widget-action.mobile.target-entity-alias' ]
  ]
);

export interface MobileActionTargetEntityConfig {
  type: MobileActionTargetEntityType;
  aliasName?: string;
  attributeSource?: MobileActionAttributeSource;
  attributeKey?: string;
}

export enum LocationKey {
  latitude = 'LATITUDE',
  longitude = 'LONGITUDE',
  accuracy = 'ACCURACY',
  altitude = 'ALTITUDE',
  altitudeAccuracy = 'ALTITUDE_ACCURACY',
  speed = 'SPEED',
  heading = 'HEADING',
  gpsActive = 'GPS_ACTIVE',
  gpsTrackedBy = 'GPS_TRACKED_BY'
}

export const locationKeyTranslationMap = new Map<LocationKey, string>(
  [
    [ LocationKey.latitude, 'widget-action.location.key-latitude' ],
    [ LocationKey.longitude, 'widget-action.location.key-longitude' ],
    [ LocationKey.accuracy, 'widget-action.location.key-accuracy' ],
    [ LocationKey.altitude, 'widget-action.location.key-altitude' ],
    [ LocationKey.altitudeAccuracy, 'widget-action.location.key-altitude-accuracy' ],
    [ LocationKey.speed, 'widget-action.location.key-speed' ],
    [ LocationKey.heading, 'widget-action.location.key-heading' ],
    [ LocationKey.gpsActive, 'widget-action.location.key-gps-active' ],
    [ LocationKey.gpsTrackedBy, 'widget-action.location.key-gps-tracked-by' ]
  ]
);

export enum LocationKeyValueType {
  attribute = 'ATTRIBUTE',
  timeseries = 'TIMESERIES'
}

export const locationKeyValueTypeTranslationMap = new Map<LocationKeyValueType, string>(
  [
    [ LocationKeyValueType.attribute, 'widget-action.location.value-type-attribute' ],
    [ LocationKeyValueType.timeseries, 'widget-action.location.value-type-timeseries' ]
  ]
);

export interface LocationKeyMapping {
  key: LocationKey;
  label?: string;
  valueType: LocationKeyValueType;
}

export const locationKeyDefaultLabelMap = new Map<LocationKey, string>(
  [
    [ LocationKey.latitude, 'latitude' ],
    [ LocationKey.longitude, 'longitude' ],
    [ LocationKey.accuracy, 'gpsAccuracy' ],
    [ LocationKey.altitude, 'gpsAltitude' ],
    [ LocationKey.altitudeAccuracy, 'gpsAltitudeAccuracy' ],
    [ LocationKey.speed, 'gpsSpeed' ],
    [ LocationKey.heading, 'gpsHeading' ],
    [ LocationKey.gpsActive, 'gpsActive' ],
    [ LocationKey.gpsTrackedBy, 'gpsTrackedBy' ]
  ]
);

export const locationKeyDefaultValueTypeMap = new Map<LocationKey, LocationKeyValueType>(
  [
    [ LocationKey.latitude, LocationKeyValueType.attribute ],
    [ LocationKey.longitude, LocationKeyValueType.attribute ],
    [ LocationKey.accuracy, LocationKeyValueType.timeseries ],
    [ LocationKey.altitude, LocationKeyValueType.timeseries ],
    [ LocationKey.altitudeAccuracy, LocationKeyValueType.timeseries ],
    [ LocationKey.speed, LocationKeyValueType.timeseries ],
    [ LocationKey.heading, LocationKeyValueType.timeseries ],
    [ LocationKey.gpsActive, LocationKeyValueType.attribute ],
    [ LocationKey.gpsTrackedBy, LocationKeyValueType.attribute ]
  ]
);

export const mandatoryLocationKeys: LocationKey[] = [LocationKey.latitude, LocationKey.longitude];

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
  high = 'HIGH',
  balanced = 'BALANCED',
  low = 'LOW'
}

export const mobileActionLocationAccuracyTranslationMap = new Map<MobileActionLocationAccuracy, string>(
  [
    [ MobileActionLocationAccuracy.high, 'widget-action.mobile.accuracy-high' ],
    [ MobileActionLocationAccuracy.balanced, 'widget-action.mobile.accuracy-balanced' ],
    [ MobileActionLocationAccuracy.low, 'widget-action.mobile.accuracy-low' ]
  ]
);

export const mobileActionLocationAccuracyHintMap = new Map<MobileActionLocationAccuracy, string>(
  [
    [ MobileActionLocationAccuracy.high, 'widget-action.mobile.accuracy-high-hint' ],
    [ MobileActionLocationAccuracy.balanced, 'widget-action.mobile.accuracy-balanced-hint' ],
    [ MobileActionLocationAccuracy.low, 'widget-action.mobile.accuracy-low-hint' ]
  ]
);

export interface StartLiveLocationDescriptor extends ProcessLaunchResultDescriptor, LocationTargetDescriptor {
  accuracy?: MobileActionLocationAccuracy;
  distanceFilterMeters?: number;
  intervalSeconds?: number;
  maxDurationSeconds?: number;
}

export type SaveBrowserLocationDescriptor = LocationTargetDescriptor;

export const defaultSaveBrowserLocationDescriptor = (): SaveBrowserLocationDescriptor => ({
  targetEntity: {type: MobileActionTargetEntityType.currentEntity},
  keys: defaultLocationKeyMappings()
});

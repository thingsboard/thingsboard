///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { DataKey, DatasourceType } from '@shared/models/widget.models';
import { EntityType } from '@shared/models/entity-type.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { mergeDeep } from '@core/utils';

export enum MapType {
  geoMap = 'geoMap',
  image = 'image'
}

export interface MapDataSourceSettings {
  dsType: DatasourceType;
  dsEntityType?: EntityType;
  dsEntityId?: string;
  dsEntityAliasId?: string;
  dsFilterId?: string;
}

export interface MapDataLayerSettings extends MapDataSourceSettings {
  additionalDataKeys?: DataKey[];
  group?: string;
}

export interface MarkersDataLayerSettings extends MapDataLayerSettings {
  xKey: DataKey;
  yKey: DataKey;
}

export const defaultMarkersDataLayerSettings = (mapType: MapType): MarkersDataLayerSettings => ({
  dsType: DatasourceType.entity,
  xKey: {
    name: MapType.geoMap === mapType ? 'latitude' : 'xPos',
    label: MapType.geoMap === mapType ? 'latitude' : 'xPos',
    type: DataKeyType.attribute
  },
  yKey: {
    name: MapType.geoMap === mapType ? 'longitude' : 'yPos',
    label: MapType.geoMap === mapType ? 'longitude' : 'yPos',
    type: DataKeyType.attribute
  }
});

export interface PolygonsDataLayerSettings extends MapDataLayerSettings {
  polygonKey: DataKey;
}

export const defaultPolygonsDataLayerSettings: PolygonsDataLayerSettings = {
  dsType: DatasourceType.entity,
  polygonKey: {
    name: 'perimeter',
    label: 'perimeter',
    type: DataKeyType.attribute
  }
};

export interface CirclesDataLayerSettings extends MapDataLayerSettings {
  circleKey: DataKey;
}

export const defaultCirclesDataLayerSettings: CirclesDataLayerSettings = {
  dsType: DatasourceType.entity,
  circleKey: {
    name: 'perimeter',
    label: 'perimeter',
    type: DataKeyType.attribute
  }
};

export interface AdditionalMapDataSourceSettings extends MapDataSourceSettings {
  dataKeys: DataKey[];
}

export enum MapControlsPosition {
  topleft = 'topleft',
  topright = 'topright',
  bottomleft = 'bottomleft',
  bottomright = 'bottomright'
}

export enum MapZoomAction {
  scroll = 'scroll',
  doubleClick = 'doubleClick',
  controlButtons = 'controlButtons'
}

export interface BaseMapSettings {
  mapType: MapType;
  markers: MarkersDataLayerSettings[];
  polygons: PolygonsDataLayerSettings[];
  circles: CirclesDataLayerSettings[];
  additionalDataSources: AdditionalMapDataSourceSettings[];
  controlsPosition: MapControlsPosition;
  zoomActions: MapZoomAction[];
  fitMapBounds: boolean;
  useDefaultCenterPosition: boolean;
  defaultCenterPosition?: string;
  defaultZoomLevel: number;
  mapPageSize: number;
}

export const DEFAULT_MAP_PAGE_SIZE = 16384;
export const DEFAULT_ZOOM_LEVEL = 8;

export const defaultBaseMapSettings: BaseMapSettings = {
  mapType: MapType.geoMap,
  markers: [],
  polygons: [],
  circles: [],
  additionalDataSources: [],
  controlsPosition: MapControlsPosition.topleft,
  zoomActions: [MapZoomAction.scroll, MapZoomAction.doubleClick, MapZoomAction.controlButtons],
  fitMapBounds: true,
  useDefaultCenterPosition: false,
  defaultCenterPosition: '0,0',
  defaultZoomLevel: null,
  mapPageSize: DEFAULT_MAP_PAGE_SIZE
};

export enum MapProvider {
  google = 'google-map',
  openstreet = 'openstreet-map',
  here = 'here',
  tencent = 'tencent-map',
  custom = 'custom'
}

export interface MapLayerSettings {
  label?: string;
  provider: MapProvider;
}

export enum GoogleLayerType {
  roadmap = 'roadmap',
  satellite = 'satellite',
  hybrid = 'hybrid',
  terrain = 'terrain'
}

export const googleMapLayerTranslationMap = new Map<GoogleLayerType, string>(
  [
    [GoogleLayerType.roadmap, 'widgets.maps.google-map-type-roadmap'],
    [GoogleLayerType.satellite, 'widgets.maps.google-map-type-satelite'],
    [GoogleLayerType.hybrid, 'widgets.maps.google-map-type-hybrid'],
    [GoogleLayerType.terrain, 'widgets.maps.google-map-type-terrain']
  ]
);

export interface GoogleMapLayerSettings extends MapLayerSettings {
  provider: MapProvider.google;
  layerType: GoogleLayerType;
  apiKey: string;
}

export const defaultGoogleMapLayerSettings: GoogleMapLayerSettings = {
  provider: MapProvider.google,
  layerType: GoogleLayerType.roadmap,
  apiKey: 'AIzaSyDoEx2kaGz3PxwbI9T7ccTSg5xjdw8Nw8Q'
};

export enum OpenStreetLayerType {
  openStreetMapnik = 'OpenStreetMap.Mapnik',
  openStreetHot = 'OpenStreetMap.HOT',
  esriWorldStreetMap = 'Esri.WorldStreetMap',
  esriWorldTopoMap = 'Esri.WorldTopoMap',
  esriWorldImagery = 'Esri.WorldImagery',
  cartoDbPositron = 'CartoDB.Positron',
  cartoDbDarkMatter = 'CartoDB.DarkMatter'
}

export const openStreetMapLayerTranslationMap = new Map<OpenStreetLayerType, string>(
  [
    [OpenStreetLayerType.openStreetMapnik, 'widgets.maps.openstreet-provider-mapnik'],
    [OpenStreetLayerType.openStreetHot, 'widgets.maps.openstreet-provider-hot'],
    [OpenStreetLayerType.esriWorldStreetMap, 'widgets.maps.openstreet-provider-esri-street'],
    [OpenStreetLayerType.esriWorldTopoMap, 'widgets.maps.openstreet-provider-esri-topo'],
    [OpenStreetLayerType.esriWorldImagery, 'widgets.maps.openstreet-provider-esri-imagery'],
    [OpenStreetLayerType.cartoDbPositron, 'widgets.maps.openstreet-provider-cartodb-positron'],
    [OpenStreetLayerType.cartoDbDarkMatter, 'widgets.maps.openstreet-provider-cartodb-dark-matter']
  ]
);

export interface OpenStreetMapLayerSettings extends MapLayerSettings {
  provider: MapProvider.openstreet;
  layerType: OpenStreetLayerType;
}

export const defaultOpenStreetMapLayerSettings: OpenStreetMapLayerSettings = {
  provider: MapProvider.openstreet,
  layerType: OpenStreetLayerType.openStreetMapnik
}

export enum HereLayerType {
  hereNormalDay = 'HEREv3.normalDay',
  hereNormalNight = 'HEREv3.normalNight',
  hereHybridDay = 'HEREv3.hybridDay',
  hereTerrainDay = 'HEREv3.terrainDay'
}

export const hereLayerTranslationMap = new Map<HereLayerType, string>(
  [
    [HereLayerType.hereNormalDay, 'widgets.maps.here-map-normal-day'],
    [HereLayerType.hereNormalNight, 'widgets.maps.here-map-normal-night'],
    [HereLayerType.hereHybridDay, 'widgets.maps.here-map-hybrid-day'],
    [HereLayerType.hereTerrainDay, 'widgets.maps.here-map-terrain-day']
  ]
);

export interface HereMapLayerSettings extends MapLayerSettings {
  provider: MapProvider.here;
  layerType: HereLayerType;
  apiKey: string;
}

export const defaultHereMapLayerSettings: HereMapLayerSettings = {
  provider: MapProvider.here,
  layerType: HereLayerType.hereNormalDay,
  apiKey: 'kVXykxAfZ6LS4EbCTO02soFVfjA7HoBzNVVH9u7nzoE'
}

export enum TencentLayerType {
  tencentNormal = 'Tencent.Normal',
  tencentSatellite = 'Tencent.Satellite',
  tencentTerrain = 'Tencent.Terrain'
}

export const tencentLayerTranslationMap = new Map<TencentLayerType, string>(
  [
    [TencentLayerType.tencentNormal, 'widgets.maps.tencent-provider-normal'],
    [TencentLayerType.tencentSatellite, 'widgets.maps.tencent-provider-satellite'],
    [TencentLayerType.tencentTerrain, 'widgets.maps.tencent-provider-terrain']
  ]
);

export interface TencentMapLayerSettings extends MapLayerSettings {
  provider: MapProvider.tencent;
  layerType: TencentLayerType
}

export const defaultTencentMapLayerSettings: TencentMapLayerSettings = {
  provider: MapProvider.tencent,
  layerType: TencentLayerType.tencentNormal
}

export interface CustomMapLayerSettings extends MapLayerSettings {
  provider: MapProvider.custom;
  tileUrl: string;
}

export const defaultCustomMapLayerSettings: CustomMapLayerSettings = {
  provider: MapProvider.custom,
  tileUrl: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'
}

export const defaultMapLayerSettings = (provider: MapProvider): MapLayerSettings => {
  switch (provider) {
    case MapProvider.google:
      return defaultGoogleMapLayerSettings;
    case MapProvider.openstreet:
      return defaultOpenStreetMapLayerSettings;
    case MapProvider.here:
      return defaultHereMapLayerSettings;
    case MapProvider.tencent:
      return defaultTencentMapLayerSettings;
    case MapProvider.custom:
      return defaultCustomMapLayerSettings;
  }
};

export const defaultMapLayers: MapLayerSettings[] = (Object.keys(OpenStreetLayerType) as OpenStreetLayerType[]).map(type => ({
  provider: MapProvider.openstreet,
  layerType: type
} as MapLayerSettings)).concat((Object.keys(GoogleLayerType) as GoogleLayerType[]).map(type =>
  mergeDeep({} as MapLayerSettings, defaultGoogleMapLayerSettings, {layerType: type} as GoogleMapLayerSettings)).concat(
  (Object.keys(TencentLayerType) as TencentLayerType[]).map(type => ({
    provider: MapProvider.tencent,
    layerType: type
  } as MapLayerSettings))
)).concat(
  (Object.keys(HereLayerType) as HereLayerType[]).map(type =>
    mergeDeep({} as MapLayerSettings, defaultHereMapLayerSettings, {layerType: type} as HereMapLayerSettings))
).concat([
  mergeDeep({} as MapLayerSettings, defaultCustomMapLayerSettings, {label: 'Custom 1'} as CustomMapLayerSettings),
  mergeDeep({} as MapLayerSettings, defaultCustomMapLayerSettings, {
    tileUrl: 'http://a.tile2.opencyclemap.org/transport/{z}/{x}/{y}.png',
    label: 'Custom 2'
  } as CustomMapLayerSettings),
  mergeDeep({} as MapLayerSettings, defaultCustomMapLayerSettings, {
    tileUrl: 'http://b.tile2.opencyclemap.org/transport/{z}/{x}/{y}.png',
    label: 'Custom 3'
  } as CustomMapLayerSettings)
]);
  /*
  (Object.keys(OpenStreetLayerType) as OpenStreetLayerType[]).map(type => ({
  provider: MapProvider.openstreet,
  layerType: type
} as MapLayerSettings)).concat(
  (Object.keys(GoogleLayerType) as GoogleLayerType[]).map(type =>
    mergeDeep({} as GoogleMapLayerSettings, defaultGoogleMapLayerSettings, {layerType: type} as GoogleMapLayerSettings)));*/

export interface GeoMapSettings extends BaseMapSettings {
  layers?: MapLayerSettings[];
}

export const defaultGeoMapSettings: GeoMapSettings = {
  mapType: MapType.geoMap,
  layers: mergeDeep([], defaultMapLayers),
  ...mergeDeep({} as BaseMapSettings, defaultBaseMapSettings)
};

export enum ImageSourceType {
  image = 'image',
  attribute = 'attribute'
}

export interface ImageMapSettings extends BaseMapSettings {
  imageSourceType?: ImageSourceType;
  imageUrl?: string;
  imageEntityAlias?: string;
  imageUrlAttribute?: string;
}

export const defaultImageMapSettings: ImageMapSettings = {
  mapType: MapType.image,
  imageSourceType: ImageSourceType.image,
  imageUrl: 'data:image/svg+xml;base64,PHN2ZyBpZD0ic3ZnMiIgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIGhlaWdodD0iMTAwIiB3aWR0aD0iMTAwIiB2ZXJzaW9uPSIxLjEiIHhtbG5zOmNjPSJodHRwOi8vY3JlYXRpdmVjb21tb25zLm9yZy9ucyMiIHhtbG5zOmRjPSJodHRwOi8vcHVybC5vcmcvZGMvZWxlbWVudHMvMS4xLyIgdmlld0JveD0iMCAwIDEwMCAxMDAiPgogPGcgaWQ9ImxheWVyMSIgdHJhbnNmb3JtPSJ0cmFuc2xhdGUoMCAtOTUyLjM2KSI+CiAgPHJlY3QgaWQ9InJlY3Q0Njg0IiBzdHJva2UtbGluZWpvaW49InJvdW5kIiBoZWlnaHQ9Ijk5LjAxIiB3aWR0aD0iOTkuMDEiIHN0cm9rZT0iIzAwMCIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIiB5PSI5NTIuODYiIHg9Ii40OTUwNSIgc3Ryb2tlLXdpZHRoPSIuOTkwMTAiIGZpbGw9IiNlZWUiLz4KICA8dGV4dCBpZD0idGV4dDQ2ODYiIHN0eWxlPSJ3b3JkLXNwYWNpbmc6MHB4O2xldHRlci1zcGFjaW5nOjBweDt0ZXh0LWFuY2hvcjptaWRkbGU7dGV4dC1hbGlnbjpjZW50ZXIiIGZvbnQtd2VpZ2h0PSJib2xkIiB4bWw6c3BhY2U9InByZXNlcnZlIiBmb250LXNpemU9IjEwcHgiIGxpbmUtaGVpZ2h0PSIxMjUlIiB5PSI5NzAuNzI4MDkiIHg9IjQ5LjM5NjQ3NyIgZm9udC1mYW1pbHk9IlJvYm90byIgZmlsbD0iIzY2NjY2NiI+PHRzcGFuIGlkPSJ0c3BhbjQ2OTAiIHg9IjUwLjY0NjQ3NyIgeT0iOTcwLjcyODA5Ij5JbWFnZSBiYWNrZ3JvdW5kIDwvdHNwYW4+PHRzcGFuIGlkPSJ0c3BhbjQ2OTIiIHg9IjQ5LjM5NjQ3NyIgeT0iOTgzLjIyODA5Ij5pcyBub3QgY29uZmlndXJlZDwvdHNwYW4+PC90ZXh0PgogIDxyZWN0IGlkPSJyZWN0NDY5NCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCIgaGVpZ2h0PSIxOS4zNiIgd2lkdGg9IjY5LjM2IiBzdHJva2U9IiMwMDAiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgeT0iOTkyLjY4IiB4PSIxNS4zMiIgc3Ryb2tlLXdpZHRoPSIuNjM5ODYiIGZpbGw9Im5vbmUiLz4KIDwvZz4KPC9zdmc+Cg==',
  ...mergeDeep({} as BaseMapSettings, defaultBaseMapSettings)
}

export type MapSetting = GeoMapSettings & ImageMapSettings;

export const defaultMapSettings: MapSetting = defaultGeoMapSettings;

export function parseCenterPosition(position: string | [number, number]): [number, number] {
  if (typeof (position) === 'string') {
    const parts = position.split(',');
    if (parts.length === 2) {
      return [Number(parts[0]), Number(parts[1])];
    }
  }
  if (typeof (position) === 'object') {
    return position;
  }
  return [0, 0];
}

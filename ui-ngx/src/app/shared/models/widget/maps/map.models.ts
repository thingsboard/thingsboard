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

import {
  DataKey,
  Datasource,
  DatasourceType,
  FormattedData,
  WidgetAction,
  WidgetActionType
} from '@shared/models/widget.models';
import { AttributeScope, DataKeyType } from '@shared/models/telemetry/telemetry.models';
import {
  guid,
  hashCode,
  isDefinedAndNotNull,
  isNotEmptyStr,
  isNumber,
  isString,
  isUndefinedOrNull,
  mergeDeep
} from '@core/utils';
import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { materialColors } from '@shared/models/material.models';
import type L from 'leaflet';
import { TbFunction } from '@shared/models/js-function.models';
import { Observable, Observer, of, switchMap } from 'rxjs';
import { map } from 'rxjs/operators';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { MarkerIconContainer, MarkerShape } from '@shared/models/widget/maps/marker-shape.models';
import { ColorRange, DateFormatSettings, simpleDateFormat } from '@shared/models/widget-settings.models';
import _ from 'lodash';

export enum MapType {
  geoMap = 'geoMap',
  image = 'image'
}

export interface MapDataSourceSettings {
  dsType: DatasourceType;
  dsLabel?: string;
  dsDeviceId?: string;
  dsEntityAliasId?: string;
  dsFilterId?: string;
}

export interface TbMapDatasource extends Datasource {
  mapDataIds: string[];
}

export const mapDataSourceSettingsToDatasource = (settings: MapDataSourceSettings, id = guid()): TbMapDatasource => {
  return {
    type: settings.dsType,
    name: settings.dsLabel,
    deviceId: settings.dsDeviceId,
    entityAliasId: settings.dsEntityAliasId,
    filterId: settings.dsFilterId,
    dataKeys: [],
    latestDataKeys: [],
    mapDataIds: [id]
  };
};


export enum DataLayerPatternType {
  pattern = 'pattern',
  function = 'function'
}

export interface DataLayerPatternSettings {
  show: boolean;
  type: DataLayerPatternType;
  pattern?: string;
  patternFunction?: TbFunction;
}

export enum DataLayerTooltipTrigger {
  click = 'click',
  hover = 'hover'
}

export const dataLayerTooltipTriggers = Object.keys(DataLayerTooltipTrigger) as DataLayerTooltipTrigger[];

export const dataLayerTooltipTriggerTranslationMap = new Map<DataLayerTooltipTrigger, string>(
  [
    [DataLayerTooltipTrigger.click, 'widgets.maps.data-layer.tooltip-trigger-click'],
    [DataLayerTooltipTrigger.hover, 'widgets.maps.data-layer.tooltip-trigger-hover']
  ]
);

export interface DataLayerTooltipSettings extends DataLayerPatternSettings {
  trigger: DataLayerTooltipTrigger;
  autoclose: boolean;
  offsetX: number;
  offsetY: number;
  tagActions?: WidgetAction[];
}

export enum DataLayerEditAction {
  add = 'add',
  edit = 'edit',
  move = 'move',
  remove = 'remove'
}

export const dataLayerEditActions = Object.keys(DataLayerEditAction) as DataLayerEditAction[];

export const dataLayerEditActionTranslationMap = new Map<DataLayerEditAction, string>(
  [
    [DataLayerEditAction.add, 'widgets.maps.data-layer.action-add'],
    [DataLayerEditAction.edit, 'widgets.maps.data-layer.action-edit'],
    [DataLayerEditAction.move, 'widgets.maps.data-layer.action-move'],
    [DataLayerEditAction.remove, 'widgets.maps.data-layer.action-remove']
  ]
);

export interface DataLayerEditSettings {
  enabledActions: DataLayerEditAction[];
  attributeScope: AttributeScope;
  snappable: boolean;
}

export interface MapDataLayerSettings extends MapDataSourceSettings {
  additionalDataSources?: MapDataSourceSettings[];
  additionalDataKeys?: DataKey[];
  label: DataLayerPatternSettings;
  tooltip: DataLayerTooltipSettings;
  click: WidgetAction;
  groups?: string[];
  edit:  DataLayerEditSettings;
}

export const defaultBaseDataLayerSettings = (mapType: MapType): Partial<MapDataLayerSettings> => ({
  label: {
    show: true,
    type: DataLayerPatternType.pattern,
    pattern: '${entityName}'
  },
  tooltip: {
    show: true,
    trigger: DataLayerTooltipTrigger.click,
    autoclose: true,
    type: DataLayerPatternType.pattern,
    pattern: mapType === MapType.geoMap ?
      '<b>${entityName}</b><br/><br/><b>Latitude:</b> ${latitude:7}<br/><b>Longitude:</b> ${longitude:7}<br/><b>Temperature:</b> ${temperature} °C<br/><small>See tooltip settings for details</small>'
    : '<b>${entityName}</b><br/><br/><b>X Pos:</b> ${xPos:2}<br/><b>Y Pos:</b> ${yPos:2}<br/><b>Temperature:</b> ${temperature} °C<br/><small>See tooltip settings for details</small>',
    offsetX: 0,
    offsetY: -1
  },
  click: {
    type: WidgetActionType.doNothing
  },
  edit: {
    enabledActions: [],
    attributeScope: AttributeScope.SERVER_SCOPE,
    snappable: false
  }
})

export type MapDataLayerType = 'trips' | 'markers' | 'polygons' | 'circles';

export const mapDataLayerTypes: MapDataLayerType[] = ['trips', 'markers', 'polygons', 'circles'];

export const mapDataLayerValid = (dataLayer: MapDataLayerSettings, type: MapDataLayerType): boolean => {
  if (!dataLayer.dsType || ![DatasourceType.function, DatasourceType.device, DatasourceType.entity].includes(dataLayer.dsType)) {
    return false;
  }
  switch (dataLayer.dsType) {
    case DatasourceType.function:
      break;
    case DatasourceType.device:
      if (!dataLayer.dsDeviceId) {
        return false;
      }
      break;
    case DatasourceType.entity:
      if (!dataLayer.dsEntityAliasId) {
        return false;
      }
      break;
  }
  switch (type) {
    case 'markers':
      const markersDataLayer = dataLayer as MarkersDataLayerSettings;
      if (!markersDataLayer.xKey?.type || !markersDataLayer.xKey?.name ||
          !markersDataLayer.yKey?.type || !markersDataLayer.xKey?.name) {
        return false;
      }
      break;
    case 'polygons':
      const polygonsDataLayer = dataLayer as PolygonsDataLayerSettings;
      if (!polygonsDataLayer.polygonKey?.type || !polygonsDataLayer.polygonKey?.name) {
        return false;
      }
      break;
    case 'circles':
      const circlesDataLayer = dataLayer as CirclesDataLayerSettings;
      if (!circlesDataLayer.circleKey?.type || !circlesDataLayer.circleKey?.name) {
        return false;
      }
      break;
  }
  return true;
};

export const mapDataLayerValidator = (type: MapDataLayerType): ValidatorFn => {
  return (control: AbstractControl): ValidationErrors | null => {
    const layer: MapDataLayerSettings = control.value;
    if (!mapDataLayerValid(layer, type)) {
      return {
        layer: true
      };
    }
    return null;
  };
};

export enum MarkerType {
  shape = 'shape',
  icon = 'icon',
  image = 'image'
}

export enum DataLayerColorType {
  constant = 'constant',
  range = 'range',
  function = 'function'
}

export interface DataLayerColorSettings {
  type: DataLayerColorType;
  color: string;
  rangeKey?: DataKey;
  range?: ColorRange[];
  colorFunction?: TbFunction;
}

export enum MarkerImageType {
  image = 'image',
  function = 'function'
}

export interface MarkerImageSettings {
  type: MarkerImageType;
  image?: string;
  imageSize?: number;
  imageFunction?: TbFunction;
  images?: string[];
}

export interface BaseMarkerShapeSettings {
  size: number;
  color: DataLayerColorSettings;
}

export interface MarkerShapeSettings extends BaseMarkerShapeSettings {
  shape: MarkerShape;
}

export interface MarkerIconSettings extends BaseMarkerShapeSettings {
  iconContainer?: MarkerIconContainer;
  icon: string;
}
export interface MarkerClusteringSettings {
  enable: boolean;
  zoomOnClick: boolean;
  maxZoom: number;
  maxClusterRadius: number;
  zoomAnimation: boolean;
  showCoverageOnHover: boolean;
  spiderfyOnMaxZoom: boolean;
  chunkedLoad: boolean;
  lazyLoad: boolean;
  useClusterMarkerColorFunction: boolean;
  clusterMarkerColorFunction: TbFunction;
}


export interface MarkersDataLayerSettings extends MapDataLayerSettings {
  xKey: DataKey;
  yKey: DataKey;
  markerType: MarkerType;
  markerShape?: MarkerShapeSettings;
  markerIcon?: MarkerIconSettings;
  markerImage?: MarkerImageSettings;
  markerOffsetX: number;
  markerOffsetY: number;
  positionFunction?: TbFunction;
  markerClustering: MarkerClusteringSettings;
}

const defaultMarkerLatitudeFunction = 'var value = prevValue || 15.833293;\n' +
  'if (time % 500 < 500) {\n' +
  '    value += Math.random() * 0.05 - 0.025;\n' +
  '}\n' +
  'return value;';

const defaultMarkerLongitudeFunction = 'var value = prevValue || -90.454350;\n' +
  'if (time % 500 < 500) {\n' +
  '    value += Math.random() * 0.05 - 0.025;\n' +
  '}\n' +
  'return value;';

const defaultMarkerXPosFunction = 'var value = prevValue || 0.2;\n' +
  'if (time % 500 < 500) {\n' +
  '    value += Math.random() * 0.05 - 0.025;\n' +
  '}\n' +
  'return value;';

const defaultMarkerYPosFunction = 'var value = prevValue || 0.3;\n' +
  'if (time % 500 < 500) {\n' +
  '    value += Math.random() * 0.05 - 0.025;\n' +
  '}\n' +
  'return value;';

const defaultMarkersDataSourceSettings = (mapType: MapType, timeSeries = false, functionsOnly = false): Partial<MarkersDataLayerSettings> => ({
  dsType: functionsOnly ? DatasourceType.function : DatasourceType.entity,
  dsLabel: functionsOnly ? 'First point' : '',
  xKey: {
    name: functionsOnly ? 'f(x)' : (MapType.geoMap === mapType ? 'latitude' : 'xPos'),
    label: MapType.geoMap === mapType ? 'latitude' : 'xPos',
    type: functionsOnly ? DataKeyType.function : (timeSeries ? DataKeyType.timeseries : DataKeyType.attribute),
    funcBody: functionsOnly ? (MapType.geoMap === mapType ? defaultMarkerLatitudeFunction : defaultMarkerXPosFunction) : undefined,
    settings: {},
    color: materialColors[0].value
  },
  yKey: {
    name: functionsOnly ? 'f(x)' : (MapType.geoMap === mapType ? 'longitude' : 'yPos'),
    label: MapType.geoMap === mapType ? 'longitude' : 'yPos',
    type: functionsOnly ? DataKeyType.function : (timeSeries ? DataKeyType.timeseries : DataKeyType.attribute),
    funcBody: functionsOnly ? (MapType.geoMap === mapType ? defaultMarkerLongitudeFunction : defaultMarkerYPosFunction) : undefined,
    settings: {},
    color: materialColors[0].value
  }
});

export const defaultMarkersDataLayerSettings = (mapType: MapType, functionsOnly = false): MarkersDataLayerSettings => mergeDeep(
  defaultMarkersDataSourceSettings(mapType, false, functionsOnly) as MarkersDataLayerSettings,
  defaultBaseMarkersDataLayerSettings(mapType) as MarkersDataLayerSettings);

export const defaultBaseMarkersDataLayerSettings = (mapType: MapType): Partial<MarkersDataLayerSettings> => mergeDeep({
  markerType: MarkerType.shape,
  markerShape: {
    shape: MarkerShape.markerShape1,
    size: 34,
    color: {
      type: DataLayerColorType.constant,
      color: '#307FE5',
    }
  },
  markerIcon: {
    iconContainer: MarkerIconContainer.iconContainer1,
    icon: 'mdi:lightbulb-on',
    size: 48,
    color: {
      type: DataLayerColorType.constant,
      color: '#307FE5',
    }
  },
  markerImage: {
    type: MarkerImageType.image,
    image: '/assets/markers/shape1.svg',
    imageSize: 34
  },
  markerOffsetX: 0.5,
  markerOffsetY: 1,
  positionFunction: 'return {x: origXPos, y: origYPos};',
  markerClustering: {
    enable: false,
    zoomOnClick: true,
    maxZoom: null,
    maxClusterRadius: 80,
    zoomAnimation: true,
    showCoverageOnHover: true,
    spiderfyOnMaxZoom: false,
    chunkedLoad: false,
    lazyLoad: true,
    useClusterMarkerColorFunction: false,
    clusterMarkerColorFunction: null
  }
} as MarkersDataLayerSettings, defaultBaseDataLayerSettings(mapType));

export enum PathDecoratorSymbol {
  arrowHead = 'arrowHead',
  dash = 'dash'
}

export const pathDecoratorSymbols = Object.keys(PathDecoratorSymbol) as PathDecoratorSymbol[];

export const pathDecoratorSymbolTranslationMap = new Map<PathDecoratorSymbol, string>(
  [
    [PathDecoratorSymbol.arrowHead, 'widgets.maps.data-layer.path.decorator-symbol-arrow-head'],
    [PathDecoratorSymbol.dash, 'widgets.maps.data-layer.path.decorator-symbol-dash']
  ]
);

export interface TripsDataLayerSettings extends MarkersDataLayerSettings {
  showMarker: boolean;
  rotateMarker: boolean;
  offsetAngle: number;
  showPath: boolean;
  pathStrokeWeight?: number;
  pathStrokeColor?: DataLayerColorSettings;
  usePathDecorator?: boolean;
  pathDecoratorSymbol?: PathDecoratorSymbol;
  pathDecoratorSymbolSize?: number;
  pathDecoratorSymbolColor?: string;
  pathDecoratorOffset?: number;
  pathEndDecoratorOffset?: number;
  pathDecoratorRepeat?: number;
  showPoints: boolean;
  pointSize?: number;
  pointColor?: DataLayerColorSettings;
  pointTooltip?: DataLayerTooltipSettings;
}

export const defaultTripsDataLayerSettings = (mapType: MapType, functionsOnly = false): TripsDataLayerSettings => mergeDeep(
  defaultMarkersDataSourceSettings(mapType, true, functionsOnly) as TripsDataLayerSettings,
  defaultBaseTripsDataLayerSettings(mapType) as TripsDataLayerSettings);

export const defaultBaseTripsDataLayerSettings = (mapType: MapType): Partial<TripsDataLayerSettings> => mergeDeep(
  defaultBaseMarkersDataLayerSettings(mapType),
  {
    showMarker: true,
    tooltip: {
      offsetY: -0.5,
      pattern: mapType === MapType.geoMap ?
        '<b>${entityName}</b><br/><br/><b>Latitude:</b> ${latitude:7}<br/><b>Longitude:</b> ${longitude:7}<br/><b>End Time:</b> ${maxTime}<br/><b>Start Time:</b> ${minTime}'
        : '<b>${entityName}</b><br/><br/><b>X Pos:</b> ${xPos:2}<br/><b>Y Pos:</b> ${yPos:2}<br/><b>End Time:</b> ${maxTime}<br/><b>Start Time:</b> ${minTime}',
    },
    rotateMarker: true,
    offsetAngle: 0,
    markerShape: {
      shape: MarkerShape.tripMarkerShape2
    },
    markerIcon: {
      iconContainer: MarkerIconContainer.tripIconContainer1,
      icon: 'arrow_forward'
    },
    markerImage: {
      image: '/assets/markers/tripShape2.svg'
    },
    markerOffsetX: 0.5,
    markerOffsetY: 0.5,
    showPath: true,
    pathStrokeWeight: 4,
    pathStrokeColor: {
      type: DataLayerColorType.constant,
      color: '#307FE5',
    },
    usePathDecorator: false,
    pathDecoratorSymbol: PathDecoratorSymbol.arrowHead,
    pathDecoratorSymbolSize: 10,
    pathDecoratorSymbolColor: '#307FE5',
    pathDecoratorOffset: 20,
    pathEndDecoratorOffset: 20,
    pathDecoratorRepeat: 20,
    showPoints: false,
    pointSize: 10,
    pointColor: {
      type: DataLayerColorType.constant,
      color: '#307FE5',
    },
    pointTooltip: {
      show: true,
      trigger: DataLayerTooltipTrigger.click,
      autoclose: true,
      type: DataLayerPatternType.pattern,
      pattern: mapType === MapType.geoMap ?
        '<b>${entityName}</b><br/><br/><b>Latitude:</b> ${latitude:7}<br/><b>Longitude:</b> ${longitude:7}<br/><b>End Time:</b> ${maxTime}<br/><b>Start Time:</b> ${minTime}'
        : '<b>${entityName}</b><br/><br/><b>X Pos:</b> ${xPos:2}<br/><b>Y Pos:</b> ${yPos:2}<br/><b>End Time:</b> ${maxTime}<br/><b>Start Time:</b> ${minTime}',
      offsetX: 0,
      offsetY: -1
    },
  } as TripsDataLayerSettings);

export enum ShapeFillType {
  color = 'color',
  image = 'image',
  stripe = 'stripe'
}

export enum ShapeFillImageType {
  image = 'image',
  function = 'function'
}

export interface ShapeFillImageSettings {
  type: ShapeFillImageType;
  image?: string;
  preserveAspectRatio?: boolean;
  opacity?: number; // (0-1)
  angle?: number; // (0-360)
  scale?: number; // (0-...)
  imageFunction?: TbFunction;
  images?: string[];
}

export interface ShapeFillStripeSettings {
  weight: number;
  color: DataLayerColorSettings;
  spaceWeight: number;
  spaceColor: DataLayerColorSettings;
  angle: number; // (0-180)
}

export interface ShapeDataLayerSettings extends MapDataLayerSettings {
  fillType: ShapeFillType;
  fillColor?: DataLayerColorSettings;
  fillImage?: ShapeFillImageSettings;
  fillStripe?: ShapeFillStripeSettings;
  strokeColor: DataLayerColorSettings;
  strokeWeight: number;
}

export interface PolygonsDataLayerSettings extends ShapeDataLayerSettings {
  polygonKey: DataKey;
}

export const defaultPolygonsDataLayerSettings = (mapType: MapType, functionsOnly = false): PolygonsDataLayerSettings => mergeDeep({
  dsType: functionsOnly ? DatasourceType.function : DatasourceType.entity,
  dsLabel: functionsOnly ? 'First polygon' : '',
  polygonKey: {
    name: functionsOnly ? 'f(x)' : 'perimeter',
    label: 'perimeter',
    type: functionsOnly ? DataKeyType.function : DataKeyType.attribute,
    settings: {},
    color: materialColors[0].value
  }
} as PolygonsDataLayerSettings, defaultBasePolygonsDataLayerSettings(mapType) as PolygonsDataLayerSettings);

export const defaultBasePolygonsDataLayerSettings = (mapType: MapType): Partial<PolygonsDataLayerSettings> => mergeDeep({
    fillType: ShapeFillType.color,
    fillColor: {
      type: DataLayerColorType.constant,
      color: 'rgba(51,136,255,0.2)',
    },
    fillImage: {
      type: ShapeFillImageType.image,
      image: '/assets/widget-preview-empty.svg',
      preserveAspectRatio: true,
      opacity: 1,
      angle: 0,
      scale: 1
    },
    fillStripe: {
      weight: 3,
      color: {
        type: DataLayerColorType.constant,
        color: '#8f8f8f'
      },
      spaceWeight: 9,
      spaceColor: {
        type: DataLayerColorType.constant,
        color: 'rgba(143,143,143,0)',
      },
      angle: 45
    },
    strokeColor: {
      type: DataLayerColorType.constant,
      color: '#3388ff',
    },
    strokeWeight: 3
} as Partial<PolygonsDataLayerSettings>, defaultBaseDataLayerSettings(mapType),
  {label: {show: false}, tooltip: {show: false, pattern: '<b>${entityName}</b><br/><br/><b>TimeStamp:</b> ${ts:7}'}} as Partial<PolygonsDataLayerSettings>)

export interface CirclesDataLayerSettings extends ShapeDataLayerSettings {
  circleKey: DataKey;
}

export const defaultCirclesDataLayerSettings = (mapType: MapType, functionsOnly = false): CirclesDataLayerSettings => mergeDeep({
  dsType: functionsOnly ? DatasourceType.function : DatasourceType.entity,
  dsLabel: functionsOnly ? 'First circle' : '',
  circleKey: {
    name: functionsOnly ? 'f(x)' : 'perimeter',
    label: 'perimeter',
    type: functionsOnly ? DataKeyType.function : DataKeyType.attribute,
    settings: {},
    color: materialColors[0].value
  }
} as CirclesDataLayerSettings, defaultBaseCirclesDataLayerSettings(mapType) as CirclesDataLayerSettings);

export const defaultBaseCirclesDataLayerSettings = (mapType: MapType): Partial<CirclesDataLayerSettings> => mergeDeep({
    fillType: ShapeFillType.color,
    fillColor: {
      type: DataLayerColorType.constant,
      color: 'rgba(51,136,255,0.2)',
    },
    fillImage: {
      type: ShapeFillImageType.image,
      image: '/assets/widget-preview-empty.svg',
      preserveAspectRatio: true,
      opacity: 1,
      angle: 0,
      scale: 1
    },
    fillStripe: {
      weight: 3,
      color: {
        type: DataLayerColorType.constant,
        color: '#8f8f8f'
      },
      spaceWeight: 9,
      spaceColor: {
        type: DataLayerColorType.constant,
        color: 'rgba(143,143,143,0)',
      },
      angle: 45
    },
    strokeColor: {
      type: DataLayerColorType.constant,
      color: '#3388ff',
    },
    strokeWeight: 3
} as Partial<CirclesDataLayerSettings>, defaultBaseDataLayerSettings(mapType),
  {label: {show: false}, tooltip: {show: false, pattern: '<b>${entityName}</b><br/><br/><b>TimeStamp:</b> ${ts:7}'}} as Partial<CirclesDataLayerSettings>)

export const defaultMapDataLayerSettings = (mapType: MapType, dataLayerType: MapDataLayerType, functionsOnly = false): MapDataLayerSettings => {
  switch (dataLayerType) {
    case 'trips':
      return defaultTripsDataLayerSettings(mapType, functionsOnly);
    case 'markers':
      return defaultMarkersDataLayerSettings(mapType, functionsOnly);
    case 'polygons':
      return defaultPolygonsDataLayerSettings(mapType, functionsOnly);
    case 'circles':
      return defaultCirclesDataLayerSettings(mapType, functionsOnly);
  }
};

export const defaultBaseMapDataLayerSettings = <T extends MapDataLayerSettings>(mapType: MapType, dataLayerType: MapDataLayerType): T => {
  switch (dataLayerType) {
    case 'trips':
      return defaultBaseTripsDataLayerSettings(mapType) as T;
    case 'markers':
      return defaultBaseMarkersDataLayerSettings(mapType) as T;
    case 'polygons':
      return defaultBasePolygonsDataLayerSettings(mapType) as T;
    case 'circles':
      return defaultBaseCirclesDataLayerSettings(mapType) as T;
  }
}

export interface AdditionalMapDataSourceSettings extends MapDataSourceSettings {
  dataKeys: DataKey[];
}

export const additionalMapDataSourcesToDatasources = (additionalMapDataSources: AdditionalMapDataSourceSettings[]): TbMapDatasource[] => {
  return additionalMapDataSources.map(addDs => {
    const res = mapDataSourceSettingsToDatasource(addDs);
    res.dataKeys = addDs.dataKeys;
    return res;
  });
};

export const mapDataSourceValid = (dataSource: MapDataSourceSettings): boolean => {
  if (!dataSource.dsType || ![DatasourceType.device, DatasourceType.entity].includes(dataSource.dsType)) {
    return false;
  }
  if (dataSource.dsType === DatasourceType.device && !dataSource.dsDeviceId) {
    return false;
  }
  return !(dataSource.dsType === DatasourceType.entity && !dataSource.dsEntityAliasId);

};

export const mapDataSourceValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const dataSource: MapDataSourceSettings = control.value;
  if (!mapDataSourceValid(dataSource)) {
    return {
      dataSource: true
    };
  }
  return null;
};

export const defaultMapDataSourceSettings = {
  dsType: DatasourceType.entity,
  dsLabel: ''
};

export const additionalMapDataSourceValid = (dataSource: AdditionalMapDataSourceSettings): boolean => {
  if (!dataSource.dsType || ![DatasourceType.function, DatasourceType.device, DatasourceType.entity].includes(dataSource.dsType)) {
    return false;
  }
  return !!dataSource.dataKeys?.length;
};

export const additionalMapDataSourceValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
    const dataSource: AdditionalMapDataSourceSettings = control.value;
    if (!additionalMapDataSourceValid(dataSource)) {
      return {
        dataSource: true
      };
    }
    return null;
};

export const defaultAdditionalMapDataSourceSettings = (functionsOnly = false): AdditionalMapDataSourceSettings => {
  return {
    dsType: functionsOnly ? DatasourceType.function : DatasourceType.entity,
    dsLabel: functionsOnly ? 'Additional data' : '',
    dataKeys: []
  };
};

export enum MapControlsPosition {
  topleft = 'topleft',
  topright = 'topright',
  bottomleft = 'bottomleft',
  bottomright = 'bottomright'
}

export const mapControlPositions = Object.keys(MapControlsPosition) as MapControlsPosition[];

export const mapControlsPositionTranslationMap = new Map<MapControlsPosition, string>(
  [
    [MapControlsPosition.topleft, 'widgets.maps.control.position-topleft'],
    [MapControlsPosition.topright, 'widgets.maps.control.position-topright'],
    [MapControlsPosition.bottomleft, 'widgets.maps.control.position-bottomleft'],
    [MapControlsPosition.bottomright, 'widgets.maps.control.position-bottomright']
  ]
);

export enum MapZoomAction {
  scroll = 'scroll',
  doubleClick = 'doubleClick',
  controlButtons = 'controlButtons'
}

export const mapZoomActions = Object.keys(MapZoomAction) as MapZoomAction[];

export const mapZoomActionTranslationMap = new Map<MapZoomAction, string>(
  [
    [MapZoomAction.scroll, 'widgets.maps.control.zoom-scroll'],
    [MapZoomAction.doubleClick, 'widgets.maps.control.zoom-double-click'],
    [MapZoomAction.controlButtons, 'widgets.maps.control.zoom-control-buttons']
  ]
);

export enum MapScale {
  metric = 'metric',
  imperial = 'imperial'
}

export const mapScales = Object.keys(MapScale) as MapScale[];

export const mapScaleTranslationMap = new Map<MapScale, string>(
  [
    [MapScale.metric, 'widgets.maps.control.scale-metric'],
    [MapScale.imperial, 'widgets.maps.control.scale-imperial'],
  ]
);

export interface MapActionButtonSettings {
  label?: string;
  icon?: string;
  color: string;
  action: WidgetAction;
}

export interface TripTimelineSettings {
  showTimelineControl: boolean;
  timeStep: number;
  speedOptions: number[];
  showTimestamp: boolean;
  timestampFormat: DateFormatSettings;
  snapToRealLocation: boolean;
  locationSnapFilter: TbFunction;
}

export interface BaseMapSettings {
  mapType: MapType;
  trips?: TripsDataLayerSettings[];
  markers: MarkersDataLayerSettings[];
  polygons: PolygonsDataLayerSettings[];
  circles: CirclesDataLayerSettings[];
  additionalDataSources: AdditionalMapDataSourceSettings[];
  controlsPosition: MapControlsPosition;
  zoomActions: MapZoomAction[];
  scales: MapScale[];
  dragModeButton: boolean;
  fitMapBounds: boolean;
  useDefaultCenterPosition: boolean;
  defaultCenterPosition?: string;
  defaultZoomLevel: number;
  minZoomLevel: number;
  mapPageSize: number;
  mapActionButtons: MapActionButtonSettings[];
  tripTimeline?: TripTimelineSettings;
}

export const DEFAULT_MAP_PAGE_SIZE = 16384;
export const DEFAULT_ZOOM_LEVEL = 8;

export const defaultBaseMapSettings: BaseMapSettings = {
  mapType: MapType.geoMap,
  trips: [],
  markers: [],
  polygons: [],
  circles: [],
  additionalDataSources: [],
  controlsPosition: MapControlsPosition.topleft,
  zoomActions: [MapZoomAction.scroll, MapZoomAction.doubleClick, MapZoomAction.controlButtons],
  scales: [],
  dragModeButton: false,
  fitMapBounds: true,
  useDefaultCenterPosition: false,
  defaultCenterPosition: '0,0',
  defaultZoomLevel: null,
  minZoomLevel: 16,
  mapPageSize: DEFAULT_MAP_PAGE_SIZE,
  mapActionButtons: [],
  tripTimeline: {
    showTimelineControl: false,
    timeStep: 1000,
    speedOptions: [1,5,10,15,25],
    showTimestamp: true,
    timestampFormat: simpleDateFormat('yyyy-MM-dd HH:mm:ss'),
    snapToRealLocation: false,
    locationSnapFilter: 'return true;'
  }
};

export const defaultMapActionButtonSettings: MapActionButtonSettings = {
  label: '',
  icon: 'add',
  color: '#0000008a',
  action: {
    type: WidgetActionType.doNothing
  }
}

export enum MapProvider {
  openstreet = 'openstreet',
  google = 'google',
  here = 'here',
  tencent = 'tencent',
  custom = 'custom'
}

export const mapProviders = Object.keys(MapProvider) as MapProvider[];

export const mapProviderTranslationMap = new Map<MapProvider, string>(
  [
    [MapProvider.openstreet, 'widgets.maps.layer.provider.openstreet.title'],
    [MapProvider.google, 'widgets.maps.layer.provider.google.title'],
    [MapProvider.here, 'widgets.maps.layer.provider.here.title'],
    [MapProvider.tencent, 'widgets.maps.layer.provider.tencent.title'],
    [MapProvider.custom, 'widgets.maps.layer.provider.custom.title']
  ]
);

export enum ReferenceLayerType {
  openstreetmap_hybrid = 'openstreetmap_hybrid',
  world_edition_hybrid = 'world_edition_hybrid',
  enhanced_contrast_hybrid = 'enhanced_contrast_hybrid'
}

export const referenceLayerTypes = Object.keys(ReferenceLayerType) as ReferenceLayerType[];

export const referenceLayerTypeTranslationMap = new Map<ReferenceLayerType, string>(
  [
    [ReferenceLayerType.openstreetmap_hybrid, 'widgets.maps.layer.reference.openstreetmap-hybrid'],
    [ReferenceLayerType.world_edition_hybrid, 'widgets.maps.layer.reference.world-edition-hybrid'],
    [ReferenceLayerType.enhanced_contrast_hybrid, 'widgets.maps.layer.reference.enhanced-contrast-hybrid']
  ]
);

export interface MapLayerSettings {
  label?: string;
  provider: MapProvider;
  referenceLayer?: ReferenceLayerType;
}

export const mapLayerValid = (layer: MapLayerSettings): boolean => {
  if (!layer.provider) {
    return false;
  }
  switch (layer.provider) {
    case MapProvider.openstreet:
      const openStreetLayer = layer as OpenStreetMapLayerSettings;
      return !!openStreetLayer.layerType;
    case MapProvider.google:
      const googleLayer = layer as GoogleMapLayerSettings;
      return !!googleLayer.layerType;
    case MapProvider.here:
      const hereLayer = layer as HereMapLayerSettings;
      return !!hereLayer.layerType;
    case MapProvider.tencent:
      const tencentLayer = layer as TencentMapLayerSettings;
      return !!tencentLayer.layerType;
    case MapProvider.custom:
      const customLayer = layer as CustomMapLayerSettings;
      return !!customLayer.tileUrl;
  }
};

export const mapLayerValidator = (control: AbstractControl): ValidationErrors | null => {
  const layer: MapLayerSettings = control.value;
  if (!mapLayerValid(layer)) {
    return {
      layer: true
    };
  }
  return null;
};

export const defaultLayerTitle = (layer: MapLayerSettings): string => {
  if (!layer.provider) {
    return null;
  }
  switch (layer.provider) {
    case MapProvider.openstreet:
      const openStreetLayer = layer as OpenStreetMapLayerSettings;
      return openStreetMapLayerTranslationMap.get(openStreetLayer.layerType);
    case MapProvider.google:
      const googleLayer = layer as GoogleMapLayerSettings;
      return googleMapLayerTranslationMap.get(googleLayer.layerType);
    case MapProvider.here:
      const hereLayer = layer as HereMapLayerSettings;
      return hereLayerTranslationMap.get(hereLayer.layerType);
    case MapProvider.tencent:
      const tencentLayer = layer as TencentMapLayerSettings;
      return tencentLayerTranslationMap.get(tencentLayer.layerType);
    case MapProvider.custom:
      return 'widgets.maps.layer.provider.custom.title';
  }
}

export enum OpenStreetLayerType {
  openStreetMapnik = 'OpenStreetMap.Mapnik',
  openStreetHot = 'OpenStreetMap.HOT',
  esriWorldStreetMap = 'Esri.WorldStreetMap',
  esriWorldTopoMap = 'Esri.WorldTopoMap',
  esriWorldImagery = 'Esri.WorldImagery',
  cartoDbPositron = 'CartoDB.Positron',
  cartoDbDarkMatter = 'CartoDB.DarkMatter'
}

export const openStreetLayerTypes = Object.values(OpenStreetLayerType) as OpenStreetLayerType[];

export const openStreetMapLayerTranslationMap = new Map<OpenStreetLayerType, string>(
  [
    [OpenStreetLayerType.openStreetMapnik, 'widgets.maps.layer.provider.openstreet.mapnik'],
    [OpenStreetLayerType.openStreetHot, 'widgets.maps.layer.provider.openstreet.hot'],
    [OpenStreetLayerType.esriWorldStreetMap, 'widgets.maps.layer.provider.openstreet.esri-street'],
    [OpenStreetLayerType.esriWorldTopoMap, 'widgets.maps.layer.provider.openstreet.esri-topo'],
    [OpenStreetLayerType.esriWorldImagery, 'widgets.maps.layer.provider.openstreet.esri-imagery'],
    [OpenStreetLayerType.cartoDbPositron, 'widgets.maps.layer.provider.openstreet.cartodb-positron'],
    [OpenStreetLayerType.cartoDbDarkMatter, 'widgets.maps.layer.provider.openstreet.cartodb-dark-matter']
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

export enum GoogleLayerType {
  roadmap = 'roadmap',
  satellite = 'satellite',
  hybrid = 'hybrid',
  terrain = 'terrain'
}

export const googleMapLayerTypes = Object.values(GoogleLayerType) as GoogleLayerType[];

export const googleMapLayerTranslationMap = new Map<GoogleLayerType, string>(
  [
    [GoogleLayerType.roadmap, 'widgets.maps.layer.provider.google.roadmap'],
    [GoogleLayerType.satellite, 'widgets.maps.layer.provider.google.satellite'],
    [GoogleLayerType.hybrid, 'widgets.maps.layer.provider.google.hybrid'],
    [GoogleLayerType.terrain, 'widgets.maps.layer.provider.google.terrain']
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

export enum HereLayerType {
  hereNormalDay = 'HEREv3.normalDay',
  hereNormalNight = 'HEREv3.normalNight',
  hereHybridDay = 'HEREv3.hybridDay',
  hereTerrainDay = 'HEREv3.terrainDay'
}

export const hereLayerTypes = Object.values(HereLayerType) as HereLayerType[];

export const hereLayerTranslationMap = new Map<HereLayerType, string>(
  [
    [HereLayerType.hereNormalDay, 'widgets.maps.layer.provider.here.normal-day'],
    [HereLayerType.hereNormalNight, 'widgets.maps.layer.provider.here.normal-night'],
    [HereLayerType.hereHybridDay, 'widgets.maps.layer.provider.here.hybrid-day'],
    [HereLayerType.hereTerrainDay, 'widgets.maps.layer.provider.here.terrain-day']
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

export const tencentLayerTypes = Object.values(TencentLayerType) as TencentLayerType[];

export const tencentLayerTranslationMap = new Map<TencentLayerType, string>(
  [
    [TencentLayerType.tencentNormal, 'widgets.maps.layer.provider.tencent.normal'],
    [TencentLayerType.tencentSatellite, 'widgets.maps.layer.provider.tencent.satellite'],
    [TencentLayerType.tencentTerrain, 'widgets.maps.layer.provider.tencent.terrain']
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
    case MapProvider.openstreet:
      return defaultOpenStreetMapLayerSettings;
    case MapProvider.google:
      return defaultGoogleMapLayerSettings;
    case MapProvider.here:
      return defaultHereMapLayerSettings;
    case MapProvider.tencent:
      return defaultTencentMapLayerSettings;
    case MapProvider.custom:
      return defaultCustomMapLayerSettings;
  }
};

export const defaultMapLayers: MapLayerSettings[] = [
  {
    label: '{i18n:widgets.maps.layer.roadmap}',
    provider: MapProvider.openstreet,
    layerType: OpenStreetLayerType.openStreetMapnik,
  } as OpenStreetMapLayerSettings,
  {
    label: '{i18n:widgets.maps.layer.satellite}',
    provider: MapProvider.openstreet,
    layerType: OpenStreetLayerType.esriWorldImagery,
  } as OpenStreetMapLayerSettings,
  {
    label: '{i18n:widgets.maps.layer.hybrid}',
    provider: MapProvider.openstreet,
    layerType: OpenStreetLayerType.esriWorldImagery,
    referenceLayer: ReferenceLayerType.openstreetmap_hybrid
  } as OpenStreetMapLayerSettings
];

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
  entityKey = 'entityKey'
}

export interface ImageMapSourceSettings {
  sourceType: ImageSourceType;
  url?: string;
  entityAliasId?: string;
  entityKey?: DataKey;
}

export const imageMapSourceSettingsValid = (imageSource: ImageMapSourceSettings): boolean => {
  if (!imageSource?.sourceType) {
    return false;
  } else if (imageSource.sourceType === ImageSourceType.image) {
    return isNotEmptyStr(imageSource.url);
  } else {
    return isNotEmptyStr(imageSource.entityAliasId) && !!imageSource.entityKey;
  }
}

export const imageMapSourceSettingsValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const imageSource: ImageMapSourceSettings = control.value;
  if (!imageMapSourceSettingsValid(imageSource)) {
    return {
      imageMapSource: true
    };
  }
  return null;
};

export const defaultImageMapSourceSettings: ImageMapSourceSettings = {
  sourceType: ImageSourceType.image,
  url: 'data:image/svg+xml;base64,PHN2ZyBpZD0ic3ZnMiIgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIGhlaWdodD0iMTAwIiB3aWR0aD0iMTAwIiB2ZXJzaW9uPSIxLjEiIHhtbG5zOmNjPSJodHRwOi8vY3JlYXRpdmVjb21tb25zLm9yZy9ucyMiIHhtbG5zOmRjPSJodHRwOi8vcHVybC5vcmcvZGMvZWxlbWVudHMvMS4xLyIgdmlld0JveD0iMCAwIDEwMCAxMDAiPgogPGcgaWQ9ImxheWVyMSIgdHJhbnNmb3JtPSJ0cmFuc2xhdGUoMCAtOTUyLjM2KSI+CiAgPHJlY3QgaWQ9InJlY3Q0Njg0IiBzdHJva2UtbGluZWpvaW49InJvdW5kIiBoZWlnaHQ9Ijk5LjAxIiB3aWR0aD0iOTkuMDEiIHN0cm9rZT0iIzAwMCIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIiB5PSI5NTIuODYiIHg9Ii40OTUwNSIgc3Ryb2tlLXdpZHRoPSIuOTkwMTAiIGZpbGw9IiNlZWUiLz4KICA8dGV4dCBpZD0idGV4dDQ2ODYiIHN0eWxlPSJ3b3JkLXNwYWNpbmc6MHB4O2xldHRlci1zcGFjaW5nOjBweDt0ZXh0LWFuY2hvcjptaWRkbGU7dGV4dC1hbGlnbjpjZW50ZXIiIGZvbnQtd2VpZ2h0PSJib2xkIiB4bWw6c3BhY2U9InByZXNlcnZlIiBmb250LXNpemU9IjEwcHgiIGxpbmUtaGVpZ2h0PSIxMjUlIiB5PSI5NzAuNzI4MDkiIHg9IjQ5LjM5NjQ3NyIgZm9udC1mYW1pbHk9IlJvYm90byIgZmlsbD0iIzY2NjY2NiI+PHRzcGFuIGlkPSJ0c3BhbjQ2OTAiIHg9IjUwLjY0NjQ3NyIgeT0iOTcwLjcyODA5Ij5JbWFnZSBiYWNrZ3JvdW5kIDwvdHNwYW4+PHRzcGFuIGlkPSJ0c3BhbjQ2OTIiIHg9IjQ5LjM5NjQ3NyIgeT0iOTgzLjIyODA5Ij5pcyBub3QgY29uZmlndXJlZDwvdHNwYW4+PC90ZXh0PgogIDxyZWN0IGlkPSJyZWN0NDY5NCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCIgaGVpZ2h0PSIxOS4zNiIgd2lkdGg9IjY5LjM2IiBzdHJva2U9IiMwMDAiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgeT0iOTkyLjY4IiB4PSIxNS4zMiIgc3Ryb2tlLXdpZHRoPSIuNjM5ODYiIGZpbGw9Im5vbmUiLz4KIDwvZz4KPC9zdmc+Cg==',
};

export const imageMapSourceSettingsToDatasource = (settings: ImageMapSourceSettings): Datasource => {
  return {
    type: DatasourceType.entity,
    name: '',
    entityAliasId: settings.entityAliasId,
    dataKeys: [settings.entityKey]
  };
};

export interface ImageMapSettings extends BaseMapSettings {
  imageSource?: ImageMapSourceSettings;
}

export const defaultImageMapSettings: ImageMapSettings = {
  mapType: MapType.image,
  imageSource: mergeDeep({} as ImageMapSourceSettings, defaultImageMapSourceSettings),
  ...mergeDeep({} as BaseMapSettings, defaultBaseMapSettings)
}

export type MapSetting = GeoMapSettings & ImageMapSettings;

export const defaultMapSettings: MapSetting = defaultGeoMapSettings;

export interface MarkerImageInfo {
  url: string;
  size: number;
  markerOffset?: [number, number];
  tooltipOffset?: [number, number];
}

export interface MarkerIconInfo {
  icon: L.Icon<L.BaseIconOptions>;
  size: [number, number];
}

export interface ShapeFillImageInfo {
  url: string;
  preserveAspectRatio?: boolean;
  opacity?: number;
  angle?: number;
  scale?: number;
}

export type MapStringFunction = (data: FormattedData<TbMapDatasource>,
                                 dsData: FormattedData<TbMapDatasource>[]) => string;

export type MapBooleanFunction = (data: FormattedData<TbMapDatasource>,
                                 dsData: FormattedData<TbMapDatasource>[]) => boolean;

export type MarkerImageFunction = (data: FormattedData<TbMapDatasource>, markerImages: string[],
                                   dsData: FormattedData<TbMapDatasource>[]) => MarkerImageInfo;

export type ClusterMarkerColorFunction = (data: FormattedData<TbMapDatasource>[], childCount: number) => string;

export type MarkerPositionFunction = (origXPos: number, origYPos: number, data: FormattedData<TbMapDatasource>,
                                      dsData: FormattedData<TbMapDatasource>[], aspect: number) => { x: number, y: number };

export type ShapeFillImageFunction = (data: FormattedData<TbMapDatasource>, images: string[],
                                      dsData: FormattedData<TbMapDatasource>[]) => ShapeFillImageInfo;

export type TbPolygonRawCoordinate = L.LatLngTuple | L.LatLngTuple[] | L.LatLngTuple[][];
export type TbPolygonRawCoordinates = TbPolygonRawCoordinate[];
export type TbPolyData = L.LatLngTuple[] | L.LatLngTuple[][] | L.LatLngTuple[][][];
export type TbPolygonCoordinate = L.LatLng | L.LatLng[] | L.LatLng[][];
export type TbPolygonCoordinates = TbPolygonCoordinate[];

export interface TbCircleData {
  latitude: number;
  longitude: number;
  radius: number;
}

export type DataKeyValuePair = {
  dataKey: DataKey;
  value: any;
}

export const isJSON = (data: string): boolean => {
  try {
    const parseData = JSON.parse(data);
    return !Array.isArray(parseData);
  } catch (e) {
    return false;
  }
}

export const isValidLatitude = (latitude: any): boolean =>
  isDefinedAndNotNull(latitude) &&
  !isString(latitude) &&
  !isNaN(latitude) && isFinite(latitude) && Math.abs(latitude) <= 90;

export const isValidLongitude = (longitude: any): boolean =>
  isDefinedAndNotNull(longitude) &&
  !isString(longitude) &&
  !isNaN(longitude) && isFinite(longitude) && Math.abs(longitude) <= 180;

export const isValidLatLng = (latitude: any, longitude: any): boolean =>
  isValidLatitude(latitude) && isValidLongitude(longitude);

export const isCutPolygon = (data: TbPolygonCoordinates | TbPolygonRawCoordinates): boolean => {
  return data.length > 1 && Array.isArray(data[0]) && (Array.isArray(data[0][0]) || (isNumber((data[0][0] as any).lat) && isNumber((data[0][0] as any).lng)) );
}

export const parseCenterPosition = (position: string | [number, number]): [number, number] => {
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

export const updateDataKeyToNewDsType = (dataKey: DataKey | null, newDsType: DatasourceType, timeSeries = false): boolean => {
  if (newDsType === DatasourceType.function) {
    if (dataKey && dataKey.type !== DataKeyType.function) {
      dataKey.type = DataKeyType.function;
      return true;
    }
  } else {
    if (dataKey?.type === DataKeyType.function) {
      dataKey.type = timeSeries ? DataKeyType.timeseries : DataKeyType.attribute;
      return true;
    }
  }
  return false;
}

export const mergeMapDatasources = (target: TbMapDatasource[], source: TbMapDatasource[]): TbMapDatasource[] => {
  const appendDatasources: TbMapDatasource[] = [];
  for (const sourceDs of source) {
    let merged = false;
    for (let i = 0; i < target.length; i++) {
      const targetDs = target[i];
      if (mapDatasourceIsSame(targetDs, sourceDs)) {
        target[i] = mergeMapDatasource(targetDs, sourceDs);
        merged = true;
        break;
      }
    }
    if (!merged) {
      appendDatasources.push(sourceDs);
    }
  }
  target.push(...appendDatasources);
  return target;
};

const mapDatasourceIsSame = (ds1: TbMapDatasource, ds2: TbMapDatasource): boolean => {
  if (ds1.type === ds2.type) {
    switch (ds1.type) {
      case DatasourceType.function:
        return ds1.name === ds2.name;
      case DatasourceType.device:
      case DatasourceType.entity:
        if (ds1.filterId === ds2.filterId) {
          if (ds1.type === DatasourceType.device) {
            return ds1.deviceId === ds2.deviceId;
          } else {
            return ds1.entityAliasId === ds2.entityAliasId;
          }
        }
    }
  }
  return false;
}

const mergeMapDatasource = (target: TbMapDatasource, source: TbMapDatasource): TbMapDatasource => {
  target.mapDataIds = _.union(target.mapDataIds, source.mapDataIds);
  const appendKeys: DataKey[] = [];
  for (const sourceKey of source.dataKeys) {
    const found =
      target.dataKeys.find(key => key.type === sourceKey.type && key.name === sourceKey.name && key.label === sourceKey.label);
    if (!found) {
      appendKeys.push(sourceKey);
    }
  }
  target.dataKeys.push(...appendKeys);
  return target;
}

const imageAspectMap: {[key: string]: ImageWithAspect} = {};

const imageLoader = (imageUrl: string): Observable<HTMLImageElement> => new Observable((observer: Observer<HTMLImageElement>) => {
  const image = document.createElement('img'); // support IE
  image.style.position = 'absolute';
  image.style.left = '-99999px';
  image.style.top = '-99999px';
  image.onload = () => {
    observer.next(image);
    document.body.removeChild(image);
    observer.complete();
  };
  image.onerror = err => {
    observer.error(err);
    document.body.removeChild(image);
    observer.complete();
  };
  document.body.appendChild(image);
  image.src = imageUrl;
});

const loadImageSize = (imageUrl: string): Observable<[number, number]> =>
  imageLoader(imageUrl).pipe(map(image => [image.width, image.height]));

export interface ImageWithAspect {
  url: string;
  width: number;
  height: number;
  aspect: number;
}

export const loadImageWithAspect = (imagePipe: ImagePipe, imageUrl: string): Observable<ImageWithAspect> => {
  if (imageUrl?.length) {
    const hash = hashCode(imageUrl);
    let imageWithAspect = imageAspectMap[hash];
    if (imageWithAspect) {
      return of(imageWithAspect);
    } else {
      return imagePipe.transform(imageUrl, {asString: true, ignoreLoadingImage: true}).pipe(
        switchMap((res) => {
          const url = res as string;
          return loadImageSize(url).pipe(
            map((size) => {
              imageWithAspect = {
                url,
                width: size[0],
                height: size[1],
                aspect: size[0]/size[1]
              };
              imageAspectMap[hash] = imageWithAspect;
              return imageWithAspect;
            })
          );
        })
      );
    }
  } else {
    return of(null);
  }
};

const linkActionRegex = /<link-act name=['"]([^['"]*)['"]>([^<]*)<\/link-act>/g;
const buttonActionRegex = /<button-act name=['"]([^['"]*)['"]>([^<]*)<\/button-act>/g;

const createTooltipLinkElement = (actionName: string, actionText: string): string => {
  return `<a href="javascript:void(0);" class="tb-custom-action" data-action-name="${actionName}">${actionText}</a>`;
}

const creatTooltipButtonElement = (actionName: string, actionText: string): string => {
  return `<button mat-button class="tb-custom-action" data-action-name="${actionName}">${actionText}</button>`;
}

export const processTooltipTemplate = (template: string): string => {
  let actionTags: string;
  let actionText: string;
  let actionName: string;
  let action: string;

  let match = linkActionRegex.exec(template);
  while (match !== null) {
    [actionTags, actionName, actionText] = match;
    action = createTooltipLinkElement(actionName, actionText);
    template = template.replace(actionTags, action);
    match = linkActionRegex.exec(template);
  }

  match = buttonActionRegex.exec(template);
  while (match !== null) {
    [actionTags, actionName, actionText] = match;
    action = creatTooltipButtonElement(actionName, actionText);
    template = template.replace(actionTags, action);
    match = buttonActionRegex.exec(template);
  }

  return template;
}

export const calculateNewPointCoordinate = (coordinate: number, imageSize: number): number => {
  let pointCoordinate = coordinate / imageSize;
  if (pointCoordinate < 0) {
    pointCoordinate = 0;
  } else if (pointCoordinate > 1) {
    pointCoordinate = 1;
  }
  return pointCoordinate;
}

export const latLngPointToBounds = (point: L.LatLng, southWest: L.LatLng, northEast: L.LatLng, offset = 0): L.LatLng => {
  const maxLngMap = northEast.lng - offset;
  const minLngMap = southWest.lng + offset;
  const maxLatMap = northEast.lat - offset;
  const minLatMap = southWest.lat + offset;
  if (point.lng > maxLngMap) {
    point.lng = maxLngMap;
  } else if (point.lng < minLngMap) {
    point.lng = minLngMap;
  }
  if (point.lat > maxLatMap) {
    point.lat = maxLatMap;
  } else if (point.lat < minLatMap) {
    point.lat = minLatMap;
  }
  return point;
}

export type TripRouteData = {[time: number]: FormattedData<TbMapDatasource>};

export const calculateInterpolationRatio = (firsMoment: number, secondMoment: number, intermediateMoment: number): number => {
  return (intermediateMoment - firsMoment) / (secondMoment - firsMoment);
}

export const interpolateLineSegment = (
  pointA: FormattedData,
  pointB: FormattedData,
  xKey: string,
  yKey: string,
  ratio: number
): { [key: string]: number } => {
  return {
    [xKey]: (pointA[xKey] + (pointB[xKey] - pointA[xKey]) * ratio),
    [yKey]: (pointA[yKey] + (pointB[yKey] - pointA[yKey]) * ratio)
  };
}

export const findRotationAngle = (startPoint: L.LatLng, endPoint: L.LatLng): number => {
  if (isUndefinedOrNull(startPoint) || isUndefinedOrNull(endPoint)) {
    return 0;
  }
  let angle = -Math.atan2(endPoint.lat - startPoint.lat, endPoint.lng - startPoint.lng);
  angle = angle * 180 / Math.PI;
  return parseInt(angle.toFixed(2), 10);
}

export const calculateLastPoints = (routeData: TripRouteData, time: number): FormattedData<TbMapDatasource> => {
  const timeArr = Object.keys(routeData);
  let index = timeArr.findIndex((dtime) => {
    return Number(dtime) >= time;
  });

  if (index !== -1) {
    if (Number(timeArr[index]) !== time && index !== 0) {
      index--;
    }
  } else {
    index = timeArr.length - 1;
  }

  return routeData[timeArr[index]];
}

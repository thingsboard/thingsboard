///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import { Datasource } from '@app/shared/models/widget.models';
import { EntityType } from '@shared/models/entity-type.models';
import tinycolor from 'tinycolor2';
import { BaseIconOptions, Icon } from 'leaflet';

export const DEFAULT_MAP_PAGE_SIZE = 16384;
export const DEFAULT_ZOOM_LEVEL = 8;

export type GenericFunction = (data: FormattedData, dsData: FormattedData[], dsIndex: number) => string;
export type MarkerImageFunction = (data: FormattedData, dsData: FormattedData[], dsIndex: number) => MarkerImageInfo;
export type PosFuncton = (origXPos, origYPos) => { x, y };
export type MarkerIconReadyFunction = (icon: MarkerIconInfo) => void;

export type MapSettings = {
    draggableMarker: boolean;
    editablePolygon: boolean;
    posFunction: PosFuncton;
    defaultZoomLevel?: number;
    disableScrollZooming?: boolean;
    disableZoomControl?: boolean;
    minZoomLevel?: number;
    useClusterMarkers: boolean;
    latKeyName?: string;
    lngKeyName?: string;
    xPosKeyName?: string;
    yPosKeyName?: string;
    imageEntityAlias: string;
    imageUrlAttribute: string;
    mapProvider: MapProviders;
    mapProviderHere: string;
    mapUrl?: string;
    mapImageUrl?: string;
    provider?: MapProviders;
    credentials?: any; // declare credentials format
    gmApiKey?: string;
    defaultCenterPosition?: [number, number];
    markerClusteringSetting?;
    useDefaultCenterPosition?: boolean;
    gmDefaultMapType?: string;
    useLabelFunction: boolean;
    zoomOnClick: boolean,
    maxZoom: number,
    showCoverageOnHover: boolean,
    animate: boolean,
    maxClusterRadius: number,
    spiderfyOnMaxZoom: boolean,
    chunkedLoading: boolean,
    removeOutsideVisibleBounds: boolean,
    useCustomProvider: boolean,
    customProviderTileUrl: string;
    mapPageSize: number;
};

export enum MapProviders {
    google = 'google-map',
    openstreet = 'openstreet-map',
    here = 'here',
    image = 'image-map',
    tencent = 'tencent-map'
}

export type MarkerImageInfo = {
    url: string;
    size: number;
    markerOffset?: [number, number];
    tooltipOffset?: [number, number];
};

export type MarkerIconInfo = {
    icon: Icon<BaseIconOptions>;
    size: [number, number];
};

export type MarkerSettings = {
    tooltipPattern?: any;
    tooltipAction: { [name: string]: actionsHandler };
    icon?: MarkerIconInfo;
    showLabel?: boolean;
    label: string;
    labelColor: string;
    labelText: string;
    useLabelFunction: boolean;
    draggableMarker: boolean;
    showTooltip?: boolean;
    useTooltipFunction: boolean;
    useColorFunction: boolean;
    color?: string;
    tinyColor?: tinycolor.Instance;
    autocloseTooltip: boolean;
    showTooltipAction: string;
    useClusterMarkers: boolean;
    currentImage?: MarkerImageInfo;
    useMarkerImageFunction?: boolean;
    markerImages?: string[];
    markerImageSize: number;
    fitMapBounds: boolean;
    markerImage: string;
    markerClick: { [name: string]: actionsHandler };
    colorFunction: GenericFunction;
    tooltipFunction: GenericFunction;
    labelFunction: GenericFunction;
    markerImageFunction?: MarkerImageFunction;
    markerOffsetX: number;
    markerOffsetY: number;
    tooltipOffsetX: number;
    tooltipOffsetY: number;
};

export interface FormattedData {
    $datasource: Datasource;
    entityName: string;
    entityId: string;
    entityType: EntityType;
    dsIndex: number;
    deviceType: string;
    [key: string]: any;
}

export interface ReplaceInfo {
  variable: string;
  valDec?: number;
  dataKeyName: string;
}

export type PolygonSettings = {
    showPolygon: boolean;
    polygonKeyName: string;
    polKeyName: string; // deprecated
    polygonStrokeOpacity: number;
    polygonOpacity: number;
    polygonStrokeWeight: number;
    polygonStrokeColor: string;
    polygonColor: string;
    showPolygonLabel?: boolean;
    polygonLabel: string;
    polygonLabelColor: string;
    polygonLabelText: string;
    usePolygonLabelFunction: boolean;
    showPolygonTooltip: boolean;
    autoClosePolygonTooltip: boolean;
    showPolygonTooltipAction: string;
    tooltipAction: { [name: string]: actionsHandler };
    polygonTooltipPattern: string;
    usePolygonTooltipFunction: boolean;
    polygonClick: { [name: string]: actionsHandler };
    usePolygonColorFunction: boolean;
    usePolygonStrokeColorFunction: boolean;
    polygonTooltipFunction: GenericFunction;
    polygonColorFunction?: GenericFunction;
    polygonStrokeColorFunction?: GenericFunction;
    polygonLabelFunction?: GenericFunction;
    editablePolygon: boolean;
};

export interface CircleSettings {
  showCircle: boolean;
  circleKeyName: string;
  editableCircle: boolean;
  showCircleLabel: boolean;
  useCircleLabelFunction: boolean;
  circleLabel: string;
  circleLabelFunction?: GenericFunction;
  circleFillColor: string;
  useCircleFillColorFunction: boolean;
  circleFillColorFunction?: GenericFunction;
  circleFillColorOpacity: number;
  circleStrokeColor: string;
  useCircleStrokeColorFunction: boolean;
  circleStrokeColorFunction: GenericFunction;
  circleStrokeOpacity: number;
  circleStrokeWeight: number;
  showCircleTooltip: boolean;
  showCircleTooltipAction: string;
  autoCloseCircleTooltip: boolean;
  useCircleTooltipFunction: boolean;
  circleTooltipPattern: string;
  circleTooltipFunction?: GenericFunction;
  circleClick?: { [name: string]: actionsHandler };
}

export type PolylineSettings = {
    usePolylineDecorator: any;
    autocloseTooltip: boolean;
    showTooltipAction: string;
    useColorFunction: any;
    tooltipAction: { [name: string]: actionsHandler };
    color: string;
    useStrokeOpacityFunction: any;
    strokeOpacity: number;
    useStrokeWeightFunction: any;
    strokeWeight: number;
    decoratorOffset: string | number;
    endDecoratorOffset: string | number;
    decoratorRepeat: string | number;
    decoratorSymbol: any;
    decoratorSymbolSize: any;
    useDecoratorCustomColor: any;
    decoratorCustomColor: any;


    colorFunction: GenericFunction;
    strokeOpacityFunction: GenericFunction;
    strokeWeightFunction: GenericFunction;
};

export interface EditorSettings {
    snappable: boolean;
    initDragMode: boolean;
    hideAllControlButton: boolean;
    hideDrawControlButton: boolean;
    hideEditControlButton: boolean;
    hideRemoveControlButton: boolean;
}

export interface HistorySelectSettings {
    buttonColor: string;
}

export interface MapImage {
    imageUrl: string;
    aspect: number;
    update?: boolean;
}

export interface TripAnimationSettings extends PolygonSettings {
    showPoints: boolean;
    pointColor: string;
    pointSize: number;
    pointTooltipOnRightPanel: boolean;
    usePointAsAnchor: boolean;
    normalizationStep: number;
    showPolygon: boolean;
    showLabel: boolean;
    showTooltip: boolean;
    latKeyName: string;
    lngKeyName: string;
    rotationAngle: number;
    label: string;
    tooltipPattern: string;
    tooltipColor: string;
    tooltipOpacity: number;
    tooltipFontColor: string;
    useTooltipFunction: boolean;
    useLabelFunction: boolean;
    pointAsAnchorFunction: GenericFunction;
    tooltipFunction: GenericFunction;
    labelFunction: GenericFunction;
    useColorPointFunction: boolean;
    colorPointFunction: GenericFunction;
}

export type actionsHandler = ($event: Event, datasource: Datasource) => void;

export type UnitedMapSettings = MapSettings & PolygonSettings & MarkerSettings & PolylineSettings
  & CircleSettings & TripAnimationSettings & EditorSettings;

export const defaultSettings: Partial<UnitedMapSettings> = {
    xPosKeyName: 'xPos',
    yPosKeyName: 'yPos',
    markerOffsetX: 0.5,
    markerOffsetY: 1,
    tooltipOffsetX: 0,
    tooltipOffsetY: -1,
    latKeyName: 'latitude',
    lngKeyName: 'longitude',
    polygonKeyName: 'perimeter',
    showLabel: false,
    label: '${entityName}',
    showTooltip: false,
    useDefaultCenterPosition: false,
    showTooltipAction: 'click',
    autocloseTooltip: false,
    showPolygon: false,
    labelColor: '#000000',
    color: '#FE7569',
    showPolygonLabel: false,
    polygonColor: '#3388ff',
    polygonStrokeColor: '#3388ff',
    polygonLabelColor: '#000000',
    polygonOpacity: 0.2,
    polygonStrokeOpacity: 1,
    polygonStrokeWeight: 3,
    showPolygonTooltipAction: 'click',
    autoClosePolygonTooltip: true,
    useLabelFunction: false,
    markerImages: [],
    strokeWeight: 2,
    strokeOpacity: 1.0,
    disableScrollZooming: false,
    minZoomLevel: 16,
    credentials: '',
    markerClusteringSetting: null,
    draggableMarker: false,
    editablePolygon: false,
    fitMapBounds: true,
    mapPageSize: DEFAULT_MAP_PAGE_SIZE,
    snappable: false,
    initDragMode: false,
    hideAllControlButton: false,
    hideDrawControlButton: false,
    hideEditControlButton: false,
    hideRemoveControlButton: false,
    showCircle: true,
    circleKeyName: 'perimeter',
    editableCircle: false,
    showCircleLabel: false,
    useCircleLabelFunction: false,
    circleLabel: '${entityName}',
    circleFillColor: '#3388ff',
    useCircleFillColorFunction: false,
    circleFillColorOpacity: 0.2,
    circleStrokeColor: '#3388ff',
    useCircleStrokeColorFunction: false,
    circleStrokeOpacity: 1,
    circleStrokeWeight: 3,
    showCircleTooltip: false,
    showCircleTooltipAction: 'click',
    autoCloseCircleTooltip: true,
    useCircleTooltipFunction: false
};

export interface CircleData {
  latitude: number;
  longitude: number;
  radius: number;
}

export const circleDataKeys: Array<keyof CircleData> = ['latitude', 'longitude', 'radius'];

export const hereProviders = [
    'HERE.normalDay',
    'HERE.normalNight',
    'HERE.hybridDay',
    'HERE.terrainDay'
];

///
/// Copyright © 2016-2020 The Thingsboard Authors
///
/// Licensed under the Apache License; Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing; software
/// distributed under the License is distributed on an "AS IS" BASIS;
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND; either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { LatLngTuple } from 'leaflet';

export type GenericFunction = (data: FormattedData, dsData: FormattedData[], dsIndex: number) => string;
export type MarkerImageFunction = (data: FormattedData, dsData: FormattedData[], dsIndex: number) => string;

export type MapSettings = {
    polygonKeyName: any;
    draggableMarker: boolean;
    initCallback?: () => any;
    defaultZoomLevel?: number;
    dontFitMapBounds?: boolean;
    disableScrollZooming?: boolean;
    minZoomLevel?: number;
    latKeyName?: string;
    lngKeyName?: string;
    xPosKeyName?: string;
    yPosKeyName?: string;
    mapProvider: MapProviders;
    mapUrl?: string;
    mapImageUrl?: string;
    provider?: MapProviders;
    credentials?: any; // declare credentials format
    defaultCenterPosition?: LatLngTuple;
    markerClusteringSetting?;
    useDefaultCenterPosition?: boolean;
    gmDefaultMapType?: string;
    useLabelFunction: string;
    icon?: any;
}

export enum MapProviders {
    google = 'google-map',
    openstreet = 'openstreet-map',
    here = 'here',
    image = 'image-map',
    tencent = 'tencent-map'
}

export type MarkerSettings = {
    tooltipPattern?: any;
    icon?: any;
    showLabel?: boolean;
    label: string;
    labelColor: string;
    labelText: string;
    useLabelFunction: boolean;
    draggableMarker: boolean;
    showTooltip?: boolean;
    color?: string;
    autocloseTooltip: boolean;
    displayTooltipAction: string;
    currentImage?: string;
    useMarkerImageFunction?: boolean;
    markerImages?: string[];
    useMarkerImage: boolean;
    markerImageSize: number;
    markerImage: {
        length: number
    }

    colorFunction: GenericFunction;
    tooltipFunction: GenericFunction;
    labelFunction: GenericFunction;
    markerImageFunction?: MarkerImageFunction;
}

export interface FormattedData {
    aliasName: string;
    entityName: string;
    $datasource: string;
    dsIndex: number;
    deviceType: string
}

export type PolygonSettings = {
    showPolygon: boolean;
    showTooltip: any;
    polygonStrokeOpacity: number;
    polygonOpacity: number;
    polygonStrokeWeight: number;
    polygonStrokeColor: string;
    polygonColor: string;
    autocloseTooltip: boolean;
    displayTooltipAction: string;

    polygonColorFunction?: GenericFunction;
}

export type PolylineSettings = {
    usePolylineDecorator: any;
    autocloseTooltip: boolean;
    displayTooltipAction: string;
    useColorFunction: any;
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
}

export interface HistorySelectSettings {
    buttonColor: string;
}

export type UnitedMapSettings = MapSettings & PolygonSettings & MarkerSettings & PolygonSettings;
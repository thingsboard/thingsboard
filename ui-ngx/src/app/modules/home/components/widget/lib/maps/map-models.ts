export interface MapOptions {
    initCallback?: Function,
    defaultZoomLevel?: number,
    dontFitMapBounds?: boolean,
    disableScrollZooming?: boolean,
    minZoomLevel?: number,
    mapProvider: MapProviders,
    mapUrl?: string;
    credentials?: any, // declare credentials format
    defaultCenterPosition?: L.LatLngExpression,
    markerClusteringSetting?
}

export enum MapProviders {
    google = 'google-map',
    openstreet = 'openstreet-map',
    here = 'here',
    image = 'image-map',
    tencent = 'tencent-map'
}

export interface MarkerSettings{
    showLabel?: boolean,
    draggable?: boolean,
    displayTooltip?: boolean,
    color: string
}
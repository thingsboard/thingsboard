// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import L from 'leaflet';
import LeafletMap from '../leaflet-map';
import { DEFAULT_ZOOM_LEVEL, WidgetUnitedMapSettings } from '../map-models';
import { WidgetContext } from '@home/models/widget-component.models';

export class OpenStreetMap extends LeafletMap {
    constructor(ctx: WidgetContext, $container: HTMLElement, options: WidgetUnitedMapSettings) {
        super(ctx, $container, options);
        const map =  L.map($container, {
          doubleClickZoom: !this.options.disableDoubleClickZooming,
          zoomControl: !this.options.disableZoomControl
        }).setView(options?.parsedDefaultCenterPosition, options?.defaultZoomLevel || DEFAULT_ZOOM_LEVEL);
        let tileLayer: L.TileLayer;
        if (options.useCustomProvider) {
          tileLayer = L.tileLayer(options.customProviderTileUrl);
        } else {
          tileLayer = L.tileLayer.provider(options.mapProvider || 'OpenStreetMap.Mapnik');
        }
        tileLayer.addTo(map);
        super.setMap(map);
    }
}

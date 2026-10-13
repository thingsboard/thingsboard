// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import L from 'leaflet';
import LeafletMap from '../leaflet-map';
import { DEFAULT_ZOOM_LEVEL, WidgetUnitedMapSettings } from '../map-models';
import { WidgetContext } from '@home/models/widget-component.models';

export class OpenStreetMap extends LeafletMap {
    constructor(ctx: WidgetContext, $container: HTMLElement, options: WidgetUnitedMapSettings) {
        super(ctx, $container, options);
        let mapUuid: string;
        if (this.ctx.reportService.reportView) {
          mapUuid = this.ctx.reportService.onWaitForMap();
        }
        const map =  L.map($container, {
          doubleClickZoom: !this.options.disableDoubleClickZooming,
          zoomControl: !this.options.disableZoomControl,
          fadeAnimation: !ctx.reportService.reportView
        }).setView(options?.parsedDefaultCenterPosition, options?.defaultZoomLevel || DEFAULT_ZOOM_LEVEL);
        let tileLayer: L.TileLayer;
        if (options.useCustomProvider) {
          tileLayer = L.tileLayer(options.customProviderTileUrl);
        } else {
          tileLayer = L.tileLayer.provider(options.mapProvider || 'OpenStreetMap.Mapnik');
        }
        tileLayer.addTo(map);
        if (this.ctx.reportService.reportView) {
            tileLayer.once('load', () => {
              this.ctx.reportService.onMapLoaded(mapUuid);
            });
        }
        super.setMap(map);
    }
}

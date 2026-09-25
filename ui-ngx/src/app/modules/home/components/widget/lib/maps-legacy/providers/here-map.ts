// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import L from 'leaflet';
import LeafletMap from '../leaflet-map';
import { DEFAULT_ZOOM_LEVEL, defaultHereMapProviderSettings, WidgetUnitedMapSettings } from '../map-models';
import { WidgetContext } from '@home/models/widget-component.models';
import { hereV3Provider } from '@shared/models/widget/maps/map.models';

export class HEREMap extends LeafletMap {
    constructor(ctx: WidgetContext, $container: HTMLElement, options: WidgetUnitedMapSettings) {
        super(ctx, $container, options);
        const map = L.map($container, {
          doubleClickZoom: !this.options.disableDoubleClickZooming,
          zoomControl: !this.options.disableZoomControl
        }).setView(options?.parsedDefaultCenterPosition, options?.defaultZoomLevel || DEFAULT_ZOOM_LEVEL);
        const apiKey = options.credentials?.apiKey || defaultHereMapProviderSettings.credentials.apiKey;
        const tileLayer = L.tileLayer.provider(hereV3Provider(options.mapProviderHere), {apiKey});
        tileLayer.addTo(map);
        super.setMap(map);
    }
}

// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import L from 'leaflet';
import LeafletMap from '../leaflet-map';
import { DEFAULT_ZOOM_LEVEL, WidgetUnitedMapSettings } from '../map-models';
import { WidgetContext } from '@home/models/widget-component.models';
import { isDefinedAndNotNull } from '@core/utils';

export class HEREMap extends LeafletMap {
    constructor(ctx: WidgetContext, $container: HTMLElement, options: WidgetUnitedMapSettings) {
        super(ctx, $container, options);
        const map = L.map($container, {
          doubleClickZoom: !this.options.disableDoubleClickZooming,
          zoomControl: !this.options.disableZoomControl
        }).setView(options?.parsedDefaultCenterPosition, options?.defaultZoomLevel || DEFAULT_ZOOM_LEVEL);
        let provider = options.mapProviderHere || 'HERE.normalDay';
        if (options.credentials.useV3 && isDefinedAndNotNull(options.credentials.apiKey)) {
          provider = options.mapProviderHere?.replace('HERE', 'HEREv3') || 'HEREv3.normalDay';
        }
        const tileLayer = (L.tileLayer as any).provider(provider, options.credentials);
        tileLayer.addTo(map);
        super.setMap(map);
    }
}
